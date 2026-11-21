package com.productivecows;

import com.productivecows.data.CowDefinition;
import com.productivecows.entity.MaterialCowEntity;
import com.productivecows.fluid.ModFluids;
import com.productivecows.network.NetworkRegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import com.productivecows.block.BreedingCageBlock;
import com.productivecows.block.CowCageBlock;
import com.productivecows.block.LiquidCoolerBlock;
import com.productivecows.block.entity.BreedingCageBlockEntity;
import com.productivecows.block.entity.CowCageBlockEntity;
import com.productivecows.block.entity.LiquidCoolerBlockEntity;
import com.productivecows.menu.BreedingCageMenu;
import com.productivecows.menu.CowCageMenu;
import com.productivecows.menu.LiquidCoolerMenu;
import com.productivecows.item.CowStickItem;
import com.productivecows.item.CapturedCowItem;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(ProductiveCows.MODID)
public class ProductiveCows {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "productivecows";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Registries
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MODID);

    // Blocks
    public static final DeferredBlock<Block> BREEDING_CAGE = BLOCKS.register("breeding_cage",
            () -> new BreedingCageBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredItem<BlockItem> BREEDING_CAGE_ITEM = ITEMS.registerSimpleBlockItem("breeding_cage", BREEDING_CAGE);

    public static final DeferredBlock<Block> COW_CAGE = BLOCKS.register("cow_cage",
            () -> new CowCageBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredItem<BlockItem> COW_CAGE_ITEM = ITEMS.registerSimpleBlockItem("cow_cage", COW_CAGE);

    public static final DeferredBlock<Block> LIQUID_COOLER = BLOCKS.register("liquid_cooler",
            () -> new LiquidCoolerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredItem<BlockItem> LIQUID_COOLER_ITEM = ITEMS.registerSimpleBlockItem("liquid_cooler", LIQUID_COOLER);

    public static final DeferredBlock<Block> MILK_GENERATOR = BLOCKS.register("milk_generator",
            () -> new com.productivecows.block.MilkGeneratorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredItem<BlockItem> MILK_GENERATOR_ITEM = ITEMS.registerSimpleBlockItem("milk_generator", MILK_GENERATOR);

    public static final DeferredBlock<Block> ADVANCED_COOLER = BLOCKS.register("advanced_cooler",
            () -> new com.productivecows.block.AdvancedCoolerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredItem<BlockItem> ADVANCED_COOLER_ITEM = ITEMS.registerSimpleBlockItem("advanced_cooler", ADVANCED_COOLER);

    // Items
    public static final DeferredItem<Item> BASE_UPGRADE = ITEMS.register("base_upgrade", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> SPEED_UPGRADE = ITEMS.register("speed_upgrade", () -> new com.productivecows.item.UpgradeItem(new Item.Properties().stacksTo(64), "tooltip.productivecows.speed_upgrade"));
    public static final DeferredItem<Item> EFFICIENCY_UPGRADE = ITEMS.register("efficiency_upgrade", () -> new com.productivecows.item.UpgradeItem(new Item.Properties().stacksTo(64), "tooltip.productivecows.efficiency_upgrade"));
    public static final DeferredItem<Item> ENERGY_UPGRADE = ITEMS.register("energy_upgrade", () -> new com.productivecows.item.UpgradeItem(new Item.Properties().stacksTo(64), "tooltip.productivecows.energy_upgrade"));
    public static final DeferredItem<Item> COW_STICK = ITEMS.register("cow_stick",
            () -> new CowStickItem(new Item.Properties().durability(64)));
    public static final DeferredItem<CapturedCowItem> CAPTURED_COW = ITEMS.register("captured_cow",
            () -> new CapturedCowItem(new Item.Properties().stacksTo(16)));

    // Entity Types & Spawn Eggs
    public static final DeferredHolder<EntityType<?>, EntityType<MaterialCowEntity>> MATERIAL_COW = ENTITY_TYPES.register("material_cow",
            () -> EntityType.Builder.of(MaterialCowEntity::new, MobCategory.CREATURE)
                    .sized(0.9F, 1.4F)
                    .build("material_cow")
    );

    public static final DeferredItem<Item> MATERIAL_COW_SPAWN_EGG = ITEMS.register("material_cow_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(MATERIAL_COW, 0xC1C1C1, 0x3F3F3F, new Item.Properties()) {
                @Override
                public Component getName(ItemStack stack) {
                    net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.ENTITY_DATA);
                    if (customData != null) {
                        CompoundTag tag = customData.copyTag();
                        if (tag.contains("CowDefinitionId")) {
                            String idStr = tag.getString("CowDefinitionId");
                            net.minecraft.resources.ResourceLocation defId = net.minecraft.resources.ResourceLocation.tryParse(idStr);
                            if (defId != null) {
                                CowDefinition def = getDisplayDefinition(defId);
                                if (def != null) {
                                    return Component.translatable("item.productivecows.spawn_egg_format", def.name());
                                }
                            }
                        }
                    }
                    return super.getName(stack);
                }
            }
    );

    // Block Entities & Menus
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BreedingCageBlockEntity>> BREEDING_CAGE_BE = BLOCK_ENTITY_TYPES.register("breeding_cage",
            () -> BlockEntityType.Builder.of(BreedingCageBlockEntity::new, BREEDING_CAGE.get()).build(null)
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CowCageBlockEntity>> COW_CAGE_BE = BLOCK_ENTITY_TYPES.register("cow_cage",
            () -> BlockEntityType.Builder.of(CowCageBlockEntity::new, COW_CAGE.get()).build(null)
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LiquidCoolerBlockEntity>> LIQUID_COOLER_BE = BLOCK_ENTITY_TYPES.register("liquid_cooler",
            () -> BlockEntityType.Builder.of(LiquidCoolerBlockEntity::new, LIQUID_COOLER.get()).build(null)
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.productivecows.block.entity.MilkGeneratorBlockEntity>> MILK_GENERATOR_BE = BLOCK_ENTITY_TYPES.register("milk_generator",
            () -> BlockEntityType.Builder.of(com.productivecows.block.entity.MilkGeneratorBlockEntity::new, MILK_GENERATOR.get()).build(null)
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.productivecows.block.entity.AdvancedCoolerBlockEntity>> ADVANCED_COOLER_BE = BLOCK_ENTITY_TYPES.register("advanced_cooler",
            () -> BlockEntityType.Builder.of(com.productivecows.block.entity.AdvancedCoolerBlockEntity::new, ADVANCED_COOLER.get()).build(null)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<BreedingCageMenu>> BREEDING_CAGE_MENU = MENU_TYPES.register("breeding_cage",
            () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(BreedingCageMenu::new)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<CowCageMenu>> COW_CAGE_MENU = MENU_TYPES.register("cow_cage",
            () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(CowCageMenu::new)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<LiquidCoolerMenu>> LIQUID_COOLER_MENU = MENU_TYPES.register("liquid_cooler",
            () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(LiquidCoolerMenu::new)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<com.productivecows.menu.MilkGeneratorMenu>> MILK_GENERATOR_MENU = MENU_TYPES.register("milk_generator",
            () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(com.productivecows.menu.MilkGeneratorMenu::new)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<com.productivecows.menu.AdvancedCoolerMenu>> ADVANCED_COOLER_MENU = MENU_TYPES.register("advanced_cooler",
            () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(com.productivecows.menu.AdvancedCoolerMenu::new)
    );

    // Creative Tabs
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ITEMS_BLOCKS_TAB = CREATIVE_MODE_TABS.register("items_blocks_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.productivecows.items_blocks"))
                    .icon(() -> COW_STICK.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(BREEDING_CAGE_ITEM.get());
                        output.accept(COW_CAGE_ITEM.get());
                        output.accept(LIQUID_COOLER_ITEM.get());
                        output.accept(MILK_GENERATOR_ITEM.get());
                        output.accept(ADVANCED_COOLER_ITEM.get());
                        output.accept(SPEED_UPGRADE.get());
                        output.accept(EFFICIENCY_UPGRADE.get());
                        output.accept(ENERGY_UPGRADE.get());
                        output.accept(BASE_UPGRADE.get());
                        output.accept(COW_STICK.get());
                    }).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> LIQUIDS_TAB = CREATIVE_MODE_TABS.register("liquids_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.productivecows.liquids"))
                    .icon(() -> ModFluids.LIQUID_OBSIDIAN.bucket().get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        java.util.Set<Item> addedBuckets = new java.util.LinkedHashSet<>();
                        for (CowDefinition def : getActiveDefinitions().values()) {
                            net.minecraft.world.level.material.Fluid fluid = BuiltInRegistries.FLUID.get(def.fluidId());
                            if (fluid != null && fluid != net.minecraft.world.level.material.Fluids.EMPTY) {
                                Item bucket = fluid.getBucket();
                                if (bucket != Items.AIR) {
                                    addedBuckets.add(bucket);
                                }
                            }
                        }
                        for (Item bucket : addedBuckets) {
                            output.accept(bucket);
                        }
                    }).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SPAWN_EGGS_TAB = CREATIVE_MODE_TABS.register("spawn_eggs_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.productivecows.spawn_eggs"))
                    .icon(() -> MATERIAL_COW_SPAWN_EGG.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        for (CowDefinition def : getActiveDefinitions().values()) {
                            ItemStack eggStack = new ItemStack(MATERIAL_COW_SPAWN_EGG.get());
                            
                            CompoundTag entityData = new CompoundTag();
                            entityData.putString("id", "productivecows:material_cow");
                            entityData.putString("CowDefinitionId", def.id().toString());
                            
                            eggStack.set(net.minecraft.core.component.DataComponents.ENTITY_DATA,
                                    net.minecraft.world.item.component.CustomData.of(entityData));
                            
                            output.accept(eggStack);
                        }
                    }).build());

    /**
     * Definitions for the side that asks: the server always reads its loaded data, the client the copy the
     * server synced. In singleplayer both sides share this JVM, so block entities must never use the client map.
     */
    public static java.util.Map<net.minecraft.resources.ResourceLocation, CowDefinition> getDefinitions(@org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level) {
        if (level != null && level.isClientSide) {
            return com.productivecows.network.ClientCowData.getDefinitions();
        }
        return com.productivecows.data.MaterialCowDataLoader.getDefinitions();
    }

    /**
     * Definition used for names and tooltips, where no level is at hand: the client's synced copy first
     * (always current for the connected server), then the server data (dedicated server, or before the sync).
     */
    @org.jetbrains.annotations.Nullable
    public static CowDefinition getDisplayDefinition(net.minecraft.resources.ResourceLocation id) {
        CowDefinition def = com.productivecows.network.ClientCowData.getDefinition(id);
        return def != null ? def : com.productivecows.data.MaterialCowDataLoader.getDefinition(id);
    }

    // For client-side display only (screens, JEI, creative tabs); game logic uses getDefinitions(level)
    public static java.util.Map<net.minecraft.resources.ResourceLocation, CowDefinition> getActiveDefinitions() {
        var clientDefs = com.productivecows.network.ClientCowData.getDefinitions();
        if (!clientDefs.isEmpty()) {
            return clientDefs;
        }
        return com.productivecows.data.MaterialCowDataLoader.getDefinitions();
    }

    public ProductiveCows(IEventBus modEventBus, ModContainer modContainer) {
// Register static event subscribers
        modEventBus.register(com.productivecows.event.ModBusEvents.class);

        // Register fluid registries
        ModFluids.FLUID_TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);

        // Register network payloads
        modEventBus.addListener(NetworkRegistry::registerPayloads);

        // Register the Deferred Registers to the mod event bus
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENU_TYPES.register(modEventBus);

if (net.neoforged.fml.loading.FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.addListener(com.productivecows.client.ClientModBusEvents::onLoggingOut);
        }

        // Balance values live in the per-world server config (serverconfig/productivecows-server.toml),
        // which servers sync to clients. Register the spec only once: a second COMMON registration
        // created a config/productivecows-common.toml that the server file silently overrode.
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    }

    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void registerRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ADVANCED_COOLER_BE.get(), com.productivecows.client.renderer.CoolerBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(LIQUID_COOLER_BE.get(), com.productivecows.client.renderer.CoolerBlockEntityRenderer::new);
        }
    }
}
