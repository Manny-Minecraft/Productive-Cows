package com.productivecows.network;

import com.productivecows.data.CowDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientCowData {
    private static Map<ResourceLocation, CowDefinition> DEFINITIONS = Collections.emptyMap();

    public static void setDefinitions(List<CowDefinition> list) {
        Map<ResourceLocation, CowDefinition> map = new HashMap<>();
        for (CowDefinition definition : list) {
            map.put(definition.id(), definition);
        }
        DEFINITIONS = Map.copyOf(map);
    }

    public static Map<ResourceLocation, CowDefinition> getDefinitions() {
        return DEFINITIONS;
    }

    public static CowDefinition getDefinition(ResourceLocation id) {
        return id == null ? null : DEFINITIONS.get(CowDefinition.resolveLegacyId(id, DEFINITIONS));
    }

    /** Called when leaving a server, so the next world never sees the previous server's cows. */
    public static void clear() {
        DEFINITIONS = Collections.emptyMap();
    }
}
