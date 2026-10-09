package dev.conveyance.client;

import dev.conveyance.factory.FactoryContent;
import dev.conveyance.player.ResearchState;
import dev.conveyance.player.Technology;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/** A clipped, pannable research canvas. Inspection is independent of the active technology. */
public final class ResearchGraphView {
    private static final int NODE = 44;
    private int canvasHeight = 128, bodyHeight = 208, fittedWidth;
    private static final int[] NODES = java.util.stream.IntStream.concat(java.util.stream.IntStream.of(0),
            java.util.Arrays.stream(Technology.values()).mapToInt(tech -> tech.id)).toArray();
    private final Minecraft game = Minecraft.getInstance();
    private int inspected = ResearchState.AUTOMATION;
    private float zoom, panX, panY;
    private boolean dragging, opened;
    public int inspected() { return inspected; }
    public void resize(int width, int height) {
        boolean changed = fittedWidth != width || bodyHeight != height;
        fittedWidth = width; bodyHeight = height; canvasHeight = Math.max(80, height - 80);
        if (changed || zoom == 0) fit(width);
    }
    public void open() {
        if (opened) return;
        opened = true;
        if (ResearchState.selected(game.level) != 0) inspected = ResearchState.selected(game.level);
        else for (Technology tech : Technology.values()) if (tech.available(ResearchState.mask(game.level))) { inspected = tech.id; break; }
    }
    public void fit(int width) {
        int graphWidth = java.util.Arrays.stream(Technology.values()).mapToInt(tech -> tech.x + NODE + 20).max().orElse(64);
        int graphHeight = java.util.Arrays.stream(Technology.values()).mapToInt(tech -> tech.y + NODE + 16).max().orElse(64);
        zoom = Math.min(1.25F, Math.min((width - 28) / (float) graphWidth, (canvasHeight - 26) / (float) graphHeight));
        panX = (width - 16 - graphWidth * zoom) / 2 + 10 * zoom;
        panY = 18 + (canvasHeight - 26 - graphHeight * zoom) / 2;
    }
    private static int x(int id) { Technology tech = Technology.byId(id); return tech == null ? 0 : tech.x; }
    private static int y(int id) { Technology tech = Technology.byId(id); return tech == null ? 96 : tech.y; }
    private static Component name(int id) { Technology tech = Technology.byId(id); return tech == null ? Component.translatable("technology.conveyance.basic_industry.name") : tech.title(); }
    private static ItemStack icon(int id) { Technology tech = Technology.byId(id); return tech == null ? new ItemStack(FactoryContent.BURNER_MINER_ITEM.get()) : tech.icon(); }
    private int color(int id) {
        Technology tech = Technology.byId(id);
        if (tech == null || tech.complete(ResearchState.mask(game.level))) return 0xFF589B70;
        if (ResearchState.selected(game.level) == id) return 0xFFE3B44F;
        return tech.available(ResearchState.mask(game.level)) ? 0xFF9D925F : 0xFF495B6D;
    }
    private String status(int id) {
        Technology tech = Technology.byId(id);
        if (tech == null || tech.complete(ResearchState.mask(game.level))) return "completed";
        if (ResearchState.selected(game.level) == id) return "active";
        return tech.available(ResearchState.mask(game.level)) ? "available" : "locked";
    }
    private static String prerequisites(Technology tech) {
        return tech == null || tech.prerequisites().isEmpty() ? Component.translatable("technology.conveyance.basic_industry.name").getString()
                : String.join(", ", tech.prerequisites().stream().map(parent -> parent.title().getString()).toList());
    }
    public void render(GuiGraphics graphics, int left, int top, int width, int mouseX, int mouseY) {
        int canvasX = left + 8, canvasY = top + 8, canvasWidth = width - 16;
        graphics.fill(canvasX - 1, canvasY - 1, canvasX + canvasWidth + 1, canvasY + canvasHeight + 1, 0xFF53697A);
        graphics.fill(canvasX, canvasY, canvasX + canvasWidth, canvasY + canvasHeight, 0xFF111B26);
        graphics.enableScissor(canvasX, canvasY, canvasX + canvasWidth, canvasY + canvasHeight);
        int step = Math.max(8, (int) (32 * zoom));
        for (int gridX = canvasX + Math.floorMod((int) panX, step); gridX < canvasX + canvasWidth; gridX += step) graphics.fill(gridX, canvasY, gridX + 1, canvasY + canvasHeight, 0xFF192633);
        for (int gridY = canvasY + Math.floorMod((int) panY, step); gridY < canvasY + canvasHeight; gridY += step) graphics.fill(canvasX, gridY, canvasX + canvasWidth, gridY + 1, 0xFF192633);
        graphics.pose().pushPose();
        graphics.pose().translate(canvasX + panX, canvasY + panY, 0);
        graphics.pose().scale(zoom, zoom, 1);
        for (Technology tech : Technology.values()) {
            if (tech.prerequisites().isEmpty()) connection(graphics, 0, tech.id);
            else for (Technology parent : tech.prerequisites()) connection(graphics, parent.id, tech.id);
        }
        for (int id : NODES) node(graphics, id);
        graphics.pose().popPose();
        graphics.disableScissor();
        graphics.drawString(game.font, Component.translatable("menu.conveyance.tree_controls"), canvasX + 4, canvasY + 3, 0xFF8397A8, false);
        details(graphics, left, top, width);
        int hovered = hit(mouseX, mouseY, left, top, width);
        if (hovered >= 0) tooltip(graphics, hovered, mouseX, mouseY);
        unlockTooltip(graphics, left, top, mouseX, mouseY);
    }
    private void connection(GuiGraphics graphics, int from, int to) {
        int sourceX = x(from) + NODE, sourceY = y(from) + NODE / 2, targetX = x(to), targetY = y(to) + NODE / 2;
        int elbow = (sourceX + targetX) / 2;
        Technology parent = Technology.byId(from);
        int color = ResearchState.selected(game.level) == to ? 0xFFE3B44F
                : parent == null || parent.complete(ResearchState.mask(game.level)) ? 0xFF729B87 : 0xFF394B5A;
        graphics.fill(sourceX, sourceY - 1, elbow + 1, sourceY + 1, color);
        graphics.fill(elbow - 1, Math.min(sourceY, targetY), elbow + 1, Math.max(sourceY, targetY) + 1, color);
        graphics.fill(elbow, targetY - 1, targetX, targetY + 1, color);
        graphics.fill(targetX - 5, targetY - 4, targetX - 3, targetY + 4, color);
        graphics.fill(targetX - 3, targetY - 2, targetX, targetY + 2, color);
    }
    private void node(GuiGraphics graphics, int id) {
        int x = x(id), y = y(id), border = color(id);
        graphics.fill(x - 2, y - 2, x + NODE + 2, y + NODE + 2, inspected == id ? 0xFFE5EEF6 : 0xFF243646);
        graphics.fill(x, y, x + NODE, y + NODE, border);
        graphics.fill(x + 2, y + 2, x + NODE - 2, y + NODE - 5, 0xFF253341);
        graphics.pose().pushPose();
        graphics.pose().translate(x + 6, y + 4, 0);
        graphics.pose().scale(2, 2, 1);
        graphics.renderItem(icon(id), 0, 0);
        graphics.pose().popPose();
        Technology tech = Technology.byId(id);
        if (tech != null) {
            graphics.fill(x + 2, y + 2, x + 7, y + 7, 0xFFDB5555);
            if (tech.green) graphics.fill(x + 8, y + 2, x + 13, y + 7, 0xFF55BE65);
        }
        if (tech != null && !tech.complete(ResearchState.mask(game.level))) {
            String cost = Integer.toString(tech.packs);
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 300);
            graphics.drawString(game.font, cost, x + NODE - game.font.width(cost) - 2, y + 32, 0xFFFFFFFF, true);
            graphics.pose().popPose();
            int done = ResearchState.progress(game.level, id);
            graphics.fill(x, y + NODE - 4, x + NODE * done / tech.packs, y + NODE, 0xFF99D2AC);
        }
        graphics.drawCenteredString(game.font, tech == null ? name(id) : tech.shortName(), x + NODE / 2, y + NODE + 6, 0xFFD3DEE8);
    }
    private void details(GuiGraphics graphics, int left, int top, int width) {
        top += canvasHeight - 128;
        Technology tech = Technology.byId(inspected);
        graphics.fill(left + 1, top + 142, left + width - 1, top + 208, 0xFF263644);
        graphics.drawString(game.font, name(inspected), left + 8, top + 149, 0xFFE7EEF6, false);
        Component state = Component.translatable("research.conveyance.state." + status(inspected));
        graphics.drawString(game.font, state, left + width - game.font.width(state) - 8, top + 149, color(inspected), false);
        if (tech == null) {
            graphics.drawString(game.font, Component.translatable("technology.conveyance.basic_industry.description"), left + 8, top + 166, 0xFFBAC9D7, false);
        } else {
            graphics.renderItem(new ItemStack(FactoryContent.RED_SCIENCE_ITEM.get()), left + 8, top + 164);
            if (tech.green) graphics.renderItem(new ItemStack(FactoryContent.GREEN_SCIENCE_ITEM.get()), left + 28, top + 164);
            graphics.drawString(game.font, ResearchState.progress(game.level, tech.id) + "/" + tech.packs, left + 50, top + 168, 0xFFD5E2ED, false);
            String needed = Component.translatable("research.conveyance.requires", prerequisites(tech)).getString();
            graphics.drawString(game.font, game.font.plainSubstrByWidth(needed, width - 116), left + 108, top + 168, 0xFFBAC9D7, false);
        }
        graphics.drawString(game.font, Component.translatable(tech != null && tech.recipes.isEmpty() ? "research.conveyance.upgrades" : "research.conveyance.unlocks"), left + 8, top + 188, 0xFFBAC9D7, false);
        List<ItemStack> unlocks = unlocks(tech);
        for (int index = 0; index < unlocks.size(); index++) graphics.renderItem(unlocks.get(index), left + 56 + index * 20, top + 184);
    }
    private static List<ItemStack> unlocks(Technology tech) {
        return tech == null ? List.of(new ItemStack(FactoryContent.BURNER_MINER_ITEM.get()), new ItemStack(FactoryContent.FURNACE_ITEM.get()),
                new ItemStack(FactoryContent.CONVEYOR_ITEM.get()), new ItemStack(FactoryContent.BURNER_INSERTER_ITEM.get()), new ItemStack(FactoryContent.LAB_ITEM.get())) : tech.unlocks();
    }
    private void unlockTooltip(GuiGraphics graphics, int left, int top, int mouseX, int mouseY) {
        Technology tech = Technology.byId(inspected);
        List<ItemStack> items = unlocks(tech);
        int iconY = top + canvasHeight + 56;
        for (int index = 0; index < items.size(); index++) {
            int iconX = left + 56 + index * 20;
            if (mouseX < iconX || mouseX >= iconX + 16 || mouseY < iconY || mouseY >= iconY + 16) continue;
            ItemStack item = items.get(index);
            var lines = new ArrayList<Component>(net.minecraft.client.gui.screens.Screen.getTooltipFromItem(game, item));
            String extra = "tooltip.conveyance." + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem()).getPath();
            if (net.minecraft.client.resources.language.I18n.exists(extra) && !net.minecraft.client.resources.language.I18n.get(extra).contains("%s")) {
                Component description = Component.translatable(extra);
                if (lines.stream().noneMatch(line -> line.getString().equals(description.getString()))) lines.add(description.copy().withStyle(ChatFormatting.GRAY));
            }
            if (tech != null && tech.recipes.isEmpty()) lines.add(tech.description().copy().withStyle(ChatFormatting.GREEN));
            graphics.renderComponentTooltip(game.font, lines, mouseX, mouseY);
            return;
        }
    }
    private void tooltip(GuiGraphics graphics, int id, int mouseX, int mouseY) {
        var lines = new ArrayList<Component>();
        lines.add(name(id));
        Technology tech = Technology.byId(id);
        lines.add(tech == null ? Component.translatable("technology.conveyance.basic_industry.description") : tech.description());
        lines.add(Component.translatable("research.conveyance.state." + status(id)).withStyle(ChatFormatting.GRAY));
        if (tech != null) {
            lines.add(Component.translatable("research.conveyance.science_cost", ResearchState.progress(game.level, id), tech.packs));
            lines.add(tech.scienceCost().copy().withStyle(ChatFormatting.YELLOW));
            for (Technology parent : tech.prerequisites()) lines.add(Component.translatable("research.conveyance.requires", parent.title())
                    .withStyle(parent.complete(ResearchState.mask(game.level)) ? ChatFormatting.GREEN : ChatFormatting.RED));
            for (ItemStack stack : tech.unlocks()) lines.add(Component.translatable(tech.recipes.isEmpty() ? "research.conveyance.upgrade_item" : "research.conveyance.unlock_item", stack.getHoverName()));
        }
        graphics.renderComponentTooltip(game.font, lines, mouseX, mouseY);
    }
    private boolean canvas(double mouseX, double mouseY, int left, int top, int width) {
        return mouseX >= left + 8 && mouseX < left + width - 8 && mouseY >= top + 8 && mouseY < top + 8 + canvasHeight;
    }
    private int hit(double mouseX, double mouseY, int left, int top, int width) {
        if (!canvas(mouseX, mouseY, left, top, width)) return -1;
        double worldX = (mouseX - left - 8 - panX) / zoom, worldY = (mouseY - top - 8 - panY) / zoom;
        for (int id : NODES) if (worldX >= x(id) - 10 && worldX < x(id) + NODE + 10 && worldY >= y(id) && worldY < y(id) + NODE + 16) return id;
        return -1;
    }
    public boolean click(double mouseX, double mouseY, int button, int left, int top, int width) {
        if (!canvas(mouseX, mouseY, left, top, width)) return false;
        int node = hit(mouseX, mouseY, left, top, width);
        if (button == 0 && node >= 0) inspected = node;
        else dragging = true;
        return true;
    }
    public boolean drag(double deltaX, double deltaY) { if (!dragging) return false; panX += (float) deltaX; panY += (float) deltaY; return true; }
    public boolean release() { boolean previous = dragging; dragging = false; return previous; }
    public boolean scroll(double mouseX, double mouseY, double amount, int left, int top, int width) {
        if (!canvas(mouseX, mouseY, left, top, width)) return false;
        float next = Mth.clamp(zoom * (amount > 0 ? 1.15F : 1 / 1.15F), 0.25F, 1.75F);
        float worldX = (float) (mouseX - left - 8 - panX) / zoom, worldY = (float) (mouseY - top - 8 - panY) / zoom;
        panX = (float) (mouseX - left - 8) - worldX * next;
        panY = (float) (mouseY - top - 8) - worldY * next;
        zoom = next;
        return true;
    }
}
