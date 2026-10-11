package dev.conveyance.map;

import dev.conveyance.Conveyance;
import dev.conveyance.player.PlayerWorkbenchMenu;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Centre-first map sampling with a shared time budget, never a chunk-generation request. */
@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID)
public final class CreativeMapReveal {
    public static final int SIDE = 2000;
    private static final long TICK_BUDGET_NS = 8_000_000;
    private static final Map<ServerLevel, LinkedHashMap<UUID, Job>> JOBS = new WeakHashMap<>();
    private static final class Job {
        final long[] chunks;
        int cursor;
        Job(long[] chunks) { this.chunks = chunks; }
    }
    public static void request(ServerPlayer player) {
        if (!player.isCreative() || !player.isAlive() || player.isSpectator() || player.level().dimension() != Level.OVERWORLD
                || !(player.containerMenu instanceof PlayerWorkbenchMenu menu) || !menu.stillValid(player)) { MapNetwork.revealProgress(player, 0, 0); return; }
        ServerLevel level = player.serverLevel();
        var jobs = JOBS.computeIfAbsent(level, ignored -> new LinkedHashMap<>());
        if (jobs.containsKey(player.getUUID())) { sync(player); return; }
        int bx = player.blockPosition().getX(), bz = player.blockPosition().getZ();
        int minX = Math.max(-1875000, Math.floorDiv(bx - SIDE / 2, 16)), minZ = Math.max(-1875000, Math.floorDiv(bz - SIDE / 2, 16));
        int maxX = Math.min(1874999, Math.floorDiv(bx + SIDE / 2 - 1, 16)), maxZ = Math.min(1874999, Math.floorDiv(bz + SIDE / 2 - 1, 16));
        int cx = bx >> 4, cz = bz >> 4;
        var map = WorldMapData.get(level); var chunks = new ArrayList<Long>();
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) if (!map.known(x, z)) chunks.add(ChunkPos.asLong(x, z));
        chunks.sort(Comparator.comparingLong(packed -> {
            long dx = (long) ChunkPos.getX(packed) - cx, dz = (long) ChunkPos.getZ(packed) - cz;
            return dx * dx + dz * dz;
        }));
        if (chunks.isEmpty()) { MapNetwork.revealProgress(player, 0, 0); player.displayClientMessage(Component.translatable("map.conveyance.reveal_complete"), true); return; }
        jobs.put(player.getUUID(), new Job(chunks.stream().mapToLong(Long::longValue).toArray())); sync(player);
    }
    public static void sync(ServerPlayer player) {
        var jobs = JOBS.get(player.serverLevel()); var job = jobs == null ? null : jobs.get(player.getUUID());
        MapNetwork.revealProgress(player, job == null ? 0 : job.cursor, job == null ? 0 : job.chunks.length);
    }
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        var jobs = JOBS.get(level); if (jobs == null || jobs.isEmpty()) return;
        long deadline = System.nanoTime() + TICK_BUDGET_NS; int sampled = 0;
        var iterator = jobs.entrySet().iterator();
        while (iterator.hasNext() && sampled < 128 && (sampled == 0 || System.nanoTime() < deadline)) {
            var entry = iterator.next(); var player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.serverLevel() != level || !player.isCreative() || !player.isAlive()) { iterator.remove(); if (player != null) MapNetwork.revealProgress(player, 0, 0); continue; }
            Job job = entry.getValue(); var map = WorldMapData.get(level);
            while (job.cursor < job.chunks.length && sampled < 128 && (sampled == 0 || System.nanoTime() < deadline)) {
                long chunk = job.chunks[job.cursor++];
                if (!map.known(ChunkPos.getX(chunk), ChunkPos.getZ(chunk))) map.reveal(level, ChunkPos.getX(chunk), ChunkPos.getZ(chunk));
                sampled++;
            }
            if (job.cursor == job.chunks.length) {
                iterator.remove(); MapNetwork.revealProgress(player, job.cursor, job.chunks.length);
                player.displayClientMessage(Component.translatable("map.conveyance.reveal_complete"), true);
            } else if (level.getGameTime() % 10 == 0) MapNetwork.revealProgress(player, job.cursor, job.chunks.length);
        }
        if (jobs.isEmpty()) JOBS.remove(level);
    }
    private CreativeMapReveal() {}
}
