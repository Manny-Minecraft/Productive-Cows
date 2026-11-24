package com.productivecows.block.entity;

import com.productivecows.ProductiveCows;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** Pushes as much RF as each neighbour accepts every tick, and gives any amount to whoever pulls. */
public class CreativeEnergyBlockEntity extends BlockEntity {

    private static final IEnergyStorage INFINITE = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return Math.max(0, toExtract);
        }

        @Override
        public int getEnergyStored() {
            return Integer.MAX_VALUE;
        }

        @Override
        public int getMaxEnergyStored() {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    };

    private final NeighborCapabilities<IEnergyStorage> neighbors = new NeighborCapabilities<>(Capabilities.EnergyStorage.BLOCK);

    public CreativeEnergyBlockEntity(BlockPos pos, BlockState state) {
        super(ProductiveCows.CREATIVE_ENERGY_BE.get(), pos, state);
    }

    public IEnergyStorage getEnergyStorageCapability() {
        return INFINITE;
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        for (Direction side : Direction.values()) {
            IEnergyStorage target = neighbors.get(serverLevel, worldPosition, side);
            if (target != null && target.canReceive()) {
                target.receiveEnergy(Integer.MAX_VALUE, false);
            }
        }
    }
}
