package dev.conveyance.factory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Server snapshots interpolate only as far as the next pickup/drop milestone. */
public final class InserterMotion {
    private final BlockEntity owner;
    private double phase, speed;
    private boolean returning, sentReturning;
    private long sampledAt, sentAt = Long.MIN_VALUE;
    private double sentSpeed;
    public InserterMotion(BlockEntity owner) { this.owner = owner; }

    public void capture(double progress, double rate, boolean emptyReturn, boolean force) {
        phase = progress; speed = rate; returning = emptyReturn;
        var level = owner.getLevel();
        if (level == null || level.isClientSide) return;
        sampledAt = level.getGameTime();
        if (force || sentAt == Long.MIN_VALUE || sentReturning != returning || Math.abs(sentSpeed - speed) > 0.000001
                || speed > 0 && sampledAt - sentAt >= 5) {
            sentAt = sampledAt; sentSpeed = speed; sentReturning = returning;
            owner.setChanged();
            level.sendBlockUpdated(owner.getBlockPos(), owner.getBlockState(), owner.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
    public double phase(float partialTick) {
        double elapsed = owner.getLevel() == null ? 0 : Math.max(0, owner.getLevel().getGameTime() - sampledAt + partialTick);
        return Math.max(0, Math.min(returning ? 1 : 0.5, phase + elapsed * speed));
    }
    public void write(CompoundTag tag) {
        var data = new CompoundTag();
        data.putDouble("Phase", phase); data.putDouble("Speed", speed); data.putLong("Time", sampledAt); data.putBoolean("Returning", returning);
        tag.put("InserterMotion", data);
    }
    public void read(CompoundTag tag, double savedPhase, boolean savedReturn) {
        if (!tag.contains("InserterMotion")) { phase = savedPhase; speed = 0; returning = savedReturn; return; }
        var data = tag.getCompound("InserterMotion");
        double progress = data.getDouble("Phase"), rate = data.getDouble("Speed");
        phase = Double.isFinite(progress) ? Math.max(0, Math.min(1, progress)) : 0;
        speed = Double.isFinite(rate) ? Math.max(0, Math.min(1, rate)) : 0;
        sampledAt = data.getLong("Time"); returning = data.getBoolean("Returning");
    }
}
