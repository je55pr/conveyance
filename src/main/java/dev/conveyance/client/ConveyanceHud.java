package dev.conveyance.client;

import dev.conveyance.Conveyance;
import dev.conveyance.player.LinkedHotbar;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, value = Dist.CLIENT)
public final class ConveyanceHud {
    private ConveyanceHud() {}

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        var game = Minecraft.getInstance();
        if (event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id()) && game.player != null && !OverheadView.active() && !game.player.isSpectator() && LinkedHotbar.enabled(game.player)) {
            event.setCanceled(true);
            int x = game.getWindow().getGuiScaledWidth() / 2 - LinkedHotbar.SIZE * 10, y = game.getWindow().getGuiScaledHeight() - 22;
            for (int slot = 0; slot < LinkedHotbar.SIZE; slot++) HotbarLinksView.cell(event.getGuiGraphics(), slot, x + slot * 20, y, 20);
        }
        if (event.getOverlay().id().equals(VanillaGuiOverlay.FOOD_LEVEL.id())
                || event.getOverlay().id().equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())) {
            event.setCanceled(true);
        }
    }
}

