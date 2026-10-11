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
    public static final int AUTOMATION = Technology.AUTOMATION.id, EXCAVATION = Technology.EXCAVATION.id;
    public static final int LOGISTICS = Technology.LOGISTICS.id, ELECTRIC_MINING = Technology.ELECTRIC_MINING.id;
    private static final Set<String> STARTER = Set.of("burner_miner", "burner_inserter", "industrial_furnace", "conveyor", "slope_conveyor", "wood_crate", "iron_crate",
            "gear", "stone_bricks", "pistol", "firearm_magazine");
    private static long clientMask;
    private static int clientSelected;
    private static int[] clientProgress = new int[Technology.values().length];
    private long mask;
    private int selected;
    private final java.util.Map<String, Integer> production = new java.util.HashMap<>();
    private final int[] science = new int[Technology.values().length];
    private static ResearchState load(CompoundTag tag) {
        ResearchState state = new ResearchState();
        if (tag.getInt("TreeVersion") != 3) { state.setDirty(); return state; }
        state.mask = tag.getLong("Unlocks");
        var produced = tag.getCompound("Production");
        for (String key : produced.getAllKeys()) state.production.put(key, Math.max(0, produced.getInt(key)));
        state.selected = tag.getInt("Selected");
        int[] saved = tag.getIntArray("ScienceProgress");
        System.arraycopy(saved, 0, state.science, 0, Math.min(saved.length, state.science.length));
        return state;
    }
    private static ResearchState data(Level level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(ResearchState::load, ResearchState::new, "conveyance_research");
    }
    public static long mask(Level level) { return level.isClientSide ? clientMask : data(level).mask; }
    public static void updateClient(long mask) { updateClient(mask, 0, new int[Technology.values().length]); }
    public static void updateClient(long mask, int selected, int[] progress) {
        clientMask = mask; clientSelected = selected;
        clientProgress = Arrays.copyOf(progress, Technology.values().length);
    }
    public static int selected(Level level) { return level.isClientSide ? clientSelected : data(level).selected; }
    public static int packCost(int research) { Technology tech = Technology.byId(research); return tech == null ? 0 : tech.target(); }
    public static int progress(Level level, int research) {
        Technology tech = Technology.byId(research);
        if (tech == null) return 0;
        if (unlocked(level, research)) return packCost(research);
        return level.isClientSide ? clientProgress[tech.ordinal()] : data(level).science[tech.ordinal()];
    }
    public static int[] progressSnapshot(Level level) {
        return Arrays.stream(Technology.values()).mapToInt(tech -> progress(level, tech.id)).toArray();
    }
    public static boolean unlocked(Level level, int research) { Technology tech = Technology.byId(research); return tech != null && tech.complete(mask(level)); }
    public static boolean recipeUnlocked(ResourceLocation recipe, long mask) {
        if (STARTER.contains(recipe.getPath())) return true;
        for (Technology tech : Technology.values()) if (tech.recipes.contains(recipe.getPath())) return tech.complete(mask);
        return false;
    }
    // Machine performance now comes from tiers and installed modules.
    public static double miningSpeed(Level level) { return 1; }
    public static double assemblySpeed(Level level) { return 1; }
    public static double smeltingSpeed(Level level) { return 1; }
    public static double inserterSpeed(Level level) { return 1; }
    public static double researchSpeed(Level level) {
        return 1 + (unlocked(level, Technology.RESEARCH_SPEED_1.id) ? .2 : 0) + (unlocked(level, Technology.RESEARCH_SPEED_2.id) ? .3 : 0);
    }
    public static double miningProductivity(Level level) { return unlocked(level, Technology.MINING_PRODUCTIVITY.id) ? .1 : 0; }
    public static double bulletDamage(Level level) { return 1 + (unlocked(level, Technology.PROJECTILE_DAMAGE_1.id) ? .1 : 0) + (unlocked(level, Technology.PROJECTILE_DAMAGE_2.id) ? .1 : 0); }
    public static double shootingSpeed(Level level) { return 1 + (unlocked(level, Technology.SHOOTING_SPEED_1.id) ? .1 : 0) + (unlocked(level, Technology.SHOOTING_SPEED_2.id) ? .2 : 0); }
    public static double explosiveDamage(Level level) { return unlocked(level, Technology.STRONGER_EXPLOSIVES.id) ? 1.25 : 1; }
    public static double laserDamage(Level level) { return 1 + (unlocked(level, Technology.LASER_DAMAGE_1.id) ? .2 : 0) + (unlocked(level, Technology.LASER_DAMAGE_2.id) ? .2 : 0) + (unlocked(level, Technology.LASER_DAMAGE_3.id) ? .3 : 0); }
    public static double laserSpeed(Level level) { return 1 + (unlocked(level, Technology.LASER_SPEED_1.id) ? .1 : 0) + (unlocked(level, Technology.LASER_SPEED_2.id) ? .2 : 0) + (unlocked(level, Technology.LASER_SPEED_3.id) ? .3 : 0); }
    public static double handHarvestSpeed(Level level) { return unlocked(level, Technology.STEEL_AXE.id) ? 2 : 1; }
    public static double powerUse(Level level) { return 1; }
    public static double fuelDuration(Level level) { return 1; }
    public static void recordCraft(Level level, net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) return;
        recordProduction(level, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount());
    }
    public static void recordProduction(Level level, String item, int count) {
        if (level == null || level.isClientSide || count <= 0 || Arrays.stream(Technology.values()).noneMatch(tech -> tech.trigger.equals(item))) return;
        var state = data(level);
        int target = Arrays.stream(Technology.values()).filter(tech -> tech.trigger.equals(item) && !tech.complete(state.mask)).mapToInt(Technology::target).max().orElse(0);
        int previous = state.production.getOrDefault(item, 0);
        if (target == 0 || previous >= target) return;
        state.production.put(item, (int) Math.min(target, (long) previous + count));
        resolveMilestones(state);
        state.setDirty(); InventoryNetwork.syncAll(level);
    }
    private static void resolveMilestones(ResearchState state) {
        boolean unlocked;
        do {
            unlocked = false;
            for (Technology tech : Technology.values()) if (tech.triggered() && !tech.complete(state.mask)) {
                int produced = Math.min(tech.target(), state.production.getOrDefault(tech.trigger, 0));
                state.science[tech.ordinal()] = produced;
                if (produced >= tech.target() && tech.available(state.mask)) { state.mask |= tech.bit; unlocked = true; }
            }
        } while (unlocked);
    }
    public static boolean select(Level level, int research) {
        Technology tech = Technology.byId(research);
        if (tech == null || tech.triggered() || !tech.available(mask(level))) return false;
        ResearchState state = data(level);
        state.selected = research;
        state.setDirty();
        InventoryNetwork.syncAll(level);
        return true;
    }
    public static boolean applyScience(Level level, int research) {
        ResearchState state = data(level);
        Technology tech = Technology.byId(research);
        if (state.selected != research || tech == null || tech.triggered() || !tech.available(state.mask)) return false;
        int progress = ++state.science[tech.ordinal()];
        if (progress >= packCost(research)) { state.mask |= tech.bit; state.selected = 0; resolveMilestones(state); }
        state.setDirty();
        InventoryNetwork.syncAll(level);
        return true;
    }
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("conveyance").requires(source -> source.hasPermission(2))
                .then(Commands.literal("research").then(Commands.literal("reset").executes(context -> {
                    var level = context.getSource().getLevel();
                    var state = data(level);
                    state.mask = 0; state.selected = 0; state.production.clear();
                    Arrays.fill(state.science, 0);
                    state.setDirty();
                    InventoryNetwork.syncAll(level);
                    context.getSource().sendSuccess(() -> Component.translatable("message.conveyance.research_reset"), true);
                    return 1;
                }))));
    }
    @Override public CompoundTag save(CompoundTag tag) {
        tag.putLong("Unlocks", mask); tag.putInt("Selected", selected);
        tag.putIntArray("ScienceProgress", science); tag.putInt("TreeVersion", 3);
        var produced = new CompoundTag(); production.forEach(produced::putInt); tag.put("Production", produced);
        return tag;
    }
}
