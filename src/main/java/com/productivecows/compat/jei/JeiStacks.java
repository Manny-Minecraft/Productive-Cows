package com.productivecows.compat.jei;

import com.productivecows.ProductiveCows;
import com.productivecows.data.CowDefinition;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;

/** Item stacks shown by the JEI categories. */
final class JeiStacks {
    /** JEI's usual dark grey for recipe text. */
    static final int TEXT_COLOR = 0xFF555555;

    private JeiStacks() {}

    static ItemStack capturedCow(ResourceLocation defId) {
        return withCow(new ItemStack(ProductiveCows.CAPTURED_COW.get()), "productivecows:material_cow", defId);
    }

    static ItemStack capturedVanillaCow() {
        ItemStack stack = new ItemStack(ProductiveCows.CAPTURED_COW.get());
        CompoundTag entityData = new CompoundTag();
        entityData.putString("id", "minecraft:cow");
        stack.set(DataComponents.ENTITY_DATA, CustomData.of(entityData));
        return stack;
    }

    static ItemStack spawnEgg(ResourceLocation defId) {
        return withCow(new ItemStack(ProductiveCows.MATERIAL_COW_SPAWN_EGG.get()), "productivecows:material_cow", defId);
    }

    private static ItemStack withCow(ItemStack stack, String entity, ResourceLocation defId) {
        if (defId == null) {
            return ItemStack.EMPTY;
        }
        CompoundTag entityData = new CompoundTag();
        entityData.putString("id", entity);
        entityData.putString("CowDefinitionId", defId.toString());
        stack.set(DataComponents.ENTITY_DATA, CustomData.of(entityData));
        return stack;
    }

    /** The fluid a cow produces, or null when it isn't registered. */
    static Fluid fluid(CowDefinition def) {
        Fluid fluid = BuiltInRegistries.FLUID.get(def.fluidId());
        return fluid == null || fluid == Fluids.EMPTY ? null : fluid;
    }

    /** What a cooler makes from this cow's fluid: the item, or every item in the tag; empty when nothing exists. */
    static List<ItemStack> coolingOutputs(CowDefinition def) {
        List<ItemStack> stacks = new ArrayList<>();
        if (def.coolingResult() != null) {
            Item item = BuiltInRegistries.ITEM.get(def.coolingResult());
            if (item != Items.AIR) {
                stacks.add(new ItemStack(item));
            }
        } else if (def.coolingResultTag() != null) {
            ResourceLocation tagId = ResourceLocation.tryParse(def.coolingResultTag());
            if (tagId != null) {
                BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, tagId)).ifPresent(tag -> {
                    for (Holder<Item> holder : tag) {
                        stacks.add(new ItemStack(holder.value()));
                    }
                });
            }
        }
        return stacks;
    }
}
