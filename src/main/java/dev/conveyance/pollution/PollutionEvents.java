package dev.conveyance.pollution;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import dev.conveyance.map.MapNetwork;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class PollutionEvents {
    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD || level.getGameTime() % 20 != 0) return;
        var pollution = PollutionData.get(level);
        pollution.tick(level);
        for (var player : level.players()) sync(player);
    }
    public static void sync(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD) return;
        int minX = player.chunkPosition().x - PollutionData.AMBIENT_WIDTH / 2;
        int minZ = player.chunkPosition().z - PollutionData.AMBIENT_WIDTH / 2;
        MapNetwork.syncPollution(player, minX, minZ, PollutionData.get(player.serverLevel()).nearby(minX, minZ));
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { if (event.getEntity() instanceof ServerPlayer player) sync(player); }
    @SubscribeEvent public static void unload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD && event.getChunk() instanceof LevelChunk chunk)
            PollutionData.get(level).observe(chunk, level.getGameTime());
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("conveyance").requires(source -> source.hasPermission(2))
                .then(Commands.literal("pollution")
                        .then(Commands.literal("get").executes(context -> {
                            var source = context.getSource(); var pos = BlockPos.containing(source.getPosition());
                            var data = PollutionData.get(source.getLevel());
                            double amount = data.at(pos), sink = data.absorption(source.getLevel(), Math.floorDiv(pos.getX(), 16), Math.floorDiv(pos.getZ(), 16));
                            source.sendSuccess(() -> Component.translatable("message.conveyance.pollution_status", String.format(java.util.Locale.ROOT, "%.2f", amount), String.format(java.util.Locale.ROOT, "%.2f", sink)), false);
                            return 1;
                        }))
                        .then(Commands.literal("add").then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01, 1_000_000)).executes(context -> {
                            var source = context.getSource(); var data = PollutionData.get(source.getLevel());
                            data.add(BlockPos.containing(source.getPosition()), DoubleArgumentType.getDouble(context, "amount")); data.changed();
                            for (var player : source.getLevel().players()) sync(player);
                            source.sendSuccess(() -> Component.translatable("message.conveyance.pollution_added"), false);
                            return 1;
                        })))
                        .then(Commands.literal("clear").executes(context -> {
                            var source = context.getSource(); PollutionData.get(source.getLevel()).clear();
                            for (var player : source.getLevel().players()) sync(player);
                            source.sendSuccess(() -> Component.translatable("message.conveyance.pollution_cleared"), false);
                            return 1;
                        }))));
    }
    private PollutionEvents() {}
}
