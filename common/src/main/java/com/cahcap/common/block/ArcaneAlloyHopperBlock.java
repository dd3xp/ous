package com.cahcap.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Arcane Alloy Hopper - behaves like a vanilla hopper, but when there is nothing to push into it
 * drops its contents into the world instead of backing up.
 * <p>
 * Deliberately reuses the vanilla {@link HopperBlockEntity}: its only constructor hardcodes
 * {@code BlockEntityType.HOPPER}, so a subclass could never carry its own type. All the custom
 * behaviour lives in the ticker instead, and the block is added to the vanilla hopper block entity
 * type at registration time.
 */
public class ArcaneAlloyHopperBlock extends HopperBlock {

    public static final MapCodec<ArcaneAlloyHopperBlock> CODEC = simpleCodec(ArcaneAlloyHopperBlock::new);

    public ArcaneAlloyHopperBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapCodec<HopperBlock> codec() {
        // HopperBlock declares MapCodec<HopperBlock>, which a subtype cannot narrow.
        return (MapCodec<HopperBlock>) (MapCodec<?>) CODEC;
    }

    @Nullable
    /**
     * Opens the menu with our own title. Vanilla's {@code useWithoutItem} calls
     * {@code player.openMenu(blockEntity)} directly rather than going through
     * {@code getMenuProvider}, and the block entity is vanilla's, so its title would read "Hopper".
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof HopperBlockEntity hopper) {
            player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (id, inventory, p) ->
                            new net.minecraft.world.inventory.HopperMenu(id, inventory, hopper),
                    net.minecraft.network.chat.Component.translatable(
                            "block.ous.arcane_alloy_hopper")));
            player.awardStat(net.minecraft.stats.Stats.INSPECT_HOPPER);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (tickLevel, pos, tickState, be) -> {
            if (be instanceof HopperBlockEntity hopper) {
                serverTick(tickLevel, pos, tickState, hopper);
            }
        };
    }

    private static void serverTick(Level level, BlockPos pos, BlockState state,
                                   HopperBlockEntity hopper) {
        // Vanilla first: pulling in, and pushing into a container if one is attached.
        HopperBlockEntity.pushItemsTick(level, pos, state, hopper);

        // Vanilla sets an 8 tick cooldown only when it actually moves something, so throttle the
        // drop ourselves to keep the same throughput as a normal hopper.
        if (level.getGameTime() % HopperBlockEntity.MOVE_ITEM_SPEED != 0) {
            return;
        }
        if (!state.getValue(ENABLED) || hopper.isEmpty()) {
            return;
        }

        Direction facing = state.getValue(FACING);
        BlockPos target = pos.relative(facing);
        if (!canEject(level, target)) {
            return;
        }

        for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
            ItemStack stack = hopper.getItem(slot);
            if (!stack.isEmpty()) {
                eject(level, target, hopper.removeItem(slot, 1));
                return;
            }
        }
    }

    /**
     * Only drop into somewhere an item could actually rest: the target has to be air or
     * something without a collision box, such as grass or a torch.
     * A container there is left to vanilla, which pushes into it rather than dropping.
     */
    private static boolean canEject(Level level, BlockPos target) {
        // getContainerAt already covers both block containers and entity ones such as chest minecarts.
        if (HopperBlockEntity.getContainerAt(level, target) != null) {
            return false;
        }
        BlockState state = level.getBlockState(target);
        return state.isAir() || state.getCollisionShape(level, target).isEmpty();
    }

    /** Spawns the item at the centre of the target block with no velocity at all. */
    private static void eject(Level level, BlockPos target, ItemStack stack) {
        ItemEntity item = new ItemEntity(level,
                target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, stack);
        item.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(item);
    }
}
