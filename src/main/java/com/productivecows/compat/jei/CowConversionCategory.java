package com.productivecows.compat.jei;

import com.productivecows.ProductiveCows;
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
import net.minecraft.world.item.Items;

/** Turning a vanilla cow into a material cow by right-clicking it with an item. */
public class CowConversionCategory implements IRecipeCategory<CowConversionRecipe> {

    public static final RecipeType<CowConversionRecipe> CONVERSION_TYPE = RecipeType.create(ProductiveCows.MODID, "cow_conversion", CowConversionRecipe.class);

    private final IDrawable icon;

    public CowConversionCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Items.COW_SPAWN_EGG));
    }

    @Override
    public RecipeType<CowConversionRecipe> getRecipeType() {
        return CONVERSION_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.productivecows.conversion");
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
        return 42;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CowConversionRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 4, 15)
                .addItemStack(new ItemStack(Items.COW_SPAWN_EGG))
                .setStandardSlotBackground();
        builder.addSlot(RecipeIngredientRole.CATALYST, 40, 15)
                .addItemStacks(recipe.catalysts())
                .setStandardSlotBackground();
        builder.addSlot(RecipeIngredientRole.OUTPUT, 100, 15)
                .addItemStack(JeiStacks.capturedCow(recipe.resultCowId()))
                .setOutputSlotBackground();
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(JeiStacks.spawnEgg(recipe.resultCowId()));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, CowConversionRecipe recipe, IFocusGroup focuses) {
        builder.addRecipePlusSign().setPosition(24, 16);
        builder.addRecipeArrow().setPosition(66, 15);
        builder.addText(Component.translatable("jei.productivecows.right_click"), 22, 2, 40, 10)
                .alignHorizontalCenter()
                .setColor(JeiStacks.TEXT_COLOR);
    }
}
