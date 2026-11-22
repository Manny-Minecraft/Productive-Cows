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

/** What a Cow Cage produces: captured cow -> fluid per milking cycle. Pressing R on a fluid lands here. */
public class MilkingRecipeCategory implements IRecipeCategory<MilkingRecipe> {

    public static final RecipeType<MilkingRecipe> MILKING_TYPE = RecipeType.create(ProductiveCows.MODID, "milking", MilkingRecipe.class);

    private final IDrawable icon;

    public MilkingRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ProductiveCows.COW_CAGE_ITEM.get()));
    }

    @Override
    public RecipeType<MilkingRecipe> getRecipeType() {
        return MILKING_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.productivecows.milking");
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
    public void setRecipe(IRecipeLayoutBuilder builder, MilkingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 8, 9)
                .addItemStack(recipe.cow())
                .setStandardSlotBackground();
        if (!recipe.spawnEgg().isEmpty()) {
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(recipe.spawnEgg());
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 72, 9)
                .addFluidStack(recipe.fluid(), recipe.amount())
                .setFluidRenderer(recipe.amount(), false, 16, 16)
                .setOutputSlotBackground();
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, MilkingRecipe recipe, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrow(MilkingRecipe.CYCLE_TICKS).setPosition(36, 9);
        builder.addText(Component.translatable("jei.productivecows.milking_cycle", recipe.amount(),
                        CoolerRecipeCategory.seconds(MilkingRecipe.CYCLE_TICKS)), 0, 32, getWidth(), 10)
                .alignHorizontalCenter()
                .setColor(JeiStacks.TEXT_COLOR);
    }
}
