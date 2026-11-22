package com.productivecows.compat.jei;

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
import net.minecraft.world.item.Items;

/** Breeding two different cows into a new one: parent + parent -> child, wheat as feed, with the mutation chance. */
public class BreedingRecipeCategory implements IRecipeCategory<CowDefinition> {

    public static final RecipeType<CowDefinition> BREEDING_TYPE = RecipeType.create(ProductiveCows.MODID, "breeding", CowDefinition.class);

    private final IDrawable icon;

    public BreedingRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ProductiveCows.BREEDING_CAGE_ITEM.get()));
    }

    @Override
    public RecipeType<CowDefinition> getRecipeType() {
        return BREEDING_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.productivecows.breeding");
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
        return 50;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CowDefinition recipe, IFocusGroup focuses) {
        // Spawn eggs are invisible ingredients so R/U work on them too
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(JeiStacks.spawnEgg(recipe.parent1()));
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(JeiStacks.spawnEgg(recipe.parent2()));
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(JeiStacks.spawnEgg(recipe.id()));

        builder.addSlot(RecipeIngredientRole.INPUT, 4, 9)
                .addItemStack(JeiStacks.capturedCow(recipe.parent1()))
                .setStandardSlotBackground();
        builder.addSlot(RecipeIngredientRole.INPUT, 40, 9)
                .addItemStack(JeiStacks.capturedCow(recipe.parent2()))
                .setStandardSlotBackground();
        builder.addSlot(RecipeIngredientRole.INPUT, 22, 31)
                .addItemStack(new ItemStack(Items.WHEAT))
                .setStandardSlotBackground();
        builder.addSlot(RecipeIngredientRole.OUTPUT, 100, 9)
                .addItemStack(JeiStacks.capturedCow(recipe.id()))
                .setOutputSlotBackground();
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, CowDefinition recipe, IFocusGroup focuses) {
        builder.addRecipePlusSign().setPosition(24, 10);
        builder.addRecipeArrow().setPosition(66, 9);
        int chance = (int) Math.round(recipe.breedChance() * 100);
        builder.addText(Component.translatable("jei.productivecows.chance", chance), 60, 31, 36, 10)
                .alignHorizontalCenter()
                .setColor(chance < 100 ? 0xFFB02E26 : JeiStacks.TEXT_COLOR);
    }
}
