package dev.conveyance.player;

import dev.conveyance.Conveyance;
import dev.conveyance.factory.FactoryOverlayStatus;
import dev.conveyance.factory.RemoteBuilding;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Four updates per second, exclusively from already loaded nearby chunks. */
@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID)
public final class FactoryOverlayNetwork {
    private static final int MAX_ENTRIES = 4096;
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(Conveyance.MOD_ID, "factory_overlay"), () -> "1", "1"::equals, "1"::equals);
    private static final WeakHashMap<ServerPlayer, Boolean> WATCHING = new WeakHashMap<>();
    public record Snapshot(ResourceLocation dimension, List<FactoryOverlayStatus.Entry> entries) {}
    public static void register() {
        CHANNEL.registerMessage(0, Snapshot.class, (message, buffer) -> {
            buffer.writeResourceLocation(message.dimension()); buffer.writeVarInt(message.entries().size());
            for (var entry : message.entries()) {
                buffer.writeBlockPos(entry.root()); buffer.writeEnum(entry.kind());
                buffer.writeVarInt(entry.width()); buffer.writeVarInt(entry.depth()); buffer.writeVarInt(entry.height()); buffer.writeEnum(entry.facing()); buffer.writeVarInt(entry.reach());
                buffer.writeItem(entry.building()); buffer.writeItem(entry.product()); buffer.writeComponent(entry.operation()); buffer.writeEnum(entry.status()); buffer.writeFloat(entry.power());
            }
        }, buffer -> {
            ResourceLocation dimension = buffer.readResourceLocation(); int size = buffer.readVarInt();
            if (size < 0 || size > MAX_ENTRIES) throw new IllegalArgumentException("Invalid factory overlay size");
            var entries = new ArrayList<FactoryOverlayStatus.Entry>(size);
            for (int index = 0; index < size; index++) entries.add(new FactoryOverlayStatus.Entry(buffer.readBlockPos(), buffer.readEnum(FactoryOverlayStatus.Kind.class),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readEnum(Direction.class), buffer.readVarInt(),
                    buffer.readItem(), buffer.readItem(), buffer.readComponent(), buffer.readEnum(FactoryOverlayStatus.Status.class), buffer.readFloat()));
            return new Snapshot(dimension, List.copyOf(entries));
        }, (message, supplier) -> {
            var context = supplier.get(); context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> dev.conveyance.client.FactoryOverlayView.accept(message)));
            context.setPacketHandled(true);
        }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.level().getGameTime() % 5 != 0) return;
        if (!RemoteBuilding.active(player) || player.containerMenu != player.inventoryMenu) {
            if (WATCHING.remove(player) != null) send(player, List.of());
            return;
        }
        WATCHING.put(player, true);
        var level = player.serverLevel(); var nearby = new ArrayList<BlockEntity>();
        int x = player.chunkPosition().x, z = player.chunkPosition().z;
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            var chunk = level.getChunkSource().getChunk(x + dx, z + dz, ChunkStatus.FULL, false);
            if (chunk instanceof LevelChunk loaded) for (BlockEntity entity : loaded.getBlockEntities().values())
                if (entity instanceof dev.conveyance.factory.MachineEntity || entity instanceof dev.conveyance.factory.MinerEntity || entity instanceof dev.conveyance.factory.InserterEntity
                        || entity instanceof dev.conveyance.factory.BurnerInserterEntity || entity instanceof dev.conveyance.factory.RadarEntity || entity instanceof dev.conveyance.factory.PowerPoleEntity) nearby.add(entity);
        }
        nearby.sort(Comparator.comparingDouble(entity -> entity.getBlockPos().distToCenterSqr(player.position())));
        var entries = new ArrayList<FactoryOverlayStatus.Entry>();
        for (var entity : nearby) {
            if (entries.size() == MAX_ENTRIES) break;
            var entry = FactoryOverlayStatus.capture(entity); if (entry != null) entries.add(entry);
        }
        send(player, entries);
    }
    private static void send(ServerPlayer player, List<FactoryOverlayStatus.Entry> entries) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Snapshot(player.level().dimension().location(), entries));
    }
    private FactoryOverlayNetwork() {}
}
