package dev.conveyance.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Stationary, shootable nest. Distant attack bookkeeping belongs to SavedData. */
public final class BiterNestEntity extends Mob {
    private BlockPos nest;
    public BiterNestEntity(EntityType<? extends BiterNestEntity> type, Level level) { super(type, level); setPersistenceRequired(); setNoGravity(true); }
    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 350).add(Attributes.MOVEMENT_SPEED, 0).add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }
    public BlockPos nestPosition() { return nest == null ? blockPosition() : nest; }
    public void setNestPosition(BlockPos pos) { nest = pos.immutable(); }
    @Override public void tick() {
        super.tick(); setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        if (level() instanceof ServerLevel server && isAlive() && tickCount % 20 == 1) EnemyNests.get(server).attach(this);
    }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return isAlive(); }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override public void die(DamageSource source) {
        if (level() instanceof ServerLevel server) EnemyNests.get(server).destroy(nestPosition());
        super.die(source);
    }
    @Override protected void dropAllDeathLoot(DamageSource source) { /* No items or XP. */ }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putLong("NestPosition", nestPosition().asLong()); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); nest = tag.contains("NestPosition") ? BlockPos.of(tag.getLong("NestPosition")) : blockPosition(); }
}
