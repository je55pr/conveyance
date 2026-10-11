package dev.conveyance.combat;

import dev.conveyance.factory.MachineBlock;
import dev.conveyance.factory.MachineEntity;
import dev.conveyance.factory.PowerNetwork;
import dev.conveyance.player.ResearchState;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Factorio's 801 kJ buffer, 800 kJ shots, 9.6 MW input and 24 kW drain. */
public final class LaserTurretLogic {
    public static final int RANGE = 24, DAMAGE = 20, INPUT_KW = 9600, DRAIN_KW = 24;
    public static final double CAPACITY_MJ = .801, SHOT_MJ = .8, SHOTS_PER_SECOND = 1.5;
    public enum Status {
        NO_COVERAGE, NO_POWER, CHARGING, READY, FIRING;
        public Component label() { return Component.translatable("status.conveyance.laser." + name().toLowerCase(java.util.Locale.ROOT)); }
    }
    private final MachineEntity owner;
    private double energyMJ, cooldown;
    private float yaw, previousYaw;
    private long yawReceived;
    private boolean syncPending;
    private int targetId = -1;
    private long lastShot = Long.MIN_VALUE;
    public LaserTurretLogic(MachineEntity owner) { this.owner = owner; }
    public double energyMJ() { return energyMJ; }
    public float yaw(float partialTick) {
        if (owner.getLevel() == null || !owner.getLevel().isClientSide) return yaw;
        float progress = Math.min(1, Math.max(0, (System.nanoTime() - yawReceived) / 250_000_000F));
        return Mth.rotLerp(progress, previousYaw, yaw);
    }
    public double demandKW() { return Math.min(INPUT_KW, DRAIN_KW + Math.max(0, CAPACITY_MJ - energyMJ) * 20000); }
    public Vec3 center() {
        return Vec3.atLowerCornerOf(owner.getBlockPos()).add(switch (owner.getBlockState().getValue(MachineBlock.FACING)) {
            case EAST -> new Vec3(0, 2.35, 1); case SOUTH -> new Vec3(0, 2.35, 0);
            case WEST -> new Vec3(1, 2.35, 0); default -> new Vec3(1, 2.35, 1);
        });
    }
    public Status status() {
        if (owner.getLevel() == null) return Status.NO_COVERAGE;
        if (!PowerNetwork.connected(owner.getLevel(), owner.getBlockPos())) return Status.NO_COVERAGE;
        if (PowerNetwork.satisfaction(owner.getLevel(), owner.getBlockPos()) <= 0) return Status.NO_POWER;
        if (energyMJ + .000001 < SHOT_MJ) return Status.CHARGING;
        return lastShot != Long.MIN_VALUE && owner.getLevel().getGameTime() - lastShot < 4 ? Status.FIRING : Status.READY;
    }
    private boolean inRange(LivingEntity target, Vec3 muzzle) {
        var box = target.getBoundingBox();
        double dx = Math.max(Math.max(box.minX - muzzle.x, muzzle.x - box.maxX), 0);
        double dy = Math.max(Math.max(box.minY - muzzle.y, muzzle.y - box.maxY), 0);
        double dz = Math.max(Math.max(box.minZ - muzzle.z, muzzle.z - box.maxZ), 0);
        return dx * dx + dy * dy + dz * dz <= RANGE * RANGE;
    }
    private Vec3 firingOrigin(Vec3 center, LivingEntity target) {
        Vec3 direction = target.getBoundingBox().getCenter().subtract(center).multiply(1, 0, 1);
        return direction.lengthSqr() < .00001 ? center : center.add(direction.normalize().scale(.75));
    }
    private boolean visible(ServerLevel level, Vec3 center, LivingEntity target) {
        Vec3 aim = target.getBoundingBox().getCenter();
        return CombatShots.unobstructedEnd(level, firingOrigin(center, target), aim, null, true, owner.getBlockPos()).distanceToSqr(aim) < .01;
    }
    public void tick() {
        if (!(owner.getLevel() instanceof ServerLevel level)) return;
        double previousEnergy = energyMJ; float oldYaw = yaw;
        energyMJ = Mth.clamp(energyMJ + (PowerNetwork.deliveredPower(level, owner.getBlockPos()) - DRAIN_KW) / 20000, 0, CAPACITY_MJ);
        if (cooldown > 0) cooldown--;
        Vec3 muzzle = center();
        LivingEntity target = level.getEntity(targetId) instanceof LivingEntity enemy && enemy.isAlive() && CombatContent.enemy(enemy)
                && inRange(enemy, muzzle) && visible(level, muzzle, enemy) ? enemy : null;
        if (target == null && level.getGameTime() % 5 == Math.floorMod(owner.getBlockPos().hashCode(), 5)) {
            double nearest = Double.POSITIVE_INFINITY;
            for (var enemy : level.getEntitiesOfClass(LivingEntity.class, new AABB(muzzle, muzzle).inflate(RANGE + 1), entity -> entity.isAlive() && CombatContent.enemy(entity))) {
                double distance = enemy.getBoundingBox().getCenter().distanceToSqr(muzzle);
                if (distance < nearest && inRange(enemy, muzzle) && visible(level, muzzle, enemy)) { nearest = distance; target = enemy; }
            }
            targetId = target == null ? -1 : target.getId();
        }
        if (target != null) {
            var delta = target.getBoundingBox().getCenter().subtract(muzzle);
            float desired = (float) Math.toDegrees(Math.atan2(-delta.x, -delta.z));
            yaw = Mth.approachDegrees(yaw, desired, 10.8F);
            if (cooldown <= 0 && energyMJ + .000001 >= SHOT_MJ && Math.abs(Mth.wrapDegrees(desired - yaw)) < 3) {
                energyMJ = Math.max(0, energyMJ - SHOT_MJ);
                cooldown += 20 / (SHOTS_PER_SECOND * ResearchState.laserSpeed(level)); lastShot = level.getGameTime();
                target.hurt(new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(CombatShots.LASER)),
                        (float) (DAMAGE * ResearchState.laserDamage(level)));
                CombatNetwork.laser(level, firingOrigin(muzzle, target), target.getBoundingBox().getCenter());
                level.playSound(null, muzzle.x, muzzle.y, muzzle.z, net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT,
                        net.minecraft.sounds.SoundSource.BLOCKS, .2F, 1.7F);
            }
        }
        if (Math.abs(previousEnergy - energyMJ) > .000001 || oldYaw != yaw) {
            owner.setChanged(); syncPending = true;
        }
        if (syncPending && level.getGameTime() % 5 == 0) {
            level.sendBlockUpdated(owner.getBlockPos(), owner.getBlockState(), owner.getBlockState(), 3); syncPending = false;
        }
    }
    public void save(CompoundTag tag) { tag.putDouble("LaserEnergyMJ", energyMJ); tag.putDouble("LaserCooldown", cooldown); tag.putFloat("LaserYaw", yaw); }
    public void load(CompoundTag tag) {
        double savedEnergy = tag.getDouble("LaserEnergyMJ"), savedCooldown = tag.getDouble("LaserCooldown");
        energyMJ = Double.isFinite(savedEnergy) ? Mth.clamp(savedEnergy, 0, CAPACITY_MJ) : 0;
        cooldown = Double.isFinite(savedCooldown) ? Mth.clamp(savedCooldown, -1, 20 / SHOTS_PER_SECOND) : 0;
        previousYaw = yaw(1); yaw = Float.isFinite(tag.getFloat("LaserYaw")) ? tag.getFloat("LaserYaw") : 0; yawReceived = System.nanoTime();
    }
}
