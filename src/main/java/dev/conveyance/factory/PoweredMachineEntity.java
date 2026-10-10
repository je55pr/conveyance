package dev.conveyance.factory;

import net.minecraft.core.BlockPos;
import dev.conveyance.player.ResearchState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

abstract class PoweredMachineEntity extends BlockEntity {
    private final int demand, cycleTicks;
    private double work;

    protected PoweredMachineEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int demand, int cycleTicks) {
        super(type, pos, state);
        this.demand = demand;
        this.cycleTicks = cycleTicks;
    }
    public int powerDemand() { return demand == 0 || !hasWork() ? 0 : level == null ? demand : (int) Math.ceil(demand * ResearchState.powerUse(level)); }
    protected boolean hasWork() { return true; }
    protected double currentWork() { return work; }
    protected void resetWork() { work = 0; setChanged(); }
    public double powerDistanceSquared(BlockPos pole) { return worldPosition.distSqr(pole); }

    protected boolean advanceWork() {
        double supplied = PowerNetwork.fraction(level, worldPosition);
        if (supplied <= 0) return false;
        double speed = this instanceof MinerEntity ? ResearchState.miningSpeed(level)
                : this instanceof InserterEntity ? ResearchState.inserterSpeed(level)
                : this instanceof MachineEntity machine && machine.kind() == MachineKind.LAB ? ResearchState.researchSpeed(level)
                : this instanceof MachineEntity machine && machine.kind() == MachineKind.ASSEMBLER ? ResearchState.assemblySpeed(level) : 1;
        work += supplied * speed;
        if (level.getGameTime() % 20 == 0) setChanged();
        if (work + 0.000001 < cycleTicks) return false;
        work = Math.max(0, work - cycleTicks);
        setChanged();
        return true;
    }
    @Override public void onLoad() {
        super.onLoad();
        if (demand > 0) PowerNetwork.register(level, worldPosition, PowerNetwork.Kind.MACHINE);
    }
    @Override public void setRemoved() {
        if (demand > 0) PowerNetwork.unregister(level, worldPosition, PowerNetwork.Kind.MACHINE);
        super.setRemoved();
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble("PoweredWork", work);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        double saved = tag.getDouble("PoweredWork");
        work = Double.isFinite(saved) ? Math.max(0, Math.min(cycleTicks, saved)) : 0;
    }
}
