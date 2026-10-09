package dev.conveyance.client;

import dev.conveyance.Conveyance;
import dev.conveyance.player.InventoryNetwork;
import dev.conveyance.player.ResearchState;
import dev.conveyance.player.TerrainHarvest;
import dev.conveyance.map.WorldMapData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, value = Dist.CLIENT)
public final class PlayerInventoryHooks {
    private static BlockPos target;
    private static int work;
    @SubscribeEvent public static void open(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof InventoryScreen || event.getNewScreen() instanceof CreativeModeInventoryScreen) {
            var game = Minecraft.getInstance();
            if (game.player != null && game.level != null) {
                event.setCanceled(true);
                InventoryNetwork.requestOpen();
            }
        }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { ResearchState.updateClient(0); WorldMapData.resetClient(); target = null; work = 0; }
    @SubscribeEvent public static void harvest(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var game = Minecraft.getInstance();
        if (game.player == null || game.level == null) { target = null; work = 0; return; }
        if (game.screen != null || !game.options.keyAttack.isDown() || ResearchState.unlocked(game.level, ResearchState.EXCAVATION)
                || !(game.hitResult instanceof BlockHitResult hit) || !TerrainHarvest.terrain(game.level.getBlockState(hit.getBlockPos()))) {
            reset(game);
            return;
        }
        if (!hit.getBlockPos().equals(target)) { reset(game); target = hit.getBlockPos().immutable(); }
        game.level.destroyBlockProgress(game.player.getId(), target, Math.min(9, ++work / 2));
        if (work >= 20) {
            InventoryNetwork.requestHarvest(target);
            game.level.destroyBlockProgress(game.player.getId(), target, -1);
            work = 0;
        }
    }
    private static void reset(Minecraft game) {
        if (target != null) game.level.destroyBlockProgress(game.player.getId(), target, -1);
        target = null; work = 0;
    }
    private PlayerInventoryHooks() {}
}
