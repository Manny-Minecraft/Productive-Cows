package com.productivecows.compat.jei;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/** One Cow Cage cycle: the caged cow, the fluid and how much of it a cycle produces. */
public record MilkingRecipe(ItemStack cow, ItemStack spawnEgg, Fluid fluid, int amount) {
    /** Ticks between two milkings in the Cow Cage. */
    public static final int CYCLE_TICKS = com.productivecows.block.entity.CowCageBlockEntity.MILKING_CYCLE_TICKS;
}
