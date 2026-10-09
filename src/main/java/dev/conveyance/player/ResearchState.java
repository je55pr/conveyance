package dev.conveyance.player;

import java.util.Set;
import java.util.Arrays;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.RegisterCommandsEvent;

/** Prototype world-wide research. The client copy is synchronized by InventoryNetwork. */
public final class ResearchState extends SavedData {
    public static final int AUTOMATION = 1, EXCAVATION = 2;
    public static final int LOGISTICS = 4, ELECTRIC_MINING = 8;
    private static final Set<String> STARTER = Set.of("burner_miner", "burner_inserter", "industrial_furnace", "conveyor", "wood_crate",
            "gear", "copper_wire", "electronic_circuit", "red_science_pack", "research_lab", "generator", "power_pole", "stone_bricks");
    private static int clientMask, clientSelected;
    private static int[] clientProgress = new int[Technology.values().length];
    private int mask, selected;
    private final int[] science = new int[Technology.values().length];
    private static ResearchState load(CompoundTag tag) {
        ResearchState state = new ResearchState();
        if (tag.getInt("TreeVersion") != 2) { state.setDirty(); return state; }
        state.mask = tag.getInt("Unlocks");
        state.selected = tag.getInt("Selected");
        int[] saved = tag.getIntArray("ScienceProgress");
        System.arraycopy(saved, 0, state.science, 0, Math.min(saved.length, state.science.length));
        return state;
    }
    private static ResearchState data(Level level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(ResearchState::load, ResearchState::new, "conveyance_research");
    }
    public static int mask(Level level) { return level.isClientSide ? clientMask : data(level).mask; }
    public static void updateClient(int mask) { updateClient(mask, 0, new int[Technology.values().length]); }
    public static void updateClient(int mask, int selected, int[] progress) {
        clientMask = mask; clientSelected = selected;
        clientProgress = Arrays.copyOf(progress, Technology.values().length);
    }
    public static int selected(Level level) { return level.isClientSide ? clientSelected : data(level).selected; }
    public static int packCost(int research) { Technology tech = Technology.byId(research); return tech == null ? 0 : tech.packs; }
    public static int progress(Level level, int research) {
        Technology tech = Technology.byId(research);
        if (tech == null) return 0;
        if (unlocked(level, research)) return packCost(research);
        return level.isClientSide ? clientProgress[tech.ordinal()] : data(level).science[tech.ordinal()];
    }
    public static int[] progressSnapshot(Level level) {
        return Arrays.stream(Technology.values()).mapToInt(tech -> progress(level, tech.id)).toArray();
    }
    public static boolean unlocked(Level level, int research) { return (mask(level) & research) != 0; }
    public static boolean recipeUnlocked(ResourceLocation recipe, int mask) {
        if (STARTER.contains(recipe.getPath())) return true;
        for (Technology tech : Technology.values()) if (tech.recipes.contains(recipe.getPath())) return tech.complete(mask);
        return false;
    }
    public static double miningSpeed(Level level) { return unlocked(level, Technology.MINING_SPEED.id) ? 1.25 : 1; }
    public static double assemblySpeed(Level level) { return unlocked(level, Technology.ASSEMBLY_SPEED.id) ? 1.5 : 1; }
    public static double smeltingSpeed(Level level) { return unlocked(level, Technology.SMELTING_SPEED.id) ? 1.5 : 1; }
    public static double inserterSpeed(Level level) { return unlocked(level, Technology.INSERTER_SPEED.id) ? 1.5 : 1; }
    public static double researchSpeed(Level level) {
        return 1 + (unlocked(level, Technology.RESEARCH_SPEED_1.id) ? 0.25 : 0) + (unlocked(level, Technology.RESEARCH_SPEED_2.id) ? 0.25 : 0);
    }
    public static double powerUse(Level level) { return unlocked(level, Technology.POWER_EFFICIENCY.id) ? 0.8 : 1; }
    public static double fuelDuration(Level level) { return unlocked(level, Technology.BURNER_EFFICIENCY.id) ? 1.25 : 1; }
    public static boolean select(Level level, int research) {
        Technology tech = Technology.byId(research);
        if (tech == null || !tech.available(mask(level))) return false;
        ResearchState state = data(level);
        state.selected = research;
        state.setDirty();
        InventoryNetwork.syncAll(level);
        return true;
    }
    public static boolean applyScience(Level level, int research) {
        ResearchState state = data(level);
        Technology tech = Technology.byId(research);
        if (state.selected != research || tech == null || !tech.available(state.mask)) return false;
        int progress = ++state.science[tech.ordinal()];
        if (progress >= packCost(research)) { state.mask |= research; state.selected = 0; }
        state.setDirty();
        InventoryNetwork.syncAll(level);
        return true;
    }
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("conveyance").requires(source -> source.hasPermission(2))
                .then(Commands.literal("research").then(Commands.literal("reset").executes(context -> {
                    var level = context.getSource().getLevel();
                    var state = data(level);
                    state.mask = state.selected = 0;
                    Arrays.fill(state.science, 0);
                    state.setDirty();
                    InventoryNetwork.syncAll(level);
                    context.getSource().sendSuccess(() -> Component.translatable("message.conveyance.research_reset"), true);
                    return 1;
                }))));
    }
    @Override public CompoundTag save(CompoundTag tag) {
        tag.putInt("Unlocks", mask); tag.putInt("Selected", selected);
        tag.putIntArray("ScienceProgress", science); tag.putInt("TreeVersion", 2);
        return tag;
    }
}
