package dev.conveyance.factory;

import dev.conveyance.Conveyance;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
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

    public static final RegistryObject<MinerBlock> MINER = BLOCKS.register("miner", () -> new MinerBlock(metal()));
    public static final RegistryObject<MinerPartBlock> MINER_PART = BLOCKS.register("miner_part", () -> new MinerPartBlock(metal()));
    public static final RegistryObject<ConveyorBlock> CONVEYOR = BLOCKS.register("conveyor", () -> new ConveyorBlock(metal().noOcclusion()));
    public static final RegistryObject<InserterBlock> INSERTER = BLOCKS.register("inserter", () -> new InserterBlock(metal().noOcclusion()));
    public static final RegistryObject<PowerPoleBlock> POWER_POLE = BLOCKS.register("power_pole", () -> new PowerPoleBlock(metal().noOcclusion()));
    public static final RegistryObject<PowerPolePartBlock> POWER_POLE_PART = BLOCKS.register("power_pole_part", () -> new PowerPolePartBlock(metal().noOcclusion()));
    public static final RegistryObject<GeneratorBlock> GENERATOR = BLOCKS.register("generator", () -> new GeneratorBlock(metal().lightLevel(state -> state.getValue(GeneratorBlock.LIT) ? 10 : 0)));
    public static final RegistryObject<MachineBlock> BURNER_MINER = BLOCKS.register("burner_miner", () -> new MachineBlock(metal(), MachineKind.BURNER_MINER));
    public static final RegistryObject<MachinePartBlock> BURNER_MINER_PART = BLOCKS.register("burner_miner_part", () -> new MachinePartBlock(metal(), MachineKind.BURNER_MINER));
    public static final RegistryObject<MachineBlock> ASSEMBLER = BLOCKS.register("assembler", () -> new MachineBlock(metal(), MachineKind.ASSEMBLER));
    public static final RegistryObject<MachinePartBlock> ASSEMBLER_PART = BLOCKS.register("assembler_part", () -> new MachinePartBlock(metal(), MachineKind.ASSEMBLER));
    public static final RegistryObject<MachineBlock> FURNACE = BLOCKS.register("industrial_furnace", () -> new MachineBlock(metal(), MachineKind.FURNACE));
    public static final RegistryObject<MachinePartBlock> FURNACE_PART = BLOCKS.register("industrial_furnace_part", () -> new MachinePartBlock(metal(), MachineKind.FURNACE));
    public static final RegistryObject<Item> MINER_ITEM = ITEMS.register("miner", () -> new MinerItem(MINER.get(), new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> CONVEYOR_ITEM = ITEMS.register("conveyor", () -> new BlockItem(CONVEYOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> INSERTER_ITEM = ITEMS.register("inserter", () -> new BlockItem(INSERTER.get(), new Item.Properties()));
    public static final RegistryObject<Item> POWER_POLE_ITEM = ITEMS.register("power_pole", () -> new PowerPoleItem(POWER_POLE.get(), new Item.Properties()));
    public static final RegistryObject<Item> GENERATOR_ITEM = ITEMS.register("generator", () -> new BlockItem(GENERATOR.get(), new Item.Properties().stacksTo(100)));
    public static final RegistryObject<Item> BURNER_MINER_ITEM = ITEMS.register("burner_miner", () -> new MachineItem(BURNER_MINER.get(), new Item.Properties().stacksTo(100), MachineKind.BURNER_MINER));
    public static final RegistryObject<Item> ASSEMBLER_ITEM = ITEMS.register("assembler", () -> new MachineItem(ASSEMBLER.get(), new Item.Properties().stacksTo(100), MachineKind.ASSEMBLER));
    public static final RegistryObject<Item> FURNACE_ITEM = ITEMS.register("industrial_furnace", () -> new MachineItem(FURNACE.get(), new Item.Properties().stacksTo(100), MachineKind.FURNACE));
    public static final RegistryObject<BlockEntityType<MinerEntity>> MINER_ENTITY = ENTITIES.register("miner",
            () -> BlockEntityType.Builder.of(MinerEntity::new, MINER.get()).build(null));
    public static final RegistryObject<BlockEntityType<ConveyorEntity>> CONVEYOR_ENTITY = ENTITIES.register("conveyor",
            () -> BlockEntityType.Builder.of(ConveyorEntity::new, CONVEYOR.get()).build(null));
    public static final RegistryObject<BlockEntityType<InserterEntity>> INSERTER_ENTITY = ENTITIES.register("inserter",
            () -> BlockEntityType.Builder.of(InserterEntity::new, INSERTER.get()).build(null));
    public static final RegistryObject<BlockEntityType<PowerPoleEntity>> POWER_POLE_ENTITY = ENTITIES.register("power_pole",
            () -> BlockEntityType.Builder.of(PowerPoleEntity::new, POWER_POLE.get()).build(null));
    public static final RegistryObject<BlockEntityType<GeneratorEntity>> GENERATOR_ENTITY = ENTITIES.register("generator",
            () -> BlockEntityType.Builder.of(GeneratorEntity::new, GENERATOR.get()).build(null));
    public static final RegistryObject<BlockEntityType<MachineEntity>> MACHINE_ENTITY = ENTITIES.register("machine",
            () -> BlockEntityType.Builder.of(MachineEntity::new, BURNER_MINER.get(), ASSEMBLER.get(), FURNACE.get()).build(null));
    public static final RegistryObject<MenuType<GeneratorMenu>> GENERATOR_MENU = MENUS.register("generator",
            () -> new MenuType<>(GeneratorMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<ProcessingMenu>> ASSEMBLER_MENU = MENUS.register("assembler",
            () -> new MenuType<>((id, inventory) -> new ProcessingMenu(id, inventory, MachineKind.ASSEMBLER), FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MenuType<ProcessingMenu>> FURNACE_MENU = MENUS.register("industrial_furnace",
            () -> new MenuType<>((id, inventory) -> new ProcessingMenu(id, inventory, MachineKind.FURNACE), FeatureFlags.DEFAULT_FLAGS));

    static {
        TABS.register("factory", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.conveyance.factory"))
                .icon(() -> MINER_ITEM.get().getDefaultInstance()).displayItems((parameters, output) -> {
                    output.accept(MINER_ITEM.get()); output.accept(CONVEYOR_ITEM.get()); output.accept(INSERTER_ITEM.get());
                    output.accept(POWER_POLE_ITEM.get()); output.accept(GENERATOR_ITEM.get());
                    output.accept(BURNER_MINER_ITEM.get()); output.accept(FURNACE_ITEM.get()); output.accept(ASSEMBLER_ITEM.get());
                }).build());
    }

    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL);
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); MENUS.register(bus); TABS.register(bus);
    }
    private FactoryContent() {}
}
