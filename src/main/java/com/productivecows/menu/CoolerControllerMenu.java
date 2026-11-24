package com.productivecows.menu;

import com.productivecows.ProductiveCows;
import com.productivecows.block.entity.CoolerControllerBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** GUI of the Cooler Controller: stored fluids, energy, tower size and the output buffer. */
public class CoolerControllerMenu extends AbstractContainerMenu {
    // Layout of the synced data; values above 32767 take two slots (see SplitInt)
    public static final int ENERGY = 0;
    public static final int MAX_ENERGY = 2;
    public static final int CAPACITY = 4;
    public static final int TANKS = 6;
    public static final int STAMPS = 7;
    public static final int HEIGHT = 8;
    public static final int PROGRESS = 9;
    public static final int CYCLE_TICKS = 10;
    public static final int ACTIVE_STAMPS = 11;
    public static final int CYCLE_INGOTS = 12;
    /** Then three slots per fluid: registry id, amount low half, amount high half. */
    public static final int FLUIDS = 13;
    public static final int DATA_COUNT = FLUIDS + CoolerControllerBlockEntity.MAX_FLUIDS * 3;
    /** Fluid layers the GUI can show: as many as the tank can hold. */
    public static final int FLUIDS_SHOWN = CoolerControllerBlockEntity.MAX_FLUIDS;

    public static final int OUTPUT_X = 8;
    public static final int OUTPUT_Y = 78;
    public static final int INVENTORY_Y = 110;

    private final BlockEntity blockEntity;
    private final ContainerData data;

    // Client constructor
    public CoolerControllerMenu(int windowId, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(windowId, inv, inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(DATA_COUNT));
    }

    public CoolerControllerMenu(int windowId, Inventory inv, BlockEntity be, ContainerData data) {
        super(ProductiveCows.COOLER_CONTROLLER_MENU.get(), windowId);
        checkContainerDataCount(data, DATA_COUNT);
        this.blockEntity = be;
        this.data = data;

        ItemStackHandler output = be instanceof CoolerControllerBlockEntity controller
                ? controller.getOutputInventory() : new ItemStackHandler(CoolerControllerBlockEntity.OUTPUT_SLOTS);
        for (int i = 0; i < CoolerControllerBlockEntity.OUTPUT_SLOTS; i++) {
            addSlot(new SlotItemHandler(output, i, OUTPUT_X + i * 18, OUTPUT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, INVENTORY_Y + 58));
        }
        addDataSlots(data);
    }

    public BlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getEnergy() {
        return SplitInt.join(data.get(ENERGY), data.get(ENERGY + 1));
    }

    public int getMaxEnergy() {
        return SplitInt.join(data.get(MAX_ENERGY), data.get(MAX_ENERGY + 1));
    }

    public int getCapacity() {
        return SplitInt.join(data.get(CAPACITY), data.get(CAPACITY + 1));
    }

    public int getTanks() {
        return data.get(TANKS);
    }

    public int getStamps() {
        return data.get(STAMPS);
    }

    public int getHeight() {
        return data.get(HEIGHT);
    }

    public int getProgress() {
        return data.get(PROGRESS);
    }

    public int getCycleTicks() {
        return data.get(CYCLE_TICKS);
    }

    public int getActiveStamps() {
        return data.get(ACTIVE_STAMPS);
    }

    public int getCycleIngots() {
        return data.get(CYCLE_INGOTS);
    }

    public Fluid getFluid(int slot) {
        return BuiltInRegistries.FLUID.byId(data.get(FLUIDS + slot * 3));
    }

    public int getFluidAmount(int slot) {
        return SplitInt.join(data.get(FLUIDS + slot * 3 + 1), data.get(FLUIDS + slot * 3 + 2));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int outputEnd = CoolerControllerBlockEntity.OUTPUT_SLOTS;
        int inventoryEnd = outputEnd + 27;
        int hotbarEnd = inventoryEnd + 9;
        if (index < outputEnd) {
            // Output -> player inventory
            if (!moveItemStackTo(stack, outputEnd, hotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index < inventoryEnd) {
            if (!moveItemStackTo(stack, inventoryEnd, hotbarEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, outputEnd, inventoryEnd, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null && !blockEntity.isRemoved()
                && player.distanceToSqr(blockEntity.getBlockPos().getCenter()) <= 64.0;
    }
}
