package dev.conveyance.combat;

import dev.conveyance.Conveyance;
import dev.conveyance.factory.StoneWallBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CombatShots {
    public static final int DAMAGE = 5, ROUNDS_PER_MAGAZINE = 10;
    public static final ResourceKey<DamageType> BULLET = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Conveyance.MOD_ID, "bullet"));
    public static final ResourceKey<DamageType> LASER = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Conveyance.MOD_ID, "laser"));
    public static Vec3 unobstructedEnd(ServerLevel level, Vec3 from, Vec3 to, Entity shooter, boolean overWalls) {
        return unobstructedEnd(level, from, to, shooter, overWalls, null);
    }
    public static Vec3 unobstructedEnd(ServerLevel level, Vec3 from, Vec3 to, Entity shooter, boolean overWalls, BlockPos ignoredBuilding) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter) {
            @Override public VoxelShape getBlockShape(BlockState state, BlockGetter world, BlockPos pos) {
                if (ignoredBuilding != null && (state.getBlock() instanceof dev.conveyance.factory.MachineBlock block && block.kind.turret()
                        || state.getBlock() instanceof dev.conveyance.factory.MachinePartBlock part && part.kind.turret())
                        && dev.conveyance.factory.FactoryActions.root(level, pos).equals(ignoredBuilding)) return Shapes.empty();
                return overWalls && state.getBlock() instanceof StoneWallBlock ? Shapes.empty() : super.getBlockShape(state, world, pos);
            }
        }).getLocation();
    }
    public static void fire(ServerLevel level, Vec3 from, Vec3 to, Entity shooter, boolean overWalls) {
        fire(level, from, to, shooter, overWalls, null);
    }
    public static void fire(ServerLevel level, Vec3 from, Vec3 to, Entity shooter, boolean overWalls, BlockPos ignoredBuilding) {
        fire(level, from, to, shooter, overWalls, ignoredBuilding, DAMAGE);
    }
    public static void fire(ServerLevel level, Vec3 from, Vec3 to, Entity shooter, boolean overWalls, BlockPos ignoredBuilding, float baseDamage) {
        Vec3 end = unobstructedEnd(level, from, to, shooter, overWalls, ignoredBuilding);
        net.minecraft.world.entity.LivingEntity target = null; double nearest = from.distanceToSqr(end);
        for (var enemy : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, new AABB(from, end).inflate(.4), entity -> entity.isAlive() && CombatContent.enemy(entity))) {
            var hit = enemy.getBoundingBox().inflate(.15).clip(from, end);
            if (hit.isPresent() && from.distanceToSqr(hit.get()) <= nearest) { target = enemy; nearest = from.distanceToSqr(hit.get()); }
        }
        if (target != null) {
            end = from.add(end.subtract(from).normalize().scale(Math.sqrt(nearest)));
            target.hurt(new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(BULLET), shooter), (float) (baseDamage * dev.conveyance.player.ResearchState.bulletDamage(level)));
        }
        CombatNetwork.tracer(level, from, end);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, .16F, 2F);
    }
    public static boolean magazine(net.minecraft.world.item.ItemStack stack) { return stack.is(dev.conveyance.factory.FactoryContent.FIREARM_MAGAZINE.get()) || stack.is(dev.conveyance.factory.FactoryContent.PIERCING_MAGAZINE_ITEM.get()); }
    private CombatShots() {}
}
