package com.productivecows.genetics;

import com.productivecows.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/**
 * Reads and writes {@link CowStats} in a cow's saved data: the entity's NBT and the {@code ENTITY_DATA} of captured
 * cows and spawn eggs. Missing keys mean 1, so cows saved before stats existed are 1/1/1, and 1/1/1 cows store
 * nothing, so they still stack.
 */
public final class CowStatsNbt {
    public static final String YIELD = "Yield";
    public static final String METABOLISM = "Metabolism";
    public static final String FERTILITY = "Fertility";

    private CowStatsNbt() {}

    public static int max() {
        return Math.max(1, Config.cowStatMax);
    }

    public static CowStats read(CompoundTag tag) {
        return new CowStats(level(tag, YIELD), level(tag, METABOLISM), level(tag, FERTILITY)).clamp(max());
    }

    private static int level(CompoundTag tag, String key) {
        return tag.contains(key) ? tag.getInt(key) : 1;
    }

    public static void write(CompoundTag tag, CowStats stats) {
        if (stats.isBase()) {
            tag.remove(YIELD);
            tag.remove(METABOLISM);
            tag.remove(FERTILITY);
        } else {
            tag.putInt(YIELD, stats.yield());
            tag.putInt(METABOLISM, stats.metabolism());
            tag.putInt(FERTILITY, stats.fertility());
        }
    }

    /** Stats of a captured cow or spawn egg; 1/1/1 when it carries none. */
    public static CowStats of(ItemStack stack) {
        CustomData data = stack.get(DataComponents.ENTITY_DATA);
        return data == null ? CowStats.BASE : read(data.getUnsafe());
    }

    /** A copy of the stack carrying these stats. */
    public static ItemStack with(ItemStack stack, CowStats stats) {
        ItemStack copy = stack.copy();
        CustomData data = copy.getOrDefault(DataComponents.ENTITY_DATA, CustomData.EMPTY);
        copy.set(DataComponents.ENTITY_DATA, data.update(tag -> write(tag, stats)));
        return copy;
    }

    /** Tooltip and Jade lines, one per stat. */
    public static List<Component> describe(CowStats stats) {
        int max = max();
        return List.of(
                Component.translatable("tooltip.productivecows.cow.yield", stats.yield(), max).withStyle(ChatFormatting.AQUA),
                Component.translatable("tooltip.productivecows.cow.metabolism", stats.metabolism(), max).withStyle(ChatFormatting.GREEN),
                Component.translatable("tooltip.productivecows.cow.fertility", stats.fertility(), max).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Fluid per Cow Cage cycle for a cow with these stats. */
    public static double yieldMultiplier(CowStats stats) {
        return CowStats.multiplier(stats.yield(), Config.cowYieldPerLevel);
    }

    /** How much faster the Cow Cage cycle and the bucket cooldown run. */
    public static double metabolismMultiplier(CowStats stats) {
        return CowStats.multiplier(stats.metabolism(), Config.cowMetabolismPerLevel);
    }

    /** How much faster breeding and the breeding cooldown run. */
    public static double fertilityMultiplier(CowStats stats) {
        return CowStats.multiplier(stats.fertility(), Config.cowFertilityPerLevel);
    }
}
