package dev.conveyance.client;

import dev.conveyance.Conveyance;
import dev.conveyance.factory.FactoryContent;
import dev.conveyance.player.ResearchState;
import dev.conveyance.player.Technology;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ResearchHud {
    @SubscribeEvent public static void register(RegisterGuiOverlaysEvent event) { event.registerAboveAll("research", ResearchHud::render); }

    private static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        var game = Minecraft.getInstance();
        if (game.options.hideGui || game.player == null || game.level == null || game.player.isSpectator()) return;
        Technology tech = Technology.byId(ResearchState.selected(game.level));
        if (tech != null && tech.complete(ResearchState.mask(game.level))) tech = null;
        int panelWidth = Math.min(208, width - 16), x = width - panelWidth - 8, y = 8;
        int textWidth = panelWidth - 36;
        graphics.fill(x, y, x + panelWidth, y + 42, 0xD94E615D);
        graphics.fill(x + 1, y + 1, x + panelWidth - 1, y + 41, 0xE51B252C);
        graphics.renderItem(tech == null ? new ItemStack(FactoryContent.LAB_ITEM.get()) : tech.icon(), x + 6, y + 6);
        Component title = tech == null ? Component.translatable("hud.conveyance.research") : tech.title();
        String name = title.getString();
        if (game.font.width(name) > textWidth) name = game.font.plainSubstrByWidth(name, textWidth - game.font.width("…")) + "…";
        graphics.drawString(game.font, name, x + 28, y + 5, 0xFFE7EEF6, false);
        graphics.fill(x + 6, y + 30, x + panelWidth - 6, y + 36, 0xFF0D1419);
        if (tech == null) {
            Component hint = Component.translatable("hud.conveyance.select_research", WorkbenchNavigation.Keys.TECHNOLOGY.getTranslatedKeyMessage());
            graphics.drawString(game.font, game.font.plainSubstrByWidth(hint.getString(), textWidth), x + 28, y + 18, 0xFFBDC9CE, false);
        } else {
            int total = Math.max(1, tech.packs), progress = Math.max(0, Math.min(total, ResearchState.progress(game.level, tech.id)));
            double fraction = progress / (double) total;
            Component completed = Component.translatable("hud.conveyance.research_progress", progress, total);
            String percent = (int) Math.floor(fraction * 100) + "%";
            int percentX = x + panelWidth - 7 - game.font.width(percent);
            graphics.drawString(game.font, game.font.plainSubstrByWidth(completed.getString(), percentX - x - 33), x + 28, y + 18, 0xFFBDC9CE, false);
            graphics.drawString(game.font, percent, percentX, y + 18, 0xFF97D6A6, false);
            graphics.fill(x + 6, y + 30, x + 6 + (int) Math.round((panelWidth - 12) * fraction), y + 36, 0xFF75B98C);
        }
    }
    private ResearchHud() {}
}
