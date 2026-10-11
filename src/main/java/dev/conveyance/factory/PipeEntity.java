package dev.conveyance.factory;

import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PipeEntity extends BlockEntity implements FluidEndpoint {
    public static final int CAPACITY = 100;
    private final FluidTank tank = new FluidTank(CAPACITY, FluidKind.EMPTY);
    public PipeEntity(BlockPos pos, BlockState state) { super(FactoryContent.PIPE_ENTITY.get(), pos, state); }
    public static List<Port> ports(BlockPos pos, FluidTank tank) {
        return Arrays.stream(Direction.values()).map(side -> new Port(pos, side, 0, tank)).toList();
    }
    @Override public List<Port> fluidPorts() { return ports(worldPosition, tank); }
    @Override public FluidTank fluidTank(int channel) { return channel == 0 ? tank : null; }
    @Override public void onLoad() { super.onLoad(); FluidNetwork.register(level, worldPosition); }
    @Override public void setRemoved() { FluidNetwork.unregister(level, worldPosition); super.setRemoved(); }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.put("FluidTank", tank.save()); }
    @Override public void load(CompoundTag tag) { super.load(tag); tank.load(tag.getCompound("FluidTank")); if (level != null) FluidNetwork.invalidate(level); }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
}
