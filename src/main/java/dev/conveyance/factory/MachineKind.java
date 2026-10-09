package dev.conveyance.factory;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public enum MachineKind {
    BURNER_MINER("burner_miner", 2, 2, 1), ASSEMBLER("assembler", 3, 2, 5), FURNACE("industrial_furnace", 2, 3, 3), LAB("research_lab", 3, 2, 2), SOLAR_PANEL("solar_panel", 3, 1, 0);
    public final String id;
    public final int width, height, slots;
    MachineKind(String id, int width, int height, int slots) { this.id = id; this.width = width; this.height = height; this.slots = slots; }
    public int parts() { return width * width * height; }
    public Item item() { return switch (this) {
        case BURNER_MINER -> FactoryContent.BURNER_MINER_ITEM.get();
        case ASSEMBLER -> FactoryContent.ASSEMBLER_ITEM.get();
        case FURNACE -> FactoryContent.FURNACE_ITEM.get();
        case LAB -> FactoryContent.LAB_ITEM.get();
        case SOLAR_PANEL -> FactoryContent.SOLAR_PANEL_ITEM.get();
    }; }
    public Block casing() { return switch (this) {
        case BURNER_MINER -> FactoryContent.BURNER_MINER_PART.get();
        case ASSEMBLER -> FactoryContent.ASSEMBLER_PART.get();
        case FURNACE -> FactoryContent.FURNACE_PART.get();
        case LAB -> FactoryContent.LAB_PART.get();
        case SOLAR_PANEL -> FactoryContent.SOLAR_PANEL_PART.get();
    }; }
}
