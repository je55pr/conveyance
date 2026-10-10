package dev.conveyance.player;

import dev.conveyance.Conveyance;
import dev.conveyance.factory.RemoteBuilding;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class RemoteBuildNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Conveyance.MOD_ID, "remote_build"), () -> "2", "2"::equals, "2"::equals);
    private record Toggle(boolean enabled) {}
    private record Action(int action, BlockPos pos, Direction facing, int slot, boolean secondary) {}
    public static void register() {
        CHANNEL.registerMessage(0, Toggle.class, (m, b) -> b.writeBoolean(m.enabled), b -> new Toggle(b.readBoolean()), (m, s) -> {
            var context = s.get(); context.enqueueWork(() -> { if (context.getSender() != null) RemoteBuilding.toggle(context.getSender(), m.enabled); });
            context.setPacketHandled(true);
        }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1, Action.class, (m, b) -> {
            b.writeVarInt(m.action); b.writeBlockPos(m.pos); b.writeEnum(m.facing); b.writeVarInt(m.slot); b.writeBoolean(m.secondary);
        }, b -> new Action(b.readVarInt(), b.readBlockPos(), b.readEnum(Direction.class), b.readVarInt(), b.readBoolean()), (m, s) -> {
            var context = s.get(); context.enqueueWork(() -> {
                if (context.getSender() != null) RemoteBuilding.act(context.getSender(), m.action, m.pos, m.facing, m.slot, m.secondary);
            }); context.setPacketHandled(true);
        }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
    public static void toggle(boolean enabled) { CHANNEL.sendToServer(new Toggle(enabled)); }
    public static void action(int action, BlockPos pos, Direction facing, int slot, boolean secondary) {
        CHANNEL.sendToServer(new Action(action, pos, facing, slot, secondary));
    }
    private RemoteBuildNetwork() {}
}
