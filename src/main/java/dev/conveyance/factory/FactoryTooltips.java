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
    public static String number(double value) { return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString(); }
    private static void row(List<Component> lines, String key, Object... values) {
        lines.add(Component.translatable("tooltip.conveyance.stat." + key, values).withStyle(ChatFormatting.GRAY));
    }
    private static void demand(List<Component> lines, int base, Level level) {
        if (base > 0) row(lines, "power_usage", (int) Math.ceil(base * (level == null ? 1 : ResearchState.powerUse(level))));
    }
    public static double fuelMJ(ItemStack item) { return GeneratorEntity.fuelTicks(item) / 20.0 * GeneratorEntity.OUTPUT_KW / 1000; }
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        ItemStack item = event.getItemStack();
        List<Component> lines = event.getToolTip();
        Level level = event.getEntity() == null ? null : event.getEntity().level();
        Block block = item.getItem() instanceof BlockItem blockItem ? blockItem.getBlock() : null;
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
            row(lines, "size", kind.width, kind.width, kind.height);
            demand(lines, kind.powerKW, level);
            if (kind == MachineKind.BURNER_MINER || kind == MachineKind.FURNACE) row(lines, "fuel_usage", GeneratorEntity.OUTPUT_KW);
            if (kind == MachineKind.BURNER_MINER) row(lines, "mining", number(20.0 / kind.cycleTicks * mining));
            if (kind == MachineKind.ASSEMBLER) row(lines, "assembly", number(20.0 / kind.cycleTicks * (level == null ? 1 : ResearchState.assemblySpeed(level))));
            if (kind == MachineKind.LAB) row(lines, "research", number(20.0 / kind.cycleTicks * (level == null ? 1 : ResearchState.researchSpeed(level))));
            if (kind == MachineKind.SOLAR_PANEL) row(lines, "generation_peak", MachineEntity.SOLAR_PEAK_KW);
        } else if (block instanceof MinerBlock) {
            row(lines, "size", MinerBlock.WIDTH, MinerBlock.WIDTH, MinerBlock.HEIGHT);
            demand(lines, MinerEntity.DEMAND_KW, level);
            row(lines, "mining", number(20.0 / MinerEntity.CYCLE_TICKS * mining));
        } else if (block instanceof InserterBlock arm) {
            row(lines, "size", 1, 1, 1);
            boolean burner = block == FactoryContent.BURNER_INSERTER.get();
            if (burner) row(lines, "fuel_usage", GeneratorEntity.OUTPUT_KW); else demand(lines, InserterEntity.DEMAND_KW, level);
            int cycle = burner ? BurnerInserterEntity.CYCLE_TICKS : arm.reach() == 2 ? InserterEntity.LONG_CYCLE_TICKS : InserterEntity.SHORT_CYCLE_TICKS;
            row(lines, "reach", arm.reach()); row(lines, "transfer", number(20.0 / cycle * inserters));
        } else if (block instanceof GeneratorBlock) { row(lines, "size", 1, 1, 1); row(lines, "generation", GeneratorEntity.OUTPUT_KW); }
        else if (block instanceof PowerPoleBlock) {
            row(lines, "size", 1, 1, PowerPoleBlock.HEIGHT);
            row(lines, "link_range", PowerNetwork.LINK_RANGE); row(lines, "coverage", PowerNetwork.COVERAGE_RANGE);
            row(lines, "electric_throughput");
        } else if (block instanceof RadarBlock) {
            row(lines, "size", RadarBlock.WIDTH, RadarBlock.WIDTH, RadarBlock.HEIGHT);
            demand(lines, RadarEntity.DEMAND_KW, level); row(lines, "scan_area", RadarEntity.AREA_BLOCKS, RadarEntity.AREA_BLOCKS);
        } else if (block instanceof ConveyorBlock) {
            row(lines, "size", 1, 1, block instanceof SlopeConveyorBlock ? 2 : 1);
            row(lines, "throughput", number(ConveyorEntity.LANES * 20.0 / ConveyorEntity.MOVE_TICKS));
            row(lines, "throughput_lane", number(20.0 / ConveyorEntity.MOVE_TICKS)); row(lines, "buffer", ConveyorEntity.CAPACITY);
        } else if (block instanceof JunctionBlock) {
            row(lines, "size", 1, 1, 1);
            row(lines, "throughput_port", number(JunctionEntity.LANES * 20.0 / JunctionEntity.MOVE_TICKS));
            row(lines, "throughput_max", number(JunctionEntity.LANES * JunctionEntity.SIDES.length * 20.0 / JunctionEntity.MOVE_TICKS));
            row(lines, "buffer", JunctionEntity.CAPACITY);
        } else if (block instanceof CrateBlock crate) { row(lines, "size", 1, 1, 1); row(lines, "storage", crate.slots); }
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
