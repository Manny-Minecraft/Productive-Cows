package com.productivecows;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = ProductiveCows.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Breeding Settings
    private static final ModConfigSpec.IntValue BREEDING_COOLDOWN = BUILDER
            .comment("Cooldown applied to parent cows after a successful breeding in ticks (20 ticks = 1 second).")
            .defineInRange("breedingCooldownTicks", 3000, 0, Integer.MAX_VALUE);

    // Basic Cooler Settings
    private static final ModConfigSpec.IntValue BASIC_COOLER_COOK_TIME = BUILDER
            .comment("Base time in ticks to process a fluid in the Basic Cooler.")
            .defineInRange("basicCoolerCookTime", 140, 1, Integer.MAX_VALUE);

    // Advanced Cooler Settings
    private static final ModConfigSpec.IntValue ADVANCED_COOLER_CAPACITY = BUILDER
            .comment("Maximum RF capacity for the Advanced Cooler.")
            .defineInRange("advancedCoolerCapacity", 100000, 1000, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue ADVANCED_COOLER_ENERGY_COST = BUILDER
            .comment("Base RF consumed per tick by the Advanced Cooler (without upgrades).")
            .defineInRange("advancedCoolerEnergyCost", 150, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue ADVANCED_COOLER_COOK_TIME = BUILDER
            .comment("Base time in ticks to process a fluid in the Advanced Cooler (without upgrades).")
            .defineInRange("advancedCoolerCookTime", 100, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue ADVANCED_COOLER_FLUID_COST = BUILDER
            .comment("Base fluid amount (mB) consumed per operation in the Advanced Cooler (without upgrades).")
            .defineInRange("advancedCoolerFluidCost", 1000, 1, 8000);

    // Cooler multiblock
    private static final ModConfigSpec.IntValue COOLER_TANK_CAPACITY = BUILDER
            .comment("Fluid (mB) each Cooler Tank adds to the multiblock's shared storage.")
            .defineInRange("coolerTankCapacity", 16000, 1000, 1000000);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static int breedingCooldownTicks;

    public static int basicCoolerCookTime;
    
    public static int advancedCoolerCapacity;
    public static int advancedCoolerEnergyCost;
    public static int advancedCoolerCookTime;
    public static int advancedCoolerFluidCost;

    public static int coolerTankCapacity;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event instanceof ModConfigEvent.Unloading) return;

        breedingCooldownTicks = BREEDING_COOLDOWN.get();

        basicCoolerCookTime = BASIC_COOLER_COOK_TIME.get();
        
        advancedCoolerCapacity = ADVANCED_COOLER_CAPACITY.get();
        advancedCoolerEnergyCost = ADVANCED_COOLER_ENERGY_COST.get();
        advancedCoolerCookTime = ADVANCED_COOLER_COOK_TIME.get();
        advancedCoolerFluidCost = ADVANCED_COOLER_FLUID_COST.get();

        coolerTankCapacity = COOLER_TANK_CAPACITY.get();
    }
}
