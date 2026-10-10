package dev.conveyance.player;

import dev.conveyance.factory.CrateEntity;
import dev.conveyance.factory.FactoryContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Each player receives one starter crate per world, including an existing prototype save. */
public final class StarterSupplies {
    private static final String GRANTED = "ConveyanceStarterCrate";
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) place(player);
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player && player.level().getGameTime() % 20 == 0) place(player);
    }
    @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone event) {
        event.getEntity().getPersistentData().putBoolean(GRANTED, event.getOriginal().getPersistentData().getBoolean(GRANTED));
    }
    private static boolean free(ServerPlayer player, BlockPos pos) {
        var level = player.serverLevel();
        return !level.isOutsideBuildHeight(pos) && level.hasChunkAt(pos) && level.getBlockState(pos).isAir()
                && level.isUnobstructed(FactoryContent.WOOD_CRATE.get().defaultBlockState(), pos, CollisionContext.of(player));
    }
    private static BlockPos supported(ServerPlayer player, BlockPos horizontal) {
        var level = player.serverLevel();
        if (!level.hasChunkAt(horizontal)) return null;
        BlockPos pos = new BlockPos(horizontal.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, horizontal.getX(), horizontal.getZ()), horizontal.getZ());
        return Math.abs(pos.getY() - player.blockPosition().getY()) <= 3 && free(player, pos)
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP) ? pos : null;
    }
    private static BlockPos position(ServerPlayer player) {
        BlockPos origin = player.blockPosition();
        for (int radius = 2; radius <= 6; radius++) {
            BlockPos front = supported(player, origin.relative(player.getDirection(), radius));
            if (front != null) return front;
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                BlockPos pos = supported(player, origin.offset(dx, 0, dz));
                if (pos != null) return pos;
            }
        }
        // Crates can be placed without a foundation; this also covers a player's first login while flying.
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos pos = origin.relative(direction, 2);
            if (free(player, pos)) return pos;
        }
        return null;
    }
    private static void place(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || player.getPersistentData().getBoolean(GRANTED)) return;
        BlockPos pos = position(player);
        if (pos == null) return;
        var level = player.serverLevel();
        if (!level.setBlockAndUpdate(pos, FactoryContent.WOOD_CRATE.get().defaultBlockState()) || !(level.getBlockEntity(pos) instanceof CrateEntity crate)) return;
        crate.setItem(0, new ItemStack(Items.COBBLESTONE, 100));
        crate.setItem(1, new ItemStack(Items.IRON_INGOT, 100));
        crate.setItem(2, new ItemStack(Items.COPPER_INGOT, 100));
        crate.setItem(3, new ItemStack(Items.COAL, 100));
        crate.setItem(4, new ItemStack(FactoryContent.WOOD_ITEM.get(), 100));
        crate.setChanged();
        player.getPersistentData().putBoolean(GRANTED, true);
        player.displayClientMessage(Component.translatable("message.conveyance.starter_crate", pos.getX(), pos.getY(), pos.getZ()), false);
    }
    private StarterSupplies() {}
}
