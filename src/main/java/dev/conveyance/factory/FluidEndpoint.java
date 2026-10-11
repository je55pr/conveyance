package dev.conveyance.factory;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public interface FluidEndpoint {
    record Port(BlockPos pos, Direction side, int channel, FluidTank tank) {}
    List<Port> fluidPorts();
    FluidTank fluidTank(int channel);
}
