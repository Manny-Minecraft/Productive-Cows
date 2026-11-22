package com.productivecows.menu;

import com.productivecows.ProductiveCows;
import com.productivecows.block.entity.MilkGeneratorBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class MilkGeneratorMenu extends AbstractContainerMenu {

    private final BlockEntity blockEntity;
    private final ContainerData data;

    // Client Constructor
    public MilkGeneratorMenu(int windowId, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(windowId, inv, inv.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(8));
    }

    // Common/Server Constructor
    public MilkGeneratorMenu(int windowId, Inventory inv, BlockEntity be, ContainerData data) {
        super(ProductiveCows.MILK_GENERATOR_MENU.get(), windowId);
        checkContainerDataCount(data, 8);
        this.blockEntity = be;
        this.data = data;

        if (be instanceof MilkGeneratorBlockEntity generator) {
            // Slot 0: fuel bucket input, validated by the block entity's handler. X=94, Y=16
            this.addSlot(new SlotItemHandler(generator.getInventory(), 0, 94, 16));
            // Slot 1: Output empty bucket. X=94, Y=52
            this.addSlot(new SlotItemHandler(generator.getInventory(), 1, 94, 52) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }

        // Player Inventory (27 slots)
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        // Player Hotbar (9 slots)
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(inv, k, 8 + k * 18, 142));
        }

        this.addDataSlots(data);
    }

    public BlockEntity getBlockEntity() {
        return blockEntity;
    }

    public int getEnergyStored() {
        return SplitInt.join(data.get(0), data.get(4));
    }

    public int getMaxEnergyStored() {
        return SplitInt.join(data.get(1), data.get(5));
    }

    /** RF the generator produced in the last tick. */
    public int getProductionRate() {
        return data.get(6);
    }

    /** The fluid in the tank, sent as its registry id. */
    public net.minecraft.world.level.material.Fluid getFluid() {
        return net.minecraft.core.registries.BuiltInRegistries.FLUID.byId(data.get(7));
    }

    public int getFluidAmount() {
        return data.get(2);
    }

    public int getFluidCapacity() {
        return data.get(3);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(net.minecraft.world.inventory.ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), player, blockEntity.getBlockState().getBlock());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 2) {
                // BE slots -> Player inventory
                if (!this.moveItemStackTo(itemstack1, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Player inventory -> BE slots
                if (itemstack1.is(Items.MILK_BUCKET) || itemstack1.is(com.productivecows.fluid.ModFluids.LIQUID_MILK.bucket().get())) {
                    if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // Hotbar <-> main inventory transfer
                    if (index < 29) {
                        if (!this.moveItemStackTo(itemstack1, 29, 38, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(itemstack1, 2, 29, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }
}
