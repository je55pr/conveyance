package dev.conveyance.combat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;

public final class GrenadeEntity extends ThrowableItemProjectile {
    public GrenadeEntity(EntityType<? extends GrenadeEntity> type, Level level) { super(type, level); }
    public GrenadeEntity(Level level, LivingEntity thrower) { super(CombatContent.GRENADE.get(), thrower, level); }
    @Override protected Item getDefaultItem() { return dev.conveyance.factory.FactoryContent.GRENADE_ITEM.get(); }
    @Override protected void onHit(HitResult hit) { super.onHit(hit); detonate(); }
    @Override public void tick() { super.tick(); if (tickCount > 100) detonate(); }
    private void detonate() {
        if (!(level() instanceof ServerLevel level) || isRemoved()) return;
        for (var enemy : level.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(6.5), entity -> entity.isAlive() && CombatContent.enemy(entity))) {
            if (enemy.position().distanceToSqr(position()) <= 6.5 * 6.5) enemy.hurt(level.damageSources().thrown(this, getOwner()), (float) (35 * dev.conveyance.player.ResearchState.explosiveDamage(level)));
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.PLAYERS, 1, 1);
        discard();
    }
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() { return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this); }
}
