package dev.conveyance.combat;

import dev.conveyance.factory.FactoryContent;
import dev.conveyance.factory.MachineBlock;
import dev.conveyance.factory.MachineEntity;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class TurretLogic {
    public static final int RANGE = 18, COOLDOWN = 2;
    private final MachineEntity owner;
    private int rounds, ammoDamage = 5, targetId = -1;
    private double cooldown;
    private float yaw, previousYaw;
    public float yaw(float partialTick) { return net.minecraft.util.Mth.rotLerp(partialTick, previousYaw, yaw); }
    public int rounds() { return rounds; }
    public TurretLogic(MachineEntity owner) { this.owner = owner; }
    public Vec3 center() {
        var root = owner.getBlockPos(); Direction facing = owner.getBlockState().getValue(MachineBlock.FACING);
        double dx = facing == Direction.SOUTH || facing == Direction.EAST ? -.5 : .5;
        double dz = facing == Direction.WEST || facing == Direction.SOUTH ? -.5 : .5;
        return Vec3.atCenterOf(root).add(dx, 1.75, dz);
    }
    public void tick() {
        if (!(owner.getLevel() instanceof ServerLevel level)) return;
        if (cooldown > 0) cooldown--;
        if (rounds == 0 && !CombatShots.magazine(owner.getItem(0))) { targetId = -1; return; }
        Vec3 muzzle = center();
        net.minecraft.world.entity.LivingEntity target = level.getEntity(targetId) instanceof net.minecraft.world.entity.LivingEntity enemy && CombatContent.enemy(enemy) && enemy.isAlive() && enemy.position().distanceToSqr(muzzle) <= RANGE * RANGE && visible(level, muzzle, enemy) ? enemy : null;
        if (target == null && level.getGameTime() % 5 == Math.floorMod(owner.getBlockPos().hashCode(), 5)) {
            double distance = RANGE * RANGE;
            for (var enemy : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, new AABB(muzzle, muzzle).inflate(RANGE), entity -> entity.isAlive() && CombatContent.enemy(entity))) {
                double candidate = enemy.position().distanceToSqr(muzzle);
                if (candidate < distance && visible(level, muzzle, enemy)) { distance = candidate; target = enemy; }
            }
            targetId = target == null ? -1 : target.getId();
        }
        if (target == null) return;
        var delta = target.position().subtract(muzzle);
        yaw = (float) Math.toDegrees(Math.atan2(-delta.x, -delta.z));
        if (level.getGameTime() % 5 == 0) level.sendBlockUpdated(owner.getBlockPos(), owner.getBlockState(), owner.getBlockState(), 3);
        if (cooldown > 0) return;
        if (rounds == 0) { ammoDamage = owner.getItem(0).is(FactoryContent.PIERCING_MAGAZINE_ITEM.get()) ? 8 : 5; owner.getItem(0).shrink(1); rounds = CombatShots.ROUNDS_PER_MAGAZINE; }
        rounds--; cooldown += COOLDOWN / dev.conveyance.player.ResearchState.shootingSpeed(level); owner.setChanged();
        CombatShots.fire(level, firingOrigin(muzzle, target), target.getBoundingBox().getCenter(), null, true, owner.getBlockPos(), (float) (ammoDamage * dev.conveyance.player.ResearchState.bulletDamage(level)));
    }
    private Vec3 firingOrigin(Vec3 center, net.minecraft.world.entity.LivingEntity enemy) {
        Vec3 direction = enemy.getBoundingBox().getCenter().subtract(center).multiply(1, 0, 1);
        return direction.lengthSqr() < .00001 ? center.add(0, .4, 0) : center.add(direction.normalize().scale(.95));
    }
    private boolean visible(ServerLevel level, Vec3 from, net.minecraft.world.entity.LivingEntity enemy) {
        Vec3 aim = enemy.getBoundingBox().getCenter(); return CombatShots.unobstructedEnd(level, firingOrigin(from, enemy), aim, null, true, owner.getBlockPos()).distanceToSqr(aim) < .01;
    }
    public void save(CompoundTag tag) { tag.putInt("TurretRounds", rounds); tag.putInt("TurretAmmoDamage", ammoDamage); tag.putFloat("TurretYaw", yaw); }
    public void load(CompoundTag tag) { ammoDamage = tag.getInt("TurretAmmoDamage") == 8 ? 8 : 5; rounds = Math.max(0, Math.min(CombatShots.ROUNDS_PER_MAGAZINE, tag.getInt("TurretRounds"))); previousYaw = yaw; yaw = Float.isFinite(tag.getFloat("TurretYaw")) ? tag.getFloat("TurretYaw") : 0; }
}
