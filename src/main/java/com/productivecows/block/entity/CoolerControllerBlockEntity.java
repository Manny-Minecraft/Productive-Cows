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
import com.productivecows.multiblock.StampPlanner;
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
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Brain of the Cooler tower. Rechecks the structure every second and switches the {@code formed} state of its
 * blocks, binds the Ports, and holds the shared fluid storage (one fluid type and {@code coolerTankCapacity} mB
 * per Tank), the energy buffer and the output buffer.
 * <p>
 * Cooling runs in cycles: at the start of a cycle {@link StampPlanner} shares the Stamps out among the stored
 * fluids (2 ingots per Stamp, 1000 mB per ingot); while the cycle runs every busy Stamp costs
 * {@code coolerEnergyPerStamp} RF/t, and without enough energy the cycle pauses. At the end the fluid is drained
 * and the ingots go to the output buffer, which output Ports empty into the inventories next to them.
 */
public class CoolerControllerBlockEntity extends BlockEntity implements MenuProvider {
    private static final int CHECK_INTERVAL = 20;
    public static final int OUTPUT_SLOTS = 9;
    /** mB one ingot costs, as in the other coolers. */
    public static final int FLUID_PER_INGOT = 1000;
    private static final int PUSH_INTERVAL = 10;
    /** Fluid types the GUI can show: the structure allows at most this many Tanks. */
    public static final int MAX_FLUIDS = CoolerStructure.MAX_TANKS;

    /** Blocks this Controller has marked as formed; saved so they can be released after a reload. */
    private final List<BlockPos> formedParts = new ArrayList<>();
    private CoolerStructure.Result structure;

    private final MultiFluidStorage<Fluid> fluids = new MultiFluidStorage<>();

    // Current cycle: ingots per fluid, busy Stamps and progress. Not saved: the fluid is only drained when a cycle
    // ends, so after a reload the cycle simply starts again
    private Map<Fluid, Integer> cycle = Map.of();
    private int activeStamps;
    private int progress;
    private int rotation;
    /** Cooling result per fluid, refreshed with the structure check (cow definitions can be reloaded). */
    private final Map<Fluid, ItemStack> results = new HashMap<>();

    private final EnergyBuffer energy = new EnergyBuffer(Math.max(1000, Config.coolerEnergyCapacity));

    /** Receives up to 10000 RF/t from cables; {@link #set} restores a saved value without that limit. */
    private final class EnergyBuffer extends EnergyStorage {
        EnergyBuffer(int capacity) {
            super(capacity, 10000, 0);
        }

        void set(int value) {
            energy = Math.max(0, Math.min(capacity, value));
        }

        void consume(int value) {
            energy = Math.max(0, energy - value);
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

    /** Pours a fluid container the player holds into the tower; false if nothing went in. */
    public boolean pourFrom(Player player, net.minecraft.world.InteractionHand hand) {
        return isFormed() && net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(player, hand, fluidInput);
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

    public int fluidCapacity() {
        return fluids.capacity();
    }

    public int progress() {
        return progress;
    }

    public int cycleTicks() {
        return Math.max(1, Config.coolerCycleTicks);
    }

    public void serverTick() {
        // Spread Controllers over the interval instead of checking them all on the same tick. A tower crossing a
        // chunk that isn't loaded can't be judged: keep the current state until it loads
        if (Math.floorMod(level.getGameTime() + worldPosition.asLong(), CHECK_INTERVAL) == 0
                && level.isAreaLoaded(worldPosition, 2)) {
            recheck();
        }
        if (!isFormed()) {
            resetCycle();
            return;
        }
        tickCooling();
        if (Math.floorMod(level.getGameTime() + worldPosition.asLong(), PUSH_INTERVAL) == 0) {
            pushOutput();
        }
    }

    // ------------------------------------------------------------------ cooling

    private void tickCooling() {
        if (cycle.isEmpty()) {
            StampPlanner.Plan<Fluid> plan = StampPlanner.plan(fluids.contents(), fluid -> !coolingResult(fluid).isEmpty(),
                    structure.stamps(), CoolerStructure.INGOTS_PER_STAMP, FLUID_PER_INGOT, rotation);
            if (plan.isEmpty() || !outputFits(plan.ingots())) {
                return;
            }
            cycle = plan.ingots();
            activeStamps = plan.activeStamps();
            progress = 0;
            rotation++;
        }
        int cost = activeStamps * Math.max(0, Config.coolerEnergyPerStamp);
        if (energy.getEnergyStored() < cost) {
            return; // paused until there is power again
        }
        energy.consume(cost);
        if (++progress >= cycleTicks()) {
            finishCycle();
        }
        setChanged();
    }

    private void finishCycle() {
        for (Map.Entry<Fluid, Integer> entry : cycle.entrySet()) {
            Fluid fluid = entry.getKey();
            // The fluid may have changed during the cycle: cool only what is still there
            int ingots = Math.min(entry.getValue(), fluids.amount(fluid) / FLUID_PER_INGOT);
            ItemStack result = coolingResult(fluid);
            if (ingots > 0 && !result.isEmpty()) {
                fluids.drain(fluid, ingots * FLUID_PER_INGOT, false);
                insertOutput(result.copyWithCount(ingots), false);
            }
        }
        resetCycle();
    }

    private void resetCycle() {
        if (!cycle.isEmpty() || progress != 0) {
            cycle = Map.of();
            activeStamps = 0;
            progress = 0;
            setChanged();
        }
    }

    public int activeStamps() {
        return activeStamps;
    }

    public int cycleIngots() {
        return cycle.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** What a fluid cools into, like the other coolers: the cow's item, or the first item of its tag. */
    private ItemStack coolingResult(Fluid fluid) {
        return results.computeIfAbsent(fluid, f -> {
            ResourceLocation key = BuiltInRegistries.FLUID.getKey(f);
            for (CowDefinition def : ProductiveCows.getDefinitions(level).values()) {
                if (!def.fluidId().equals(key)) {
                    continue;
                }
                if (def.coolingResult() != null) {
                    return new ItemStack(BuiltInRegistries.ITEM.get(def.coolingResult()));
                }
                ResourceLocation tagId = def.coolingResultTag() == null ? null : ResourceLocation.tryParse(def.coolingResultTag());
                if (tagId != null) {
                    var tag = BuiltInRegistries.ITEM.getTag(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, tagId));
                    if (tag.isPresent() && tag.get().size() > 0) {
                        return new ItemStack(tag.get().get(0).value());
                    }
                }
            }
            return ItemStack.EMPTY;
        });
    }

    /** Whether every result of a planned cycle fits in the output buffer. */
    private boolean outputFits(Map<Fluid, Integer> ingots) {
        List<ItemStack> slots = new ArrayList<>();
        for (int i = 0; i < output.getSlots(); i++) {
            slots.add(output.getStackInSlot(i).copy());
        }
        for (Map.Entry<Fluid, Integer> entry : ingots.entrySet()) {
            if (!merge(slots, coolingResult(entry.getKey()).copyWithCount(entry.getValue())).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Puts items in the output buffer (which refuses them from outside); returns what didn't fit. */
    private ItemStack insertOutput(ItemStack stack, boolean simulate) {
        List<ItemStack> slots = new ArrayList<>();
        for (int i = 0; i < output.getSlots(); i++) {
            slots.add(output.getStackInSlot(i).copy());
        }
        ItemStack rest = merge(slots, stack);
        if (!simulate) {
            for (int i = 0; i < slots.size(); i++) {
                if (!ItemStack.matches(slots.get(i), output.getStackInSlot(i))) {
                    output.setStackInSlot(i, slots.get(i));
                }
            }
        }
        return rest;
    }

    private static ItemStack merge(List<ItemStack> slots, ItemStack stack) {
        ItemStack rest = stack.copy();
        for (int pass = 0; pass < 2 && !rest.isEmpty(); pass++) {
            for (int i = 0; i < slots.size() && !rest.isEmpty(); i++) {
                ItemStack slot = slots.get(i);
                if (pass == 0 && !slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, rest)) {
                    int moved = Math.min(rest.getCount(), slot.getMaxStackSize() - slot.getCount());
                    slot.grow(moved);
                    rest.shrink(moved);
                } else if (pass == 1 && slot.isEmpty()) {
                    slots.set(i, rest.copy());
                    rest = ItemStack.EMPTY;
                }
            }
        }
        return rest;
    }

    /** Output Ports hand the buffer's items to the inventories next to them (not to the tower itself). */
    private void pushOutput() {
        for (BlockPos portPos : structure.ports()) {
            BlockState state = level.getBlockState(portPos);
            if (!state.hasProperty(com.productivecows.block.cooler.CoolerPortBlock.MODE)
                    || state.getValue(com.productivecows.block.cooler.CoolerPortBlock.MODE) != com.productivecows.block.cooler.CoolerPortBlock.Mode.OUTPUT) {
                continue;
            }
            for (net.minecraft.core.Direction side : net.minecraft.core.Direction.values()) {
                BlockPos target = portPos.relative(side);
                if (!level.isLoaded(target) || level.getBlockState(target).getBlock() instanceof CoolerPartBlock) {
                    continue;
                }
                IItemHandler inventory = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, target, side.getOpposite());
                if (inventory == null) {
                    continue;
                }
                for (int i = 0; i < output.getSlots(); i++) {
                    ItemStack stack = output.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        ItemStack rest = ItemHandlerHelper.insertItem(inventory, stack.copy(), false);
                        if (rest.getCount() != stack.getCount()) {
                            output.setStackInSlot(i, rest);
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ structure

    /** Checks the tower now and updates the formed blocks; returns the outcome. */
    public CoolerStructure.Result recheck() {
        results.clear();
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
                case CoolerControllerMenu.PROGRESS -> progress;
                case CoolerControllerMenu.CYCLE_TICKS -> cycleTicks();
                case CoolerControllerMenu.ACTIVE_STAMPS -> activeStamps;
                case CoolerControllerMenu.CYCLE_INGOTS -> cycleIngots();
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
