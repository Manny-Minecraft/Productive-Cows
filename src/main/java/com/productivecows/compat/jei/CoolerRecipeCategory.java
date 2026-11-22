package com.productivecows.compat.jei;

import com.productivecows.Config;
import com.productivecows.ProductiveCows;
import com.productivecows.data.CowDefinition;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/** Cooling a cow's fluid into its material: fluid -> arrow -> item, with the Basic Cooler's cost and time. */
public class CoolerRecipeCategory implements IRecipeCategory<CowDefinition> {

    public static final RecipeType<CowDefinition> COOLER_TYPE = RecipeType.create(ProductiveCows.MODID, "cooler", CowDefinition.class);
    private static final int FLUID_PER_ITEM = 1000;

    private final IDrawable icon;

    public CoolerRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ProductiveCows.LIQUID_COOLER_ITEM.get()));
    }

    @Override
    public RecipeType<CowDefinition> getRecipeType() {
        return COOLER_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.productivecows.cooling");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 100;
    }

    @Override
    public int getHeight() {
        return 42;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CowDefinition recipe, IFocusGroup focuses) {
        Fluid fluid = JeiStacks.fluid(recipe);
        if (fluid != null) {
            builder.addSlot(RecipeIngredientRole.INPUT, 8, 9)
                    .addFluidStack(fluid, FLUID_PER_ITEM)
                    .setFluidRenderer(FLUID_PER_ITEM, false, 16, 16)
                    .setStandardSlotBackground();
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 72, 9)
                .addItemStacks(JeiStacks.coolingOutputs(recipe))
                .setOutputSlotBackground();
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, CowDefinition recipe, IFocusGroup focuses) {
        int cookTime = Math.max(1, Config.basicCoolerCookTime);
        builder.addAnimatedRecipeArrow(cookTime).setPosition(36, 9);
        builder.addText(Component.translatable("jei.productivecows.cooling_cost", FLUID_PER_ITEM, seconds(cookTime)), 0, 32, getWidth(), 10)
                .alignHorizontalCenter()
                .setColor(JeiStacks.TEXT_COLOR);
    }

    static String seconds(int ticks) {
        return ticks % 20 == 0 ? String.valueOf(ticks / 20) : String.format(java.util.Locale.ROOT, "%.1f", ticks / 20f);
    }
}
