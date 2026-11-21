package com.productivecows.block.entity;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Item handler exposed to hoppers and pipes. Insertion goes through the machine's own
 * handler (which already validates slots); extraction is limited to the output slots,
 * so automation cannot pull parents, input buckets or upgrades out of a machine.
 * Menus keep using the machine's handler directly, so players can still take any item.
 */
public class AutomationItemHandler implements IItemHandler {

    private final IItemHandler inner;
    private final int[] outputSlots;

    public AutomationItemHandler(IItemHandler inner, int... outputSlots) {
        this.inner = inner;
        this.outputSlots = outputSlots;
    }

    private boolean isOutput(int slot) {
        for (int output : outputSlots) {
            if (output == slot) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getSlots() {
        return inner.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inner.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return inner.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!isOutput(slot)) {
            return ItemStack.EMPTY;
        }
        return inner.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return inner.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return inner.isItemValid(slot, stack);
    }
}
