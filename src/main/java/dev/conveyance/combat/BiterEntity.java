package dev.conveyance.combat;

import dev.conveyance.factory.BuildingDamage;
import dev.conveyance.factory.FactoryActions;
import dev.conveyance.factory.StoneWallBlock;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;

/** Ground attacker: path to a factory, chew obstructing buildings, engage nearby players. */
public final class BiterEntity extends PathfinderMob {
    private BlockPos order;
    private BlockPos home;
    public BiterEntity(EntityType<? extends BiterEntity> type, Level level) {
        super(type, level); setPersistenceRequired(); setPathfindingMalus(BlockPathTypes.WATER, -1); setPathfindingMalus(BlockPathTypes.WATER_BORDER, 8);
    }
    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 15).add(Attributes.MOVEMENT_SPEED, .32).add(Attributes.FOLLOW_RANGE, 48).add(Attributes.ATTACK_DAMAGE, 2);
    }
    public void attackFactory(BlockPos pos) { order = pos.immutable(); home = null; }
    public void guardNest(BlockPos pos) { home = pos.immutable(); order = null; }
    public boolean defends(BlockPos pos) { return home != null && home.equals(pos); }
    @Override protected void registerGoals() { goalSelector.addGoal(0, new FloatGoal(this)); goalSelector.addGoal(1, new FactoryAttackGoal()); }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected void dropAllDeathLoot(net.minecraft.world.damagesource.DamageSource source) { /* Combat never creates loose items or XP. */ }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); if (order != null) tag.putLong("FactoryTarget", order.asLong()); if (home != null) tag.putLong("NestHome", home.asLong()); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); order = tag.contains("FactoryTarget") ? BlockPos.of(tag.getLong("FactoryTarget")) : null; home = tag.contains("NestHome") ? BlockPos.of(tag.getLong("NestHome")) : null; }
    private final class FactoryAttackGoal extends Goal {
        private BlockPos factory;
        private int cooldown, search, repath;
        FactoryAttackGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() { return true; }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void tick() {
            if (!(level() instanceof ServerLevel server)) return;
            if (cooldown > 0) cooldown--;
            Vec3 watch = home == null ? position() : Vec3.atCenterOf(home);
            Player player = server.getNearestPlayer(watch.x, watch.y, watch.z, home == null ? 8 : 24, candidate -> candidate instanceof Player visitor && visitor.isAlive() && !visitor.isCreative() && !visitor.isSpectator());
            if (getLastHurtByMob() instanceof Player attacker && attacker.isAlive() && !attacker.isCreative() && !attacker.isSpectator() && distanceToSqr(attacker) < 48 * 48) player = attacker;
            if (--search <= 0) { factory = home == null ? nearestFactory(server) : null; search = 40 + getId() % 20; }
            Vec3 destination = player != null ? player.position() : home != null ? home.distToCenterSqr(position()) > 36 ? Vec3.atBottomCenterOf(home) : null
                    : factory != null ? Vec3.atBottomCenterOf(factory) : order == null ? null : Vec3.atBottomCenterOf(order);
            if (destination == null) { getNavigation().stop(); return; }
            getLookControl().setLookAt(destination.x, destination.y + .4, destination.z);
            if (--repath <= 0) {
                Vec3 waypoint = destination;
                Vec3 delta = destination.subtract(position()).multiply(1, 0, 1);
                if (delta.lengthSqr() > 24 * 24) {
                    waypoint = position().add(delta.normalize().scale(24));
                    BlockPos step = BlockPos.containing(waypoint);
                    if (server.hasChunkAt(step)) waypoint = new Vec3(waypoint.x, server.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, step.getX(), step.getZ()), waypoint.z);
                }
                if (server.hasChunkAt(BlockPos.containing(waypoint))) getNavigation().moveTo(waypoint.x, waypoint.y, waypoint.z, 1);
                repath = 15 + getId() % 10;
            }
            BlockPos obstruction = obstruction(server, destination);
            if (obstruction != null) {
                getNavigation().stop();
                if (cooldown == 0) { BuildingDamage.hurt(server, obstruction, 7); cooldown = 10; swing(net.minecraft.world.InteractionHand.MAIN_HAND); }
            } else if (player != null && distanceToSqr(player) < 2.2 && getSensing().hasLineOfSight(player) && cooldown == 0) {
                doHurtTarget(player); cooldown = 10;
            }
        }
        private BlockPos nearestFactory(ServerLevel server) {
            if (order != null && server.hasChunkAt(order) && BuildingDamage.building(server.getBlockState(order))) return FactoryActions.root(server, order);
            BlockPos best = null; double score = 48 * 48;
            int cx = blockPosition().getX() >> 4, cz = blockPosition().getZ() >> 4;
            for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
                var chunk = server.getChunkSource().getChunkNow(cx + dx, cz + dz); if (chunk == null) continue;
                for (var entity : chunk.getBlockEntities().values()) {
                    var state = entity.getBlockState(); var pos = entity.getBlockPos();
                    if (!BuildingDamage.building(state)) continue;
                    double distance = pos.distToCenterSqr(position());
                    // Prefer substantial buildings over incidental belts and pipes.
                    if (state.getBlock() instanceof dev.conveyance.factory.ConveyorBlock || state.getBlock() instanceof dev.conveyance.factory.PipeBlock) distance += 400;
                    if (distance < score) { score = distance; best = pos; }
                }
            }
            return best;
        }
        private BlockPos obstruction(ServerLevel server, Vec3 destination) {
            Vec3 towards = destination.subtract(position());
            BlockPos best = null; double bestDistance = Double.MAX_VALUE;
            var feet = blockPosition();
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = 0; dy <= 1; dy++) {
                var pos = feet.offset(dx, dy, dz);
                if (!server.hasChunkAt(pos)) continue;
                var state = server.getBlockState(pos);
                if (!BuildingDamage.building(state) || state.getCollisionShape(server, pos).isEmpty()) continue;
                Vec3 delta = Vec3.atCenterOf(pos).subtract(position());
                if (delta.x * towards.x + delta.z * towards.z < -.05) continue;
                var box = state.getCollisionShape(server, pos).bounds().move(pos);
                if (!getBoundingBox().inflate(.4, .15, .4).intersects(box)) continue;
                double distance = delta.lengthSqr();
                if (state.getBlock() instanceof StoneWallBlock) distance -= 2;
                if (distance < bestDistance) { bestDistance = distance; best = pos; }
            }
            return best;
        }
    }
}
