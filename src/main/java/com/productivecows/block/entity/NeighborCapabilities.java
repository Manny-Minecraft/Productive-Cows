package com.productivecows.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Cached lookups of one capability on the six neighbours of a block entity, so machines don't query
 * the level on every tick. Caches are created lazily on the server and follow neighbour changes.
 */
final class NeighborCapabilities<T> {
    private final BlockCapability<T, Direction> capability;
    private final Map<Direction, BlockCapabilityCache<T, Direction>> caches = new EnumMap<>(Direction.class);

    NeighborCapabilities(BlockCapability<T, Direction> capability) {
        this.capability = capability;
    }

    /** The capability of the block next to {@code pos} on {@code side}, seen from the face touching us. */
    @Nullable
    T get(ServerLevel level, BlockPos pos, Direction side) {
        return caches.computeIfAbsent(side,
                d -> BlockCapabilityCache.create(capability, level, pos.relative(d), d.getOpposite())).getCapability();
    }
}
