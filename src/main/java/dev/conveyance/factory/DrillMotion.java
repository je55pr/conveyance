package dev.conveyance.factory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Cosmetic rotor snapshots; rate follows actual mining rather than fuel stock. */
public final class DrillMotion {
    private final BlockEntity owner;
    private double angle, speed, sentSpeed;
    private long sampledAt = -1, sentAt = Long.MIN_VALUE;
    public DrillMotion(BlockEntity owner) { this.owner = owner; }
    public void capture(double rate) {
        var level = owner.getLevel();
        if (level == null || level.isClientSide) return;
        long now = level.getGameTime();
        if (sampledAt >= 0) angle = (angle + Math.max(0, now - sampledAt) * speed) % 360;
        speed = Math.max(0, rate); sampledAt = now;
        if (sentAt == Long.MIN_VALUE || Math.abs(sentSpeed - speed) > 0.000001 || speed > 0 && now - sentAt >= 20) {
            sentAt = now; sentSpeed = speed;
            level.sendBlockUpdated(owner.getBlockPos(), owner.getBlockState(), owner.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
    public float angle(float partialTick) {
        double elapsed = owner.getLevel() == null || sampledAt < 0 ? 0 : Math.max(0, owner.getLevel().getGameTime() - sampledAt + partialTick);
        return (float) ((angle + elapsed * speed) % 360);
    }
    public void write(CompoundTag tag) {
        var motion = new CompoundTag();
        motion.putDouble("Angle", angle); motion.putDouble("Speed", speed); motion.putLong("Time", sampledAt);
        tag.put("DrillMotion", motion);
    }
    public void read(CompoundTag tag) {
        var motion = tag.getCompound("DrillMotion");
        double savedAngle = motion.getDouble("Angle"), savedSpeed = motion.getDouble("Speed");
        angle = Double.isFinite(savedAngle) ? savedAngle % 360 : 0;
        speed = Double.isFinite(savedSpeed) ? Math.max(0, Math.min(90, savedSpeed)) : 0;
        sampledAt = motion.contains("Time") ? motion.getLong("Time") : -1;
    }
}
