package com.cahcap.common.block;

import com.cahcap.common.blockentity.ArcaneAlloyAnvilBlockEntity;
import com.cahcap.common.inventory.ArcaneAlloyAnvilMenu;
import com.cahcap.common.registry.ModRegistries;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Arcane Alloy Anvil - an anvil that never gets prohibitively expensive and heals its own damage.
 * <p>
 * Extends {@link AnvilBlock} so the falling physics, landing damage and support requirements come
 * along unchanged. Three separate blocks model the damage levels exactly like vanilla; the block
 * entity only carries the repair timer.
 */
public class ArcaneAlloyAnvilBlock extends AnvilBlock implements EntityBlock {

    public static final MapCodec<ArcaneAlloyAnvilBlock> CODEC = simpleCodec(ArcaneAlloyAnvilBlock::new);


    private static final Component TITLE = Component.translatable("block.ous.arcane_alloy_anvil");

    public ArcaneAlloyAnvilBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapCodec<AnvilBlock> codec() {
        // AnvilBlock declares MapCodec<AnvilBlock>, which a subtype cannot narrow.
        return (MapCodec<AnvilBlock>) (MapCodec<?>) CODEC;
    }

    /**
     * Next damage level, or {@code null} when the anvil is already at its most damaged.
     * <p>
     * Mirrors {@link AnvilBlock#damage} but over our own blocks — the vanilla one only knows the
     * vanilla chain and returns {@code null} for these, which would make callers destroy the block.
     */
    @Nullable
    public static BlockState damage(BlockState state) {
        Direction facing = state.getValue(FACING);
        if (state.is(ModRegistries.ARCANE_ALLOY_ANVIL.get())) {
            return ModRegistries.CHIPPED_ARCANE_ALLOY_ANVIL.get().defaultBlockState()
                    .setValue(FACING, facing);
        }
        if (state.is(ModRegistries.CHIPPED_ARCANE_ALLOY_ANVIL.get())) {
            return ModRegistries.DAMAGED_ARCANE_ALLOY_ANVIL.get().defaultBlockState()
                    .setValue(FACING, facing);
        }
        return null;
    }

    /** Previous damage level, or {@code null} when the anvil is already intact. */
    @Nullable
    public static BlockState repair(BlockState state) {
        Direction facing = state.getValue(FACING);
        if (state.is(ModRegistries.DAMAGED_ARCANE_ALLOY_ANVIL.get())) {
            return ModRegistries.CHIPPED_ARCANE_ALLOY_ANVIL.get().defaultBlockState()
                    .setValue(FACING, facing);
        }
        if (state.is(ModRegistries.CHIPPED_ARCANE_ALLOY_ANVIL.get())) {
            return ModRegistries.ARCANE_ALLOY_ANVIL.get().defaultBlockState()
                    .setValue(FACING, facing);
        }
        return null;
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider(
                (id, inventory, player) -> createMenu(id, inventory, level, pos), TITLE);
    }

    private static AbstractContainerMenu createMenu(int id, Inventory inventory, Level level,
                                                    BlockPos pos) {
        return new ArcaneAlloyAnvilMenu(id, inventory, ContainerLevelAccess.create(level, pos));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArcaneAlloyAnvilBlockEntity(pos, state);
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
                (BlockEntityType<ArcaneAlloyAnvilBlockEntity>) ModRegistries.ARCANE_ALLOY_ANVIL_BE.get(),
                ArcaneAlloyAnvilBlockEntity::serverTick);
    }

    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> given, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == given ? (BlockEntityTicker<A>) ticker : null;
    }

    /**
     * Vanilla's landing logic calls {@link AnvilBlock#damage}, which does not know these blocks and
     * returns {@code null}, destroying the anvil on its first fall. Damage it over our own chain.
     */
    @Override
    public void onLand(Level level, BlockPos pos, BlockState state, BlockState replaceableState,
                       net.minecraft.world.entity.item.FallingBlockEntity fallingBlock) {
        if (!fallingBlock.isSilent()) {
            level.levelEvent(1031, pos, 0);
        }
        BlockState damaged = damage(state);
        if (damaged != null) {
            level.setBlock(pos, damaged, UPDATE_ALL);
        }
    }
}
