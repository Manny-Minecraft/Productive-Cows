package com.productivecows.multiblock;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fluid storage of the Cooler multiblock: one shared tank holding any mix of fluids, stacked in the order they
 * arrived (the GUI draws them as layers). Each Tank of the tower adds to the capacity. Lowering the capacity (a
 * Tank removed) never deletes fluid: the storage simply accepts nothing new until it is back under it.
 * {@code maxTypes} is only a safety cap so the GUI can always show every fluid.
 * <p>
 * Generic and free of Minecraft classes, so it ports unchanged between versions.
 */
public final class MultiFluidStorage<K> {
    private final Map<K, Integer> amounts = new LinkedHashMap<>();
    private final int maxTypes;
    private int capacity;

    public MultiFluidStorage(int maxTypes) {
        this.maxTypes = maxTypes;
    }

    public void setCapacity(int capacity) {
        this.capacity = Math.max(0, capacity);
    }

    public int capacity() {
        return capacity;
    }

    public int total() {
        int sum = 0;
        for (int amount : amounts.values()) {
            sum += amount;
        }
        return sum;
    }

    public int amount(K fluid) {
        return amounts.getOrDefault(fluid, 0);
    }

    /** Fluids from the bottom layer up, with their amounts. */
    public Map<K, Integer> contents() {
        return Collections.unmodifiableMap(amounts);
    }

    /** Adds up to {@code amount}; returns how much was (or would be) accepted. */
    public int fill(K fluid, int amount, boolean simulate) {
        if (amount <= 0 || (!amounts.containsKey(fluid) && amounts.size() >= maxTypes)) {
            return 0;
        }
        int accepted = Math.min(amount, Math.max(0, capacity - total()));
        if (!simulate && accepted > 0) {
            amounts.merge(fluid, accepted, Integer::sum);
        }
        return accepted;
    }

    /** Removes up to {@code amount}; returns how much was (or would be) removed. */
    public int drain(K fluid, int amount, boolean simulate) {
        int stored = amount(fluid);
        int drained = Math.min(Math.max(0, amount), stored);
        if (!simulate && drained > 0) {
            if (drained == stored) {
                amounts.remove(fluid);
            } else {
                amounts.put(fluid, stored - drained);
            }
        }
        return drained;
    }

    /** Puts a saved amount back, bypassing the capacity (used when loading). */
    public void restore(K fluid, int amount) {
        if (amount > 0) {
            amounts.put(fluid, amount);
        }
    }

    public void clear() {
        amounts.clear();
    }
}
