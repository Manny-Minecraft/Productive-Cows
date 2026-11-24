package com.productivecows.block.entity;

import com.productivecows.Config;
import com.productivecows.ProductiveCows;
import com.productivecows.block.cooler.CoolerControllerBlock;
import com.productivecows.block.cooler.CoolerPartBlock;
import com.productivecows.data.CowDefinition;
import com.productivecows.menu.CoolerControllerMenu;
import com.productivecows.menu.SplitInt;
import com.productivecows.multiblock.CoolerPart;
import com.productivecows.multiblock.CoolerStructure;
import com.productivecows.multiblock.MultiFluidStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Brain of the Cooler tower. Rechecks the structure every second and switches the {@code formed} state of its
 * blocks, binds the Ports, and holds the shared fluid storage (one fluid type and {@code coolerTankCapacity} mB
 * per Tank), the energy buffer and the output buffer. Cooling itself arrives in milestone M4.
 */
public class CoolerControllerBlockEntity extends BlockEntity implements MenuProvider {
    private static final int CHECK_INTERVAL = 20;
    public static final int OUTPUT_SLOTS = 9;
    /** Fluid types the GUI can show: the structure allows at most this many Tanks. */
    public static final int MAX_FLUIDS = CoolerStructure.MAX_TANKS;

    /** Blocks this Controller has marked as formed; saved so they can be released after a reload. */
    private final List<BlockPos> formedParts = new ArrayList<>();
    private CoolerStructure.Result structure;

    private final MultiFluidStorage<Fluid> fluids = new MultiFluidStorage<>();

    private final EnergyBuffer energy = new EnergyBuffer(Math.max(1000, Config.coolerEnergyCapacity));

    /** Receives up to 10000 RF/t from cables; {@link #set} restores a saved value without that limit. */
    private final class EnergyBuffer extends EnergyStorage {
        EnergyBuffer(int capacity) {
            super(capacity, 10000, 0);
        }

        void set(int value) {
            energy = Math.max(0, Math.min(capacity, value));
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int received = super.receiveEnergy(toReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
            }
            return received;
        }
    }

    /** Cooled items waiting to leave through an output Port or the GUI. Nothing can be inserted from outside. */
    private final ItemStackHandler output = new ItemStackHandler(OUTPUT_SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final IFluidHandler fluidInput = new FluidInput();
    private final IEnergyStorage energyInput = new EnergyInput();
    private final IItemHandler itemOutput = new AutomationItemHandler(output, 0, 1, 2, 3, 4, 5, 6, 7, 8);

    public CoolerControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ProductiveCows.COOLER_CONTROLLER_BE.get(), pos, state);
    }

    public boolean isFormed() {
        return structure != null && structure.formed();
    }

    @Nullable
    public CoolerStructure.Result structure() {
        return structure;
    }

    public IFluidHandler getFluidInput() {
        return fluidInput;
    }

    public IEnergyStorage getEnergyInput() {
        return energyInput;
    }

    public IItemHandler getItemOutput() {
        return itemOutput;
    }

    public ItemStackHandler getOutputInventory() {
        return output;
    }

    public void serverTick() {
        // Spread Controllers over the interval instead of checking them all on the same tick. A tower crossing a
        // chunk that isn't loaded can't be judged: keep the current state until it loads
        if (Math.floorMod(level.getGameTime() + worldPosition.asLong(), CHECK_INTERVAL) == 0
                && level.isAreaLoaded(worldPosition, 2)) {
            recheck();
        }
    }

    // ------------------------------------------------------------------ structure

    /** Checks the tower now and updates the formed blocks; returns the outcome. */
    public CoolerStructure.Result recheck() {
        CoolerStructure.Result result = CoolerStructure.check(worldPosition, facing(),
                pos -> CoolerPartBlock.partOf(level.getBlockState(pos)));
        apply(result);
        return result;
    }

    /** Releases every block of the tower, e.g. when the Controller is broken. */
    public void unform() {
        for (BlockPos pos : formedParts) {
            setFormed(pos, false);
            bindPort(pos, null);
        }
        formedParts.clear();
        structure = null;
        setChanged();
    }

    private void apply(CoolerStructure.Result result) {
        Set<BlockPos> next = result.formed() ? new HashSet<>(result.parts()) : Set.of();
        boolean changed = false;
        for (BlockPos pos : formedParts) {
            if (!next.contains(pos)) {
                setFormed(pos, false);
                bindPort(pos, null);
                changed = true;
            }
        }
        for (BlockPos pos : next) {
            changed |= setFormed(pos, true);
        }
        for (BlockPos pos : result.ports()) {
            bindPort(pos, worldPosition);
        }
        if (changed || next.size() != formedParts.size()) {
            formedParts.clear();
            formedParts.addAll(next);
            setChanged();
        }
        if (result.formed()) {
            fluids.setLimits(result.tanks() * Config.coolerTankCapacity, result.tanks());
        }
        structure = result;
    }

    private boolean setFormed(BlockPos pos, boolean formed) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(CoolerPartBlock.FORMED) && state.getValue(CoolerPartBlock.FORMED) != formed) {
            level.setBlock(pos, state.setValue(CoolerPartBlock.FORMED, formed), Block.UPDATE_CLIENTS);
            return true;
        }
        return false;
    }

    private void bindPort(BlockPos pos, @Nullable BlockPos controller) {
        if (level.getBlockEntity(pos) instanceof CoolerPortBlockEntity port) {
            port.bind(controller);
        }
    }

    private net.minecraft.core.Direction facing() {
        return getBlockState().getValue(CoolerControllerBlock.FACING);
    }

    // ------------------------------------------------------------------ fluids

    /** Fluids a cooler can turn into something: those of a cow with a cooling result. */
    private boolean isCoolable(Fluid fluid) {
        ResourceLocation key = BuiltInRegistries.FLUID.getKey(fluid);
        for (CowDefinition def : ProductiveCows.getDefinitions(level).values()) {
            if (def.fluidId().equals(key) && (def.coolingResult() != null || def.coolingResultTag() != null)) {
                return true;
            }
        }
        return false;
    }

    public Map<Fluid, Integer> fluidContents() {
        return fluids.contents();
    }

    /** Fluid and energy enter through the input Ports; nothing can be pulled back out. */
    private final class FluidInput implements IFluidHandler {
        @Override
        public int getTanks() {
            return Math.max(1, fluids.maxTypes());
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            int i = 0;
            for (Map.Entry<Fluid, Integer> entry : fluids.contents().entrySet()) {
                if (i++ == tank) {
                    return new FluidStack(entry.getKey(), entry.getValue());
                }
            }
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return fluids.capacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return !stack.isEmpty() && isCoolable(stack.getFluid());
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !isFormed() || !isCoolable(resource.getFluid())) {
                return 0;
            }
            int filled = fluids.fill(resource.getFluid(), resource.getAmount(), action.simulate());
            if (filled > 0 && action.execute()) {
                setChanged();
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }

    private final class EnergyInput implements IEnergyStorage {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return isFormed() ? energy.receiveEnergy(toReceive, simulate) : 0;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return energy.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return energy.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return isFormed();
        }
    }

    // ------------------------------------------------------------------ menu

    /** Values the GUI needs; ints above 32767 travel as two halves (see SplitInt). */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            CoolerStructure.Result s = structure;
            return switch (index) {
                case CoolerControllerMenu.ENERGY -> SplitInt.low(energy.getEnergyStored());
                case CoolerControllerMenu.ENERGY + 1 -> SplitInt.high(energy.getEnergyStored());
                case CoolerControllerMenu.MAX_ENERGY -> SplitInt.low(energy.getMaxEnergyStored());
                case CoolerControllerMenu.MAX_ENERGY + 1 -> SplitInt.high(energy.getMaxEnergyStored());
                case CoolerControllerMenu.CAPACITY -> SplitInt.low(fluids.capacity());
                case CoolerControllerMenu.CAPACITY + 1 -> SplitInt.high(fluids.capacity());
                case CoolerControllerMenu.TANKS -> s != null && s.formed() ? s.tanks() : 0;
                case CoolerControllerMenu.STAMPS -> s != null && s.formed() ? s.stamps() : 0;
                case CoolerControllerMenu.HEIGHT -> s != null && s.formed() ? s.height() : 0;
                default -> fluidData(index - CoolerControllerMenu.FLUIDS);
            };
        }

        private int fluidData(int i) {
            if (i < 0 || i >= MAX_FLUIDS * 3) {
                return 0;
            }
            int slot = i / 3;
            int n = 0;
            for (Map.Entry<Fluid, Integer> entry : fluids.contents().entrySet()) {
                if (n++ == slot) {
                    return switch (i % 3) {
                        case 0 -> BuiltInRegistries.FLUID.getId(entry.getKey());
                        case 1 -> SplitInt.low(entry.getValue());
                        default -> SplitInt.high(entry.getValue());
                    };
                }
            }
            // Empty slot: the id of the empty fluid, amount 0
            return i % 3 == 0 ? BuiltInRegistries.FLUID.getId(Fluids.EMPTY) : 0;
        }

        @Override
        public void set(int index, int value) {
            // Only the client's copy of the data is written
        }

        @Override
        public int getCount() {
            return CoolerControllerMenu.DATA_COUNT;
        }
    };

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.productivecows.cooler_controller");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CoolerControllerMenu(id, inventory, this, data);
    }

    // ------------------------------------------------------------------ status message

    /** The message shown above the hotbar when the Controller is right-clicked. */
    public Component describe(CoolerStructure.Result result) {
        String key = "message.productivecows.cooler.";
        if (result.formed()) {
            int tanks = result.tanks();
            return Component.translatable(key + "formed", result.height(),
                    result.stamps(), result.stamps() * CoolerStructure.INGOTS_PER_STAMP,
                    tanks, tanks, (long) tanks * Config.coolerTankCapacity);
        }
        BlockPos at = result.problemPos();
        return switch (result.problem()) {
            case WRONG_BLOCK -> Component.translatable(key + "wrong_block", at.getX(), at.getY(), at.getZ(), partNames(result.expected()));
            case EXTRA_CONTROLLER -> Component.translatable(key + "extra_controller", at.getX(), at.getY(), at.getZ());
            case TOO_TALL -> Component.translatable(key + "too_tall", result.count(), CoolerStructure.MAX_HEIGHT);
            case TOO_MANY_TANKS -> Component.translatable(key + "too_many_tanks", result.count(), CoolerStructure.MAX_TANKS);
            case NO_BOTTOM -> Component.translatable(key + "no_bottom", CoolerStructure.MAX_HEIGHT);
            case NO_TOP -> Component.translatable(key + "no_top", CoolerStructure.MAX_HEIGHT);
            case CONTROLLER_FACING -> Component.translatable(key + "controller_facing");
            case NO_TANK -> Component.translatable(key + "no_tank");
            case NO_PORT -> Component.translatable(key + "no_port");
            case NO_STAMP -> Component.translatable(key + "no_stamp");
        };
    }

    private static Component partNames(Set<CoolerPart> parts) {
        MutableComponent names = Component.empty();
        boolean first = true;
        for (CoolerPart part : parts) {
            if (!first) {
                names.append(" / ");
            }
            names.append(partName(part));
            first = false;
        }
        return names;
    }

    private static Component partName(CoolerPart part) {
        return switch (part) {
            case CASING -> ProductiveCows.COOLER_BLOCK.get().getName();
            case TANK -> ProductiveCows.COOLER_TANK.get().getName();
            case PORT -> ProductiveCows.COOLER_PORT.get().getName();
            case CONTROLLER -> ProductiveCows.COOLER_CONTROLLER.get().getName();
            case STAMP -> ProductiveCows.COOLER_STAMP.get().getName();
            case EMPTY, OTHER -> Component.translatable("message.productivecows.cooler.air");
        };
    }

    // ------------------------------------------------------------------ save

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("FormedParts", new LongArrayTag(formedParts.stream().mapToLong(BlockPos::asLong).toArray()));
        ListTag fluidList = new ListTag();
        for (Map.Entry<Fluid, Integer> entry : fluids.contents().entrySet()) {
            CompoundTag fluidTag = new CompoundTag();
            fluidTag.putString("Fluid", BuiltInRegistries.FLUID.getKey(entry.getKey()).toString());
            fluidTag.putInt("Amount", entry.getValue());
            fluidList.add(fluidTag);
        }
        tag.put("Fluids", fluidList);
        tag.putInt("FluidCapacity", fluids.capacity());
        tag.putInt("FluidTypes", fluids.maxTypes());
        tag.putInt("Energy", energy.getEnergyStored());
        tag.put("Output", output.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        formedParts.clear();
        for (long packed : tag.getLongArray("FormedParts")) {
            formedParts.add(BlockPos.of(packed));
        }
        fluids.clear();
        fluids.setLimits(tag.getInt("FluidCapacity"), tag.getInt("FluidTypes"));
        for (Tag element : tag.getList("Fluids", Tag.TAG_COMPOUND)) {
            CompoundTag fluidTag = (CompoundTag) element;
            ResourceLocation id = ResourceLocation.tryParse(fluidTag.getString("Fluid"));
            Fluid fluid = id == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(id);
            // A fluid whose mod was removed is dropped instead of crashing the load
            if (fluid != Fluids.EMPTY) {
                fluids.restore(fluid, fluidTag.getInt("Amount"));
            }
        }
        energy.set(tag.getInt("Energy"));
        if (tag.contains("Output")) {
            output.deserializeNBT(registries, tag.getCompound("Output"));
        }
    }
}
