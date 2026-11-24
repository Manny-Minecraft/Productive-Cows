package com.productivecows.multiblock;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fluid storage of the Cooler multiblock: up to {@code maxTypes} different fluids sharing one {@code capacity}.
 * Each Tank of the tower adds one fluid type and its share of capacity. Lowering the limits (a Tank removed)
 * never deletes fluid: the storage simply accepts nothing new until it is back under the limits.
 * <p>
 * Generic on the fluid key and free of Minecraft classes, so it ports unchanged between versions.
 */
public final class MultiFluidStorage<K> {
    private final Map<K, Integer> amounts = new LinkedHashMap<>();
    private int capacity;
    private int maxTypes;

    public void setLimits(int capacity, int maxTypes) {
        this.capacity = Math.max(0, capacity);
        this.maxTypes = Math.max(0, maxTypes);
    }

    public int capacity() {
        return capacity;
    }

    public int maxTypes() {
        return maxTypes;
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

    /** Fluids in the order they arrived, with their amounts. */
    public Map<K, Integer> contents() {
        return Collections.unmodifiableMap(amounts);
    }

    /** Whether a fluid could be added right now, ignoring how much. */
    public boolean accepts(K fluid) {
        return amounts.containsKey(fluid) || amounts.size() < maxTypes;
    }

    /** Adds up to {@code amount}; returns how much was (or would be) accepted. */
    public int fill(K fluid, int amount, boolean simulate) {
        if (amount <= 0 || !accepts(fluid)) {
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

    /** Puts a saved amount back, bypassing the limits (used when loading). */
    public void restore(K fluid, int amount) {
        if (amount > 0) {
            amounts.put(fluid, amount);
        }
    }

    public void clear() {
        amounts.clear();
    }
}
