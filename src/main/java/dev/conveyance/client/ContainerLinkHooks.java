package dev.conveyance.client;

import dev.conveyance.Conveyance;
import dev.conveyance.player.InventoryNetwork;
import dev.conveyance.player.LinkedHotbar;
import dev.conveyance.player.LinkedStorageSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, value = Dist.CLIENT)
public final class ContainerLinkHooks {
    private static boolean linkPressed;
    @SubscribeEvent public static void render(ScreenEvent.Render.Post event) {
        var player = Minecraft.getInstance().player;
        if (player == null || !LinkedHotbar.enabled(player) || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        for (var slot : screen.getMenu().slots) if (slot instanceof LinkedStorageSlot) {
            int x = screen.getGuiLeft() + slot.x - 1, y = screen.getGuiTop() + slot.y - 1;
            HotbarLinksView.cell(event.getGuiGraphics(), slot.getContainerSlot(), x, y, 18);
            if (screen.getMenu().getCarried().isEmpty() && over(x, y, event.getMouseX(), event.getMouseY()))
                HotbarLinksView.tooltip(event.getGuiGraphics(), slot.getContainerSlot(), event.getMouseX(), event.getMouseY());
        }
    }
    @SubscribeEvent public static void click(ScreenEvent.MouseButtonPressed.Pre event) {
        linkPressed = false;
        var player = Minecraft.getInstance().player;
        if (player == null || !LinkedHotbar.enabled(player) || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        for (var slot : screen.getMenu().slots) if (slot instanceof LinkedStorageSlot
                && over(screen.getGuiLeft() + slot.x - 1, screen.getGuiTop() + slot.y - 1, event.getMouseX(), event.getMouseY())) {
            event.setCanceled(true);
            linkPressed = true;
            if (event.getButton() == 1) InventoryNetwork.requestLink(screen.getMenu().containerId, slot.getContainerSlot(), BuiltInRegistries.ITEM.getKey(Items.AIR));
            else if (event.getButton() == 0) {
                var carried = screen.getMenu().getCarried();
                if (carried.isEmpty()) player.getInventory().selected = slot.getContainerSlot();
                else InventoryNetwork.requestLink(screen.getMenu().containerId, slot.getContainerSlot(), BuiltInRegistries.ITEM.getKey(carried.getItem()));
            }
            return;
        }
    }
    @SubscribeEvent public static void release(ScreenEvent.MouseButtonReleased.Pre event) {
        var player = Minecraft.getInstance().player;
        if (linkPressed) { linkPressed = false; return; }
        if (player == null || !LinkedHotbar.enabled(player) || event.getButton() != 0 || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                || screen.getMenu().getCarried().isEmpty()) return;
        for (var slot : screen.getMenu().slots) if (slot instanceof LinkedStorageSlot
                && over(screen.getGuiLeft() + slot.x - 1, screen.getGuiTop() + slot.y - 1, event.getMouseX(), event.getMouseY())) {
            InventoryNetwork.requestLink(screen.getMenu().containerId, slot.getContainerSlot(), BuiltInRegistries.ITEM.getKey(screen.getMenu().getCarried().getItem()));
            return;
        }
    }
    private static boolean over(int x, int y, double mouseX, double mouseY) { return mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18; }
    private ContainerLinkHooks() {}
}
