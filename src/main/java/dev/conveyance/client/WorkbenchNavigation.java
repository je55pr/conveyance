package dev.conveyance.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.conveyance.Conveyance;
import dev.conveyance.player.InventoryNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, value = Dist.CLIENT)
public final class WorkbenchNavigation {
    private static int openingTab, pendingTab = -1;
    private static boolean configuredChat;
    @Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Keys {
        public static final KeyMapping MAP = new KeyMapping("key.conveyance.map", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.conveyance");
        public static final KeyMapping TECHNOLOGY = new KeyMapping("key.conveyance.technology", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_T, "key.categories.conveyance");
        @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) { event.register(MAP); event.register(TECHNOLOGY); }
    }
    static int takeOpeningTab() { int tab = openingTab; openingTab = 0; return tab; }
    public static void open(int tab) {
        var game = Minecraft.getInstance();
        if (game.player == null || game.level == null) return;
        if (game.screen instanceof PlayerWorkbenchScreen screen) {
            if (screen.getMenu().getCarried().isEmpty()) screen.openTab(tab);
            else pendingTab = tab;
        } else openingTab = tab;
        InventoryNetwork.requestOpen(tab);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        var game = Minecraft.getInstance();
        if (event.phase == TickEvent.Phase.START) {
            if (!configuredChat && game.options != null) {
                configuredChat = true;
                if (Keys.TECHNOLOGY.matches(GLFW.GLFW_KEY_T, 0) && game.options.keyChat.matches(GLFW.GLFW_KEY_T, 0)) {
                    game.options.keyChat.setKey(InputConstants.getKey(GLFW.GLFW_KEY_ENTER, 0));
                    KeyMapping.resetMapping(); game.options.save();
                }
            }
            return;
        }
        if (game.player == null || game.level == null) { openingTab = 0; pendingTab = -1; return; }
        if (game.screen instanceof PlayerWorkbenchScreen screen && pendingTab >= 0 && screen.getMenu().getCarried().isEmpty()) {
            screen.openTab(pendingTab); pendingTab = -1;
        } else if (game.screen == null) pendingTab = -1;
        while (Keys.MAP.consumeClick()) if (game.screen == null) open(2);
        while (Keys.TECHNOLOGY.consumeClick()) if (game.screen == null) open(1);
    }
    @SubscribeEvent public static void key(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        var game = Minecraft.getInstance();
        if (screen instanceof FactoryWindowScreen<?> && game.options.keyInventory.matches(event.getKeyCode(), event.getScanCode())) {
            event.setCanceled(true); screen.onClose(); return;
        }
        if (screen.getFocused() instanceof EditBox edit && edit.isFocused()) return;
        int tab = game.options.keyInventory.matches(event.getKeyCode(), event.getScanCode()) ? 0
                : Keys.MAP.matches(event.getKeyCode(), event.getScanCode()) ? 2
                : Keys.TECHNOLOGY.matches(event.getKeyCode(), event.getScanCode()) ? 1 : -1;
        if (tab < 0) return;
        event.setCanceled(true);
        if (screen instanceof PlayerWorkbenchScreen workbench && workbench.selectedTab() == tab) workbench.onClose();
        else open(tab);
    }
    private WorkbenchNavigation() {}
}
