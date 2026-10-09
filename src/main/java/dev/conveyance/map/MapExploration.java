package dev.conveyance.map;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class MapExploration {
    @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || !player.isAlive() || player.level().dimension() != Level.OVERWORLD || player.level().getGameTime() % 20 != 0) return;
        var level = player.serverLevel();
        var map = WorldMapData.get(level);
        var chunk = player.chunkPosition();
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) map.reveal(level, chunk.x + dx, chunk.z + dz);
    }
    private MapExploration() {}
}
