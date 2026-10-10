package dev.conveyance.pollution;

import dev.conveyance.factory.FactoryContent;
import dev.conveyance.factory.GeneratorBlock;
import dev.conveyance.factory.MachineBlock;
import dev.conveyance.factory.MachineKind;
import dev.conveyance.factory.MinerBlock;
import dev.conveyance.player.ResearchState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Pollution units per minute at full activity; shared by ticking and tooltips. */
public final class PollutionRates {
    public static final double GENERATOR = 30;
    public static double effective(Block block, Level level) {
        double rate = of(block);
        return level != null && (block instanceof MinerBlock || block instanceof MachineBlock machine && machine.kind == MachineKind.ASSEMBLER)
                ? rate * ResearchState.powerUse(level) : rate;
    }
    public static double of(Block block) {
        if (block instanceof GeneratorBlock) return GENERATOR;
        if (block instanceof MinerBlock) return 10;
        if (block == FactoryContent.BURNER_INSERTER.get()) return 1;
        if (block instanceof MachineBlock machine) return switch (machine.kind) {
            case BURNER_MINER -> 12;
            case FURNACE -> 2;
            case ASSEMBLER -> 4;
            default -> 0;
        };
        return 0;
    }
    private PollutionRates() {}
}
