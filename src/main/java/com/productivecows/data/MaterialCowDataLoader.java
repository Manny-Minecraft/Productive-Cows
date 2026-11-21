package com.productivecows.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MaterialCowDataLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    
    // Directory we read from: data/<namespace>/cows/
    private static final String DIRECTORY = "cows";
    
    private static Map<ResourceLocation, CowDefinition> DEFINITIONS = Collections.emptyMap();

    public MaterialCowDataLoader() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objectMap, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        Map<ResourceLocation, CowDefinition> newDefinitions = new HashMap<>();

        objectMap.forEach((location, jsonElement) -> {
            CowDefinition.CODEC.parse(JsonOps.INSTANCE, jsonElement)
                    .resultOrPartial(error -> LOGGER.error("Failed to parse material cow definition {}: {}", location, error))
                    .ifPresent(definition -> {
                        String problem = validate(definition);
                        if (problem != null) {
                            LOGGER.warn("Skipping material cow definition {}: {}", location, problem);
                        } else {
                            newDefinitions.put(location, definition.withId(location));
                        }
                    });
        });

        if (net.neoforged.fml.ModList.get().isLoaded("kubejs")) {
            com.productivecows.compat.kubejs.KubeJSCompat.fireRegisterCowsEvent(newDefinitions);
        }

        DEFINITIONS = Map.copyOf(newDefinitions);
        LOGGER.info("Successfully loaded {} material cow definitions.", DEFINITIONS.size());
    }

    /**
     * Why a definition can't be used, or null when it is valid. Shared by datapack loading and the KubeJS builder.
     */
    @org.jetbrains.annotations.Nullable
    public static String validate(CowDefinition definition) {
        if (!BuiltInRegistries.FLUID.containsKey(definition.fluidId())) {
            return "unknown fluid " + definition.fluidId();
        }
        if (definition.coolingResult() != null && !BuiltInRegistries.ITEM.containsKey(definition.coolingResult())) {
            return "unknown cooling result item " + definition.coolingResult();
        }
        if (definition.coolingResultTag() != null && ResourceLocation.tryParse(definition.coolingResultTag()) == null) {
            return "invalid cooling result tag '" + definition.coolingResultTag() + "' (write it without '#', e.g. c:ingots/iron)";
        }
        if (!(definition.breedChance() >= 0.0 && definition.breedChance() <= 1.0)) {
            return "breed chance must be between 0 and 1, got " + definition.breedChance();
        }
        return null;
    }

    /**
     * Gets all loaded cow definitions.
     */
    public static Map<ResourceLocation, CowDefinition> getDefinitions() {
        return DEFINITIONS;
    }
    
    /**
     * Gets a single cow definition by its ID.
     */
    public static CowDefinition getDefinition(ResourceLocation id) {
        return id == null ? null : DEFINITIONS.get(resolveId(id));
    }

    /** The current id for a cow id read from a save, see {@link CowDefinition#resolveLegacyId}. */
    public static ResourceLocation resolveId(ResourceLocation id) {
        return CowDefinition.resolveLegacyId(id, DEFINITIONS);
    }
}
