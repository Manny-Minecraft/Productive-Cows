package com.productivecows.genetics;

import java.util.function.DoubleSupplier;

/**
 * The three stats of a material cow, each from 1 to a maximum (10 by default):
 * <ul>
 *   <li>Yield: fluid per Cow Cage cycle;</li>
 *   <li>Metabolism: speed of the Cow Cage cycle and of the bucket milking cooldown;</li>
 *   <li>Fertility: speed of breeding and of the parents' breeding cooldown.</li>
 * </ul>
 * Plain logic, free of Minecraft classes, so it ports unchanged between versions.
 */
public record CowStats(int yield, int metabolism, int fertility) {
    public static final CowStats BASE = new CowStats(1, 1, 1);

    public CowStats {
        yield = Math.max(1, yield);
        metabolism = Math.max(1, metabolism);
        fertility = Math.max(1, fertility);
    }

    public boolean isBase() {
        return yield == 1 && metabolism == 1 && fertility == 1;
    }

    public CowStats clamp(int max) {
        return new CowStats(Math.min(yield, max), Math.min(metabolism, max), Math.min(fertility, max));
    }

    /** 1 at level 1, then {@code percentPerLevel}% more for every level above it. */
    public static double multiplier(int level, int percentPerLevel) {
        return 1.0 + (Math.max(1, level) - 1) * Math.max(0, percentPerLevel) / 100.0;
    }

    /**
     * A calf's stats: for each stat, the parents' average rounded down, then a chance of +1 that shrinks as the stat
     * grows, from {@code growthChancePercent} at level 1 to {@code minGrowthChancePercent} just below {@code max};
     * never more than the better parent + 1, never above {@code max}.
     *
     * @param random returns a number in [0, 1), one call per stat
     */
    public static CowStats breed(CowStats a, CowStats b, int max, int growthChancePercent, int minGrowthChancePercent,
                                 DoubleSupplier random) {
        return new CowStats(
                child(a.yield, b.yield, max, growthChancePercent, minGrowthChancePercent, random),
                child(a.metabolism, b.metabolism, max, growthChancePercent, minGrowthChancePercent, random),
                child(a.fertility, b.fertility, max, growthChancePercent, minGrowthChancePercent, random));
    }

    static int child(int x, int y, int max, int growthChancePercent, int minGrowthChancePercent, DoubleSupplier random) {
        int value = (x + y) / 2;
        double high = growthChancePercent / 100.0;
        double low = Math.min(high, minGrowthChancePercent / 100.0);
        double chance = max <= 1 || value >= max ? 0 : low + (high - low) * (max - value) / (max - 1);
        if (random.getAsDouble() < chance) {
            value++;
        }
        return Math.max(1, Math.min(Math.min(value, Math.max(x, y) + 1), max));
    }
}
