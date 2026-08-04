package com.cahcap.common.blockentity;

import com.cahcap.common.block.StarryCakeBlock;
import com.cahcap.common.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Regrows the Starry Cake a slice at a time while it can see the sky.
 * <p>
 * Only the timer lives here; how much of the cake is left is a block state, so it survives being
 * picked up and put back down.
 */
public class StarryCakeBlockEntity extends BlockEntity {

    /** Ticks to regrow one slice: 30 seconds. */
    public static final int REGROW_INTERVAL = 600;
    /** How often the ambient particles puff while a slice is on its way back. */
    private static final int PARTICLE_INTERVAL = 10;

    private int regrowTimer;

    public StarryCakeBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.STARRY_CAKE_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  StarryCakeBlockEntity cake) {
        int bites = state.getValue(StarryCakeBlock.BITES);
        if (bites <= 0 || !StarryCakeBlock.isOpenToSky(level, pos)) {
            if (cake.regrowTimer != 0) {
                cake.regrowTimer = 0;
                cake.setChanged();
            }
            return;
        }

        // Particles run for the whole wait, not just the instant a slice lands, so the cake
        // visibly signals that it is busy regrowing.
        if (level instanceof ServerLevel serverLevel) {
            if (cake.regrowTimer % PARTICLE_INTERVAL == 0) {
                serverLevel.sendParticles(ParticleTypes.PORTAL,
                        pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                        3, 0.3, 0.2, 0.3, 0.3);
            }
        }

        if (++cake.regrowTimer >= REGROW_INTERVAL) {
            cake.regrowTimer = 0;
            level.setBlock(pos, state.setValue(StarryCakeBlock.BITES, bites - 1), Block.UPDATE_ALL);
        }
        cake.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("RegrowTimer", regrowTimer);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        regrowTimer = tag.getInt("RegrowTimer");
    }
}
