package dev.conveyance.factory;

import dev.conveyance.Conveyance;
import dev.conveyance.player.PlayerWorkbenchMenu;
import dev.conveyance.world.ForestLogBlock;
import dev.conveyance.world.ForestLeavesBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class FactoryContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Conveyance.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Conveyance.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Conveyance.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Conveyance.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Conveyance.MOD_ID);
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Conveyance.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Conveyance.MOD_ID);
    public static final RegistryObject<RecipeType<BulkSmeltingRecipe>> BULK_SMELTING_TYPE = RECIPE_TYPES.register("bulk_smelting", () -> new RecipeType<BulkSmeltingRecipe>() {});
    public static final RegistryObject<RecipeSerializer<BulkSmeltingRecipe>> BULK_SMELTING_SERIALIZER = RECIPE_SERIALIZERS.register("bulk_smelting", BulkSmeltingRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<FactoryCraftingRecipe>> FACTORY_CRAFTING_SERIALIZER = RECIPE_SERIALIZERS.register("factory_crafting", FactoryCraftingRecipe.Serializer::new);

    public static final RegistryObject<CrateBlock> WOOD_CRATE = BLOCKS.register("wood_crate", () -> new CrateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD), 16));
    public static final RegistryObject<CrateBlock> IRON_CRATE = BLOCKS.register("iron_crate", () -> new CrateBlock(metal(), 32));
    public static final RegistryObject<ForestLogBlock> TREE_LOG = BLOCKS.register("tree_log", () -> new ForestLogBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD)));
    public static final RegistryObject<ForestLeavesBlock> TREE_LEAVES = BLOCKS.register("tree_leaves", () -> new ForestLeavesBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.1F).sound(SoundType.GRASS).noOcclusion().isViewBlocking((state, level, pos) -> false).isSuffocating((state, level, pos) -> false)));
    public static final RegistryObject<Item> WOOD_ITEM = ITEMS.register("wood", () -> new WoodItem(new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> WOOD_CRATE_ITEM = ITEMS.register("wood_crate", () -> new BlockItem(WOOD_CRATE.get(), new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> IRON_CRATE_ITEM = ITEMS.register("iron_crate", () -> new BlockItem(IRON_CRATE.get(), new Item.Properties().stacksTo(100)));
    public static final RegistryObject<MinerBlock> MINER = BLOCKS.register("miner", () -> new MinerBlock(metal()));
    public static final RegistryObject<MinerPartBlock> MINER_PART = BLOCKS.register("miner_part", () -> new MinerPartBlock(metal()));
    public static final RegistryObject<ConveyorBlock> CONVEYOR = BLOCKS.register("conveyor", () -> new ConveyorBlock(metal().noOcclusion()));
    public static final RegistryObject<SlopeConveyorBlock> SLOPE_CONVEYOR = BLOCKS.register("slope_conveyor", () -> new SlopeConveyorBlock(metal().noOcclusion()));
    public static final RegistryObject<JunctionBlock> JUNCTION = BLOCKS.register("junction", () -> new JunctionBlock(metal().noOcclusion()));
    public static final RegistryObject<InserterBlock> INSERTER = BLOCKS.register("inserter", () -> new InserterBlock(metal().noOcclusion()));
    public static final RegistryObject<InserterBlock> LONG_INSERTER = BLOCKS.register("long_inserter", () -> new InserterBlock(metal().noOcclusion(), 2));
    public static final RegistryObject<InserterBlock> BURNER_INSERTER = BLOCKS.register("burner_inserter", () -> new InserterBlock(metal().noOcclusion(), true));
    public static final RegistryObject<PowerPoleBlock> POWER_POLE = BLOCKS.register("power_pole", () -> new PowerPoleBlock(metal().noOcclusion()));
    public static final RegistryObject<PowerPolePartBlock> POWER_POLE_PART = BLOCKS.register("power_pole_part", () -> new PowerPolePartBlock(metal().noOcclusion()));
    public static final RegistryObject<GeneratorBlock> GENERATOR = BLOCKS.register("generator", () -> new GeneratorBlock(metal().lightLevel(state -> state.getValue(GeneratorBlock.LIT) ? 10 : 0)));
    public static final RegistryObject<RadarBlock> RADAR = BLOCKS.register("radar", () -> new RadarBlock(metal().noOcclusion()));
    public static final RegistryObject<RadarPartBlock> RADAR_PART = BLOCKS.register("radar_part", () -> new RadarPartBlock(metal().noOcclusion()));
    public static final RegistryObject<Item> RADAR_ITEM = ITEMS.register("radar", () -> new RadarItem(RADAR.get(), new Item.Properties().stacksTo(100)));
    public static final RegistryObject<MachineBlock> BURNER_MINER = BLOCKS.register("burner_miner", () -> new MachineBlock(metal(), MachineKind.BURNER_MINER));
    public static final RegistryObject<MachinePartBlock> BURNER_MINER_PART = BLOCKS.register("burner_miner_part", () -> new MachinePartBlock(metal(), MachineKind.BURNER_MINER));
    public static final RegistryObject<MachineBlock> ASSEMBLER = BLOCKS.register("assembler", () -> new MachineBlock(metal(), MachineKind.ASSEMBLER));
    public static final RegistryObject<MachinePartBlock> ASSEMBLER_PART = BLOCKS.register("assembler_part", () -> new MachinePartBlock(metal(), MachineKind.ASSEMBLER));
    public static final RegistryObject<MachineBlock> FURNACE = BLOCKS.register("industrial_furnace", () -> new MachineBlock(metal(), MachineKind.FURNACE));
    public static final RegistryObject<MachinePartBlock> FURNACE_PART = BLOCKS.register("industrial_furnace_part", () -> new MachinePartBlock(metal(), MachineKind.FURNACE));
    public static final RegistryObject<MachineBlock> LAB = BLOCKS.register("research_lab", () -> new MachineBlock(metal().lightLevel(state -> state.getValue(MachineBlock.LIT) ? 8 : 0), MachineKind.LAB));
    public static final RegistryObject<MachinePartBlock> LAB_PART = BLOCKS.register("research_lab_part", () -> new MachinePartBlock(metal(), MachineKind.LAB));
    public static final RegistryObject<MachineBlock> SOLAR_PANEL = BLOCKS.register("solar_panel", () -> new MachineBlock(metal().noOcclusion(), MachineKind.SOLAR_PANEL));
    public static final RegistryObject<MachinePartBlock> SOLAR_PANEL_PART = BLOCKS.register("solar_panel_part", () -> new MachinePartBlock(metal().noOcclusion(), MachineKind.SOLAR_PANEL));
    public static final RegistryObject<Item> MINER_ITEM = ITEMS.register("miner", () -> new MinerItem(MINER.get(), new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> CONVEYOR_ITEM = ITEMS.register("conveyor", () -> new ConveyorItem(CONVEYOR.get(), new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> SLOPE_CONVEYOR_ITEM = ITEMS.register("slope_conveyor", () -> new ConveyorItem(SLOPE_CONVEYOR.get(), new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> JUNCTION_ITEM = ITEMS.register("junction", () -> new BlockItem(JUNCTION.get(), new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> INSERTER_ITEM = ITEMS.register("inserter", () -> new BlockItem(INSERTER.get(), new Item.Properties()));
    public static final RegistryObject<Item> LONG_INSERTER_ITEM = ITEMS.register("long_inserter", () -> new BlockItem(LONG_INSERTER.get(), new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> BURNER_INSERTER_ITEM = ITEMS.register("burner_inserter", () -> new BlockItem(BURNER_INSERTER.get(), new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> POWER_POLE_ITEM = ITEMS.register("power_pole", () -> new PowerPoleItem(POWER_POLE.get(), new Item.Properties()));
    public static final RegistryObject<Item> GENERATOR_ITEM = ITEMS.register("generator", () -> new BlockItem(GENERATOR.get(), new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> BURNER_MINER_ITEM = ITEMS.register("burner_miner", () -> new MachineItem(BURNER_MINER.get(), new Item.Properties().stacksTo(100), MachineKind.BURNER_MINER));
    public static final RegistryObject<Item> ASSEMBLER_ITEM = ITEMS.register("assembler", () -> new MachineItem(ASSEMBLER.get(), new Item.Properties().stacksTo(100), MachineKind.ASSEMBLER));
    public static final RegistryObject<Item> FURNACE_ITEM = ITEMS.register("industrial_furnace", () -> new MachineItem(FURNACE.get(), new Item.Properties().stacksTo(100), MachineKind.FURNACE));
    public static final RegistryObject<Item> LAB_ITEM = ITEMS.register("research_lab", () -> new MachineItem(LAB.get(), new Item.Properties().stacksTo(100), MachineKind.LAB));
    public static final RegistryObject<Item> GEAR_ITEM = ITEMS.register("gear", () -> new Item(new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> WIRE_ITEM = ITEMS.register("copper_wire", () -> new Item(new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> CIRCUIT_ITEM = ITEMS.register("electronic_circuit", () -> new Item(new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> RED_SCIENCE_ITEM = ITEMS.register("red_science_pack", () -> new Item(new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> GREEN_SCIENCE_ITEM = ITEMS.register("green_science_pack", () -> new Item(new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> STEEL_ITEM = ITEMS.register("steel_ingot", () -> new Item(new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> SOLAR_PANEL_ITEM = ITEMS.register("solar_panel", () -> new MachineItem(SOLAR_PANEL.get(), new Item.Properties().stacksTo(100), MachineKind.SOLAR_PANEL));
    public static final RegistryObject<BlockEntityType<MinerEntity>> MINER_ENTITY = ENTITIES.register("miner",
            () -> BlockEntityType.Builder.of(MinerEntity::new, MINER.get()).build(null));
    public static final RegistryObject<BlockEntityType<CrateEntity>> CRATE_ENTITY = ENTITIES.register("crate",
            () -> BlockEntityType.Builder.of(CrateEntity::new, WOOD_CRATE.get(), IRON_CRATE.get()).build(null));
    public static final RegistryObject<BlockEntityType<ConveyorEntity>> CONVEYOR_ENTITY = ENTITIES.register("conveyor",
            () -> BlockEntityType.Builder.of(ConveyorEntity::new, CONVEYOR.get(), SLOPE_CONVEYOR.get()).build(null));
    public static final RegistryObject<BlockEntityType<JunctionEntity>> JUNCTION_ENTITY = ENTITIES.register("junction",
            () -> BlockEntityType.Builder.of(JunctionEntity::new, JUNCTION.get()).build(null));
    public static final RegistryObject<BlockEntityType<InserterEntity>> INSERTER_ENTITY = ENTITIES.register("inserter",
            () -> BlockEntityType.Builder.of(InserterEntity::new, INSERTER.get()).build(null));
    public static final RegistryObject<BlockEntityType<InserterEntity>> LONG_INSERTER_ENTITY = ENTITIES.register("long_inserter",
            () -> BlockEntityType.Builder.of(InserterEntity::new, LONG_INSERTER.get()).build(null));
    public static final RegistryObject<BlockEntityType<BurnerInserterEntity>> BURNER_INSERTER_ENTITY = ENTITIES.register("burner_inserter",
            () -> BlockEntityType.Builder.of(BurnerInserterEntity::new, BURNER_INSERTER.get()).build(null));
    public static final RegistryObject<BlockEntityType<PowerPoleEntity>> POWER_POLE_ENTITY = ENTITIES.register("power_pole",
            () -> BlockEntityType.Builder.of(PowerPoleEntity::new, POWER_POLE.get()).build(null));
    public static final RegistryObject<BlockEntityType<GeneratorEntity>> GENERATOR_ENTITY = ENTITIES.register("generator",
            () -> BlockEntityType.Builder.of(GeneratorEntity::new, GENERATOR.get()).build(null));
    public static final RegistryObject<BlockEntityType<RadarEntity>> RADAR_ENTITY = ENTITIES.register("radar",
            () -> BlockEntityType.Builder.of(RadarEntity::new, RADAR.get()).build(null));
    public static final RegistryObject<BlockEntityType<MachineEntity>> MACHINE_ENTITY = ENTITIES.register("machine",
            () -> BlockEntityType.Builder.of(MachineEntity::new, BURNER_MINER.get(), ASSEMBLER.get(), FURNACE.get(), LAB.get(), SOLAR_PANEL.get()).build(null));
    public static final RegistryObject<MenuType<GeneratorMenu>> GENERATOR_MENU = MENUS.register("generator",
            () -> new MenuType<>(GeneratorMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<CrateMenu>> WOOD_CRATE_MENU = MENUS.register("wood_crate",
            () -> new MenuType<>((id, inventory) -> new CrateMenu(id, inventory, 16), FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<CrateMenu>> IRON_CRATE_MENU = MENUS.register("iron_crate",
            () -> new MenuType<>((id, inventory) -> new CrateMenu(id, inventory, 32), FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<GeneratorMenu>> LAB_MENU = MENUS.register("research_lab",
            () -> new MenuType<>((id, inventory) -> new GeneratorMenu(id, inventory, new SimpleContainer(2), true), FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<ProcessingMenu>> ASSEMBLER_MENU = MENUS.register("assembler",
            () -> new MenuType<>((id, inventory) -> new ProcessingMenu(id, inventory, MachineKind.ASSEMBLER), FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<ProcessingMenu>> FURNACE_MENU = MENUS.register("industrial_furnace",
            () -> new MenuType<>((id, inventory) -> new ProcessingMenu(id, inventory, MachineKind.FURNACE), FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<PlayerWorkbenchMenu>> PLAYER_WORKBENCH_MENU = MENUS.register("player_workbench",
            () -> new MenuType<>(PlayerWorkbenchMenu::new, FeatureFlags.DEFAULT_FLAGS));

    static {
        TABS.register("factory", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.conveyance.factory"))
                .icon(() -> MINER_ITEM.get().getDefaultInstance()).displayItems((parameters, output) -> {
                    output.accept(WOOD_ITEM.get()); output.accept(WOOD_CRATE_ITEM.get()); output.accept(IRON_CRATE_ITEM.get());
                    output.accept(MINER_ITEM.get()); output.accept(CONVEYOR_ITEM.get()); output.accept(SLOPE_CONVEYOR_ITEM.get()); output.accept(INSERTER_ITEM.get());
                    output.accept(JUNCTION_ITEM.get());
                    output.accept(POWER_POLE_ITEM.get()); output.accept(GENERATOR_ITEM.get());
                    output.accept(RADAR_ITEM.get());
                    output.accept(LONG_INSERTER_ITEM.get()); output.accept(SOLAR_PANEL_ITEM.get()); output.accept(STEEL_ITEM.get()); output.accept(GREEN_SCIENCE_ITEM.get());
                    output.accept(BURNER_MINER_ITEM.get()); output.accept(FURNACE_ITEM.get()); output.accept(ASSEMBLER_ITEM.get());
                    output.accept(BURNER_INSERTER_ITEM.get());
                    output.accept(GEAR_ITEM.get()); output.accept(WIRE_ITEM.get()); output.accept(CIRCUIT_ITEM.get()); output.accept(RED_SCIENCE_ITEM.get()); output.accept(LAB_ITEM.get());
                }).build());
    }

    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL);
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); MENUS.register(bus); TABS.register(bus);
        RECIPE_TYPES.register(bus); RECIPE_SERIALIZERS.register(bus);
    }
    private FactoryContent() {}
}
