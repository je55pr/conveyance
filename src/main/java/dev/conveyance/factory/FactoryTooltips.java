package dev.conveyance.factory;

import dev.conveyance.Conveyance;
import dev.conveyance.player.ResearchState;
import dev.conveyance.pollution.PollutionRates;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Item facts come from the same definitions used by ticking machines. */
public final class FactoryTooltips {
    public static String number(double value) { return Double.isInfinite(value) ? "\u221e" : BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString(); }
    private static void row(List<Component> lines, String key, Object... values) {
        lines.add(Component.translatable("tooltip.conveyance.stat." + key, values).withStyle(ChatFormatting.GRAY));
    }
    private static void demand(List<Component> lines, double base, Level level) { demand(lines, base, 0, level); }
    private static void demand(List<Component> lines, double base, double idle, Level level) {
        if (base + idle > 0) row(lines, "power_usage", number(base * (level == null ? 1 : ResearchState.powerUse(level)) + idle));
        if (idle > 0) row(lines, "idle_usage", number(idle));
    }
    public static double fuelMJ(ItemStack item) { return FuelEnergy.mj(item); }
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        ItemStack item = event.getItemStack();
        List<Component> lines = event.getToolTip();
        Level level = event.getEntity() == null ? null : event.getEntity().level();
        Block block = item.getItem() instanceof BlockItem blockItem ? blockItem.getBlock() : null;
        if (block != null && BuildingDamage.building(block.defaultBlockState())) row(lines, "health", BuildingDamage.maximum(block.defaultBlockState()));
        if (item.is(FactoryContent.PISTOL.get()) || item.is(FactoryContent.GUN_TURRET_ITEM.get())) {
            boolean turret = item.is(FactoryContent.GUN_TURRET_ITEM.get());
            row(lines, "weapon_range", turret ? dev.conveyance.combat.TurretLogic.RANGE : dev.conveyance.combat.PistolItem.RANGE);
            row(lines, "shots", turret ? 10 : 4); row(lines, "shot_damage", dev.conveyance.combat.CombatShots.DAMAGE);
        }
        if (item.is(FactoryContent.FIREARM_MAGAZINE.get())) row(lines, "rounds", dev.conveyance.combat.CombatShots.ROUNDS_PER_MAGAZINE);
        if (BuiltInRegistries.ITEM.getKey(item.getItem()).getNamespace().equals(Conveyance.MOD_ID)) {
            String key = block instanceof CrateBlock ? "tooltip.conveyance.crate" : "tooltip.conveyance." + BuiltInRegistries.ITEM.getKey(item.getItem()).getPath();
            if (Language.getInstance().has(key)) {
                Component description = Component.translatable(key);
                if (lines.stream().noneMatch(line -> line.getString().equals(description.getString()))) lines.add(description.copy().withStyle(ChatFormatting.GRAY));
            }
        }
        double mining = level == null ? 1 : ResearchState.miningSpeed(level);
        double inserters = level == null ? 1 : ResearchState.inserterSpeed(level);
        if (block instanceof MachineBlock machine) {
            MachineKind kind = machine.kind;
            row(lines, "size", kind.width, kind.depth, kind.height);
            if (kind == MachineKind.LASER_TURRET) {
                double rate = dev.conveyance.combat.LaserTurretLogic.SHOTS_PER_SECOND * (level == null ? 1 : ResearchState.laserSpeed(level));
                double damage = dev.conveyance.combat.LaserTurretLogic.DAMAGE * (level == null ? 1 : ResearchState.laserDamage(level));
                demand(lines, dev.conveyance.combat.LaserTurretLogic.SHOT_MJ * 1000 * rate, kind.idleKW(), level);
                row(lines, "weapon_range", dev.conveyance.combat.LaserTurretLogic.RANGE); row(lines, "shots", number(rate)); row(lines, "shot_damage", number(damage));
                row(lines, "shot_energy", 800); row(lines, "laser_buffer", 801); row(lines, "laser_input", dev.conveyance.combat.LaserTurretLogic.INPUT_KW);
            } else demand(lines, kind.powerKW, kind.idleKW(), level);
            if (kind.burnerKW() > 0) row(lines, "fuel_usage", kind.burnerKW());
            if (kind == MachineKind.BURNER_MINER) row(lines, "mining", number(20.0 / kind.cycleTicks * mining));
            if (kind.assembler()) row(lines, "crafting_speed", number(kind.craftingSpeed()));
            if (kind == MachineKind.PUMPJACK) row(lines, "oil_production", 10);
            if (kind == MachineKind.LAB) row(lines, "research", number(level == null ? 1 : ResearchState.researchSpeed(level)));
            if (kind.moduleSlots() > 0) row(lines, "module_slots", kind.moduleSlots());
            if (kind == MachineKind.ACCUMULATOR) { row(lines, "energy_storage", UtilityMachine.CAPACITY_MJ); row(lines, "energy_transfer", UtilityMachine.TRANSFER_KW); }
            if (kind == MachineKind.STORAGE_TANK) row(lines, "fluid_storage", 25000);
            if (kind == MachineKind.SOLAR_PANEL) row(lines, "generation_peak", MachineEntity.SOLAR_PEAK_KW);
            if (kind == MachineKind.BOILER) {
                row(lines, "fuel_usage", SteamMachine.BOILER_KW); row(lines, "steam_production", SteamMachine.STEAM_PER_SECOND);
                row(lines, "water_consumption", SteamMachine.STEAM_PER_SECOND * SteamMachine.WATER_PER_STEAM); row(lines, "steam_temperature", 165);
            }
            if (kind == MachineKind.STEAM_ENGINE) { row(lines, "generation", SteamMachine.ENGINE_KW); row(lines, "steam_consumption", 30); }
            if (kind == MachineKind.OFFSHORE_PUMP) row(lines, "water_production", SteamMachine.PUMP_PER_SECOND);
        } else if (block instanceof StoneWallBlock) {
            row(lines, "size", 1, 1, 2);
        } else if (block instanceof PipeBlock) {
            row(lines, "size", 1, 1, 1); row(lines, "fluid_throughput"); row(lines, "fluid_storage", PipeEntity.CAPACITY);
        } else if (block instanceof MinerBlock) {
            row(lines, "size", MinerBlock.WIDTH, MinerBlock.WIDTH, MinerBlock.HEIGHT);
            demand(lines, MinerEntity.DEMAND_KW, level);
            row(lines, "mining", number(20.0 / MinerEntity.CYCLE_TICKS * mining));
        } else if (block instanceof InserterBlock arm) {
            row(lines, "size", 1, 1, 1);
            boolean burner = block == FactoryContent.BURNER_INSERTER.get();
            double activeScale = inserters * (burner || level == null ? 1 : ResearchState.powerUse(level));
            row(lines, burner ? "fuel_usage_peak" : "power_usage_peak",
                    number(arm.fast() ? 59.3 : (InserterPower.peakKW(arm.reach(), burner) - (burner ? 0 : InserterPower.IDLE_KW)) * activeScale + (burner ? 0 : InserterPower.IDLE_KW)));
            row(lines, burner ? "fuel_rotation" : "power_rotation", number(arm.fast() ? 16.8 : InserterPower.rotationKW(arm.reach(), burner) * activeScale));
            if (!burner) row(lines, "idle_usage", number(arm.fast() ? .5 : InserterPower.IDLE_KW));
            double cycle = burner ? BurnerInserterEntity.CYCLE_TICKS : arm.fast() ? 26.0 / 3 : arm.reach() == 2 ? 50.0 / 3 : InserterEntity.SHORT_CYCLE_TICKS;
            row(lines, "reach", arm.reach()); row(lines, "transfer", number(20.0 / cycle * inserters));
        } else if (block instanceof GeneratorBlock) { row(lines, "size", 1, 1, 1); row(lines, "generation_unlimited"); row(lines, "creative_only"); }
        else if (block instanceof PowerPoleBlock) {
            row(lines, "size", 1, 1, PowerPoleBlock.HEIGHT);
            row(lines, "link_range", PowerNetwork.LINK_RANGE); row(lines, "coverage", PowerNetwork.COVERAGE_RANGE);
            row(lines, "electric_throughput");
        } else if (block instanceof RadarBlock) {
            row(lines, "size", RadarBlock.WIDTH, RadarBlock.WIDTH, RadarBlock.HEIGHT);
            demand(lines, RadarEntity.DEMAND_KW, level); row(lines, "scan_area", RadarEntity.AREA_BLOCKS, RadarEntity.AREA_BLOCKS);
        } else if (block instanceof ConveyorBlock) {
            row(lines, "size", 1, 1, block instanceof SlopeConveyorBlock ? 2 : 1);
            row(lines, "throughput", number(ConveyorEntity.LANES * 20.0 / (block == FactoryContent.FAST_CONVEYOR.get() || block == FactoryContent.FAST_SLOPE_CONVEYOR.get() ? ConveyorEntity.MOVE_TICKS / 2 : ConveyorEntity.MOVE_TICKS)));
            row(lines, "throughput_lane", number(20.0 / (block == FactoryContent.FAST_CONVEYOR.get() || block == FactoryContent.FAST_SLOPE_CONVEYOR.get() ? ConveyorEntity.MOVE_TICKS / 2 : ConveyorEntity.MOVE_TICKS))); row(lines, "buffer", ConveyorEntity.CAPACITY);
        } else if (block instanceof JunctionBlock) {
            row(lines, "size", 1, 1, 1);
            row(lines, "throughput_port", number(JunctionEntity.LANES * 20.0 / (block == FactoryContent.FAST_JUNCTION.get() ? JunctionEntity.MOVE_TICKS / 2 : JunctionEntity.MOVE_TICKS)));
            row(lines, "throughput_max", number(JunctionEntity.LANES * JunctionEntity.SIDES.length * 20.0 / (block == FactoryContent.FAST_JUNCTION.get() ? JunctionEntity.MOVE_TICKS / 2 : JunctionEntity.MOVE_TICKS)));
            row(lines, "buffer", JunctionEntity.CAPACITY);
        } else if (block instanceof CrateBlock crate) { row(lines, "size", 1, 1, 1); row(lines, "storage", crate.slots); }
        if (MachineModules.module(item)) {
            lines.add(Component.translatable("tooltip.conveyance.module_install").withStyle(ChatFormatting.GRAY));
        }
        double emissions = block == null ? 0 : PollutionRates.effective(block, level);
        if (emissions > 0) row(lines, "pollution", number(emissions));
        double fuel = fuelMJ(item);
        if (fuel > 0) {
            row(lines, "fuel_energy", number(fuel));
            double efficiency = level == null ? 1 : ResearchState.fuelDuration(level);
            if (efficiency != 1) row(lines, "fuel_energy_effective", number(fuel * efficiency));
        }
    }
    private FactoryTooltips() {}
}
