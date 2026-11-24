package com.productivecows.block.entity;

import com.productivecows.Config;
import com.productivecows.ProductiveCows;
import com.productivecows.block.cooler.CoolerControllerBlock;
import com.productivecows.block.cooler.CoolerPartBlock;
import com.productivecows.multiblock.CoolerPart;
import com.productivecows.multiblock.CoolerStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Keeps the Cooler tower formed: rechecks the structure every second and switches the {@code formed} state of its
 * blocks. Fluids, energy and cooling arrive in the next milestones (M3, M4).
 */
public class CoolerControllerBlockEntity extends BlockEntity {
    private static final int CHECK_INTERVAL = 20;

    /** Blocks this Controller has marked as formed; saved so they can be released after a reload. */
    private final List<BlockPos> formedParts = new ArrayList<>();
    private CoolerStructure.Result structure;

    public CoolerControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ProductiveCows.COOLER_CONTROLLER_BE.get(), pos, state);
    }

    public boolean isFormed() {
        return structure != null && structure.formed();
    }

    public void serverTick() {
        // Spread Controllers over the interval instead of checking them all on the same tick. A tower crossing a
        // chunk that isn't loaded can't be judged: keep the current state until it loads
        if (Math.floorMod(level.getGameTime() + worldPosition.asLong(), CHECK_INTERVAL) == 0
                && level.isAreaLoaded(worldPosition, 2)) {
            recheck();
        }
    }

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
                changed = true;
            }
        }
        for (BlockPos pos : next) {
            changed |= setFormed(pos, true);
        }
        if (changed || next.size() != formedParts.size()) {
            formedParts.clear();
            formedParts.addAll(next);
            setChanged();
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

    private net.minecraft.core.Direction facing() {
        return getBlockState().getValue(CoolerControllerBlock.FACING);
    }

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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("FormedParts", new LongArrayTag(formedParts.stream().mapToLong(BlockPos::asLong).toArray()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        formedParts.clear();
        for (long packed : tag.getLongArray("FormedParts")) {
            formedParts.add(BlockPos.of(packed));
        }
    }
}
