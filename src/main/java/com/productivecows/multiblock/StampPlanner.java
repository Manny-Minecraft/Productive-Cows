package com.productivecows.multiblock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Decides what the Stamps of the Cooler cool in one cycle. Stamps are handed out one at a time, in turn, to the
 * fluids that still have enough for an ingot; each Stamp takes up to {@code ingotsPerStamp} ingots of its fluid.
 * The starting fluid rotates every cycle, so with fewer Stamps than fluids every fluid gets its turn.
 * <p>
 * Generic and free of Minecraft classes, so it ports unchanged between versions.
 */
public final class StampPlanner {
    /** Ingots to cool per fluid, and how many Stamps are busy doing it (each one costs energy). */
    public record Plan<K>(Map<K, Integer> ingots, int activeStamps) {
        public boolean isEmpty() {
            return activeStamps == 0;
        }
    }

    private StampPlanner() {}

    /**
     * @param amounts        fluids in storage order with their amounts
     * @param coolable       which fluids have a result to cool into
     * @param stamps         Stamps in the tower
     * @param ingotsPerStamp ingots one Stamp cools at a time
     * @param fluidPerIngot  mB one ingot costs
     * @param rotation       a counter that grows every cycle, to rotate the starting fluid
     * @return ingots to cool per fluid this cycle and the Stamps in use; empty when there is nothing to do
     */
    public static <K> Plan<K> plan(Map<K, Integer> amounts, Predicate<K> coolable, int stamps,
                                           int ingotsPerStamp, int fluidPerIngot, int rotation) {
        List<K> fluids = new ArrayList<>();
        Map<K, Integer> left = new LinkedHashMap<>();
        for (Map.Entry<K, Integer> entry : amounts.entrySet()) {
            if (coolable.test(entry.getKey()) && entry.getValue() >= fluidPerIngot) {
                fluids.add(entry.getKey());
                left.put(entry.getKey(), entry.getValue());
            }
        }
        Map<K, Integer> plan = new LinkedHashMap<>();
        if (fluids.isEmpty() || stamps <= 0 || ingotsPerStamp <= 0 || fluidPerIngot <= 0) {
            return new Plan<>(plan, 0);
        }
        int active = 0;
        int start = Math.floorMod(rotation, fluids.size());
        int next = 0;
        for (int stamp = 0; stamp < stamps; stamp++) {
            // Next fluid in turn that can still pay for at least one ingot
            K chosen = null;
            for (int tries = 0; tries < fluids.size(); tries++) {
                K candidate = fluids.get(Math.floorMod(start + next++, fluids.size()));
                if (left.get(candidate) >= fluidPerIngot) {
                    chosen = candidate;
                    break;
                }
            }
            if (chosen == null) {
                break;
            }
            int ingots = Math.min(ingotsPerStamp, left.get(chosen) / fluidPerIngot);
            left.put(chosen, left.get(chosen) - ingots * fluidPerIngot);
            plan.merge(chosen, ingots, Integer::sum);
            active++;
        }
        return new Plan<>(plan, active);
    }
}
