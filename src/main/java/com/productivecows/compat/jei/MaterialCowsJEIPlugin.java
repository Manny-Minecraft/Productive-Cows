package com.productivecows.compat.jei;

import com.productivecows.ProductiveCows;
import com.productivecows.data.CowDefinition;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class MaterialCowsJEIPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(ProductiveCows.MODID, "jei_plugin");
    }

    @Override
    public void registerItemSubtypes(mezz.jei.api.registration.ISubtypeRegistration registration) {
        mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter<net.minecraft.world.item.ItemStack> interpreter = (stack, context) -> {
            net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.ENTITY_DATA);
            if (customData != null) {
                net.minecraft.nbt.CompoundTag tag = customData.copyTag();
                if (tag.contains("CowDefinitionId")) {
                    return tag.getString("CowDefinitionId");
                }
            }
            return mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter.NONE;
        };
        registration.registerSubtypeInterpreter(ProductiveCows.CAPTURED_COW.get(), interpreter);
        registration.registerSubtypeInterpreter(ProductiveCows.MATERIAL_COW_SPAWN_EGG.get(), interpreter);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new BreedingRecipeCategory(guiHelper));
        registration.addRecipeCategories(new CowConversionCategory(guiHelper));
        registration.addRecipeCategories(new MilkingRecipeCategory(guiHelper));
        registration.addRecipeCategories(new CoolerRecipeCategory(guiHelper));
        registration.addRecipeCategories(new GeneratorFuelCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var definitions = ProductiveCows.getActiveDefinitions();

        List<CowDefinition> breedingRecipes = new ArrayList<>();
        for (CowDefinition def : definitions.values()) {
            if (def.parent1() != null && def.parent2() != null && !def.parent1().equals(def.parent2())) {
                breedingRecipes.add(def);
            }
        }
        breedingRecipes.sort(java.util.Comparator.comparingInt(CowDefinition::tier).thenComparing(CowDefinition::name));
        registration.addRecipes(BreedingRecipeCategory.BREEDING_TYPE, breedingRecipes);

        List<CowConversionRecipe> conversionRecipes = new ArrayList<>();
        conversionRecipes.add(new CowConversionRecipe(List.of(new ItemStack(Items.WATER_BUCKET)), ResourceLocation.fromNamespaceAndPath(ProductiveCows.MODID, "water")));
        conversionRecipes.add(new CowConversionRecipe(List.of(new ItemStack(Items.LAVA_BUCKET)), ResourceLocation.fromNamespaceAndPath(ProductiveCows.MODID, "lava")));
        conversionRecipes.add(new CowConversionRecipe(List.of(new ItemStack(Items.CLAY), new ItemStack(Items.CLAY_BALL)), ResourceLocation.fromNamespaceAndPath(ProductiveCows.MODID, "clay")));
        conversionRecipes.add(new CowConversionRecipe(List.of(new ItemStack(Items.STONE), new ItemStack(Items.COBBLESTONE)), ResourceLocation.fromNamespaceAndPath(ProductiveCows.MODID, "stone")));
        registration.addRecipes(CowConversionCategory.CONVERSION_TYPE, conversionRecipes);

        // Cow Cage: every cow and the fluid it gives per cycle, plus the vanilla cow's milk
        List<MilkingRecipe> milkingRecipes = new ArrayList<>();
        milkingRecipes.add(new MilkingRecipe(JeiStacks.capturedVanillaCow(), ItemStack.EMPTY,
                com.productivecows.fluid.ModFluids.LIQUID_MILK.source().get(), 1000));
        List<CowDefinition> byTier = new ArrayList<>(definitions.values());
        byTier.sort(java.util.Comparator.comparingInt(CowDefinition::tier).thenComparing(CowDefinition::name));
        for (CowDefinition def : byTier) {
            Fluid fluid = JeiStacks.fluid(def);
            if (fluid != null) {
                milkingRecipes.add(new MilkingRecipe(JeiStacks.capturedCow(def.id()), JeiStacks.spawnEgg(def.id()), fluid,
                        com.productivecows.block.entity.CowCageBlockEntity.getYieldPerCycle(def)));
            }
        }
        registration.addRecipes(MilkingRecipeCategory.MILKING_TYPE, milkingRecipes);

        // Coolers: item results and tag results alike, as long as something fills the tag
        List<CowDefinition> coolerRecipes = new ArrayList<>();
        for (CowDefinition def : byTier) {
            if (JeiStacks.fluid(def) != null && !JeiStacks.coolingOutputs(def).isEmpty()) {
                coolerRecipes.add(def);
            }
        }
        registration.addRecipes(CoolerRecipeCategory.COOLER_TYPE, coolerRecipes);

        // Milk Generator fuels, one per fluid
        List<GeneratorFuelRecipe> fuelRecipes = new ArrayList<>();
        java.util.Set<Fluid> seen = new java.util.HashSet<>();
        Fluid milk = com.productivecows.fluid.ModFluids.LIQUID_MILK.source().get();
        fuelRecipes.add(new GeneratorFuelRecipe(milk, 0));
        seen.add(milk);
        for (CowDefinition def : byTier) {
            Fluid fluid = JeiStacks.fluid(def);
            int tier = com.productivecows.block.entity.GeneratorFuel.tier(fluid, definitions);
            if (fluid != null && tier >= 0 && seen.add(fluid)) {
                fuelRecipes.add(new GeneratorFuelRecipe(fluid, tier));
            }
        }
        registration.addRecipes(GeneratorFuelCategory.FUEL_TYPE, fuelRecipes);

        // How to build the Cooler multiblock, on each of its blocks
        for (var block : List.of(ProductiveCows.COOLER_CONTROLLER_ITEM, ProductiveCows.COOLER_BLOCK_ITEM, ProductiveCows.COOLER_TANK_ITEM,
                ProductiveCows.COOLER_PORT_ITEM, ProductiveCows.COOLER_STAMP_ITEM)) {
            registration.addItemStackInfo(new ItemStack(block.get()), net.minecraft.network.chat.Component.translatable("jei.productivecows.cooler_info"));
        }
    }

    @Override
    public void registerRecipeCatalysts(mezz.jei.api.registration.IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ProductiveCows.BREEDING_CAGE_ITEM.get()), BreedingRecipeCategory.BREEDING_TYPE);
        registration.addRecipeCatalyst(new ItemStack(Items.COW_SPAWN_EGG), CowConversionCategory.CONVERSION_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ProductiveCows.COW_CAGE_ITEM.get()), MilkingRecipeCategory.MILKING_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ProductiveCows.LIQUID_COOLER_ITEM.get()), CoolerRecipeCategory.COOLER_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ProductiveCows.ADVANCED_COOLER_ITEM.get()), CoolerRecipeCategory.COOLER_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ProductiveCows.COOLER_CONTROLLER_ITEM.get()), CoolerRecipeCategory.COOLER_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ProductiveCows.MILK_GENERATOR_ITEM.get()), GeneratorFuelCategory.FUEL_TYPE);
    }

    @Override
    public void registerGuiHandlers(mezz.jei.api.registration.IGuiHandlerRegistration registration) {
        // The whole progress bar of each machine (drawn 39x2 or 16x2), with some height so it is easy to hit
        registration.addRecipeClickArea(com.productivecows.client.screen.BreedingCageScreen.class, 67, 37, 39, 12, BreedingRecipeCategory.BREEDING_TYPE);
        registration.addRecipeClickArea(com.productivecows.client.screen.LiquidCoolerScreen.class, 83, 37, 39, 12, CoolerRecipeCategory.COOLER_TYPE);
        registration.addRecipeClickArea(com.productivecows.client.screen.AdvancedCoolerScreen.class, 83, 37, 39, 12, CoolerRecipeCategory.COOLER_TYPE);
        registration.addRecipeClickArea(com.productivecows.client.screen.CowCageScreen.class, 127, 47, 20, 12, MilkingRecipeCategory.MILKING_TYPE);
        registration.addRecipeClickArea(com.productivecows.client.screen.CoolerControllerScreen.class,
                com.productivecows.client.screen.CoolerControllerScreen.PROGRESS_X, com.productivecows.client.screen.CoolerControllerScreen.PROGRESS_Y - 4,
                com.productivecows.client.screen.CoolerControllerScreen.PROGRESS_W, com.productivecows.client.screen.CoolerControllerScreen.PROGRESS_H + 8,
                CoolerRecipeCategory.COOLER_TYPE);
    }
}
