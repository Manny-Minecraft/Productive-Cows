package com.productivecows.block.entity;

import com.productivecows.ProductiveCows;
import com.productivecows.block.cooler.CoolerPortBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Remembers which Controller a Port belongs to and forwards that Controller's handlers to pipes and cables:
 * fluids and energy in INPUT mode, items in OUTPUT mode, nothing while the tower isn't formed.
 */
public class CoolerPortBlockEntity extends BlockEntity {
    @Nullable
    private BlockPos controllerPos;

    public CoolerPortBlockEntity(BlockPos pos, BlockState state) {
        super(ProductiveCows.COOLER_PORT_BE.get(), pos, state);
    }

    /** Called by the Controller when the tower forms (with its position) or breaks (with null). */
    public void bind(@Nullable BlockPos controller) {
        if (!java.util.Objects.equals(controllerPos, controller)) {
            controllerPos = controller;
            setChanged();
            if (level != null) {
                level.invalidateCapabilities(worldPosition);
            }
        }
    }

    /** The formed tower this Port belongs to, if any. */
    @Nullable
    public CoolerControllerBlockEntity boundController() {
        return controller();
    }

    @Nullable
    private CoolerControllerBlockEntity controller() {
        if (controllerPos == null || level == null || !level.isLoaded(controllerPos)) {
            return null;
        }
        return level.getBlockEntity(controllerPos) instanceof CoolerControllerBlockEntity controller && controller.isFormed()
                ? controller : null;
    }

    private CoolerPortBlock.Mode mode() {
        return getBlockState().getValue(CoolerPortBlock.MODE);
    }

    @Nullable
    public IFluidHandler getFluidHandlerCapability() {
        CoolerControllerBlockEntity controller = controller();
        return controller != null && mode() == CoolerPortBlock.Mode.INPUT ? controller.getFluidInput() : null;
    }

    @Nullable
    public IEnergyStorage getEnergyStorageCapability() {
        CoolerControllerBlockEntity controller = controller();
        return controller != null && mode() == CoolerPortBlock.Mode.INPUT ? controller.getEnergyInput() : null;
    }

    @Nullable
    public IItemHandler getItemHandlerCapability() {
        CoolerControllerBlockEntity controller = controller();
        return controller != null && mode() == CoolerPortBlock.Mode.OUTPUT ? controller.getItemOutput() : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (controllerPos != null) {
            tag.putLong("Controller", controllerPos.asLong());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        controllerPos = tag.contains("Controller") ? BlockPos.of(tag.getLong("Controller")) : null;
    }
}
