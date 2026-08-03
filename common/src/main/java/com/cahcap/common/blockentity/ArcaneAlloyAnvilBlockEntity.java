package com.cahcap.common.blockentity;

import com.cahcap.common.block.ArcaneAlloyAnvilBlock;
import com.cahcap.common.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Drives the Arcane Alloy Anvil's self repair: one damage level every 15 minutes, so a fully
 * damaged anvil takes half an hour to come back.
 * <p>
 * The damage level itself lives in the block state (three separate blocks, as in vanilla); this
 * only holds the timer, which a block state cannot.
 */
public class ArcaneAlloyAnvilBlockEntity extends BlockEntity {

    /** Ticks to recover one damage level. 15 minutes, i.e. 30 minutes from damaged to intact. */
    public static final int REPAIR_INTERVAL = 18000;

    private int repairTimer;

    public ArcaneAlloyAnvilBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.ARCANE_ALLOY_ANVIL_BE.get(), pos, state);
    }

    /** Ticks accumulated towards the next repair; exposed for the look-at tooltip. */
    public int getRepairTimer() {
        return repairTimer;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  ArcaneAlloyAnvilBlockEntity be) {
        BlockState repaired = ArcaneAlloyAnvilBlock.repair(state);
        if (repaired == null) {
            // Already intact; keep the timer at zero so the next damage starts a fresh interval.
            if (be.repairTimer != 0) {
                be.repairTimer = 0;
                be.setChanged();
            }
            return;
        }

        if (++be.repairTimer >= REPAIR_INTERVAL) {
            be.repairTimer = 0;
            level.setBlock(pos, repaired, Block.UPDATE_ALL);
            level.levelEvent(1030, pos, 0);
        }
        be.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("RepairTimer", repairTimer);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        repairTimer = tag.getInt("RepairTimer");
    }
}
