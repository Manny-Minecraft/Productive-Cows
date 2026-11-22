package com.productivecows.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Batches block entity update packets. Changes that the client renders (tank contents, the caged cow)
 * call {@link #mark()}; the server tick calls {@link #flush} and sends at most one packet every
 * {@link #INTERVAL} ticks, only to clients and without neighbour or comparator updates.
 * Energy, progress and GUI values reach the client through each menu's ContainerData instead.
 */
final class ClientSyncThrottle {
    static final int INTERVAL = 10;

    private boolean pending;

    void mark() {
        pending = true;
    }

    void flush(Level level, BlockPos pos, BlockState state) {
        // Spread machines over the interval so a large farm doesn't send all its packets on the same tick
        if (pending && Math.floorMod(level.getGameTime() + pos.asLong(), INTERVAL) == 0) {
            pending = false;
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }
}
