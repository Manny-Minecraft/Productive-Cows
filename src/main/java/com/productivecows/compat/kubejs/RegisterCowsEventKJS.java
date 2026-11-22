package com.productivecows.compat.kubejs;

import com.productivecows.data.CowDefinition;
import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class RegisterCowsEventKJS implements KubeEvent {
    private final Map<ResourceLocation, CowDefinition> registry;
    private final java.util.List<CowBuilder> builders = new java.util.ArrayList<>();

    public RegisterCowsEventKJS(Map<ResourceLocation, CowDefinition> registry) {
        this.registry = registry;
    }

    /** Cow ids without a namespace belong to kubejs, like other KubeJS content: 'vibranium' is kubejs:vibranium. */
    public static ResourceLocation parseCowId(String id) {
        return id.contains(":") ? ResourceLocation.parse(id) : ResourceLocation.fromNamespaceAndPath("kubejs", id);
    }

    public CowBuilder create(String id) {
        ResourceLocation location = parseCowId(id);
        CowBuilder builder = new CowBuilder(location, this);
        builders.add(builder);
        return builder;
    }

    public void register(ResourceLocation id, CowDefinition definition) {
        registry.put(id, definition);
    }
    
    public void registerAll() {
        for (CowBuilder builder : builders) {
            builder.register();
        }
    }
}
