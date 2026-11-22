package com.productivecows.block.entity;

import com.productivecows.ProductiveCows;
import com.productivecows.menu.MilkGeneratorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class MilkGeneratorBlockEntity extends BlockEntity implements MenuProvider {

    // Custom energy storage subclass to allow manual energy manipulation
    public static class CustomEnergyStorage extends EnergyStorage {
        public CustomEnergyStorage(int capacity, int maxReceive, int maxExtract) {
            super(capacity, maxReceive, maxExtract);
        }

        public void setEnergy(int energy) {
            this.energy = Math.max(0, Math.min(capacity, energy));
        }

        public void addEnergy(int amount) {
            this.energy = Math.max(0, Math.min(capacity, this.energy + amount));
        }

        public void consumeEnergy(int amount) {
            this.energy = Math.max(0, this.energy - amount);
        }
    }

    // No renderer reads this block entity and the GUI uses ContainerData, so changes only mark it for saving
    private final NeighborCapabilities<IEnergyStorage> neighborEnergy = new NeighborCapabilities<>(Capabilities.EnergyStorage.BLOCK);

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) {
                return isFuel(GeneratorFuel.bucketFluid(stack));
            }
            return false; // Slot 1 is output only
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot == 1) return stack;
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final FluidTank fluidTank = new FluidTank(4000) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return isFuel(stack.getFluid());
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final CustomEnergyStorage energyStorage = new CustomEnergyStorage(50000, 0, 200);
    private double fractionalMilk = 0.0;
    /** RF produced in the last tick, shown in the GUI; not saved. */
    private int currentRate = 0;

    private boolean isFuel(Fluid fluid) {
        return GeneratorFuel.tier(fluid, com.productivecows.ProductiveCows.getDefinitions(level)) >= 0;
    }

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                // Energy can exceed a short: low halves here, high halves at the end (see SplitInt)
                case 0 -> com.productivecows.menu.SplitInt.low(energyStorage.getEnergyStored());
                case 1 -> com.productivecows.menu.SplitInt.low(energyStorage.getMaxEnergyStored());
                case 2 -> fluidTank.getFluidAmount();
                case 3 -> fluidTank.getCapacity();
                case 4 -> com.productivecows.menu.SplitInt.high(energyStorage.getEnergyStored());
                case 5 -> com.productivecows.menu.SplitInt.high(energyStorage.getMaxEnergyStored());
                case 6 -> currentRate;
                case 7 -> BuiltInRegistries.FLUID.getId(fluidTank.getFluid().getFluid());
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
case 2 -> {
                    Fluid milkFluid = com.productivecows.fluid.ModFluids.LIQUID_MILK.source().get();
                    fluidTank.setFluid(new FluidStack(milkFluid, value));
                }
            }
        }

        @Override
        public int getCount() {
            return 8;
        }
    };

    public MilkGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ProductiveCows.MILK_GENERATOR_BE.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return itemHandler;
    }

    public FluidTank getFluidTank() {
        return fluidTank;
    }

    public CustomEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    // Hoppers and pipes may only take empty buckets; the menu uses itemHandler directly
    private final IItemHandler automationItemHandler = new AutomationItemHandler(itemHandler, 1);

    public IItemHandler getItemHandlerCapability() {
        return automationItemHandler;
    }

    public IFluidHandler getFluidHandlerCapability() {
        return fluidTank;
    }

    public IEnergyStorage getEnergyStorageCapability() {
        return energyStorage;
    }

    public void tick(Level lvl, BlockPos pos, BlockState state) {
        tickBucketEmptying();

        // 1. Burn fuel: milk or a material cow's fluid; higher tiers give more RF per mB and more RF/t
        currentRate = 0;
        int tier = GeneratorFuel.tier(fluidTank.getFluid().getFluid(), com.productivecows.ProductiveCows.getDefinitions(lvl));
        int space = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
        if (tier >= 0 && space > 0 && fluidTank.getFluidAmount() > 0) {
            int rfPerMb = GeneratorFuel.rfPerMb(tier);
            int toGenerate = Math.min(GeneratorFuel.rfPerTick(tier), space);
            double fuelCost = (double) toGenerate / rfPerMb;

            // fractionalMilk is the part of a mB already burned but not yet drained from the tank
            double availableFuel = fluidTank.getFluidAmount() - fractionalMilk;
            if (availableFuel <= 0.0) {
                fractionalMilk = 0.0;
            } else {
                if (fuelCost > availableFuel) {
                    fuelCost = availableFuel;
                    toGenerate = (int) (fuelCost * rfPerMb);
                }
                if (toGenerate > 0) {
                    energyStorage.addEnergy(toGenerate);
                    currentRate = toGenerate;
                    fractionalMilk += fuelCost;
                    if (fractionalMilk >= 1.0) {
                        int wholeMbToDrain = (int) Math.floor(fractionalMilk);
                        fluidTank.drain(wholeMbToDrain, IFluidHandler.FluidAction.EXECUTE);
                        fractionalMilk -= wholeMbToDrain;
                    }
                    setChanged();
                }
            }
        }

        // 2. Auto-push generated energy to neighboring blocks (max 400 RF/t, above the best fuel's output)
        int energyStored = energyStorage.getEnergyStored();
        if (energyStored > 0 && lvl instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            int maxSend = Math.min(400, energyStored);
            int sent = 0;

            for (Direction dir : Direction.values()) {
                if (sent >= maxSend) break;

                IEnergyStorage targetEnergy = neighborEnergy.get(serverLevel, pos, dir);
                if (targetEnergy != null && targetEnergy.canReceive()) {
                    int toSend = maxSend - sent;
                    int accepted = targetEnergy.receiveEnergy(toSend, false);
                    if (accepted > 0) {
                        sent += accepted;
                        energyStorage.consumeEnergy(accepted);
                        setChanged();
                    }
                }
            }
        }
    }

    private void tickBucketEmptying() {
        ItemStack inputBucket = itemHandler.getStackInSlot(0);
        Fluid bucketFluid = GeneratorFuel.bucketFluid(inputBucket);
        if (bucketFluid == null || !isFuel(bucketFluid)) {
            return;
        }
        FluidStack fuel = new FluidStack(bucketFluid, 1000);
        if (fluidTank.fill(fuel, IFluidHandler.FluidAction.SIMULATE) < 1000) {
            return;
        }
        ItemStack outputSlot = itemHandler.getStackInSlot(1);
        if (outputSlot.isEmpty() || (outputSlot.is(Items.BUCKET) && outputSlot.getCount() < outputSlot.getMaxStackSize())) {
            fluidTank.fill(fuel, IFluidHandler.FluidAction.EXECUTE);
            inputBucket.shrink(1);
            if (outputSlot.isEmpty()) {
                itemHandler.setStackInSlot(1, new ItemStack(Items.BUCKET));
            } else {
                outputSlot.grow(1);
            }
            setChanged();
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        this.fluidTank.readFromNBT(registries, tag.getCompound("FluidTank"));
        this.energyStorage.setEnergy(tag.getInt("Energy"));
        this.fractionalMilk = tag.getDouble("FractionalMilk");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", this.itemHandler.serializeNBT(registries));
        CompoundTag tankTag = new CompoundTag();
        this.fluidTank.writeToNBT(registries, tankTag);
        tag.put("FluidTank", tankTag);
        tag.putInt("Energy", this.energyStorage.getEnergyStored());
        tag.putDouble("FractionalMilk", this.fractionalMilk);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.productivecows.milk_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new MilkGeneratorMenu(id, playerInventory, this, this.dataAccess);
    }
}
