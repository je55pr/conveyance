package dev.conveyance.client;

import dev.conveyance.player.LinkedHotbar;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class HotbarLinksView {
    static void cell(GuiGraphics graphics, int slot, int x, int y, int size) {
        var game = Minecraft.getInstance();
        var player = game.player;
        var item = slot == 0 ? LinkedHotbar.temporary(player) : LinkedHotbar.link(player, slot);
        boolean selected = LinkedHotbar.selectedSlot(player) == slot;
        graphics.fill(x, y, x + size, y + size, selected ? 0xFF8AD8A0 : 0xFF607080);
        graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, selected ? 0xFF375749 : 0xFF202A35);
        if (item.isEmpty()) {
            if (slot == 0) graphics.drawCenteredString(game.font, "Q", x + size / 2, y + (size - 8) / 2, selected ? 0xFFE4F2E8 : 0xFF94A0AD);
            return;
        }
        int inset = (size - 16) / 2;
        graphics.renderItem(item, x + inset, y + inset);
        int amount = LinkedHotbar.count(player, item.getItem());
        graphics.pose().pushPose(); graphics.pose().translate(0, 0, 250);
        if (amount == 0 && !player.isCreative()) graphics.fill(x + inset, y + inset, x + inset + 16, y + inset + 16, 0xA510141B);
        String count = player.isCreative() && amount == 0 ? "∞" : Integer.toString(amount);
        float scale = Math.min(0.8F, (size - 3.0F) / Math.max(1, game.font.width(count)));
        graphics.pose().translate(x + size - 2, y + size - 8, 50); graphics.pose().scale(scale, scale, 1);
        graphics.drawString(game.font, count, -game.font.width(count), 0, amount > 0 || player.isCreative() ? 0xFFFFFFFF : 0xFF94A0AD, true);
        graphics.pose().popPose();
    }
    static void tooltip(GuiGraphics graphics, int slot, int mouseX, int mouseY) {
        var game = Minecraft.getInstance();
        var item = slot == 0 ? LinkedHotbar.temporary(game.player) : LinkedHotbar.link(game.player, slot);
        var lines = item.isEmpty() ? new ArrayList<Component>() : new ArrayList<>(Screen.getTooltipFromItem(game, item));
        if (!item.isEmpty()) lines.add(Component.translatable("menu.conveyance.link_count", LinkedHotbar.count(game.player, item.getItem())));
        if (slot == 0) {
            lines.add(0, Component.translatable("menu.conveyance.empty_hand"));
            lines.add(Component.translatable(item.isEmpty() ? "menu.conveyance.empty_hand_help" : "menu.conveyance.temporary_hand_help"));
        } else lines.add(Component.translatable("menu.conveyance.link_help"));
        var rows = IconTooltips.itemLines(item, lines);
        if (slot == 0 && !item.isEmpty() && rows.size() > 1) {
            rows.set(0, IconTooltips.text(lines.get(0)));
            rows.set(1, IconTooltips.item(item, lines.get(1)));
        }
        IconTooltips.render(graphics, rows, mouseX, mouseY);
    }
    private HotbarLinksView() {}
}
