package com.productivecows.data.provider;

import com.productivecows.ProductiveCows;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // Breeding Cage: iron bars around a hay block
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ProductiveCows.BREEDING_CAGE_ITEM.get())
                .pattern("III")
                .pattern("IHI")
                .pattern("III")
                .define('I', Items.IRON_BARS)
                .define('H', Items.HAY_BLOCK)
                .unlockedBy("has_iron_bars", has(Items.IRON_BARS))
                .save(output);

        // Cow Cage: iron bars around a bucket
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ProductiveCows.COW_CAGE_ITEM.get())
                .pattern("III")
                .pattern("IBI")
                .pattern("III")
                .define('I', Items.IRON_BARS)
                .define('B', Items.BUCKET)
                .unlockedBy("has_bucket", has(Items.BUCKET))
                .save(output);

        // Liquid Cooler: iron casing with glass and redstone
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ProductiveCows.LIQUID_COOLER_ITEM.get())
                .pattern("IGI")
                .pattern("IRI")
                .pattern("III")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('G', Tags.Items.GLASS_BLOCKS_COLORLESS)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_iron", has(Tags.Items.INGOTS_IRON))
                .save(output);

        // Advanced Cooler: liquid cooler surrounded by gold and diamonds
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ProductiveCows.ADVANCED_COOLER_ITEM.get())
                .pattern("DGD")
                .pattern("GCG")
                .pattern("DGD")
                .define('D', Tags.Items.GEMS_DIAMOND)
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('C', ProductiveCows.LIQUID_COOLER_ITEM.get())
                .unlockedBy("has_liquid_cooler", has(ProductiveCows.LIQUID_COOLER_ITEM.get()))
                .save(output);

        // Milk Generator: iron casing with a bucket, a furnace and redstone
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ProductiveCows.MILK_GENERATOR_ITEM.get())
                .pattern("IBI")
                .pattern("RFR")
                .pattern("III")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('B', Items.BUCKET)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('F', Items.FURNACE)
                .unlockedBy("has_bucket", has(Items.BUCKET))
                .save(output);

        // Base Upgrade (x3): iron and redstone
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ProductiveCows.BASE_UPGRADE.get(), 3)
                .pattern(" I ")
                .pattern("IRI")
                .pattern(" I ")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_redstone", has(Tags.Items.DUSTS_REDSTONE))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ProductiveCows.SPEED_UPGRADE.get())
                .requires(ProductiveCows.BASE_UPGRADE.get())
                .requires(Items.SUGAR)
                .unlockedBy("has_base_upgrade", has(ProductiveCows.BASE_UPGRADE.get()))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ProductiveCows.EFFICIENCY_UPGRADE.get())
                .requires(ProductiveCows.BASE_UPGRADE.get())
                .requires(Tags.Items.DUSTS_GLOWSTONE)
                .unlockedBy("has_base_upgrade", has(ProductiveCows.BASE_UPGRADE.get()))
                .save(output);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ProductiveCows.ENERGY_UPGRADE.get())
                .requires(ProductiveCows.BASE_UPGRADE.get())
                .requires(Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_base_upgrade", has(ProductiveCows.BASE_UPGRADE.get()))
                .save(output);

        // Cow Stick: sticks and leather
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ProductiveCows.COW_STICK.get())
                .pattern("  L")
                .pattern(" S ")
                .pattern("S  ")
                .define('L', Tags.Items.LEATHERS)
                .define('S', Tags.Items.RODS_WOODEN)
                .unlockedBy("has_leather", has(Tags.Items.LEATHERS))
                .save(output);
    }
}
