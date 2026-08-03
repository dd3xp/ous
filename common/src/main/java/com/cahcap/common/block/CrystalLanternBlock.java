package com.cahcap.common.block;

import com.cahcap.common.blockentity.CrystalLanternBlockEntity;
import com.cahcap.common.item.CrystalLanternItem;
import com.cahcap.common.registry.ModRegistries;
import com.cahcap.common.util.CustomVoxelShapes;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Crystal Lantern - holds a stack of fuel and projects its bound potion effect while lit.
 * <p>
 * The lamp body is white in the texture and is not part of this block's model at all — it is
 * drawn by CrystalLanternRenderer so its colour can follow the block entity per frame
 * (amethyst while idle, the bound potion's colour while working).
 */
public class CrystalLanternBlock extends BaseEntityBlock {

    public static final MapCodec<CrystalLanternBlock> CODEC = simpleCodec(CrystalLanternBlock::new);

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private static final CustomVoxelShapes SHAPES =
            CustomVoxelShapes.loadFromModel("/assets/ous/models/block/crystal_lantern.json");

    public CrystalLanternBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return SHAPES.getByIndex(net.minecraft.core.Direction.NORTH, 0, false);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrystalLanternBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type,
                (BlockEntityType<CrystalLanternBlockEntity>) ModRegistries.CRYSTAL_LANTERN_BE.get(),
                CrystalLanternBlockEntity::serverTick);
    }

    /** Carries the craft-time binding from the placed item onto the block entity. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof CrystalLanternBlockEntity lantern) {
            lantern.setBinding(CrystalLanternItem.getBoundEffects(stack),
                    CrystalLanternItem.getPotionColor(stack));
        }
    }

    /** Right-click with any datapack-defined fuel to fill the slot. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hitResult) {
        if (hand == InteractionHand.OFF_HAND || !CrystalLanternBlockEntity.isFuel(level, stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof CrystalLanternBlockEntity lantern)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        int accepted = lantern.insertFuel(stack);
        if (accepted <= 0) {
            return ItemInteractionResult.CONSUME;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(accepted);
        }
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
        updateLit(level, pos, state, lantern);
        return ItemInteractionResult.SUCCESS;
    }

    /** Sneak + empty hand takes the fuel back out. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof CrystalLanternBlockEntity lantern)) {
            return InteractionResult.PASS;
        }

        ItemStack out = lantern.extractFuel();
        if (out.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!player.getInventory().add(out)) {
            player.drop(out, false);
        }
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
        updateLit(level, pos, state, lantern);
        return InteractionResult.SUCCESS;
    }

    /** Flips the lit state right away so the light and the lamp tint react to the interaction. */
    private static void updateLit(Level level, BlockPos pos, BlockState state,
                                  CrystalLanternBlockEntity lantern) {
        boolean lit = lantern.isWorking();
        if (state.getValue(LIT) != lit) {
            level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_ALL);
        }
    }

    /** Spills the stored fuel when the lantern is broken; the loot table only covers the block itself. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                            boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof CrystalLanternBlockEntity lantern) {
            ItemStack contents = lantern.extractFuel();
            if (!contents.isEmpty()) {
                Block.popResource(level, pos, contents);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Keeps the binding on the dropped item so it survives being broken and replaced. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        if (builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY)
                instanceof CrystalLanternBlockEntity lantern) {
            for (ItemStack drop : drops) {
                if (drop.getItem() instanceof CrystalLanternItem) {
                    CrystalLanternItem.setBinding(drop, lantern.getBoundEffects(),
                            lantern.getPotionColor());
                }
            }
        }
        return drops;
    }
}
