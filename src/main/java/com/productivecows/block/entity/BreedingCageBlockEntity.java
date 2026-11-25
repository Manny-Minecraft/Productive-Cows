package com.productivecows.block.entity;

import com.productivecows.ProductiveCows;
import com.productivecows.data.CowDefinition;
import com.productivecows.item.CapturedCowItem;
import com.productivecows.menu.BreedingCageMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Map;

public class BreedingCageBlockEntity extends BlockEntity implements MenuProvider {

    private final ItemStackHandler itemHandler = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0 || slot == 1) {
                // Only adults breed, as in vanilla
                return stack.is(ProductiveCows.CAPTURED_COW.get()) && !com.productivecows.item.CapturedCowItem.isCalf(stack);
            } else if (slot == 2) {
                return stack.is(Items.WHEAT);
            }
            return false; // slot 3 is output
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot == 3) return stack; // Cannot insert into output
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            // One cow per parent slot: the breeding cooldown is written on the item, so a stack would all go on cooldown
            return slot == 0 || slot == 1 ? 1 : super.getSlotLimit(slot);
        }
    };

    // Hoppers and pipes may only take the offspring; the menu uses itemHandler directly
    private final net.neoforged.neoforge.items.IItemHandler automationItemHandler = new AutomationItemHandler(itemHandler, 3);

    public net.neoforged.neoforge.items.IItemHandler getItemHandlerCapability() {
        return automationItemHandler;
    }

    private int progress = 0;
    private int maxProgress = 200; // 10 seconds

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> BreedingCageBlockEntity.this.progress;
                case 1 -> BreedingCageBlockEntity.this.maxProgress;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> BreedingCageBlockEntity.this.progress = value;
                case 1 -> BreedingCageBlockEntity.this.maxProgress = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public BreedingCageBlockEntity(BlockPos pos, BlockState blockState) {
        super(ProductiveCows.BREEDING_CAGE_BE.get(), pos, blockState);
    }

    public void serverTick(Level level, BlockPos pos, BlockState state, BreedingCageBlockEntity blockEntity) {
        if (level.isClientSide) return;

        long gameTime = level.getGameTime();
        clearExpiredCooldown(0, gameTime);
        clearExpiredCooldown(1, gameTime);

        boolean hasRecipe = hasRecipe(gameTime);
        if (hasRecipe) {
            this.maxProgress = breedingTicks();
            this.progress++;
            setChanged(level, pos, state);
            if (this.progress >= this.maxProgress) {
                craftItem(level);
            }
        } else {
            if (this.progress > 0) {
                this.progress--; // slowly decay if recipe becomes invalid
                setChanged(level, pos, state);
            }
        }
    }

    /**
     * Whether the cage can breed now. Does not roll the result: every possible outcome
     * (either parent or the mutation) must fit in the output slot, so the single roll
     * done in craftItem can always be stored.
     */
    private boolean hasRecipe(long gameTime) {
        ItemStack parent1Stack = itemHandler.getStackInSlot(0);
        ItemStack parent2Stack = itemHandler.getStackInSlot(1);
        ItemStack wheatStack = itemHandler.getStackInSlot(2);

        if (parent1Stack.isEmpty() || parent2Stack.isEmpty() || wheatStack.isEmpty()) {
            return false;
        }

        ResourceLocation def1 = getCowDef(parent1Stack);
        ResourceLocation def2 = getCowDef(parent2Stack);

        if (def1 == null || def2 == null) {
            return false;
        }

        if (CapturedCowItem.getBreedingCooldownRemaining(parent1Stack, gameTime) > 0
                || CapturedCowItem.getBreedingCooldownRemaining(parent2Stack, gameTime) > 0) {
            return false;
        }

        // Every calf has its own stats, so calves never stack: the output must be empty
        if (!itemHandler.getStackInSlot(3).isEmpty()) {
            return false;
        }
        return def1.equals(def2) || findMutation(def1, def2) != null;
    }

    /** Breeding takes 10 seconds, shortened by the parents' average Fertility. */
    private int breedingTicks() {
        double speed = (com.productivecows.genetics.CowStatsNbt.fertilityMultiplier(com.productivecows.genetics.CowStatsNbt.of(itemHandler.getStackInSlot(0)))
                + com.productivecows.genetics.CowStatsNbt.fertilityMultiplier(com.productivecows.genetics.CowStatsNbt.of(itemHandler.getStackInSlot(1)))) / 2;
        return Math.max(20, (int) Math.round(200 / speed));
    }

    /** A parent's breeding cooldown, shortened by its own Fertility. */
    private static long cooldownTicks(com.productivecows.genetics.CowStats stats) {
        return Math.round(com.productivecows.Config.breedingCooldownTicks / com.productivecows.genetics.CowStatsNbt.fertilityMultiplier(stats));
    }

    private void craftItem(Level level) {
        ItemStack parent1Stack = itemHandler.getStackInSlot(0);
        ItemStack parent2Stack = itemHandler.getStackInSlot(1);
        ResourceLocation def1 = getCowDef(parent1Stack);
        ResourceLocation def2 = getCowDef(parent2Stack);
        com.productivecows.genetics.CowStats stats1 = com.productivecows.genetics.CowStatsNbt.of(parent1Stack);
        com.productivecows.genetics.CowStats stats2 = com.productivecows.genetics.CowStatsNbt.of(parent2Stack);

        ItemStack result = getBreedingResult(def1, def2, level.getRandom());
        if (!result.isEmpty()) {
            // The calf inherits from both parents, whatever species it turns out to be
            result = com.productivecows.genetics.CowStatsNbt.with(result, com.productivecows.genetics.CowStats.breed(stats1, stats2, com.productivecows.genetics.CowStatsNbt.max(),
                    com.productivecows.Config.cowStatGrowthChance, com.productivecows.Config.cowStatMinGrowthChance, () -> level.getRandom().nextDouble()));
        }
        if (result.isEmpty() || !canInsertIntoOutput(result)) {
            return; // Nothing consumed; hasRecipe guarantees this does not happen
        }

        itemHandler.extractItem(2, 1, false); // Consume 1 wheat

        ItemStack outputSlot = itemHandler.getStackInSlot(3);
        if (outputSlot.isEmpty()) {
            itemHandler.setStackInSlot(3, result);
        } else {
            itemHandler.setStackInSlot(3, outputSlot.copyWithCount(outputSlot.getCount() + result.getCount()));
        }

        applyBreedingCooldown(0, level.getGameTime() + cooldownTicks(stats1));
        applyBreedingCooldown(1, level.getGameTime() + cooldownTicks(stats2));

        this.progress = 0;
    }

    private ResourceLocation getCowDef(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.ENTITY_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("CowDefinitionId")) {
                return com.productivecows.data.MaterialCowDataLoader.resolveId(ResourceLocation.tryParse(tag.getString("CowDefinitionId")));
            }
        }
        return null;
    }

    /**
     * Removes cooldown data that no longer applies: an expired BreedingCooldownUntil, and the
     * legacy BreedingCooldown counter (it never counted down, so old items treat it as expired).
     * Keeps captured cows stackable with fresh ones.
     */
    private void clearExpiredCooldown(int slot, long gameTime) {
        ItemStack stack = itemHandler.getStackInSlot(slot);
        CustomData customData = stack.get(DataComponents.ENTITY_DATA);
        if (customData == null) {
            return;
        }
        boolean hasLegacy = customData.contains(CapturedCowItem.LEGACY_BREEDING_COOLDOWN);
        boolean expired = customData.contains(CapturedCowItem.BREEDING_COOLDOWN_UNTIL)
                && CapturedCowItem.getBreedingCooldownRemaining(stack, gameTime) == 0;
        if (!hasLegacy && !expired) {
            return;
        }
        ItemStack updated = stack.copy();
        updated.set(DataComponents.ENTITY_DATA, customData.update(tag -> {
            tag.remove(CapturedCowItem.LEGACY_BREEDING_COOLDOWN);
            if (expired) {
                tag.remove(CapturedCowItem.BREEDING_COOLDOWN_UNTIL);
            }
        }));
        itemHandler.setStackInSlot(slot, updated);
    }

    private void applyBreedingCooldown(int slot, long cooldownUntil) {
        ItemStack updated = itemHandler.getStackInSlot(slot).copy();
        CustomData customData = updated.getOrDefault(DataComponents.ENTITY_DATA, CustomData.EMPTY);
        updated.set(DataComponents.ENTITY_DATA, customData.update(tag -> {
            tag.remove(CapturedCowItem.LEGACY_BREEDING_COOLDOWN);
            tag.putLong(CapturedCowItem.BREEDING_COOLDOWN_UNTIL, cooldownUntil);
        }));
        itemHandler.setStackInSlot(slot, updated);
    }

    private CowDefinition findMutation(ResourceLocation p1, ResourceLocation p2) {
        Map<ResourceLocation, CowDefinition> defs = ProductiveCows.getDefinitions(level);
        for (CowDefinition def : defs.values()) {
            ResourceLocation defP1 = def.parent1();
            ResourceLocation defP2 = def.parent2();
            if (defP1 != null && defP2 != null) {
                if ((defP1.equals(p1) && defP2.equals(p2)) || (defP1.equals(p2) && defP2.equals(p1))) {
                    return def;
                }
            }
        }
        return null;
    }

    private ItemStack getBreedingResult(ResourceLocation p1, ResourceLocation p2, RandomSource random) {
        if (p1.equals(p2)) {
            return createCowItem(p1);
        }

        CowDefinition mutation = findMutation(p1, p2);
        if (mutation == null) {
            return ItemStack.EMPTY; // No valid recipe
        }
        if (random.nextDouble() < mutation.breedChance()) {
            return createCowItem(mutation.id());
        }
        // Failed mutation, return one of the parents randomly
        return createCowItem(random.nextBoolean() ? p1 : p2);
    }

    private ItemStack createCowItem(ResourceLocation defId) {
        ItemStack stack = new ItemStack(ProductiveCows.CAPTURED_COW.get());
        CompoundTag entityData = new CompoundTag();
        entityData.putString("id", "productivecows:material_cow");
        entityData.putString("CowDefinitionId", defId.toString());
        stack.set(DataComponents.ENTITY_DATA, CustomData.of(entityData));
        return stack;
    }

    private boolean canInsertIntoOutput(ItemStack stack) {
        ItemStack output = itemHandler.getStackInSlot(3);
        if (output.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(output, stack)) {
            return false;
        }
        return output.getCount() + stack.getCount() <= output.getMaxStackSize();
    }

    public ItemStackHandler getInventory() {
        return itemHandler;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        progress = tag.getInt("progress");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.productivecows.breeding_cage");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new BreedingCageMenu(id, playerInventory, this, this.data);
    }
}
