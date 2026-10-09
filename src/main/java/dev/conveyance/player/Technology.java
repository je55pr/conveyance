package dev.conveyance.player;

import dev.conveyance.factory.FactoryContent;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Every node unlocks a usable recipe/ability or upgrades existing machinery. */
public enum Technology {
    AUTOMATION(1, "automation", 10, false, 0, 100, 64, Set.of("assembler")),
    EXCAVATION(2, "excavation", 30, true, 8 | 128 | 256, 500, 128, Set.of("iron_pickaxe")),
    LOGISTICS(4, "logistics", 20, false, 1, 200, 0, Set.of("inserter", "junction")),
    ELECTRIC_MINING(8, "electric_mining", 25, false, 1, 200, 128, Set.of("miner")),
    METAL_STORAGE(16, "metal_storage", 10, false, 0, 100, 192, Set.of("iron_crate")),
    SURVEYING(32, "surveying", 20, false, 8, 300, 128, Set.of("radar")),
    LONG_INSERTERS(64, "long_inserters", 20, false, 4, 300, 0, Set.of("long_inserter")),
    STEEL_PROCESSING(128, "steel_processing", 30, false, 1 | 16, 200, 192, Set.of("steel_ingot")),
    GREEN_SCIENCE(256, "green_science", 30, false, 4, 300, 64, Set.of("green_science_pack")),
    SOLAR_ENERGY(512, "solar_energy", 50, true, 128 | 256, 500, 192, Set.of("solar_panel")),
    RESEARCH_SPEED_1(1024, "research_speed_1", 30, true, 256, 400, 64, Set.of()),
    RESEARCH_SPEED_2(2048, "research_speed_2", 50, true, 1024, 500, 64, Set.of()),
    MINING_SPEED(4096, "mining_speed", 40, true, 8 | 256, 400, 128, Set.of()),
    ASSEMBLY_SPEED(8192, "assembly_speed", 40, true, 1 | 128 | 256, 500, 0, Set.of()),
    SMELTING_SPEED(16384, "smelting_speed", 40, true, 128 | 256, 400, 192, Set.of()),
    INSERTER_SPEED(32768, "inserter_speed", 30, true, 64 | 256, 400, 0, Set.of()),
    POWER_EFFICIENCY(65536, "power_efficiency", 50, true, 512, 600, 192, Set.of()),
    BURNER_EFFICIENCY(131072, "burner_efficiency", 35, true, 128 | 256, 600, 128, Set.of());

    public final int id, packs, prerequisiteMask, x, y;
    public final boolean green;
    public final String key;
    public final Set<String> recipes;
    Technology(int id, String key, int packs, boolean green, int prerequisites, int x, int y, Set<String> recipes) {
        this.id = id; this.key = key; this.packs = packs; this.green = green;
        this.prerequisiteMask = prerequisites; this.x = x; this.y = y; this.recipes = recipes;
    }
    public Component title() { return Component.translatable("technology.conveyance." + key + ".name"); }
    public Component description() { return Component.translatable("technology.conveyance." + key + ".description"); }
    public Component shortName() { return Component.translatable("technology.conveyance." + key + ".short"); }
    public Component scienceCost() {
        return green ? Component.translatable("research.conveyance.cost_red_green", packs, packs)
                : Component.translatable("research.conveyance.cost_red", packs);
    }
    public boolean complete(int mask) { return (mask & id) != 0; }
    public boolean available(int mask) { return !complete(mask) && (mask & prerequisiteMask) == prerequisiteMask; }
    public List<Technology> prerequisites() { return Arrays.stream(values()).filter(tech -> (prerequisiteMask & tech.id) != 0).toList(); }
    public ItemStack icon() { return switch (this) {
        case AUTOMATION, ASSEMBLY_SPEED -> new ItemStack(FactoryContent.ASSEMBLER_ITEM.get());
        case LOGISTICS -> new ItemStack(FactoryContent.JUNCTION_ITEM.get());
        case ELECTRIC_MINING, MINING_SPEED -> new ItemStack(FactoryContent.MINER_ITEM.get());
        case EXCAVATION -> new ItemStack(Items.IRON_PICKAXE);
        case METAL_STORAGE -> new ItemStack(FactoryContent.IRON_CRATE_ITEM.get());
        case SURVEYING -> new ItemStack(FactoryContent.RADAR_ITEM.get());
        case LONG_INSERTERS -> new ItemStack(FactoryContent.LONG_INSERTER_ITEM.get());
        case STEEL_PROCESSING -> new ItemStack(FactoryContent.STEEL_ITEM.get());
        case GREEN_SCIENCE -> new ItemStack(FactoryContent.GREEN_SCIENCE_ITEM.get());
        case SOLAR_ENERGY, POWER_EFFICIENCY -> new ItemStack(FactoryContent.SOLAR_PANEL_ITEM.get());
        case RESEARCH_SPEED_1, RESEARCH_SPEED_2 -> new ItemStack(FactoryContent.LAB_ITEM.get());
        case SMELTING_SPEED -> new ItemStack(FactoryContent.FURNACE_ITEM.get());
        case INSERTER_SPEED -> new ItemStack(FactoryContent.INSERTER_ITEM.get());
        case BURNER_EFFICIENCY -> new ItemStack(FactoryContent.GENERATOR_ITEM.get());
    }; }
    public List<ItemStack> unlocks() { return switch (this) {
        case LOGISTICS -> List.of(new ItemStack(FactoryContent.INSERTER_ITEM.get()), new ItemStack(FactoryContent.JUNCTION_ITEM.get()));
        case METAL_STORAGE -> List.of(new ItemStack(FactoryContent.IRON_CRATE_ITEM.get()));
        default -> List.of(icon());
    }; }
    public int menuId() { return ordinal() + 1; }
    public static Technology byMenuId(int id) { return id > 0 && id <= values().length ? values()[id - 1] : null; }
    public static Technology byId(int id) { return Arrays.stream(values()).filter(tech -> tech.id == id).findFirst().orElse(null); }
}
