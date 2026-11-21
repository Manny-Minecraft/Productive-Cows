package com.productivecows.item;

import com.productivecows.ProductiveCows;
import com.productivecows.data.CowDefinition;
import com.productivecows.entity.MaterialCowEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class CapturedCowItem extends Item {

    /** Game time (ticks) until which the captured cow cannot breed in a Breeding Cage. */
    public static final String BREEDING_COOLDOWN_UNTIL = "BreedingCooldownUntil";
    /** Old countdown that was never decremented: read as expired and removed by the Breeding Cage. */
    public static final String LEGACY_BREEDING_COOLDOWN = "BreedingCooldown";

    /** Vanilla AgeableMob age: negative for calves, positive while an adult can't breed. Saved only when not 0. */
    public static final String AGE = "Age";

    public CapturedCowItem(Properties properties) {
        super(properties);
    }

    public static boolean isCalf(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.ENTITY_DATA);
        return customData != null && customData.getUnsafe().getInt(AGE) < 0;
    }

    /** Writes the cow's age into captured cow data; adults with age 0 keep no key so they stay stackable. */
    public static void saveAge(CompoundTag entityData, net.minecraft.world.entity.AgeableMob mob) {
        if (mob.getAge() != 0) {
            entityData.putInt(AGE, mob.getAge());
        }
    }

    /** Remaining breeding cooldown in ticks, 0 when the cow can breed. */
    public static long getBreedingCooldownRemaining(ItemStack stack, long gameTime) {
        CustomData customData = stack.get(DataComponents.ENTITY_DATA);
        if (customData == null || !customData.contains(BREEDING_COOLDOWN_UNTIL)) {
            return 0;
        }
        return Math.max(0, customData.getUnsafe().getLong(BREEDING_COOLDOWN_UNTIL) - gameTime);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockPos clickedPos = context.getClickedPos();
        BlockState blockState = level.getBlockState(clickedPos);
        BlockPos spawnPos = clickedPos;
        if (!blockState.getCollisionShape(level, clickedPos).isEmpty()) {
            spawnPos = clickedPos.relative(context.getClickedFace());
        }

        ItemStack stack = context.getItemInHand();
        CustomData customData = stack.get(DataComponents.ENTITY_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if ("minecraft:cow".equals(tag.getString("id"))) {
                net.minecraft.world.entity.animal.Cow cow = new net.minecraft.world.entity.animal.Cow(net.minecraft.world.entity.EntityType.COW, level);
                if (stack.has(DataComponents.CUSTOM_NAME)) {
                    cow.setCustomName(stack.get(DataComponents.CUSTOM_NAME));
                }
                cow.setAge(tag.getInt(AGE));
                cow.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, 0.0F, 0.0F);
                level.addFreshEntity(cow);
                level.playSound(null, spawnPos, SoundEvents.COW_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
                    stack.shrink(1);
                }
                return InteractionResult.CONSUME;
            }
            if (tag.contains("CowDefinitionId")) {
                String idStr = tag.getString("CowDefinitionId");
                ResourceLocation defId = ResourceLocation.tryParse(idStr);
                if (defId != null) {
                    MaterialCowEntity cow = new MaterialCowEntity(ProductiveCows.MATERIAL_COW.get(), level);
                    cow.setCowDefinitionId(defId);
                    
                    // Set milking cooldown: if none, default to 24000 ticks (20 min) to prevent exploits
                    if (tag.contains("MilkingCooldown")) {
                        cow.setMilkingCooldown(tag.getInt("MilkingCooldown"));
                    } else if (tag.contains("milkingCooldown")) {
                        cow.setMilkingCooldown(tag.getInt("milkingCooldown"));
                    } else {
                        cow.setMilkingCooldown(24000);
                    }

                    if (stack.has(DataComponents.CUSTOM_NAME)) {
                        cow.setCustomName(stack.get(DataComponents.CUSTOM_NAME));
                    }
                    cow.setAge(tag.getInt(AGE));

                    cow.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, 0.0F, 0.0F);
                    level.addFreshEntity(cow);

                    level.playSound(null, spawnPos, SoundEvents.COW_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F);

                    if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    return InteractionResult.CONSUME;
                }
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CustomData customData = stack.get(DataComponents.ENTITY_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if ("minecraft:cow".equals(tag.getString("id"))) {
                tooltip.add(Component.translatable("entity.minecraft.cow").withStyle(ChatFormatting.GRAY));
                if (tag.getInt(AGE) < 0) {
                    tooltip.add(Component.translatable("tooltip.productivecows.captured_cow.calf").withStyle(ChatFormatting.AQUA));
                }
                return;
            }
            if (tag.contains("CowDefinitionId")) {
                String idStr = tag.getString("CowDefinitionId");
                ResourceLocation defId = ResourceLocation.tryParse(idStr);
                if (defId != null) {
                    // Check definition (both on client and server depending on logical side, 
                    // ClientCowData handles client side definitions perfectly!)
                    CowDefinition def = com.productivecows.network.ClientCowData.getDefinition(defId);
                    if (def != null) {
                        tooltip.add(Component.translatable("tooltip.productivecows.captured_cow.tier", def.tier())
                                .withStyle(ChatFormatting.GOLD));
                    } else {
                        tooltip.add(Component.translatable("tooltip.productivecows.captured_cow.unknown", defId.getPath()).withStyle(ChatFormatting.RED));
                    }
                    if (tag.getInt(AGE) < 0) {
                        tooltip.add(Component.translatable("tooltip.productivecows.captured_cow.calf").withStyle(ChatFormatting.AQUA));
                    }
                    
                    int cooldown = 0;
                    if (tag.contains("MilkingCooldown")) {
                        cooldown = tag.getInt("MilkingCooldown");
                    } else if (tag.contains("milkingCooldown")) {
                        cooldown = tag.getInt("milkingCooldown");
                    }
                    
                    if (cooldown > 0) {
                        int minutes = cooldown / 1200;
                        int seconds = (cooldown % 1200) / 20;
                        tooltip.add(Component.translatable("tooltip.productivecows.captured_cow.cooldown", minutes, seconds)
                                .withStyle(ChatFormatting.RED));
                    } else {
                        tooltip.add(Component.translatable("tooltip.productivecows.captured_cow.ready")
                                .withStyle(ChatFormatting.GREEN));
                    }

                    Level level = context.level();
                    int breedingCooldown = level == null ? 0
                            : (int) Math.min(Integer.MAX_VALUE, getBreedingCooldownRemaining(stack, level.getGameTime()));
                    if (breedingCooldown > 0) {
                        int minutes = breedingCooldown / 1200;
                        int seconds = (breedingCooldown % 1200) / 20;
                        tooltip.add(Component.translatable("tooltip.productivecows.captured_cow.breeding_cooldown", minutes, seconds)
                                .withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                    return;
                }
            }
        }
        tooltip.add(Component.translatable("tooltip.productivecows.captured_cow.empty").withStyle(ChatFormatting.DARK_RED));
    }

    @Override
    public Component getName(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.ENTITY_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if ("minecraft:cow".equals(tag.getString("id"))) {
                return Component.translatable("entity.minecraft.cow");
            }
            if (tag.contains("CowDefinitionId")) {
                String idStr = tag.getString("CowDefinitionId");
                ResourceLocation defId = ResourceLocation.tryParse(idStr);
                if (defId != null) {
                    CowDefinition def = ProductiveCows.getDisplayDefinition(defId);
                    if (def != null) {
                        return Component.literal(def.name());
                    }
                }
            }
        }
        return super.getName(stack);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.neoforged.neoforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.productivecows.client.renderer.CapturedCowItemRenderer.getInstance();
            }
        });
    }
}
