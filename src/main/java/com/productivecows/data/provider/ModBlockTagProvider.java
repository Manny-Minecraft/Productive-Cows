package com.productivecows.data.provider;

import com.productivecows.ProductiveCows;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {

    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                               @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, ProductiveCows.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // All machines need a pickaxe (requiresCorrectToolForDrops) and at least a stone one.
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
                ProductiveCows.BREEDING_CAGE.get(),
                ProductiveCows.COW_CAGE.get(),
                ProductiveCows.LIQUID_COOLER.get(),
                ProductiveCows.MILK_GENERATOR.get(),
                ProductiveCows.ADVANCED_COOLER.get()
        );
        tag(BlockTags.NEEDS_STONE_TOOL).add(
                ProductiveCows.BREEDING_CAGE.get(),
                ProductiveCows.COW_CAGE.get(),
                ProductiveCows.LIQUID_COOLER.get(),
                ProductiveCows.MILK_GENERATOR.get(),
                ProductiveCows.ADVANCED_COOLER.get()
        );
    }
}
