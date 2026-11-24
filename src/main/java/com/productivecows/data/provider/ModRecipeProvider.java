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

        // Cooler multiblock. Casing (x4): polished deepslate and copper around iron
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ProductiveCows.COOLER_BLOCK_ITEM.get(), 4)
                .pattern("DCD")
                .pattern("CIC")
                .pattern("DCD")
                .define('D', Items.POLISHED_DEEPSLATE)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('I', Tags.Items.INGOTS_IRON)
                .unlockedBy("has_advanced_cooler", has(ProductiveCows.ADVANCED_COOLER_ITEM.get()))
                .save(output);

        // Cooler Tank: casing and glass around a bucket
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ProductiveCows.COOLER_TANK_ITEM.get())
                .pattern("CGC")
                .pattern("GBG")
                .pattern("CGC")
                .define('C', ProductiveCows.COOLER_BLOCK_ITEM.get())
                .define('G', Tags.Items.GLASS_BLOCKS_COLORLESS)
                .define('B', Items.BUCKET)
                .unlockedBy("has_cooler_block", has(ProductiveCows.COOLER_BLOCK_ITEM.get()))
                .save(output);

        // Cooler Port: casing with hoppers and redstone
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ProductiveCows.COOLER_PORT_ITEM.get())
                .pattern("CHC")
                .pattern("RCR")
                .pattern("CHC")
                .define('C', ProductiveCows.COOLER_BLOCK_ITEM.get())
                .define('H', Items.HOPPER)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_cooler_block", has(ProductiveCows.COOLER_BLOCK_ITEM.get()))
                .save(output);

        // Cooler Stamp: a Basic Cooler packed in ice and gold, so each stamp costs a mid-game machine
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ProductiveCows.COOLER_STAMP_ITEM.get())
                .pattern("PGP")
                .pattern("GLG")
                .pattern("PGP")
                .define('P', Items.PACKED_ICE)
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('L', ProductiveCows.LIQUID_COOLER_ITEM.get())
                .unlockedBy("has_liquid_cooler", has(ProductiveCows.LIQUID_COOLER_ITEM.get()))
                .save(output);

        // Cooler Controller: an Advanced Cooler wired into the casing
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ProductiveCows.COOLER_CONTROLLER_ITEM.get())
                .pattern("CRC")
                .pattern("DAD")
                .pattern("CRC")
                .define('C', ProductiveCows.COOLER_BLOCK_ITEM.get())
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('D', Tags.Items.GEMS_DIAMOND)
                .define('A', ProductiveCows.ADVANCED_COOLER_ITEM.get())
                .unlockedBy("has_advanced_cooler", has(ProductiveCows.ADVANCED_COOLER_ITEM.get()))
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
