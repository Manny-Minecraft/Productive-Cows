package com.productivecows.compat.jei;

import net.minecraft.world.level.material.Fluid;

/** A fluid the Milk Generator burns and its fuel tier (0 for milk). */
public record GeneratorFuelRecipe(Fluid fluid, int tier) {
}
