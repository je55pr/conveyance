package dev.conveyance.combat;

import dev.conveyance.Conveyance;
import dev.conveyance.map.WorldMapData;
import dev.conveyance.pollution.PollutionData;
import dev.conveyance.world.TerracedChunkGenerator;
import dev.conveyance.world.TerrainBlocks;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Persistent abstract nests/budgets; entity spawning only touches already loaded terrain. */
@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID)
public final class EnemyNests extends SavedData {
    public static final int REGION = 128, WAVE_INTERVAL = 1200, MIN_WAVE = 4, MAX_WAVE = 8;
    public static final double POLLUTION_PER_BITER = 4, ABSORPTION_PER_SECOND = 2, MAX_BUDGET = 64;
    private static final class Nest {
        final BlockPos pos;
        UUID entity;
        double budget;
        long nextWave, nextDefender;
        boolean destroyed;
        BlockPos debugTarget;
        BlockPos pendingTarget;
        long arrival;
        Nest(BlockPos pos, long now) { this.pos = pos.immutable(); nextWave = now + WAVE_INTERVAL; }
    }
    private final Map<Long, Nest> nests = new HashMap<>();
    private final Set<Long> regions = new HashSet<>();
    private final Set<Long> pendingChunks = new HashSet<>();
    public static EnemyNests get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(EnemyNests::load, EnemyNests::new, "conveyance_enemy_nests"); }
    public static long mix(long value) { value = (value ^ value >>> 30) * 0xBF58476D1CE4E5B9L; value = (value ^ value >>> 27) * 0x94D049BB133111EBL; return value ^ value >>> 31; }
    private void discoverRegion(ServerLevel level, int chunkX, int chunkZ) {
        int rx = Math.floorDiv(chunkX, 8), rz = Math.floorDiv(chunkZ, 8);
        long region = ChunkPos.asLong(rx, rz);
        if (!regions.add(region)) return;
        if (level.getChunkSource().getGenerator() instanceof TerracedChunkGenerator generator) {
            for (BlockPos pos : generator.nestPositions(level.getChunkSource().randomState(), level.getSeed(), rx, rz, level.getSharedSpawnPos()))
                nests.putIfAbsent(pos.asLong(), new Nest(pos, level.getGameTime()));
        }
        setDirty();
    }
    public void discoverMap(ServerLevel level, int x, int z) { discoverRegion(level, x, z); }
    @SubscribeEvent public static void chunkLoaded(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) get(level).pendingChunks.add(event.getChunk().getPos().toLong());
    }
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || level.getGameTime() % 20 != 0) return;
        var data = get(level);
        var pending = data.pendingChunks.iterator(); int processed = 0;
        while (pending.hasNext() && processed++ < 64) { long chunk = pending.next(); pending.remove(); data.discoverRegion(level, ChunkPos.getX(chunk), ChunkPos.getZ(chunk)); }
        var pollution = PollutionData.get(level);
        for (long chunk : pollution.pollutedChunks()) data.discoverRegion(level, ChunkPos.getX(chunk), ChunkPos.getZ(chunk));
        // Native entity data may be joining during this tick; use a stable snapshot.
        for (Nest nest : new ArrayList<>(data.nests.values())) data.tickNest(level, pollution, nest);
    }
    private void tickNest(ServerLevel level, PollutionData pollution, Nest nest) {
        if (nest.destroyed) return;
        if (nest.entity == null && fullyLoaded(level, nest.pos) && !clear(level, nest.pos)) {
            // Existing factories/terrain edits take precedence over a predicted site.
            nest.destroyed = true; nest.budget = 0; setDirty(); return;
        }
        double consumed = pollution.consume(nest.pos, Math.min(ABSORPTION_PER_SECOND, MAX_BUDGET - nest.budget));
        if (consumed > 0) { nest.budget += consumed; setDirty(); }
        BiterNestEntity entity = nest.entity == null ? null : level.getEntity(nest.entity) instanceof BiterNestEntity alive && alive.isAlive() ? alive : null;
        if (nest.entity == null && clear(level, nest.pos)) entity = spawnNest(level, nest);
        long now = level.getGameTime();
        if (entity != null && now >= nest.nextDefender && nearbyPlayer(level, nest.pos, 28)) {
            int defenders = level.getEntitiesOfClass(BiterEntity.class, new AABB(nest.pos).inflate(40), biter -> biter.isAlive() && biter.defends(nest.pos)).size();
            if (defenders < 2 && localCount(level, nest.pos) < 48 && activeBiters(level) < 256) {
                if (spawnBiter(level, nest.pos, null, true, defenders) != null) { nest.nextDefender = now + 200; setDirty(); }
            }
        }
        if (now < nest.nextWave || nest.budget < MIN_WAVE * POLLUTION_PER_BITER || localCount(level, nest.pos) >= 48) return;
        BlockPos target = nest.pendingTarget != null ? nest.pendingTarget : nest.debugTarget == null ? pollution.strongestNearby(nest.pos, 24) : nest.debugTarget;
        if (target == null || !nearbyPlayer(level, target, 256) || !level.isPositionEntityTicking(target)) return;
        BlockPos entry = entryPoint(level, nest.pos, target);
        if (entry == null) return;
        if (nest.pendingTarget == null) {
            nest.pendingTarget = target;
            nest.arrival = now + (long) Math.ceil(Math.sqrt(nest.pos.distSqr(entry)) / 4 * 20);
            setDirty();
        }
        if (now < nest.arrival) return;
        int requested = Math.min(Math.max(0, 256 - activeBiters(level)), Math.min(MAX_WAVE, (int) (nest.budget / POLLUTION_PER_BITER))), spawned = 0;
        for (int i = 0; i < requested; i++) if (spawnBiter(level, entry, target, false, i) != null) spawned++;
        if (spawned == 0) return;
        nest.budget -= spawned * POLLUTION_PER_BITER; nest.nextWave = now + WAVE_INTERVAL; nest.debugTarget = null; nest.pendingTarget = null; setDirty();
        for (var player : level.players()) if (player.blockPosition().distSqr(target) < 256 * 256) {
            var data = player.getPersistentData();
            if (data.contains("ConveyanceAttackAlert") && now - data.getLong("ConveyanceAttackAlert") < 200) continue;
            data.putLong("ConveyanceAttackAlert", now);
            CombatNetwork.alert(player, nest.pos, spawned);
        }
    }
    private static boolean nearbyPlayer(ServerLevel level, BlockPos pos, double radius) {
        return level.getNearestPlayer(pos.getX() + .5, pos.getY(), pos.getZ() + .5, radius, player -> player.isAlive() && !player.isSpectator()) != null;
    }
    private static int localCount(ServerLevel level, BlockPos pos) { return level.getEntitiesOfClass(BiterEntity.class, new AABB(pos).inflate(128), BiterEntity::isAlive).size(); }
    private static int activeBiters(ServerLevel level) { int count = 0; for (var entity : level.getAllEntities()) if (entity instanceof BiterEntity biter && biter.isAlive()) count++; return count; }
    private static BlockPos entryPoint(ServerLevel level, BlockPos nest, BlockPos target) {
        if (level.isPositionEntityTicking(nest)) return nest;
        Vec3 delta = Vec3.atCenterOf(nest).subtract(Vec3.atCenterOf(target)).multiply(1, 0, 1);
        double distance = delta.length(); if (distance < 48) return null;
        for (int step = 96; step >= 48; step -= 8) {
            Vec3 point = Vec3.atCenterOf(target).add(delta.normalize().scale(Math.min(step, distance - 8)));
            BlockPos pos = BlockPos.containing(point);
            if (!level.hasChunkAt(pos) || !level.isPositionEntityTicking(pos)) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()); pos = new BlockPos(pos.getX(), y, pos.getZ());
            if (TerrainBlocks.natural(level.getBlockState(pos.below())) && level.getFluidState(pos.below()).isEmpty()) return pos;
        }
        return null;
    }
    private static boolean clear(ServerLevel level, BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            var cell = pos.offset(dx, 0, dz);
            if (!level.hasChunkAt(cell) || !TerrainBlocks.natural(level.getBlockState(cell.below())) || !level.getFluidState(cell.below()).isEmpty()
                    || !level.getBlockState(cell).isAir() || !level.getBlockState(cell.above()).isAir()) return false;
        }
        return true;
    }
    private static boolean fullyLoaded(ServerLevel level, BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (!level.hasChunkAt(pos.offset(dx, 0, dz))) return false;
        return true;
    }
    private BiterNestEntity spawnNest(ServerLevel level, Nest nest) {
        var entity = CombatContent.NEST.get().create(level); if (entity == null) return null;
        entity.setNestPosition(nest.pos); entity.moveTo(nest.pos.getX() + .5, nest.pos.getY(), nest.pos.getZ() + .5, 0, 0);
        if (!level.noCollision(entity) || !level.addFreshEntity(entity)) return null;
        nest.entity = entity.getUUID(); setDirty(); return entity;
    }
    private static BiterEntity spawnBiter(ServerLevel level, BlockPos origin, BlockPos target, boolean defender, int index) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = (index + attempt * .7) * 2.399963229728653 + level.random.nextDouble() * .4;
            int x = origin.getX() + (int) Math.round(Math.cos(angle) * (3 + attempt % 4)), z = origin.getZ() + (int) Math.round(Math.sin(angle) * (3 + attempt % 4));
            if (!level.hasChunk(x >> 4, z >> 4) || !level.isPositionEntityTicking(new BlockPos(x, origin.getY(), z))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z); var pos = new BlockPos(x, y, z);
            if (!TerrainBlocks.natural(level.getBlockState(pos.below())) || !level.getFluidState(pos.below()).isEmpty() || Math.abs(y - origin.getY()) > 1) continue;
            var biter = CombatContent.BITER.get().create(level); if (biter == null) return null;
            biter.moveTo(x + .5, y, z + .5, 0, 0);
            if (!level.noCollision(biter)) continue;
            biter.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            if (defender) biter.guardNest(origin); else biter.attackFactory(target);
            if (level.addFreshEntity(biter)) return biter;
        }
        return null;
    }
    public void attach(BiterNestEntity entity) {
        BlockPos pos = entity.nestPosition(); var nest = nests.computeIfAbsent(pos.asLong(), ignored -> new Nest(pos, entity.level().getGameTime()));
        if (nest.destroyed) { entity.discard(); return; }
        if (nest.entity != null && !nest.entity.equals(entity.getUUID()) && entity.level() instanceof ServerLevel level
                && level.getEntity(nest.entity) instanceof BiterNestEntity existing && existing.isAlive()) { entity.discard(); return; }
        if (nest.entity == null || !nest.entity.equals(entity.getUUID())) { nest.entity = entity.getUUID(); setDirty(); }
    }
    public void destroy(BlockPos pos) { var nest = nests.get(pos.asLong()); if (nest != null) { nest.destroyed = true; nest.budget = 0; setDirty(); } }
    public BlockPos nearest(BlockPos pos, double range) {
        Nest closest = null; double distance = range * range;
        for (Nest nest : nests.values()) if (!nest.destroyed && nest.pos.distSqr(pos) < distance) { distance = nest.pos.distSqr(pos); closest = nest; }
        return closest == null ? null : closest.pos;
    }
    public BlockPos debugNest(ServerLevel level, BlockPos near) {
        for (int radius = 0; radius <= 16; radius += 4) for (int dx = -radius; dx <= radius; dx += 4) for (int dz = -radius; dz <= radius; dz += 4) {
            int x = near.getX() + dx, z = near.getZ() + dz;
            if (!level.hasChunk(x >> 4, z >> 4)) continue;
            var pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (!clear(level, pos)) continue;
            Nest nest = new Nest(pos, level.getGameTime());
            if (spawnNest(level, nest) == null) continue;
            nests.put(pos.asLong(), nest); setDirty(); return pos;
        }
        return null;
    }
    public boolean provoke(ServerLevel level, BlockPos pos, BlockPos target) {
        var nest = nests.get(pos.asLong()); if (nest == null || nest.destroyed) return false;
        PollutionData.get(level).add(pos, 64); nest.nextWave = level.getGameTime(); nest.debugTarget = target.immutable(); setDirty(); return true;
    }
    public Component info(ServerLevel level, BlockPos pos) {
        var nest = nests.get(pos.asLong()); if (nest == null) return Component.literal("No nest.");
        return Component.translatable("message.conveyance.nest_info", pos.getX(), pos.getZ(), dev.conveyance.factory.FactoryTooltips.number(PollutionData.get(level).at(pos)),
                dev.conveyance.factory.FactoryTooltips.number(nest.budget), Math.max(0, (nest.nextWave - level.getGameTime() + 19) / 20));
    }
    public long[] mapNests(WorldMapData map, int minX, int minZ, int maxX, int maxZ) {
        return nests.values().stream().filter(nest -> !nest.destroyed && knownInside(map, nest.pos, minX, minZ, maxX, maxZ)).limit(4096).mapToLong(nest -> nest.pos.asLong()).sorted().toArray();
    }
    public float[] territory(int minX, int minZ, int width) {
        var field = new float[width * width];
        for (Nest nest : nests.values()) {
            if (nest.destroyed) continue;
            int fromX = Math.max(minX, (nest.pos.getX() - 48) >> 4), toX = Math.min(minX + width - 1, (nest.pos.getX() + 48) >> 4);
            int fromZ = Math.max(minZ, (nest.pos.getZ() - 48) >> 4), toZ = Math.min(minZ + width - 1, (nest.pos.getZ() + 48) >> 4);
            for (int x = fromX; x <= toX; x++) for (int z = fromZ; z <= toZ; z++) {
                double dx = x * 16 + 8 - nest.pos.getX(), dz = z * 16 + 8 - nest.pos.getZ();
                float value = (float) Math.max(0, 1 - Math.sqrt(dx * dx + dz * dz) / 48);
                int index = (z - minZ) * width + x - minX; field[index] = Math.max(field[index], value);
            }
        }
        return field;
    }
    public static boolean knownInside(WorldMapData map, BlockPos pos, int minX, int minZ, int maxX, int maxZ) {
        int x = pos.getX() >> 4, z = pos.getZ() >> 4; return x >= minX && x <= maxX && z >= minZ && z <= maxZ && map.known(x, z);
    }
    private static EnemyNests load(CompoundTag tag) {
        var data = new EnemyNests(); for (long region : tag.getLongArray("Regions")) data.regions.add(region);
        var saved = tag.getList("Nests", 10);
        for (int i = 0; i < saved.size(); i++) {
            var entry = saved.getCompound(i); var nest = new Nest(BlockPos.of(entry.getLong("Pos")), 0);
            if (entry.hasUUID("Entity")) nest.entity = entry.getUUID("Entity");
            double budget = entry.getDouble("Budget"); nest.budget = Double.isFinite(budget) ? Math.max(0, Math.min(MAX_BUDGET, budget)) : 0;
            nest.nextWave = entry.getLong("NextWave"); nest.nextDefender = entry.getLong("NextDefender"); nest.destroyed = entry.getBoolean("Destroyed"); data.nests.put(nest.pos.asLong(), nest);
            if (entry.contains("DebugTarget")) nest.debugTarget = BlockPos.of(entry.getLong("DebugTarget"));
            if (entry.contains("PendingTarget")) nest.pendingTarget = BlockPos.of(entry.getLong("PendingTarget")); nest.arrival = entry.getLong("Arrival");
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        tag.putLongArray("Regions", regions.stream().mapToLong(Long::longValue).toArray()); var saved = new ListTag();
        for (Nest nest : nests.values()) {
            var entry = new CompoundTag(); entry.putLong("Pos", nest.pos.asLong()); if (nest.entity != null) entry.putUUID("Entity", nest.entity);
            entry.putDouble("Budget", nest.budget); entry.putLong("NextWave", nest.nextWave); entry.putLong("NextDefender", nest.nextDefender); entry.putBoolean("Destroyed", nest.destroyed); saved.add(entry);
            if (nest.debugTarget != null) entry.putLong("DebugTarget", nest.debugTarget.asLong());
            if (nest.pendingTarget != null) entry.putLong("PendingTarget", nest.pendingTarget.asLong()); entry.putLong("Arrival", nest.arrival);
        }
        tag.put("Nests", saved); return tag;
    }
    private EnemyNests() {}
}
