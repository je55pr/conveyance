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
    public static final RegistryObject<RecipeType<ChemicalRecipe>> CHEMICAL_TYPE = RECIPE_TYPES.register("chemical_processing", () -> new RecipeType<ChemicalRecipe>() {});
    public static final RegistryObject<RecipeSerializer<ChemicalRecipe>> CHEMICAL_SERIALIZER = RECIPE_SERIALIZERS.register("chemical_processing", ChemicalRecipe.Serializer::new);
    public static final RegistryObject<Block> OIL_DEPOSIT = BLOCKS.register("oil_deposit", () -> new dev.conveyance.world.OilDepositBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(-1, 3600000).noLootTable()));
    public static final RegistryObject<Block> URANIUM_ORE = BLOCKS.register("uranium_ore", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(-1, 3600000).sound(SoundType.STONE).noLootTable()));
    public static final RegistryObject<WreckBlock> WRECK = BLOCKS.register("wreck", () -> new WreckBlock(metal().noOcclusion().noLootTable()));
    public static final RegistryObject<StoneWallBlock> STONE_WALL = BLOCKS.register("stone_wall", () -> new StoneWallBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5F).sound(SoundType.STONE).noLootTable()));
    public static final RegistryObject<Item> STONE_WALL_ITEM = ITEMS.register("stone_wall", () -> new StoneWallItem(STONE_WALL.get(), new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> FIREARM_MAGAZINE = ITEMS.register("firearm_magazine", () -> new Item(new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> REPAIR_PACK = ITEMS.register("repair_pack", () -> new Item(new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> PISTOL = ITEMS.register("pistol", () -> new dev.conveyance.combat.PistolItem(new Item.Properties().stacksTo(100)));
    public static final RegistryObject<MachineBlock> GUN_TURRET = BLOCKS.register("gun_turret", () -> new MachineBlock(metal().noOcclusion(), MachineKind.GUN_TURRET));
    public static final RegistryObject<MachinePartBlock> GUN_TURRET_PART = BLOCKS.register("gun_turret_part", () -> new MachinePartBlock(metal().noOcclusion(), MachineKind.GUN_TURRET));
    public static final RegistryObject<Item> GUN_TURRET_ITEM = ITEMS.register("gun_turret", () -> new MachineItem(GUN_TURRET.get(), new Item.Properties().stacksTo(100), MachineKind.GUN_TURRET));
    public static final RegistryObject<MachineBlock> LASER_TURRET = BLOCKS.register("laser_turret", () -> new MachineBlock(metal().noOcclusion(), MachineKind.LASER_TURRET));
    public static final RegistryObject<MachinePartBlock> LASER_TURRET_PART = BLOCKS.register("laser_turret_part", () -> new MachinePartBlock(metal().noOcclusion(), MachineKind.LASER_TURRET));
    public static final RegistryObject<Item> LASER_TURRET_ITEM = ITEMS.register("laser_turret", () -> new MachineItem(LASER_TURRET.get(), new Item.Properties().stacksTo(100), MachineKind.LASER_TURRET));
    public static final RegistryObject<Item> MILITARY_SCIENCE_ITEM = ITEMS.register("military_science_pack", () -> new Item(new Item.Properties().stacksTo(100)));
    public static final RegistryObject<BlockEntityType<WreckEntity>> WRECK_ENTITY = ENTITIES.register("wreck", () -> BlockEntityType.Builder.of(WreckEntity::new, WRECK.get()).build(null));

    public static final RegistryObject<CrateBlock> WOOD_CRATE = BLOCKS.register("wood_crate", () -> new CrateBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD), 16));
    public static final RegistryObject<CrateBlock> IRON_CRATE = BLOCKS.register("iron_crate", () -> new CrateBlock(metal(), 32));
    public static final RegistryObject<ForestLogBlock> TREE_LOG = BLOCKS.register("tree_log", () -> new ForestLogBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD)));
    public static final RegistryObject<ForestLeavesBlock> TREE_LEAVES = BLOCKS.register("tree_leaves", () -> new ForestLeavesBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.1F).sound(SoundType.GRASS).noOcclusion().isViewBlocking((state, level, pos) -> false).isSuffocating((state, level, pos) -> false)));
    public static final RegistryObject<Item> WOOD_ITEM = ITEMS.register("wood", () -> new WoodItem(new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> WOOD_CRATE_ITEM = ITEMS.register("wood_crate", () -> new BlockItem(WOOD_CRATE.get(), new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> IRON_CRATE_ITEM = ITEMS.register("iron_crate", () -> new BlockItem(IRON_CRATE.get(), new Item.Properties().stacksTo(100)));
    public static final RegistryObject<MinerBlock> MINER = BLOCKS.register("miner", () -> new MinerBlock(metal().noOcclusion()));
    public static final RegistryObject<MinerPartBlock> MINER_PART = BLOCKS.register("miner_part", () -> new MinerPartBlock(metal().noOcclusion()));
    public static final RegistryObject<ConveyorBlock> CONVEYOR = BLOCKS.register("conveyor", () -> new ConveyorBlock(metal().noOcclusion()));
    public static final RegistryObject<SlopeConveyorBlock> SLOPE_CONVEYOR = BLOCKS.register("slope_conveyor", () -> new SlopeConveyorBlock(metal().noOcclusion()));
    public static final RegistryObject<JunctionBlock> JUNCTION = BLOCKS.register("junction", () -> new JunctionBlock(metal().noOcclusion()));
    public static final RegistryObject<InserterBlock> INSERTER = BLOCKS.register("inserter", () -> new InserterBlock(metal().noOcclusion()));
    public static final RegistryObject<InserterBlock> LONG_INSERTER = BLOCKS.register("long_inserter", () -> new InserterBlock(metal().noOcclusion(), 2));
    public static final RegistryObject<InserterBlock> BURNER_INSERTER = BLOCKS.register("burner_inserter", () -> new InserterBlock(metal().noOcclusion(), true));
    public static final RegistryObject<PowerPoleBlock> POWER_POLE = BLOCKS.register("power_pole", () -> new PowerPoleBlock(metal().noOcclusion()));
    public static final RegistryObject<PowerPolePartBlock> POWER_POLE_PART = BLOCKS.register("power_pole_part", () -> new PowerPolePartBlock(metal().noOcclusion()));
    public static final RegistryObject<GeneratorBlock> GENERATOR = BLOCKS.register("generator", () -> new GeneratorBlock(metal().noOcclusion().lightLevel(state -> state.getValue(GeneratorBlock.LIT) ? 10 : 0)));
    public static final RegistryObject<RadarBlock> RADAR = BLOCKS.register("radar", () -> new RadarBlock(metal().noOcclusion()));
    public static final RegistryObject<RadarPartBlock> RADAR_PART = BLOCKS.register("radar_part", () -> new RadarPartBlock(metal().noOcclusion()));
    public static final RegistryObject<Item> RADAR_ITEM = ITEMS.register("radar", () -> new RadarItem(RADAR.get(), new Item.Properties().stacksTo(100)));
    public static final RegistryObject<MachineBlock> BURNER_MINER = BLOCKS.register("burner_miner", () -> new MachineBlock(metal().noOcclusion(), MachineKind.BURNER_MINER));
    public static final RegistryObject<MachinePartBlock> BURNER_MINER_PART = BLOCKS.register("burner_miner_part", () -> new MachinePartBlock(metal().noOcclusion(), MachineKind.BURNER_MINER));
    public static final RegistryObject<MachineBlock> ASSEMBLER = BLOCKS.register("assembler", () -> new MachineBlock(metal().noOcclusion(), MachineKind.ASSEMBLER));
    public static final RegistryObject<MachinePartBlock> ASSEMBLER_PART = BLOCKS.register("assembler_part", () -> new MachinePartBlock(metal().noOcclusion(), MachineKind.ASSEMBLER));
    public static final RegistryObject<MachineBlock> FURNACE = BLOCKS.register("industrial_furnace", () -> new MachineBlock(metal().noOcclusion(), MachineKind.FURNACE));
    public static final RegistryObject<MachinePartBlock> FURNACE_PART = BLOCKS.register("industrial_furnace_part", () -> new MachinePartBlock(metal().noOcclusion(), MachineKind.FURNACE));
    public static final RegistryObject<MachineBlock> LAB = BLOCKS.register("research_lab", () -> new MachineBlock(metal().noOcclusion().lightLevel(state -> state.getValue(MachineBlock.LIT) ? 8 : 0), MachineKind.LAB));
    public static final RegistryObject<MachinePartBlock> LAB_PART = BLOCKS.register("research_lab_part", () -> new MachinePartBlock(metal().noOcclusion(), MachineKind.LAB));
    public static final RegistryObject<MachineBlock> SOLAR_PANEL = BLOCKS.register("solar_panel", () -> new MachineBlock(metal().noOcclusion(), MachineKind.SOLAR_PANEL));
    public static final RegistryObject<MachinePartBlock> SOLAR_PANEL_PART = BLOCKS.register("solar_panel_part", () -> new MachinePartBlock(metal().noOcclusion(), MachineKind.SOLAR_PANEL));
    public static final RegistryObject<PipeBlock> PIPE = BLOCKS.register("pipe", () -> new PipeBlock(metal().noOcclusion()));
    public static final RegistryObject<MachineBlock> BOILER = BLOCKS.register("boiler", () -> new MachineBlock(metal().noOcclusion(), MachineKind.BOILER));
    public static final RegistryObject<MachinePartBlock> BOILER_PART = BLOCKS.register("boiler_part", () -> new MachinePartBlock(metal().noOcclusion(), MachineKind.BOILER));
    public static final RegistryObject<MachineBlock> STEAM_ENGINE = BLOCKS.register("steam_engine", () -> new MachineBlock(metal().noOcclusion(), MachineKind.STEAM_ENGINE));
    public static final RegistryObject<MachinePartBlock> STEAM_ENGINE_PART = BLOCKS.register("steam_engine_part", () -> new MachinePartBlock(metal().noOcclusion(), MachineKind.STEAM_ENGINE));
    public static final RegistryObject<MachineBlock> OFFSHORE_PUMP = BLOCKS.register("offshore_pump", () -> new MachineBlock(metal().noOcclusion(), MachineKind.OFFSHORE_PUMP));
    public static final RegistryObject<MachinePartBlock> OFFSHORE_PUMP_PART = BLOCKS.register("offshore_pump_part", () -> new MachinePartBlock(metal().noOcclusion(), MachineKind.OFFSHORE_PUMP));
    public static final RegistryObject<MachineBlock> PUMPJACK = BLOCKS.register("pumpjack", () -> new MachineBlock(metal().noOcclusion(), MachineKind.PUMPJACK));
    public static final RegistryObject<MachinePartBlock> PUMPJACK_PART = BLOCKS.register("pumpjack_part", () -> new MachinePartBlock(metal().noOcclusion(), MachineKind.PUMPJACK));
    public static final RegistryObject<MachineBlock> REFINERY = BLOCKS.register("oil_refinery", () -> new MachineBlock(metal().noOcclusion(), MachineKind.REFINERY));
    public static final RegistryObject<MachinePartBlock> REFINERY_PART = BLOCKS.register("oil_refinery_part", () -> new MachinePartBlock(metal().noOcclusion(), MachineKind.REFINERY));
    public static final RegistryObject<MachineBlock> CHEMICAL_PLANT = BLOCKS.register("chemical_plant", () -> new MachineBlock(metal().noOcclusion(), MachineKind.CHEMICAL_PLANT));
    public static final RegistryObject<MachinePartBlock> CHEMICAL_PLANT_PART = BLOCKS.register("chemical_plant_part", () -> new MachinePartBlock(metal().noOcclusion(), MachineKind.CHEMICAL_PLANT));
    public static final RegistryObject<Item> MINER_ITEM = ITEMS.register("miner", () -> new MinerItem(MINER.get(), new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> CONVEYOR_ITEM = ITEMS.register("conveyor", () -> new ConveyorItem(CONVEYOR.get(), new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> SLOPE_CONVEYOR_ITEM = ITEMS.register("slope_conveyor", () -> new ConveyorItem(SLOPE_CONVEYOR.get(), new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> JUNCTION_ITEM = ITEMS.register("junction", () -> new BlockItem(JUNCTION.get(), new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> INSERTER_ITEM = ITEMS.register("inserter", () -> new BlockItem(INSERTER.get(), new Item.Properties()));
    public static final RegistryObject<Item> LONG_INSERTER_ITEM = ITEMS.register("long_inserter", () -> new BlockItem(LONG_INSERTER.get(), new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> BURNER_INSERTER_ITEM = ITEMS.register("burner_inserter", () -> new BlockItem(BURNER_INSERTER.get(), new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> POWER_POLE_ITEM = ITEMS.register("power_pole", () -> new PowerPoleItem(POWER_POLE.get(), new Item.Properties()));
    public static final RegistryObject<Item> GENERATOR_ITEM = ITEMS.register("generator", () -> new CreativeOnlyItem(GENERATOR.get(), new Item.Properties().stacksTo(100)));
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
    public static final RegistryObject<Item> PIPE_ITEM = ITEMS.register("pipe", () -> new PipeItem(PIPE.get(), new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> BOILER_ITEM = ITEMS.register("boiler", () -> new MachineItem(BOILER.get(), new Item.Properties().stacksTo(100), MachineKind.BOILER));
    public static final RegistryObject<Item> STEAM_ENGINE_ITEM = ITEMS.register("steam_engine", () -> new MachineItem(STEAM_ENGINE.get(), new Item.Properties().stacksTo(100), MachineKind.STEAM_ENGINE));
    public static final RegistryObject<Item> OFFSHORE_PUMP_ITEM = ITEMS.register("offshore_pump", () -> new MachineItem(OFFSHORE_PUMP.get(), new Item.Properties().stacksTo(100), MachineKind.OFFSHORE_PUMP));
    public static final RegistryObject<Item> PUMPJACK_ITEM = ITEMS.register("pumpjack", () -> new MachineItem(PUMPJACK.get(), new Item.Properties().stacksTo(100), MachineKind.PUMPJACK));
    public static final RegistryObject<Item> REFINERY_ITEM = ITEMS.register("oil_refinery", () -> new MachineItem(REFINERY.get(), new Item.Properties().stacksTo(100), MachineKind.REFINERY));
    public static final RegistryObject<Item> CHEMICAL_PLANT_ITEM = ITEMS.register("chemical_plant", () -> new MachineItem(CHEMICAL_PLANT.get(), new Item.Properties().stacksTo(100), MachineKind.CHEMICAL_PLANT));
    public static final RegistryObject<Item> ENGINE_ITEM = ITEMS.register("engine_unit", () -> new Item(new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> PLASTIC_ITEM = ITEMS.register("plastic_bar", () -> new Item(new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> SULFUR_ITEM = ITEMS.register("sulfur", () -> new Item(new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> ADVANCED_CIRCUIT_ITEM = ITEMS.register("advanced_circuit", () -> new Item(new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> BLUE_SCIENCE_ITEM = ITEMS.register("chemical_science_pack", () -> new Item(new Item.Properties().stacksTo(100)));
    public static final RegistryObject<MachineBlock> ASSEMBLER_2 = BLOCKS.register("assembler_2", () -> new MachineBlock(metal().noOcclusion(), MachineKind.ASSEMBLER_2));
    public static final RegistryObject<MachinePartBlock> ASSEMBLER_2_PART = BLOCKS.register("assembler_2_part", () -> new MachinePartBlock(metal().noOcclusion(), MachineKind.ASSEMBLER_2));
    public static final RegistryObject<Item> ASSEMBLER_2_ITEM = ITEMS.register("assembler_2", () -> new MachineItem(ASSEMBLER_2.get(), new Item.Properties().stacksTo(100), MachineKind.ASSEMBLER_2));
    public static final RegistryObject<MachineBlock> STEEL_FURNACE = BLOCKS.register("steel_furnace", () -> new MachineBlock(metal().noOcclusion(), MachineKind.STEEL_FURNACE));
    public static final RegistryObject<MachinePartBlock> STEEL_FURNACE_PART = BLOCKS.register("steel_furnace_part", () -> new MachinePartBlock(metal().noOcclusion(), MachineKind.STEEL_FURNACE));
    public static final RegistryObject<Item> STEEL_FURNACE_ITEM = ITEMS.register("steel_furnace", () -> new MachineItem(STEEL_FURNACE.get(), new Item.Properties().stacksTo(100), MachineKind.STEEL_FURNACE));
    public static final RegistryObject<MachineBlock> STORAGE_TANK = BLOCKS.register("storage_tank", () -> new MachineBlock(metal().noOcclusion(), MachineKind.STORAGE_TANK));
    public static final RegistryObject<MachinePartBlock> STORAGE_TANK_PART = BLOCKS.register("storage_tank_part", () -> new MachinePartBlock(metal().noOcclusion(), MachineKind.STORAGE_TANK));
    public static final RegistryObject<Item> STORAGE_TANK_ITEM = ITEMS.register("storage_tank", () -> new MachineItem(STORAGE_TANK.get(), new Item.Properties().stacksTo(100), MachineKind.STORAGE_TANK));
    public static final RegistryObject<MachineBlock> FLUID_PUMP = BLOCKS.register("fluid_pump", () -> new MachineBlock(metal().noOcclusion(), MachineKind.FLUID_PUMP));
    public static final RegistryObject<MachinePartBlock> FLUID_PUMP_PART = BLOCKS.register("fluid_pump_part", () -> new MachinePartBlock(metal().noOcclusion(), MachineKind.FLUID_PUMP));
    public static final RegistryObject<Item> FLUID_PUMP_ITEM = ITEMS.register("fluid_pump", () -> new MachineItem(FLUID_PUMP.get(), new Item.Properties().stacksTo(100), MachineKind.FLUID_PUMP));
    public static final RegistryObject<MachineBlock> ACCUMULATOR = BLOCKS.register("accumulator", () -> new MachineBlock(metal().noOcclusion(), MachineKind.ACCUMULATOR));
    public static final RegistryObject<MachinePartBlock> ACCUMULATOR_PART = BLOCKS.register("accumulator_part", () -> new MachinePartBlock(metal().noOcclusion(), MachineKind.ACCUMULATOR));
    public static final RegistryObject<Item> ACCUMULATOR_ITEM = ITEMS.register("accumulator", () -> new MachineItem(ACCUMULATOR.get(), new Item.Properties().stacksTo(100), MachineKind.ACCUMULATOR));
    public static final RegistryObject<InserterBlock> FAST_INSERTER = BLOCKS.register("fast_inserter", () -> new InserterBlock(metal().noOcclusion(), 1));
    public static final RegistryObject<Item> FAST_INSERTER_ITEM = ITEMS.register("fast_inserter", () -> new BlockItem(FAST_INSERTER.get(), new Item.Properties().stacksTo(100)));
    public static final RegistryObject<ConveyorBlock> FAST_CONVEYOR = BLOCKS.register("fast_conveyor", () -> new ConveyorBlock(metal().noOcclusion()));
    public static final RegistryObject<ConveyorBlock> FAST_SLOPE_CONVEYOR = BLOCKS.register("fast_slope_conveyor", () -> new SlopeConveyorBlock(metal().noOcclusion()));
    public static final RegistryObject<JunctionBlock> FAST_JUNCTION = BLOCKS.register("fast_junction", () -> new JunctionBlock(metal().noOcclusion()));
    public static final RegistryObject<Item> FAST_CONVEYOR_ITEM = ITEMS.register("fast_conveyor", () -> new ConveyorItem(FAST_CONVEYOR.get(), new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> FAST_SLOPE_CONVEYOR_ITEM = ITEMS.register("fast_slope_conveyor", () -> new ConveyorItem(FAST_SLOPE_CONVEYOR.get(), new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> FAST_JUNCTION_ITEM = ITEMS.register("fast_junction", () -> new BlockItem(FAST_JUNCTION.get(), new Item.Properties().stacksTo(100)));
    public static final RegistryObject<CrateBlock> STEEL_CRATE = BLOCKS.register("steel_crate", () -> new CrateBlock(metal(), 48));
    public static final RegistryObject<Item> STEEL_CRATE_ITEM = ITEMS.register("steel_crate", () -> new BlockItem(STEEL_CRATE.get(), new Item.Properties().stacksTo(100)));
    public static final RegistryObject<MenuType<ModuleMenu>> MODULE_MENU = MENUS.register("modules", () -> new MenuType<>(ModuleMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<CrateMenu>> STEEL_CRATE_MENU = MENUS.register("steel_crate", () -> new MenuType<>((id, inventory) -> new CrateMenu(id, inventory, 48), FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<Item> BATTERY_ITEM = ITEMS.register("battery", () -> new Item(new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> SOLID_FUEL_ITEM = ITEMS.register("solid_fuel", () -> new Item(new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> SPEED_MODULE_ITEM = ITEMS.register("speed_module", () -> new Item(new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> PRODUCTIVITY_MODULE_ITEM = ITEMS.register("productivity_module", () -> new Item(new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> EFFICIENCY_MODULE_ITEM = ITEMS.register("efficiency_module", () -> new Item(new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> PIERCING_MAGAZINE_ITEM = ITEMS.register("piercing_magazine", () -> new Item(new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> SHOTGUN_SHELL_ITEM = ITEMS.register("shotgun_shell", () -> new Item(new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> SUBMACHINE_GUN_ITEM = ITEMS.register("submachine_gun", () -> new dev.conveyance.combat.PistolItem(new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> SHOTGUN_ITEM = ITEMS.register("shotgun", () -> new dev.conveyance.combat.PistolItem(new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> GRENADE_ITEM = ITEMS.register("grenade", () -> new dev.conveyance.combat.GrenadeItem(new Item.Properties().stacksTo(100)));
    public static final RegistryObject<BlockEntityType<MinerEntity>> MINER_ENTITY = ENTITIES.register("miner",
            () -> BlockEntityType.Builder.of(MinerEntity::new, MINER.get()).build(null));
    public static final RegistryObject<BlockEntityType<CrateEntity>> CRATE_ENTITY = ENTITIES.register("crate",
            () -> BlockEntityType.Builder.of(CrateEntity::new, WOOD_CRATE.get(), IRON_CRATE.get(), STEEL_CRATE.get()).build(null));
    public static final RegistryObject<BlockEntityType<ConveyorEntity>> CONVEYOR_ENTITY = ENTITIES.register("conveyor",
            () -> BlockEntityType.Builder.of(ConveyorEntity::new, CONVEYOR.get(), SLOPE_CONVEYOR.get(), FAST_CONVEYOR.get(), FAST_SLOPE_CONVEYOR.get()).build(null));
    public static final RegistryObject<BlockEntityType<JunctionEntity>> JUNCTION_ENTITY = ENTITIES.register("junction",
            () -> BlockEntityType.Builder.of(JunctionEntity::new, JUNCTION.get(), FAST_JUNCTION.get()).build(null));
    public static final RegistryObject<BlockEntityType<InserterEntity>> INSERTER_ENTITY = ENTITIES.register("inserter",
            () -> BlockEntityType.Builder.of(InserterEntity::new, INSERTER.get(), FAST_INSERTER.get()).build(null));
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
            () -> BlockEntityType.Builder.of(MachineEntity::new, BURNER_MINER.get(), ASSEMBLER.get(), FURNACE.get(), LAB.get(), SOLAR_PANEL.get(), BOILER.get(), STEAM_ENGINE.get(), OFFSHORE_PUMP.get(), PUMPJACK.get(), REFINERY.get(), CHEMICAL_PLANT.get(), GUN_TURRET.get(), ASSEMBLER_2.get(), STEEL_FURNACE.get(), STORAGE_TANK.get(), FLUID_PUMP.get(), ACCUMULATOR.get(), LASER_TURRET.get()).build(null));
    public static final RegistryObject<MenuType<dev.conveyance.combat.TurretMenu>> TURRET_MENU = MENUS.register("gun_turret", () -> new MenuType<>(dev.conveyance.combat.TurretMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<dev.conveyance.combat.LaserTurretMenu>> LASER_TURRET_MENU = MENUS.register("laser_turret", () -> new MenuType<>(dev.conveyance.combat.LaserTurretMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<BlockEntityType<PipeEntity>> PIPE_ENTITY = ENTITIES.register("pipe",
            () -> BlockEntityType.Builder.of(PipeEntity::new, PIPE.get()).build(null));
    public static final RegistryObject<MenuType<GeneratorMenu>> GENERATOR_MENU = MENUS.register("generator",
            () -> new MenuType<>(GeneratorMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<CrateMenu>> WOOD_CRATE_MENU = MENUS.register("wood_crate",
            () -> new MenuType<>((id, inventory) -> new CrateMenu(id, inventory, 16), FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<CrateMenu>> IRON_CRATE_MENU = MENUS.register("iron_crate",
            () -> new MenuType<>((id, inventory) -> new CrateMenu(id, inventory, 32), FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<GeneratorMenu>> LAB_MENU = MENUS.register("research_lab",
            () -> new MenuType<>((id, inventory) -> new GeneratorMenu(id, inventory, new SimpleContainer(dev.conveyance.player.Technology.SCIENCE_SLOTS), true), FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<ProcessingMenu>> ASSEMBLER_MENU = MENUS.register("assembler",
            () -> new MenuType<>((id, inventory) -> new ProcessingMenu(id, inventory, MachineKind.ASSEMBLER), FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<ProcessingMenu>> FURNACE_MENU = MENUS.register("industrial_furnace",
            () -> new MenuType<>((id, inventory) -> new ProcessingMenu(id, inventory, MachineKind.FURNACE), FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<PlayerWorkbenchMenu>> PLAYER_WORKBENCH_MENU = MENUS.register("player_workbench",
            () -> new MenuType<>(PlayerWorkbenchMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<FluidMenu>> BOILER_MENU = MENUS.register("boiler",
            () -> new MenuType<>((id, inventory) -> new FluidMenu(id, inventory, MachineKind.BOILER), FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<FluidMenu>> STEAM_ENGINE_MENU = MENUS.register("steam_engine",
            () -> new MenuType<>((id, inventory) -> new FluidMenu(id, inventory, MachineKind.STEAM_ENGINE), FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<FluidMenu>> OFFSHORE_PUMP_MENU = MENUS.register("offshore_pump",
            () -> new MenuType<>((id, inventory) -> new FluidMenu(id, inventory, MachineKind.OFFSHORE_PUMP), FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<ChemicalMenu>> PUMPJACK_MENU = MENUS.register("pumpjack", () -> new MenuType<>((id, inventory) -> new ChemicalMenu(id, inventory, MachineKind.PUMPJACK), FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<ChemicalMenu>> REFINERY_MENU = MENUS.register("oil_refinery", () -> new MenuType<>((id, inventory) -> new ChemicalMenu(id, inventory, MachineKind.REFINERY), FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<ChemicalMenu>> CHEMICAL_MENU = MENUS.register("chemical_plant", () -> new MenuType<>((id, inventory) -> new ChemicalMenu(id, inventory, MachineKind.CHEMICAL_PLANT), FeatureFlags.DEFAULT_FLAGS));

    static {
        TABS.register("factory", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.conveyance.factory"))
                .icon(() -> MINER_ITEM.get().getDefaultInstance()).displayItems((parameters, output) -> {
                    output.accept(ASSEMBLER_2_ITEM.get()); output.accept(STEEL_FURNACE_ITEM.get()); output.accept(STORAGE_TANK_ITEM.get()); output.accept(FLUID_PUMP_ITEM.get()); output.accept(ACCUMULATOR_ITEM.get()); output.accept(FAST_INSERTER_ITEM.get()); output.accept(FAST_CONVEYOR_ITEM.get()); output.accept(FAST_SLOPE_CONVEYOR_ITEM.get()); output.accept(FAST_JUNCTION_ITEM.get()); output.accept(STEEL_CRATE_ITEM.get()); output.accept(BATTERY_ITEM.get()); output.accept(SOLID_FUEL_ITEM.get()); output.accept(SPEED_MODULE_ITEM.get()); output.accept(PRODUCTIVITY_MODULE_ITEM.get()); output.accept(EFFICIENCY_MODULE_ITEM.get()); output.accept(PIERCING_MAGAZINE_ITEM.get()); output.accept(SHOTGUN_SHELL_ITEM.get()); output.accept(SUBMACHINE_GUN_ITEM.get()); output.accept(SHOTGUN_ITEM.get()); output.accept(GRENADE_ITEM.get());
                    output.accept(WOOD_ITEM.get()); output.accept(WOOD_CRATE_ITEM.get()); output.accept(IRON_CRATE_ITEM.get());
                    output.accept(PISTOL.get()); output.accept(FIREARM_MAGAZINE.get()); output.accept(REPAIR_PACK.get()); output.accept(GUN_TURRET_ITEM.get()); output.accept(STONE_WALL_ITEM.get()); output.accept(LASER_TURRET_ITEM.get()); output.accept(MILITARY_SCIENCE_ITEM.get());
                    output.accept(MINER_ITEM.get()); output.accept(CONVEYOR_ITEM.get()); output.accept(SLOPE_CONVEYOR_ITEM.get()); output.accept(INSERTER_ITEM.get());
                    output.accept(JUNCTION_ITEM.get());
                    output.accept(POWER_POLE_ITEM.get()); output.accept(GENERATOR_ITEM.get());
                    output.accept(PIPE_ITEM.get()); output.accept(OFFSHORE_PUMP_ITEM.get()); output.accept(BOILER_ITEM.get()); output.accept(STEAM_ENGINE_ITEM.get());
                    output.accept(PUMPJACK_ITEM.get()); output.accept(REFINERY_ITEM.get()); output.accept(CHEMICAL_PLANT_ITEM.get());
                    output.accept(ENGINE_ITEM.get()); output.accept(PLASTIC_ITEM.get()); output.accept(SULFUR_ITEM.get()); output.accept(ADVANCED_CIRCUIT_ITEM.get()); output.accept(BLUE_SCIENCE_ITEM.get());
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
