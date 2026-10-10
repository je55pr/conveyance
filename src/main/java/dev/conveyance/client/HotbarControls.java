package dev.conveyance.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.conveyance.Conveyance;
import dev.conveyance.player.InventoryNetwork;
import dev.conveyance.player.LinkedHotbar;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, value = Dist.CLIENT)
public final class HotbarControls {
    private static boolean configuredDrop;
    @Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Keys {
        public static final KeyMapping EMPTY = new KeyMapping("key.conveyance.empty_hand", KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Q, "key.categories.conveyance");
        @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) { event.register(EMPTY); }
    }
    static void select(int slot) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        LinkedHotbar.select(player, slot); InventoryNetwork.requestSelection(slot);
    }
    static void pipette(BlockPos pos) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        LinkedHotbar.pipette(player, pos); InventoryNetwork.requestPipette(pos);
    }
    @SubscribeEvent public static void key(InputEvent.Key event) {
        var game = Minecraft.getInstance();
        if (event.getAction() != GLFW.GLFW_PRESS || game.screen != null || game.player == null || !LinkedHotbar.enabled(game.player)) return;
        for (int slot = 0; slot < 9; slot++) if (game.options.keyHotbarSlots[slot].matches(event.getKey(), event.getScanCode())) { select(slot + 1); break; }
    }
    @SubscribeEvent public static void screenKey(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                || screen.getFocused() instanceof EditBox edit && edit.isFocused() || !Keys.EMPTY.matches(event.getKeyCode(), event.getScanCode())) return;
        event.setCanceled(true); pipette(null);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        var game = Minecraft.getInstance();
        if (event.phase == TickEvent.Phase.START) {
            if (!configuredDrop && game.options != null) {
                configuredDrop = true;
                if (Keys.EMPTY.matches(GLFW.GLFW_KEY_Q, 0) && game.options.keyDrop.matches(GLFW.GLFW_KEY_Q, 0)) {
                    game.options.keyDrop.setKey(InputConstants.UNKNOWN); KeyMapping.resetMapping(); game.options.save();
                }
            }
            return;
        }
        while (Keys.EMPTY.consumeClick()) if (game.player != null && game.level != null && game.screen == null && !game.player.isSpectator()) {
            BlockPos pos = game.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : null;
            pipette(pos);
        }
    }
    private HotbarControls() {}
}
