package com.productivecows.data.provider;

import com.productivecows.ProductiveCows;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Set;

/**
 * Every machine simply drops itself. Fluid blocks are registered with noLootTable() and are skipped.
 */
public class ModBlockLootProvider extends BlockLootSubProvider {

    public ModBlockLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        for (Block block : getKnownBlocks()) {
            dropSelf(block);
        }
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return List.of(
                ProductiveCows.BREEDING_CAGE.get(),
                ProductiveCows.COW_CAGE.get(),
                ProductiveCows.LIQUID_COOLER.get(),
                ProductiveCows.MILK_GENERATOR.get(),
                ProductiveCows.ADVANCED_COOLER.get()
        );
    }
}
