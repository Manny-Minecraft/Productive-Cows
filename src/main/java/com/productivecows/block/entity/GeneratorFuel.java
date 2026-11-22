package com.productivecows.block.entity;

import com.productivecows.data.CowDefinition;
import com.productivecows.fluid.ModFluids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Fuels of the Milk Generator. Milk is tier 0; every fluid a material cow produces burns at its cow's tier,
 * and each tier doubles the energy per mB and adds 40 RF/t of output. Vanilla water and lava are not fuels,
 * since they can be had in unlimited amounts without any cow.
 */
public final class GeneratorFuel {
    public static final int BASE_RF_PER_MB = 100;
    public static final int BASE_RF_PER_TICK = 40;
    private static final int MAX_TIER = 10;

    private GeneratorFuel() {}

    /** The fuel tier of a fluid, or -1 when the generator can't burn it. */
    public static int tier(@Nullable Fluid fluid, Map<ResourceLocation, CowDefinition> definitions) {
        if (fluid == null || fluid == Fluids.EMPTY) {
            return -1;
        }
        ResourceLocation key = BuiltInRegistries.FLUID.getKey(fluid);
        if (fluid == ModFluids.LIQUID_MILK.source().get() || "milk".equals(key.getPath())) {
            return 0;
        }
        if ("minecraft".equals(key.getNamespace())) {
            return -1;
        }
        for (CowDefinition def : definitions.values()) {
            if (def.fluidId().equals(key)) {
                return Math.max(1, Math.min(MAX_TIER, def.tier()));
            }
        }
        return -1;
    }

    public static int rfPerMb(int tier) {
        return BASE_RF_PER_MB << tier;
    }

    public static int rfPerTick(int tier) {
        return BASE_RF_PER_TICK * (tier + 1);
    }

    /** The fluid in a bucket the generator can be fed with; the vanilla milk bucket counts as our milk. */
    @Nullable
    public static Fluid bucketFluid(ItemStack stack) {
        if (stack.is(Items.MILK_BUCKET)) {
            return ModFluids.LIQUID_MILK.source().get();
        }
        if (stack.getItem() instanceof BucketItem bucket && bucket.content != Fluids.EMPTY) {
            return bucket.content;
        }
        return null;
    }
}
