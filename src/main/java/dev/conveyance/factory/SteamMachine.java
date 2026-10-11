package dev.conveyance.factory;

import java.util.List;
import dev.conveyance.player.ResearchState;
import dev.conveyance.pollution.PollutionData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;

/** Factorio 2.0 steam: 6 water/s -> 60 steam/s at 165 C; 30 steam/s -> 900 kW. */
public final class SteamMachine {
    public static final int ENGINE_KW = 900, BOILER_KW = 1800, PUMP_PER_SECOND = 1200, STEAM_PER_SECOND = 60;
    public static final double MJ_PER_STEAM = 0.03, WATER_PER_STEAM = 0.1, ENGINE_PER_TICK = 1.5;
    public enum Status {
        IDLE, WORKING, NO_WATER, NO_FUEL, OUTPUT_FULL, NO_STEAM, NO_COVERAGE;
        public Component title() { return Component.translatable("status.conveyance.fluid." + name().toLowerCase(java.util.Locale.ROOT)); }
    }
    private final MachineEntity owner;
    private final FluidTank water, steam = new FluidTank(200, FluidKind.STEAM);
    private double fuelMJ, flow;
    private double outputKW;
    private Status status = Status.IDLE;
    SteamMachine(MachineEntity owner) {
        this.owner = owner;
        water = new FluidTank(owner.kind() == MachineKind.OFFSHORE_PUMP ? 100 : 200, FluidKind.WATER);
    }
    public double fuelMJ() { return fuelMJ; }
    public double flow() { return flow; }
    public double outputKW() { return outputKW; }
    public Status status() { return status; }
    public FluidTank tank(int channel) {
        if (channel == 0 && (owner.kind() == MachineKind.BOILER || owner.kind() == MachineKind.OFFSHORE_PUMP)) return water;
        return channel == 1 && (owner.kind() == MachineKind.BOILER || owner.kind() == MachineKind.STEAM_ENGINE) ? steam : null;
    }
    public List<FluidEndpoint.Port> ports() {
        return ports(owner.getBlockPos(), owner.getBlockState().getValue(MachineBlock.FACING), owner.kind(), water, steam);
    }
    public static List<FluidEndpoint.Port> previewPorts(BlockPos root, Direction facing, MachineKind kind) {
        if (kind.utility()) return UtilityMachine.preview(root, facing, kind);
        return ports(root, facing, kind, new FluidTank(200, FluidKind.WATER), new FluidTank(200, FluidKind.STEAM));
    }
    private static FluidEndpoint.Port port(BlockPos root, Direction facing, int x, int z, Direction side, int channel, FluidTank tank) {
        int turns = switch (facing) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
        for (int i = 0; i < turns; i++) side = side.getClockWise();
        return new FluidEndpoint.Port(MachineBlock.localPosition(root, facing, x, 0, z), side, channel, tank);
    }
    private static List<FluidEndpoint.Port> ports(BlockPos root, Direction facing, MachineKind kind, FluidTank water, FluidTank steam) {
        return switch (kind) {
            case BOILER -> List.of(port(root, facing, 0, 1, Direction.WEST, 0, water), port(root, facing, 2, 1, Direction.EAST, 0, water),
                    port(root, facing, 1, 0, Direction.NORTH, 1, steam));
            case STEAM_ENGINE -> List.of(port(root, facing, 1, 0, Direction.NORTH, 1, steam), port(root, facing, 1, 4, Direction.SOUTH, 1, steam));
            case OFFSHORE_PUMP -> {
                var output = port(root, facing, 0, 1, Direction.SOUTH, 0, water);
                yield List.of(new FluidEndpoint.Port(output.pos().above(), output.side(), output.channel(), output.tank()));
            }
            default -> List.of();
        };
    }
    public static boolean waterAtIntake(Level level, BlockPos root, Direction facing) {
        BlockPos intake = root.relative(facing).below();
        return level.hasChunkAt(intake) && level.getFluidState(intake).is(FluidTags.WATER);
    }
    static Direction pumpFacing(Level level, BlockPos root, Direction preferred) {
        if (waterAtIntake(level, root, preferred)) return preferred;
        for (Direction facing : Direction.Plane.HORIZONTAL) if (waterAtIntake(level, root, facing)) return facing;
        return preferred;
    }
    boolean tick() {
        Level level = owner.getLevel(); BlockPos pos = owner.getBlockPos();
        flow = 0; outputKW = 0; status = Status.IDLE;
        if (owner.kind() == MachineKind.OFFSHORE_PUMP) {
            if (!waterAtIntake(level, pos, owner.getBlockState().getValue(MachineBlock.FACING))) status = Status.NO_WATER;
            else {
                flow = FluidNetwork.fill(level, pos, 0, FluidKind.WATER, PUMP_PER_SECOND / 20.0) * 20;
                status = flow > 0 ? Status.WORKING : Status.OUTPUT_FULL;
            }
        } else if (owner.kind() == MachineKind.BOILER) boil(level, pos);
        else if (owner.kind() == MachineKind.STEAM_ENGINE) {
            if (FluidNetwork.amount(level, pos, 1) <= 0) status = Status.NO_STEAM;
            else if (!PowerNetwork.connected(level, pos)) status = Status.NO_COVERAGE;
            else {
                double wanted = PowerNetwork.generatorDraw(level, pos) / ENGINE_KW * ENGINE_PER_TICK;
                double used = FluidNetwork.drain(level, pos, 1, FluidKind.STEAM, wanted);
                flow = used * 20; outputKW = used / ENGINE_PER_TICK * ENGINE_KW;
                status = used > 0 ? Status.WORKING : Status.IDLE;
            }
        }
        if (flow > 0 || level.getGameTime() % 20 == 0) owner.setChanged();
        return status == Status.WORKING;
    }
    private void boil(Level level, BlockPos pos) {
        double availableWater = FluidNetwork.amount(level, pos, 0);
        double room = FluidNetwork.room(level, pos, 1, FluidKind.STEAM);
        if (availableWater <= 0) { status = Status.NO_WATER; return; }
        if (room <= 0.00000001) { status = Status.OUTPUT_FULL; return; }
        double wanted = Math.min(STEAM_PER_SECOND / 20.0, Math.min(availableWater / WATER_PER_STEAM, room));
        if (fuelMJ + 0.00000001 < wanted * MJ_PER_STEAM && FuelEnergy.mj(owner.getItem(0)) > 0) {
            fuelMJ += FuelEnergy.mj(owner.getItem(0)) * ResearchState.fuelDuration(level);
            owner.getItem(0).shrink(1); owner.setChanged();
        }
        double produced = Math.min(wanted, fuelMJ / MJ_PER_STEAM);
        if (produced <= 0.00000001) { status = Status.NO_FUEL; return; }
        FluidNetwork.drain(level, pos, 0, FluidKind.WATER, produced * WATER_PER_STEAM);
        FluidNetwork.fill(level, pos, 1, FluidKind.STEAM, produced);
        fuelMJ = Math.max(0, fuelMJ - produced * MJ_PER_STEAM);
        flow = produced * 20; outputKW = flow * MJ_PER_STEAM * 1000;
        PollutionData.emit(level, pos, 30 * produced / (STEAM_PER_SECOND / 20.0));
        status = Status.WORKING;
    }
    double availablePower() {
        return owner.getLevel() == null ? 0 : Math.min(ENGINE_KW,
                FluidNetwork.steamPerEngine(owner.getLevel(), owner.getBlockPos()) / ENGINE_PER_TICK * ENGINE_KW);
    }
    CompoundTag save() {
        var tag = new CompoundTag(); tag.put("Water", water.save()); tag.put("Steam", steam.save()); tag.putDouble("FuelMJ", fuelMJ); return tag;
    }
    void load(CompoundTag tag) {
        water.load(tag.getCompound("Water")); steam.load(tag.getCompound("Steam"));
        double saved = tag.getDouble("FuelMJ"); fuelMJ = Double.isFinite(saved) ? Math.max(0, Math.min(10, saved)) : 0;
        if (owner.getLevel() != null) FluidNetwork.invalidate(owner.getLevel());
    }
}
