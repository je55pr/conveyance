package dev.conveyance.combat;

import dev.conveyance.Conveyance;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class CombatNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(Conveyance.MOD_ID, "combat"), () -> "3", "3"::equals, "3"::equals);
    private record Shoot(Vec3 aim) {}
    public record Tracer(Vec3 from, Vec3 to) {}
    public record LaserBeam(Vec3 from, Vec3 to) {}
    public record Alert(net.minecraft.core.BlockPos origin, int count) {}
    private static Vec3 read(net.minecraft.network.FriendlyByteBuf buffer) { return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()); }
    private static void write(net.minecraft.network.FriendlyByteBuf buffer, Vec3 pos) { buffer.writeDouble(pos.x); buffer.writeDouble(pos.y); buffer.writeDouble(pos.z); }
    public static void register() {
        CHANNEL.registerMessage(0, Shoot.class, (m, b) -> write(b, m.aim), b -> new Shoot(read(b)), (m, supplier) -> {
            var context = supplier.get(); context.enqueueWork(() -> { if (context.getSender() != null) PistolItem.shoot(context.getSender(), m.aim); }); context.setPacketHandled(true);
        }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1, Tracer.class, (m, b) -> { write(b, m.from); write(b, m.to); }, b -> new Tracer(read(b), read(b)), (m, supplier) -> {
            var context = supplier.get(); context.enqueueWork(() -> dev.conveyance.client.CombatClient.tracer(m)); context.setPacketHandled(true);
        }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(2, Alert.class, (m, b) -> { b.writeBlockPos(m.origin); b.writeVarInt(m.count); }, b -> new Alert(b.readBlockPos(), b.readVarInt()), (m, supplier) -> {
            var context = supplier.get(); context.enqueueWork(() -> dev.conveyance.client.CombatClient.alert(m)); context.setPacketHandled(true);
        }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(3, LaserBeam.class, (m, b) -> { write(b, m.from); write(b, m.to); }, b -> new LaserBeam(read(b), read(b)), (m, supplier) -> {
            var context = supplier.get(); context.enqueueWork(() -> dev.conveyance.client.LaserBeamRenderer.accept(m)); context.setPacketHandled(true);
        }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    public static void shoot(Vec3 aim) { CHANNEL.sendToServer(new Shoot(aim)); }
    public static void laser(ServerLevel level, Vec3 from, Vec3 to) {
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(from.x, from.y, from.z, 96, level.dimension())), new LaserBeam(from, to));
    }
    public static void tracer(ServerLevel level, Vec3 from, Vec3 to) {
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(from.x, from.y, from.z, 96, level.dimension())), new Tracer(from, to));
    }
    public static void alert(net.minecraft.server.level.ServerPlayer player, net.minecraft.core.BlockPos origin, int count) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Alert(origin, count)); }
    private CombatNetwork() {}
}
