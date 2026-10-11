package dev.conveyance.factory;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** Storage and directional transfers share the existing pole and fluid networks. */
public final class UtilityMachine {
    public static final double CAPACITY_MJ = 5, TRANSFER_KW = 300;
    private final MachineEntity owner;
    private final FluidTank input, output = new FluidTank(200, FluidKind.EMPTY);
    private double energyMJ;
    UtilityMachine(MachineEntity owner) {
        this.owner = owner;
        input = new FluidTank(owner.kind() == MachineKind.STORAGE_TANK ? 25000 : 200, FluidKind.EMPTY);
    }
    public FluidTank tank(int channel) { return channel == 0 ? input : channel == 1 ? output : null; }
    public double energyMJ() { return energyMJ; }
    public double availablePower() { return Math.min(TRANSFER_KW, energyMJ * 20000); }
    public double chargePower() { return Math.min(TRANSFER_KW, (CAPACITY_MJ - energyMJ) * 20000); }
    public List<FluidEndpoint.Port> ports() { return ports(owner.getBlockPos(), owner.getBlockState().getValue(MachineBlock.FACING), owner.kind(), input, output); }
    public static List<FluidEndpoint.Port> preview(BlockPos root, Direction facing, MachineKind kind) {
        return ports(root, facing, kind, new FluidTank(kind == MachineKind.STORAGE_TANK ? 25000 : 200, FluidKind.EMPTY), new FluidTank(200, FluidKind.EMPTY));
    }
    private static FluidEndpoint.Port port(BlockPos root, Direction facing, int x, int y, int z, Direction side, int channel, FluidTank tank) {
        int turns = switch (facing) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
        if (side.getAxis().isHorizontal()) for (int i = 0; i < turns; i++) side = side.getClockWise();
        return new FluidEndpoint.Port(MachineBlock.localPosition(root, facing, x, y, z), side, channel, tank);
    }
    private static List<FluidEndpoint.Port> ports(BlockPos root, Direction facing, MachineKind kind, FluidTank input, FluidTank output) {
        if (kind == MachineKind.STORAGE_TANK) return List.of(
                port(root, facing, 1, 0, 0, Direction.NORTH, 0, input), port(root, facing, 1, 0, 2, Direction.SOUTH, 0, input),
                port(root, facing, 0, 0, 1, Direction.WEST, 0, input), port(root, facing, 2, 0, 1, Direction.EAST, 0, input),
                port(root, facing, 1, 1, 1, Direction.UP, 0, input));
        if (kind == MachineKind.FLUID_PUMP) return List.of(port(root, facing, 0, 0, 1, Direction.SOUTH, 0, input), port(root, facing, 0, 0, 0, Direction.NORTH, 1, output));
        return List.of();
    }
    public boolean hasWork() {
        var level = owner.getLevel(); var root = owner.getBlockPos();
        if (owner.kind() != MachineKind.FLUID_PUMP || level == null) return false;
        var fluid = FluidNetwork.fluid(level, root, 0);
        return fluid != FluidKind.EMPTY && FluidNetwork.amount(level, root, 0) > 0 && FluidNetwork.room(level, root, 1, fluid) > 0;
    }
    public void tick() {
        var level = owner.getLevel(); var root = owner.getBlockPos();
        if (owner.kind() == MachineKind.ACCUMULATOR) {
            double draw = PowerNetwork.accumulatorDraw(level, root);
            if (draw != 0) { energyMJ = Math.max(0, Math.min(CAPACITY_MJ, energyMJ - draw / 20000)); owner.setChanged(); }
        } else if (hasWork()) {
            var fluid = FluidNetwork.fluid(level, root, 0);
            double quantity = Math.min(FluidNetwork.amount(level, root, 0), FluidNetwork.room(level, root, 1, fluid)) * PowerNetwork.fraction(level, root);
            double removed = FluidNetwork.drain(level, root, 0, fluid, quantity);
            double accepted = FluidNetwork.fill(level, root, 1, fluid, removed);
            if (accepted < removed) FluidNetwork.fill(level, root, 0, fluid, removed - accepted);
            if (accepted > 0) owner.setChanged();
        }
    }
    public void showStatus(Player player) {
        if (owner.kind() == MachineKind.ACCUMULATOR) {
            player.displayClientMessage(Component.translatable("message.conveyance.accumulator", FactoryTooltips.number(energyMJ), CAPACITY_MJ), true);
            return;
        }
        var fluid = FluidNetwork.fluid(owner.getLevel(), owner.getBlockPos(), 0);
        player.displayClientMessage(Component.translatable("message.conveyance.fluid_storage", fluid.title(), FactoryTooltips.number(FluidNetwork.localAmount(owner.getLevel(), owner.getBlockPos(), 0))), true);
    }
    public CompoundTag save() { var tag = new CompoundTag(); tag.putDouble("EnergyMJ", energyMJ); tag.put("Input", input.save()); tag.put("Output", output.save()); return tag; }
    public void load(CompoundTag tag) { double saved = tag.getDouble("EnergyMJ"); energyMJ = Double.isFinite(saved) ? Math.max(0, Math.min(CAPACITY_MJ, saved)) : 0; input.load(tag.getCompound("Input")); output.load(tag.getCompound("Output")); }
}
