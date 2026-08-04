package com.cahcap.common.block;

import com.cahcap.common.blockentity.StarryCakeBlockEntity;
import com.cahcap.common.registry.ModRegistries;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Starry Cake - an expensive cake that survives explosions, keeps its eaten progress when picked
 * up, and slowly regrows itself under open sky.
 * <p>
 * Unlike the vanilla cake this is a block entity, because regrowing needs a timer. How much has
 * been eaten stays in the block state so it can travel on the dropped item.
 */
public class StarryCakeBlock extends BaseEntityBlock {

    public static final MapCodec<StarryCakeBlock> CODEC = simpleCodec(StarryCakeBlock::new);

    public static final int MAX_BITES = 6;
    public static final IntegerProperty BITES = BlockStateProperties.BITES;

    /** NBT key on the dropped item that carries the eaten progress. */
    public static final String BITES_TAG = "Bites";

    /** Nutrition per slice: 8 points, i.e. four hunger icons. A vanilla cake slice gives 2. */
    private static final int NUTRITION = 8;
    private static final float SATURATION = 0.4F;

    /** Each bite eats two pixels off the west side, same as the vanilla cake. */
    private static final VoxelShape[] SHAPE_BY_BITE = new VoxelShape[MAX_BITES + 1];

    static {
        for (int bite = 0; bite <= MAX_BITES; bite++) {
            SHAPE_BY_BITE[bite] = Block.box(1.0 + bite * 2.0, 0.0, 1.0, 15.0, 8.0, 15.0);
        }
    }

    public StarryCakeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BITES, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BITES);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return SHAPE_BY_BITE[state.getValue(BITES)];
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StarryCakeBlockEntity(pos, state);
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
                (BlockEntityType<StarryCakeBlockEntity>) ModRegistries.STARRY_CAKE_BE.get(),
                StarryCakeBlockEntity::serverTick);
    }

    /**
     * Whether the sky is reachable straight up, using the beacon beam rule rather than
     * {@code canSeeSky}: anything that does not fully block light — glass, water, slabs — still
     * counts as open.
     */
    public static boolean isOpenToSky(LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        while (above.getY() < level.getMaxBuildHeight()) {
            BlockState state = level.getBlockState(above);
            if (state.getLightBlock(level, above) >= 15) {
                return false;
            }
            above = above.above();
        }
        return true;
    }

    // ==================== eating ====================

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player,
                                              net.minecraft.world.InteractionHand hand,
                                              BlockHitResult hitResult) {
        return eat(level, pos, state, player).consumesAction()
                ? ItemInteractionResult.SUCCESS
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        return eat(level, pos, state, player);
    }

    private static InteractionResult eat(Level level, BlockPos pos, BlockState state, Player player) {
        if (!player.canEat(false)) {
            return InteractionResult.PASS;
        }
        player.awardStat(Stats.EAT_CAKE_SLICE);
        player.getFoodData().eat(NUTRITION, SATURATION);
        level.gameEvent(player, GameEvent.EAT, pos);

        int bites = state.getValue(BITES);
        if (bites < MAX_BITES) {
            level.setBlock(pos, state.setValue(BITES, bites + 1), Block.UPDATE_ALL);
        } else {
            level.removeBlock(pos, false);
            level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
        }
        return InteractionResult.SUCCESS;
    }

    // ==================== keeping progress across pickup ====================

    /** Puts the eaten progress on the dropped item so putting it back down resumes where it was. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack drop = new ItemStack(this);
        int bites = state.getValue(BITES);
        if (bites > 0) {
            CompoundTag tag = new CompoundTag();
            tag.putInt(BITES_TAG, bites);
            drop.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return Collections.singletonList(drop);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        int bites = readBites(stack);
        if (bites > 0) {
            level.setBlock(pos, state.setValue(BITES, Math.min(bites, MAX_BITES)), Block.UPDATE_ALL);
        }
    }

    /** Eaten progress stored on an item, or 0 for an untouched cake. */
    public static int readBites(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0 : data.copyTag().getInt(BITES_TAG);
    }

    // ==================== comparator ====================

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return (MAX_BITES + 1 - state.getValue(BITES)) * 2;
    }
}
