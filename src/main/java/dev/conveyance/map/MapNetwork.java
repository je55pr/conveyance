package dev.conveyance.map;

import dev.conveyance.Conveyance;
import dev.conveyance.player.PlayerWorkbenchMenu;
import dev.conveyance.pollution.PollutionData;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Only revealed tiles in the open map's viewport are sent to its viewer. */
public final class MapNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Conveyance.MOD_ID, "map"), () -> "7", "7"::equals, "7"::equals);
    public record Query(int minX, int minZ, int maxX, int maxZ, int step, boolean pollution) {}
    public record Update(long revision, int count, List<WorldMapData.ViewTile> tiles, long[] radars, long[] nests, long[] biters) {}
    public record Ambient(int minX, int minZ, long seed, float[] values, float[] territory) {}
    private record Reveal() {}
    private record RevealProgress(int done, int total) {}
    public static void register() {
        CHANNEL.registerMessage(0, Query.class, (message, buffer) -> {
            buffer.writeInt(message.minX); buffer.writeInt(message.minZ); buffer.writeInt(message.maxX); buffer.writeInt(message.maxZ);
            buffer.writeVarInt(message.step); buffer.writeBoolean(message.pollution);
        }, buffer -> new Query(buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readVarInt(), buffer.readBoolean()), (message, supplier) -> {
            var context = supplier.get();
            context.enqueueWork(() -> {
                var player = context.getSender();
                if (player == null || !(player.containerMenu instanceof PlayerWorkbenchMenu menu) || !menu.stillValid(player)
                        || player.level().dimension() != Level.OVERWORLD || !valid(message)) return;
                long now = player.level().getGameTime();
                var tag = player.getPersistentData();
                if (tag.contains("ConveyanceMapRequestTime") && now - tag.getLong("ConveyanceMapRequestTime") < 5) return;
                tag.putLong("ConveyanceMapRequestTime", now);
                if (player.isCreative()) CreativeMapReveal.sync(player);
                var data = WorldMapData.get(player.serverLevel());
                var tiles = data.window(message.minX, message.minZ, message.maxX, message.maxZ, message.step);
                if (message.pollution) {
                    var pollution = PollutionData.get(player.serverLevel());
                    var overlay = pollution.overlay(data, message.minX, message.minZ, message.maxX, message.maxZ, message.step);
                    tiles = tiles.stream().map(tile -> new WorldMapData.ViewTile(tile.x(), tile.z(), tile.step(), tile.revision(), tile.pixels(),
                            pollution.revision(), overlay.getOrDefault(ChunkPos.asLong(tile.x(), tile.z()), new byte[256]))).toList();
                }
                long[] radars = data.radars(message.minX * message.step, message.minZ * message.step,
                        (message.maxX + 1) * message.step - 1, (message.maxZ + 1) * message.step - 1);
                int minX = message.minX * message.step, minZ = message.minZ * message.step, maxX = (message.maxX + 1) * message.step - 1, maxZ = (message.maxZ + 1) * message.step - 1;
                long[] nests = dev.conveyance.combat.EnemyNests.get(player.serverLevel()).mapNests(data, minX, minZ, maxX, maxZ);
                var moving = new ArrayList<Long>();
                for (var entity : player.serverLevel().getAllEntities()) if (entity instanceof dev.conveyance.combat.BiterEntity biter && biter.isAlive()
                        && dev.conveyance.combat.EnemyNests.knownInside(data, biter.blockPosition(), minX, minZ, maxX, maxZ) && moving.size() < 4096) moving.add(biter.blockPosition().asLong());
                long[] biters = moving.stream().mapToLong(Long::longValue).toArray();
                for (int offset = 0; offset < Math.max(1, tiles.size()); offset += 64) {
                    var batch = List.copyOf(tiles.subList(offset, Math.min(tiles.size(), offset + 64)));
                    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Update(data.revision(), data.count(), batch, radars, nests, biters));
                }
            });
            context.setPacketHandled(true);
        }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1, Update.class, (message, buffer) -> {
            buffer.writeVarLong(message.revision); buffer.writeVarInt(message.count); buffer.writeVarInt(message.tiles.size());
            for (var tile : message.tiles) {
                buffer.writeInt(tile.x()); buffer.writeInt(tile.z()); buffer.writeVarInt(tile.step()); buffer.writeVarLong(tile.revision()); buffer.writeByteArray(tile.pixels());
                buffer.writeVarLong(tile.pollutionRevision()); buffer.writeByteArray(tile.pollution());
            }
            buffer.writeVarInt(message.radars.length);
            for (long radar : message.radars) buffer.writeLong(radar);
            writeMarkers(buffer, message.nests); writeMarkers(buffer, message.biters);
        }, buffer -> {
            long revision = buffer.readVarLong(); int count = buffer.readVarInt(), size = buffer.readVarInt();
            if (size < 0 || size > 64) throw new IllegalArgumentException("Invalid map batch");
            var tiles = new ArrayList<WorldMapData.ViewTile>(size);
            for (int index = 0; index < size; index++) {
                int x = buffer.readInt(), z = buffer.readInt(), step = buffer.readVarInt(); long version = buffer.readVarLong();
                if (!validStep(step)) throw new IllegalArgumentException("Invalid map scale");
                byte[] pixels = buffer.readByteArray(256);
                if (pixels.length != 256) throw new IllegalArgumentException("Invalid map tile");
                long pollutionRevision = buffer.readVarLong(); byte[] pollution = buffer.readByteArray(256);
                if (pollution.length != 256) throw new IllegalArgumentException("Invalid pollution tile");
                tiles.add(new WorldMapData.ViewTile(x, z, step, version, pixels, pollutionRevision, pollution));
            }
            int radarCount = buffer.readVarInt();
            if (radarCount < 0 || radarCount > 4096) throw new IllegalArgumentException("Invalid radar count");
            long[] radars = new long[radarCount];
            for (int index = 0; index < radarCount; index++) radars[index] = buffer.readLong();
            return new Update(revision, count, tiles, radars, readMarkers(buffer), readMarkers(buffer));
        }, (message, supplier) -> {
            var context = supplier.get();
            context.enqueueWork(() -> WorldMapData.updateClient(message.revision, message.count, message.tiles, message.radars, message.nests, message.biters));
            context.setPacketHandled(true);
        }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(3, Reveal.class, (message, buffer) -> {}, buffer -> new Reveal(), (message, supplier) -> {
            var context = supplier.get(); context.enqueueWork(() -> { if (context.getSender() != null) CreativeMapReveal.request(context.getSender()); }); context.setPacketHandled(true);
        }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(4, RevealProgress.class, (message, buffer) -> { buffer.writeVarInt(message.done); buffer.writeVarInt(message.total); }, buffer -> {
            int done = buffer.readVarInt(), total = buffer.readVarInt();
            if (done < 0 || total < done || total > 20000) throw new IllegalArgumentException("Invalid reveal progress");
            return new RevealProgress(done, total);
        }, (message, supplier) -> {
            var context = supplier.get(); context.enqueueWork(() -> WorldMapData.revealProgress(message.done, message.total)); context.setPacketHandled(true);
        }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(2, Ambient.class, (message, buffer) -> {
            buffer.writeInt(message.minX); buffer.writeInt(message.minZ);
            buffer.writeLong(message.seed);
            for (float value : message.values) buffer.writeFloat(value);
            for (float value : message.territory) buffer.writeFloat(value);
        }, buffer -> {
            int minX = buffer.readInt(), minZ = buffer.readInt();
            long seed = buffer.readLong();
            var values = new float[PollutionData.AMBIENT_WIDTH * PollutionData.AMBIENT_WIDTH];
            for (int i = 0; i < values.length; i++) {
                values[i] = buffer.readFloat();
                if (!Float.isFinite(values[i]) || values[i] < 0 || values[i] > 1_000_000) throw new IllegalArgumentException("Invalid local pollution");
            }
            var territory = new float[values.length];
            for (int i = 0; i < territory.length; i++) { territory[i] = buffer.readFloat(); if (!Float.isFinite(territory[i]) || territory[i] < 0 || territory[i] > 1) throw new IllegalArgumentException("Invalid nest territory"); }
            return new Ambient(minX, minZ, seed, values, territory);
        }, (message, supplier) -> {
            var context = supplier.get();
            context.enqueueWork(() -> dev.conveyance.client.SurfaceColors.receive(message.minX, message.minZ, message.seed, message.values, message.territory));
            context.setPacketHandled(true);
        }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    private static boolean valid(Query query) {
        return validStep(query.step) && (long) query.minX * query.step >= -1875000 && (long) query.minZ * query.step >= -1875000
                && (long) query.maxX * query.step <= 1875000 && (long) query.maxZ * query.step <= 1875000
                && query.minX <= query.maxX && query.minZ <= query.maxZ
                && query.maxX - query.minX < 64 && query.maxZ - query.minZ < 64;
    }
    private static void writeMarkers(net.minecraft.network.FriendlyByteBuf buffer, long[] values) { buffer.writeVarInt(values.length); for (long value : values) buffer.writeLong(value); }
    private static long[] readMarkers(net.minecraft.network.FriendlyByteBuf buffer) {
        int count = buffer.readVarInt(); if (count < 0 || count > 4096) throw new IllegalArgumentException("Invalid enemy marker count");
        long[] values = new long[count]; for (int i = 0; i < count; i++) values[i] = buffer.readLong(); return values;
    }
    private static boolean validStep(int step) { return step >= 1 && step <= 256 && (step & (step - 1)) == 0; }
    public static void request(int minX, int minZ, int maxX, int maxZ, int step, boolean pollution) {
        if (!validStep(step)) return;
        int limit = 1875000 / step;
        CHANNEL.sendToServer(new Query(Math.max(-limit, minX), Math.max(-limit, minZ), Math.min(limit, maxX), Math.min(limit, maxZ), step, pollution));
    }
    public static void syncPollution(ServerPlayer player, int minX, int minZ, float[] values) {
        var level = player.serverLevel(); long seed = level.getSeed();
        if (level.getChunkSource().getGenerator() instanceof dev.conveyance.world.TerracedChunkGenerator generator && generator.generation() >= 3)
            seed = generator.paletteSeed(level.getChunkSource().randomState());
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Ambient(minX, minZ, seed, values,
                dev.conveyance.combat.EnemyNests.get(level).territory(minX, minZ, PollutionData.AMBIENT_WIDTH)));
    }
    public static void reveal() { WorldMapData.revealProgress(0, 1); CHANNEL.sendToServer(new Reveal()); }
    public static void revealProgress(ServerPlayer player, int done, int total) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RevealProgress(done, total)); }
    private MapNetwork() {}
}
