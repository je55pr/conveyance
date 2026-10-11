package dev.conveyance.player;

import dev.conveyance.factory.FactoryContent;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Base Factorio 2.0.77 progression; terrain tools are a Conveyance addition. */
public enum Technology {
    STEAM_POWER("steam_power", 0, 0, 0, "conveyance:boiler", "minecraft:iron_ingot", 50, List.of(), Set.of("pipe", "offshore_pump", "boiler", "steam_engine")),
    ELECTRONICS("electronics", 0, 0, 0, "conveyance:electronic_circuit", "minecraft:copper_ingot", 10, List.of(), Set.of("copper_wire", "electronic_circuit", "research_lab", "inserter", "power_pole")),
    RED_SCIENCE("red_science", 0, 0, 0, "conveyance:red_science_pack", "conveyance:research_lab", 1, List.of("steam_power", "electronics"), Set.of("red_science_pack")),
    AUTOMATION("automation", 10, 10, 1, "conveyance:assembler", "", 0, List.of("red_science"), Set.of("assembler", "long_inserter")),
    LOGISTICS("logistics", 20, 15, 1, "conveyance:junction", "", 0, List.of("red_science"), Set.of("junction")),
    ELECTRIC_MINING("electric_mining", 25, 10, 1, "conveyance:miner", "", 0, List.of("red_science"), Set.of("miner")),
    SURVEYING("surveying", 20, 10, 1, "conveyance:radar", "", 0, List.of("red_science"), Set.of("radar")),
    REPAIR("repair", 25, 10, 1, "conveyance:repair_pack", "", 0, List.of("red_science"), Set.of("repair_pack")),
    STEEL_PROCESSING("steel_processing", 50, 5, 1, "conveyance:steel_ingot", "", 0, List.of("red_science"), Set.of("steel_ingot", "steel_crate")),
    GREEN_SCIENCE("green_science", 75, 5, 1, "conveyance:green_science_pack", "", 0, List.of("red_science"), Set.of("green_science_pack")),
    FAST_INSERTERS("fast_inserters", 30, 15, 1, "conveyance:fast_inserter", "", 0, List.of("red_science"), Set.of("fast_inserter")),
    STONE_WALLS("stone_walls", 10, 10, 1, "conveyance:stone_wall", "", 0, List.of("red_science"), Set.of("stone_wall")),
    BASIC_DEFENCE("basic_defence", 10, 10, 1, "conveyance:gun_turret", "", 0, List.of("red_science"), Set.of("gun_turret")),
    MILITARY("military", 10, 15, 1, "conveyance:submachine_gun", "", 0, List.of("red_science"), Set.of("submachine_gun", "shotgun", "shotgun_shell")),
    MILITARY_2("military_2", 20, 15, 3, "conveyance:piercing_magazine", "", 0, List.of("military", "steel_processing", "green_science"), Set.of("piercing_magazine", "grenade")),
    AUTOMATION_2("automation_2", 40, 15, 3, "conveyance:assembler_2", "", 0, List.of("automation", "steel_processing", "green_science"), Set.of("assembler_2")),
    ADVANCED_SMELTING("advanced_smelting", 75, 30, 3, "conveyance:steel_furnace", "", 0, List.of("steel_processing", "green_science"), Set.of("steel_furnace")),
    SOLAR_ENERGY("solar_energy", 250, 30, 3, "conveyance:solar_panel", "", 0, List.of("steel_processing", "green_science"), Set.of("solar_panel")),
    RESEARCH_SPEED_1("research_speed_1", 100, 30, 3, "conveyance:research_lab", "", 0, List.of("automation_2"), Set.of()),
    RESEARCH_SPEED_2("research_speed_2", 200, 30, 3, "conveyance:research_lab", "", 0, List.of("research_speed_1"), Set.of()),
    ENGINE("engine", 100, 15, 3, "conveyance:engine_unit", "", 0, List.of("steel_processing", "green_science"), Set.of("engine_unit")),
    FLUID_HANDLING("fluid_handling", 50, 15, 3, "conveyance:storage_tank", "", 0, List.of("automation_2", "engine"), Set.of("storage_tank", "fluid_pump")),
    OIL_GATHERING("oil_gathering", 100, 30, 3, "conveyance:pumpjack", "", 0, List.of("fluid_handling"), Set.of("pumpjack")),
    OIL_PROCESSING("oil_processing", 0, 0, 0, "conveyance:oil_refinery", "crude_oil", 1, List.of("oil_gathering"), Set.of("oil_refinery", "chemical_plant", "basic_oil_processing", "solid_fuel")),
    PLASTICS("plastics", 200, 30, 3, "conveyance:plastic_bar", "", 0, List.of("oil_processing"), Set.of("plastic_bar")),
    SULFUR_PROCESSING("sulfur_processing", 150, 30, 3, "conveyance:sulfur", "", 0, List.of("oil_processing"), Set.of("sulfuric_acid", "sulfur")),
    ADVANCED_CIRCUITS("advanced_circuits", 200, 15, 3, "conveyance:advanced_circuit", "", 0, List.of("plastics"), Set.of("advanced_circuit")),
    CHEMICAL_SCIENCE("chemical_science", 75, 10, 3, "conveyance:chemical_science_pack", "", 0, List.of("advanced_circuits", "sulfur_processing"), Set.of("chemical_science_pack")),
    BATTERY("battery", 150, 30, 3, "conveyance:battery", "", 0, List.of("sulfur_processing"), Set.of("battery")),
    ACCUMULATORS("accumulators", 150, 30, 3, "conveyance:accumulator", "", 0, List.of("steel_processing", "green_science", "battery"), Set.of("accumulator")),
    LOGISTICS_2("logistics_2", 200, 30, 3, "conveyance:fast_conveyor", "", 0, List.of("logistics", "green_science"), Set.of("fast_conveyor", "fast_junction", "fast_slope_conveyor")),
    MODULES("modules", 100, 30, 3, "conveyance:speed_module", "", 0, List.of("advanced_circuits"), Set.of()),
    SPEED_MODULE("speed_module", 50, 30, 3, "conveyance:speed_module", "", 0, List.of("modules"), Set.of("speed_module")),
    PRODUCTIVITY_MODULE("productivity_module", 50, 30, 3, "conveyance:productivity_module", "", 0, List.of("modules"), Set.of("productivity_module")),
    EFFICIENCY_MODULE("efficiency_module", 50, 30, 3, "conveyance:efficiency_module", "", 0, List.of("modules"), Set.of("efficiency_module")),
    MINING_PRODUCTIVITY("mining_productivity", 250, 60, 3, "conveyance:miner", "", 0, List.of("advanced_circuits"), Set.of()),
    PROJECTILE_DAMAGE_1("projectile_damage_1", 100, 30, 1, "conveyance:firearm_magazine", "", 0, List.of("military"), Set.of()),
    PROJECTILE_DAMAGE_2("projectile_damage_2", 100, 30, 3, "conveyance:piercing_magazine", "", 0, List.of("projectile_damage_1", "green_science"), Set.of()),
    SHOOTING_SPEED_1("shooting_speed_1", 100, 30, 1, "conveyance:pistol", "", 0, List.of("military"), Set.of()),
    SHOOTING_SPEED_2("shooting_speed_2", 100, 30, 3, "conveyance:submachine_gun", "", 0, List.of("shooting_speed_1", "green_science"), Set.of()),
    STEEL_AXE("steel_axe", 0, 0, 0, "conveyance:steel_ingot", "conveyance:steel_ingot", 50, List.of("steel_processing"), Set.of()),
    STRONGER_EXPLOSIVES("stronger_explosives", 100, 30, 3, "conveyance:grenade", "", 0, List.of("military_2"), Set.of()),
    EXCAVATION("excavation", 30, 15, 3, "minecraft:iron_pickaxe", "", 0, List.of("steel_processing", "green_science"), Set.of("iron_pickaxe")),
    ADVANCED_OIL_PROCESSING("advanced_oil_processing", 75, 30, 7, "conveyance:oil_refinery", "", 0, List.of("chemical_science"), Set.of("advanced_oil_processing", "heavy_oil_cracking", "light_oil_cracking")),
    MILITARY_SCIENCE("military_science", 30, 15, 3, "conveyance:military_science_pack", "", 0, List.of("military_2", "stone_walls"), Set.of("military_science_pack")),
    LASERS("lasers", 100, 30, 7, "conveyance:laser_turret", "", 0, List.of("battery", "chemical_science"), Set.of()),
    LASER_TURRETS("laser_turrets", 150, 30, 15, "conveyance:laser_turret", "", 0, List.of("lasers", "military_science"), Set.of("laser_turret")),
    LASER_DAMAGE_1("laser_damage_1", 100, 30, 15, "conveyance:laser_turret", "", 0, List.of("lasers", "military_science"), Set.of()),
    LASER_DAMAGE_2("laser_damage_2", 200, 30, 15, "conveyance:laser_turret", "", 0, List.of("laser_damage_1"), Set.of()),
    LASER_DAMAGE_3("laser_damage_3", 300, 60, 15, "conveyance:laser_turret", "", 0, List.of("laser_damage_2"), Set.of()),
    LASER_SPEED_1("laser_speed_1", 50, 30, 15, "conveyance:laser_turret", "", 0, List.of("lasers", "military_science"), Set.of()),
    LASER_SPEED_2("laser_speed_2", 100, 30, 15, "conveyance:laser_turret", "", 0, List.of("laser_speed_1"), Set.of()),
    LASER_SPEED_3("laser_speed_3", 200, 60, 15, "conveyance:laser_turret", "", 0, List.of("laser_speed_2"), Set.of());

    public static final int SCIENCE_SLOTS = 4;
    public final int id, packs, unitTicks, scienceMask, triggerCount;
    public final long bit;
    public long prerequisiteMask;
    public final boolean green, blue, military;
    public final String key, trigger;
    private final String icon;
    private final List<String> parentKeys;
    public final Set<String> recipes;
    Technology(String key, int packs, int seconds, int scienceMask, String icon, String trigger, int triggerCount, List<String> parents, Set<String> recipes) {
        this.id = ordinal() + 1; this.bit = 1L << ordinal(); this.key = key; this.packs = packs;
        this.unitTicks = seconds * 20; this.scienceMask = scienceMask; this.green = (scienceMask & 2) != 0; this.blue = (scienceMask & 4) != 0; this.military = (scienceMask & 8) != 0;
        this.icon = icon; this.trigger = trigger; this.triggerCount = triggerCount; this.parentKeys = parents; this.recipes = recipes;
    }
    static {
        if (values().length > 63) throw new IllegalStateException("Research mask needs expansion");
        for (Technology tech : values()) for (String parent : tech.parentKeys) {
            Technology prerequisite = Arrays.stream(values()).filter(candidate -> candidate.key.equals(parent)).findFirst().orElseThrow();
            tech.prerequisiteMask |= prerequisite.bit;
        }
    }
    public boolean triggered() { return !trigger.isEmpty(); }
    public int target() { return triggered() ? triggerCount : packs; }
    public Component title() { return Component.translatable("technology.conveyance." + key + ".name"); }
    public Component description() { return Component.translatable("technology.conveyance." + key + ".description"); }
    public Component shortName() { return Component.translatable("technology.conveyance." + key + ".short"); }
    public Component scienceCost() {
        if (triggered()) return Component.translatable(trigger.equals("crude_oil") ? "research.conveyance.extract_oil" : "research.conveyance.craft_trigger", triggerCount, triggerIcon().getHoverName());
        return military ? Component.translatable("research.conveyance.cost_red_green_blue_grey", packs, packs, packs, packs)
                : blue ? Component.translatable("research.conveyance.cost_red_green_blue", packs, packs, packs)
                : green ? Component.translatable("research.conveyance.cost_red_green", packs, packs)
                : Component.translatable("research.conveyance.cost_red", packs);
    }
    public boolean needsPack(int slot) { return (scienceMask & 1 << slot) != 0; }
    public static net.minecraft.world.item.Item packItem(int slot) { return switch (slot) {
        case 0 -> FactoryContent.RED_SCIENCE_ITEM.get(); case 1 -> FactoryContent.GREEN_SCIENCE_ITEM.get();
        case 2 -> FactoryContent.BLUE_SCIENCE_ITEM.get(); case 3 -> FactoryContent.MILITARY_SCIENCE_ITEM.get(); default -> net.minecraft.world.item.Items.AIR;
    }; }
    public boolean complete(long mask) { return (mask & bit) != 0; }
    public boolean available(long mask) { return !complete(mask) && (mask & prerequisiteMask) == prerequisiteMask; }
    public List<Technology> prerequisites() { return Arrays.stream(values()).filter(tech -> (prerequisiteMask & tech.bit) != 0).toList(); }
    public ItemStack icon() { return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(icon))); }
    public ItemStack triggerIcon() { return trigger.equals("crude_oil") ? new ItemStack(FactoryContent.PUMPJACK_ITEM.get()) : new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(trigger))); }
    public List<ItemStack> unlocks() {
        var items = recipes.stream().sorted().map(recipe -> BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("conveyance", recipe))).filter(item -> item != net.minecraft.world.item.Items.AIR).map(ItemStack::new).toList();
        return items.isEmpty() ? List.of(icon()) : items;
    }
    public int menuId() { return id; }
    public static Technology byMenuId(int id) { return byId(id); }
    public static Technology byId(int id) { return id > 0 && id <= values().length ? values()[id - 1] : null; }
}
