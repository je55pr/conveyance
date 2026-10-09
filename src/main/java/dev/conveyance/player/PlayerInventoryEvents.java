package dev.conveyance.player;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import dev.conveyance.factory.FactoryPickups;

public final class PlayerInventoryEvents {
    private static final String PLAYER_THROW = "ConveyancePlayerThrow";
    @SubscribeEvent public static void toss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            if (event.getEntity().getOwner() == player && InventoryAccess.deliberateThrow())
                event.getEntity().getPersistentData().putBoolean(PLAYER_THROW, true);
            else {
                event.setCanceled(true);
                InventoryAccess.returnItem(player, event.getEntity().getItem());
            }
        }
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player && player.isAlive()) {
            if (player.level().getGameTime() % 20 == 0) FactoryPickups.normalizeInventory(player);
            InventoryAccess.flushReturns(player);
        }
    }
    @SubscribeEvent public static void openContainer(PlayerContainerEvent.Open event) {
        if (event.getEntity() instanceof ServerPlayer player) InventoryAccess.restoreCursor(player, event.getContainer());
    }
    @SubscribeEvent public static void itemSpawn(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof ItemEntity drop
                && !drop.getPersistentData().getBoolean(PLAYER_THROW)) event.setCanceled(true);
    }
    @SubscribeEvent public static void levelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
            level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true, level.getServer());
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) InventoryNetwork.sync(player);
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) InventoryNetwork.sync(player);
    }
    @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer) {
            event.getEntity().getPersistentData().put(ExtraInventory.DATA_KEY, event.getOriginal().getPersistentData().getCompound(ExtraInventory.DATA_KEY).copy());
            event.getEntity().getPersistentData().put(InventoryAccess.RETURNS_KEY,
                    event.getOriginal().getPersistentData().getList(InventoryAccess.RETURNS_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND).copy());
        }
    }
    @SubscribeEvent public static void pickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.isCanceled()) return;
        ItemEntity drop = event.getItem();
        var extra = InventoryAccess.extra(player);
        var items = InventoryAccess.snapshot(player, extra);
        ItemStack original = drop.getItem(), remaining = InventoryAccess.remainder(items, original);
        int taken = original.getCount() - remaining.getCount();
        if (taken == 0) return;
        event.setCanceled(true);
        InventoryAccess.commit(player, extra, items);
        player.take(drop, taken);
        player.awardStat(Stats.ITEM_PICKED_UP.get(original.getItem()), taken);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 1.0F);
        if (remaining.isEmpty()) drop.discard(); else drop.setItem(remaining);
    }
    @SubscribeEvent public static void craftingTable(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().getBlockState(event.getPos()).is(Blocks.CRAFTING_TABLE)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getEntity() instanceof ServerPlayer player) InventoryNetwork.open(player);
    }
    private PlayerInventoryEvents() {}
}
