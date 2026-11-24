package com.productivecows.multiblock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Decides what the Stamps of the Cooler cool in one cycle. A cycle always makes every ingot it can
 * ({@code stamps * ingotsPerStamp}), handing them out one at a time, in turn, to the fluids that can still pay for
 * one. The turn is remembered between cycles ({@link Plan#nextRotation()}), so with more fluids than ingots per
 * cycle every fluid gets its share over time.
 * <p>
 * Generic and free of Minecraft classes, so it ports unchanged between versions.
 */
public final class StampPlanner {
    /**
     * Ingots to cool per fluid, the Stamps busy doing it (each one costs energy) and where the next cycle's turn
     * starts.
     */
    public record Plan<K>(Map<K, Integer> ingots, int activeStamps, int nextRotation) {
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
     * @param rotation       where the turn starts, as returned by the previous cycle
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
            return new Plan<>(plan, 0, rotation);
        }
        int slots = stamps * ingotsPerStamp;
        int turn = Math.floorMod(rotation, fluids.size());
        int made = 0;
        for (int n = 0; n < slots; n++) {
            // Next fluid in turn that can still pay for an ingot
            K chosen = null;
            for (int tries = 0; tries < fluids.size(); tries++) {
                K candidate = fluids.get(turn);
                turn = (turn + 1) % fluids.size();
                if (left.get(candidate) >= fluidPerIngot) {
                    chosen = candidate;
                    break;
                }
            }
            if (chosen == null) {
                break;
            }
            left.put(chosen, left.get(chosen) - fluidPerIngot);
            plan.merge(chosen, 1, Integer::sum);
            made++;
        }
        int busy = (made + ingotsPerStamp - 1) / ingotsPerStamp;
        return new Plan<>(plan, busy, turn);
    }
}
