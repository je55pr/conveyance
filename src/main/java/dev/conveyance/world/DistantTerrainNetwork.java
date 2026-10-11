package dev.conveyance.world;

import dev.conveyance.Conveyance;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Sends the generator rules once; distant views never request or reveal chunks. */
@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID)
public final class DistantTerrainNetwork {
    public record Source(boolean enabled, long seed, long colourSeed, int generation, int foliage) {}
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Conveyance.MOD_ID, "distant_terrain"), () -> "1", "1"::equals, "1"::equals);
    public static void register() {
        CHANNEL.registerMessage(0, Source.class, (message, buffer) -> {
            buffer.writeBoolean(message.enabled()); buffer.writeLong(message.seed()); buffer.writeLong(message.colourSeed());
            buffer.writeVarInt(message.generation()); buffer.writeInt(message.foliage());
        }, buffer -> {
            var source = new Source(buffer.readBoolean(), buffer.readLong(), buffer.readLong(), buffer.readVarInt(), buffer.readInt());
            if (source.generation() < 1 || source.generation() > TerracedTerrain.CURRENT_GENERATION) throw new IllegalArgumentException("Invalid terrain generation");
            return source;
        }, (message, supplier) -> {
            var context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> dev.conveyance.client.DistantTerrainRenderer.configure(message)));
            context.setPacketHandled(true);
        }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    private static void sync(ServerPlayer player) {
        var level = player.serverLevel();
        Source source = new Source(false, 0, 0, 1, 0x929B59);
        if (level.dimension() == Level.OVERWORLD && level.getChunkSource().getGenerator() instanceof TerracedChunkGenerator generator) {
            long seed = generator.paletteSeed(level.getChunkSource().randomState());
            source = new Source(true, seed, generator.generation() >= 3 ? seed : level.getSeed(), generator.generation(),
                    level.getBiome(player.blockPosition()).value().getFoliageColor());
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), source);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { if (event.getEntity() instanceof ServerPlayer player) sync(player); }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) { if (event.getEntity() instanceof ServerPlayer player) sync(player); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { if (event.getEntity() instanceof ServerPlayer player) sync(player); }
    private DistantTerrainNetwork() {}
}
