package com.productivecows.compat.jei;

import com.productivecows.ProductiveCows;
import com.productivecows.block.entity.GeneratorFuel;
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
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Fuels of the Milk Generator and their energy: pressing U on a fluid lands here. */
public class GeneratorFuelCategory implements IRecipeCategory<GeneratorFuelRecipe> {

    public static final RecipeType<GeneratorFuelRecipe> FUEL_TYPE = RecipeType.create(ProductiveCows.MODID, "generator_fuel", GeneratorFuelRecipe.class);
    private static final int SHOWN_AMOUNT = 1000;

    private final IDrawable icon;

    public GeneratorFuelCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ProductiveCows.MILK_GENERATOR_ITEM.get()));
    }

    @Override
    public RecipeType<GeneratorFuelRecipe> getRecipeType() {
        return FUEL_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.productivecows.generator_fuel");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 124;
    }

    @Override
    public int getHeight() {
        return 34;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GeneratorFuelRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 8, 9)
                .addFluidStack(recipe.fluid(), SHOWN_AMOUNT)
                .setFluidRenderer(SHOWN_AMOUNT, false, 16, 16)
                .setStandardSlotBackground();
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, GeneratorFuelRecipe recipe, IFocusGroup focuses) {
        builder.addRecipeArrow().setPosition(30, 9);
        List<FormattedText> lines = List.of(
                Component.translatable("jei.productivecows.fuel_energy", GeneratorFuel.rfPerMb(recipe.tier())),
                Component.translatable("jei.productivecows.fuel_rate", GeneratorFuel.rfPerTick(recipe.tier())));
        builder.addText(lines, 60, 7, 64, 20)
                .setColor(JeiStacks.TEXT_COLOR);
    }
}
