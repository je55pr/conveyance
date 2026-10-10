package dev.conveyance.client;

import dev.conveyance.Conveyance;
import dev.conveyance.factory.FactoryTooltips;
import dev.conveyance.player.HandCraftingQueue;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CraftingHud {
    @SubscribeEvent public static void register(RegisterGuiOverlaysEvent event) { event.registerAboveAll("hand_crafting", CraftingHud::render); }
    private static double elapsed(float partialTick) {
        var game = Minecraft.getInstance(); var queue = HandCraftingQueue.client();
        return game.level == null || queue.blocked() ? 0 : Math.max(0, game.level.getGameTime() - queue.time() + partialTick);
    }
    public static double progress(float partialTick) {
        var queue = HandCraftingQueue.client();
        return Math.min(1, (queue.elapsed() + elapsed(partialTick)) / queue.duration());
    }
    public static Component eta(float partialTick) {
        var queue = HandCraftingQueue.client();
        if (queue.blocked()) return Component.translatable("hud.conveyance.crafting_blocked");
        long remaining = queue.orders().stream().mapToLong(HandCraftingQueue.OrderView::remainingTicks).sum();
        return Component.translatable("hud.conveyance.crafting_eta", FactoryTooltips.number(Math.ceil(Math.max(0, remaining - elapsed(partialTick)) / 2) / 10.0));
    }
    private static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        var game = Minecraft.getInstance(); var queue = HandCraftingQueue.client();
        if (game.options.hideGui || game.screen != null || game.player == null || game.player.isSpectator() || game.level == null || queue.orders().isEmpty()) return;
        int x = 8, y = height - (width < 500 ? 94 : 65);
        graphics.fill(x - 2, y - 2, x + 220, y + 54, 0xD91B252C);
        graphics.renderItem(queue.active(), x + 2, y + 2);
        graphics.drawString(game.font, game.font.plainSubstrByWidth(queue.active().getHoverName().getString(), 190), x + 23, y + 1, 0xFFE7EEF6, false);
        graphics.drawString(game.font, eta(partialTick), x + 23, y + 13, 0xFFBAC8D3, false);
        graphics.fill(x + 2, y + 24, x + 216, y + 27, 0xFF10161C);
        graphics.fill(x + 2, y + 24, x + 2 + (int) Math.round(214 * progress(partialTick)), y + 27, queue.blocked() ? 0xFFCA9565 : 0xFF75B98C);
        cells(graphics, x + 2, y + 31, 9, 0, partialTick);
    }
    public static void cells(GuiGraphics graphics, int x, int y, int visible, int offset, float partialTick) {
        var game = Minecraft.getInstance(); var orders = HandCraftingQueue.client().orders();
        for (int cell = 0; cell < visible && offset + cell < orders.size(); cell++) {
            int index = offset + cell, sx = x + cell * 20;
            var order = orders.get(index);
            graphics.fill(sx, y, sx + 18, y + 20, index == 0 ? 0xFF465F55 : 0xFF303D49);
            graphics.renderItem(order.output(), sx + 1, y + 1);
            String count = Integer.toString(order.batches() * order.output().getCount());
            graphics.pose().pushPose(); graphics.pose().translate(sx + 17, y + 11, 300); graphics.pose().scale(.65F, .65F, 1);
            graphics.drawString(game.font, count, -game.font.width(count), 0, 0xFFE7EEF6, true); graphics.pose().popPose();
            double ticks = order.remainingTicks() - (index == 0 ? elapsed(partialTick) : 0);
            double completed = Math.max(0, Math.min(1, 1 - ticks / Math.max(1, order.totalTicks())));
            graphics.fill(sx, y + 18, sx + (int) Math.round(18 * completed), y + 20, 0xFF8DCF9D);
        }
    }
    public static void tooltip(GuiGraphics graphics, HandCraftingQueue.OrderView order, int mouseX, int mouseY) {
        var game = Minecraft.getInstance();
        graphics.renderComponentTooltip(game.font, List.of(order.output().getHoverName(),
                Component.translatable("menu.conveyance.queued_items", order.batches() * order.output().getCount()),
                Component.translatable("menu.conveyance.crafting_cancel")), mouseX, mouseY);
    }
    private CraftingHud() {}
}
