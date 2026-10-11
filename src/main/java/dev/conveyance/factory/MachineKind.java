package dev.conveyance.factory;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public enum MachineKind {
    BURNER_MINER("burner_miner", 2, 2, 1, 0, 80), ASSEMBLER("assembler", 3, 2, 6, 75, 40),
    FURNACE("industrial_furnace", 2, 3, 3, 0, 40), LAB("research_lab", 3, 2, 4, 60, 60), SOLAR_PANEL("solar_panel", 3, 1, 0, 0, 40),
    BOILER("boiler", 3, 2, 2, 1, 0, 40), STEAM_ENGINE("steam_engine", 3, 5, 1, 0, 0, 40), OFFSHORE_PUMP("offshore_pump", 1, 2, 2, 0, 0, 40),
    PUMPJACK("pumpjack", 3, 2, 0, 90, 20), REFINERY("oil_refinery", 5, 2, 3, 420, 20), CHEMICAL_PLANT("chemical_plant", 3, 2, 3, 210, 20), GUN_TURRET("gun_turret", 2, 3, 1, 0, 2),
    ASSEMBLER_2("assembler_2", 3, 2, 6, 150, 40), STEEL_FURNACE("steel_furnace", 2, 3, 3, 0, 40),
    STORAGE_TANK("storage_tank", 3, 2, 0, 0, 40), FLUID_PUMP("fluid_pump", 1, 2, 1, 0, 30, 20), ACCUMULATOR("accumulator", 2, 2, 0, 0, 20),
    LASER_TURRET("laser_turret", 2, 3, 0, 1200, 40);
    public final String id;
    public final int width, depth, height, slots, powerKW, cycleTicks;
    MachineKind(String id, int width, int height, int slots, int powerKW, int cycleTicks) {
        this(id, width, width, height, slots, powerKW, cycleTicks);
    }
    MachineKind(String id, int width, int depth, int height, int slots, int powerKW, int cycleTicks) {
        this.id = id; this.width = width; this.depth = depth; this.height = height; this.slots = slots; this.powerKW = powerKW; this.cycleTicks = cycleTicks;
    }
    public boolean assembler() { return this == ASSEMBLER || this == ASSEMBLER_2; }
    public boolean furnace() { return this == FURNACE || this == STEEL_FURNACE; }
    public boolean turret() { return this == GUN_TURRET || this == LASER_TURRET; }
    public boolean utility() { return this == STORAGE_TANK || this == FLUID_PUMP || this == ACCUMULATOR; }
    public int moduleSlots() { return this == ASSEMBLER_2 || this == LAB ? 2 : this == PUMPJACK || this == REFINERY || this == CHEMICAL_PLANT ? 3 : 0; }
    public double craftingSpeed() { return this == ASSEMBLER_2 ? .75 : .5; }
    public double smeltingSpeed() { return this == STEEL_FURNACE ? 2 : 1; }
    public boolean chemical() { return this == PUMPJACK || this == REFINERY || this == CHEMICAL_PLANT; }
    public boolean fluids() { return this == BOILER || this == STEAM_ENGINE || this == OFFSHORE_PUMP || chemical() || this == STORAGE_TANK || this == FLUID_PUMP; }
    public double idleKW() { return this == LASER_TURRET ? 24 : this == ASSEMBLER_2 ? 5 : this == ASSEMBLER ? 2.5 : this == REFINERY ? 14 : this == CHEMICAL_PLANT ? 7 : 0; }
    public int burnerKW() { return this == BURNER_MINER ? 150 : this == STEEL_FURNACE ? 90 : this == FURNACE ? 90 : 0; }
    public int parts() { return width * depth * height; }
    public Item item() { return switch (this) {
        case LASER_TURRET -> FactoryContent.LASER_TURRET_ITEM.get();
        case ACCUMULATOR -> FactoryContent.ACCUMULATOR_ITEM.get();
        case FLUID_PUMP -> FactoryContent.FLUID_PUMP_ITEM.get();
        case STORAGE_TANK -> FactoryContent.STORAGE_TANK_ITEM.get();
        case STEEL_FURNACE -> FactoryContent.STEEL_FURNACE_ITEM.get();
        case ASSEMBLER_2 -> FactoryContent.ASSEMBLER_2_ITEM.get();
        case BURNER_MINER -> FactoryContent.BURNER_MINER_ITEM.get();
        case ASSEMBLER -> FactoryContent.ASSEMBLER_ITEM.get();
        case FURNACE -> FactoryContent.FURNACE_ITEM.get();
        case LAB -> FactoryContent.LAB_ITEM.get();
        case SOLAR_PANEL -> FactoryContent.SOLAR_PANEL_ITEM.get();
        case BOILER -> FactoryContent.BOILER_ITEM.get();
        case STEAM_ENGINE -> FactoryContent.STEAM_ENGINE_ITEM.get();
        case OFFSHORE_PUMP -> FactoryContent.OFFSHORE_PUMP_ITEM.get();
        case PUMPJACK -> FactoryContent.PUMPJACK_ITEM.get();
        case REFINERY -> FactoryContent.REFINERY_ITEM.get();
        case CHEMICAL_PLANT -> FactoryContent.CHEMICAL_PLANT_ITEM.get();
        case GUN_TURRET -> FactoryContent.GUN_TURRET_ITEM.get();
    }; }
    public Block casing() { return switch (this) {
        case LASER_TURRET -> FactoryContent.LASER_TURRET_PART.get();
        case ACCUMULATOR -> FactoryContent.ACCUMULATOR_PART.get();
        case FLUID_PUMP -> FactoryContent.FLUID_PUMP_PART.get();
        case STORAGE_TANK -> FactoryContent.STORAGE_TANK_PART.get();
        case STEEL_FURNACE -> FactoryContent.STEEL_FURNACE_PART.get();
        case ASSEMBLER_2 -> FactoryContent.ASSEMBLER_2_PART.get();
        case BURNER_MINER -> FactoryContent.BURNER_MINER_PART.get();
        case ASSEMBLER -> FactoryContent.ASSEMBLER_PART.get();
        case FURNACE -> FactoryContent.FURNACE_PART.get();
        case LAB -> FactoryContent.LAB_PART.get();
        case SOLAR_PANEL -> FactoryContent.SOLAR_PANEL_PART.get();
        case BOILER -> FactoryContent.BOILER_PART.get();
        case STEAM_ENGINE -> FactoryContent.STEAM_ENGINE_PART.get();
        case OFFSHORE_PUMP -> FactoryContent.OFFSHORE_PUMP_PART.get();
        case PUMPJACK -> FactoryContent.PUMPJACK_PART.get();
        case REFINERY -> FactoryContent.REFINERY_PART.get();
        case CHEMICAL_PLANT -> FactoryContent.CHEMICAL_PLANT_PART.get();
        case GUN_TURRET -> FactoryContent.GUN_TURRET_PART.get();
    }; }
}
