package dev.conveyance.client;

import dev.conveyance.player.ResearchState;
import dev.conveyance.player.Technology;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/** Research cards and a local graph containing only the selected technology's direct neighbours. */
public final class ResearchGraphView {
    private static final int RED = 0xFFCE6258, AMBER = 0xFFE3B44F, GREEN = 0xFF69B781;
    private static final int COLUMNS = 3, CARD_HEIGHT = 80, CARD_STEP = 84, HEADER = 112, FOOTER = 32;
    private static final int NODE_W = 68, NODE_H = 54, ROW = 62, SELECT_X = 110, CHILD_X = 246;
    private final Minecraft game = Minecraft.getInstance();
    private int inspected = ResearchState.AUTOMATION, bodyHeight = 216, fittedWidth = 396, listScroll;
    private float zoom = 1, panX, panY;
    private boolean dragging, draggingList, opened;
    private LocalGraph local;
    private record Box(int x, int y, int width, int height) {
        boolean contains(double mx, double my) { return mx >= x && mx < x + width && my >= y && my < y + height; }
    }
    private record Node(Technology tech, int x, int y, int width, int height) {}
    private record LocalGraph(List<Node> nodes, int width, int height) {}
    public int inspected() { return inspected; }
    public void resize(int width, int height) {
        boolean changed = fittedWidth != width || bodyHeight != height;
        fittedWidth = width; bodyHeight = height;
        if (changed) fit(width);
    }
    public void open() {
        if (opened) return;
        opened = true;
        int active = ResearchState.selected(game.level);
        if (Technology.byId(active) != null) select(active);
        else for (Technology tech : Technology.values()) if (tech.available(mask())) { select(tech.id); break; }
        fit(fittedWidth);
        keepVisible();
    }
    private long mask() { return ResearchState.mask(game.level); }
    private Technology selected() { return Technology.byId(inspected); }
    private int sidebar(int width) { return Mth.clamp((int) (width * .48F), 236, 300); }
    private Box listBox(int left, int top, int width) { return new Box(left + 8, top + 28, sidebar(width) - 16, Math.max(40, bodyHeight - 76)); }
    private Box chartBox(int left, int top, int width) {
        int side = sidebar(width);
        return new Box(left + side + 16, top + HEADER + 4, width - side - 24, Math.max(40, bodyHeight - HEADER - FOOTER - 8));
    }
    private List<Technology> cards() {
        long complete = mask(); int active = ResearchState.selected(game.level);
        return Arrays.stream(Technology.values()).sorted(Comparator.comparingInt((Technology tech) -> tech.id == active ? 0
                : tech.available(complete) ? 1 : tech.complete(complete) ? 3 : 2).thenComparingInt(Technology::ordinal)).toList();
    }
    private int cardRows() { return (Technology.values().length + COLUMNS - 1) / COLUMNS; }
    private int maxScroll() { return Math.max(0, cardRows() * CARD_STEP - 4 - listBox(0, 0, fittedWidth).height()); }
    private void keepVisible() {
        var cards = cards(); int index = cards.indexOf(selected());
        if (index < 0) return;
        int y = index / COLUMNS * CARD_STEP, height = listBox(0, 0, fittedWidth).height();
        if (y < listScroll) listScroll = y;
        else if (y + CARD_HEIGHT > listScroll + height) listScroll = y + CARD_HEIGHT - height;
        listScroll = Mth.clamp(listScroll, 0, maxScroll());
    }
    private void select(int id) {
        if (Technology.byId(id) == null) return;
        inspected = id; local = null; dragging = draggingList = false;
        fit(fittedWidth); keepVisible();
    }
    private LocalGraph graph() {
        if (local != null) return local;
        Technology tech = selected();
        var parents = tech.prerequisites();
        var children = Arrays.stream(Technology.values()).filter(child -> (child.prerequisiteMask & tech.bit) != 0).toList();
        int rows = Math.max(1, Math.max(parents.size(), Math.min(5, children.size()))), contentHeight = Math.max(70, rows * ROW);
        var nodes = new ArrayList<Node>();
        nodes.add(new Node(tech, SELECT_X, 28 + (contentHeight - 70) / 2, 86, 70));
        for (int index = 0; index < parents.size(); index++)
            nodes.add(new Node(parents.get(index), 0, 28 + (contentHeight - (parents.size() - 1) * ROW - NODE_H) / 2 + index * ROW, NODE_W, NODE_H));
        for (int index = 0; index < children.size(); index++) {
            int column = index / 5, count = Math.min(5, children.size() - column * 5);
            nodes.add(new Node(children.get(index), CHILD_X + column * 90,
                    28 + (contentHeight - (count - 1) * ROW - NODE_H) / 2 + index % 5 * ROW, NODE_W, NODE_H));
        }
        int columns = Math.max(1, (children.size() + 4) / 5);
        return local = new LocalGraph(nodes, CHILD_X + (columns - 1) * 90 + NODE_W, contentHeight + 40);
    }
    public void fit(int width) {
        var box = chartBox(0, 0, width); var graph = graph();
        zoom = Math.max(.25F, Math.min(1.25F, Math.min((box.width() - 20F) / graph.width(), (box.height() - 12F) / graph.height())));
        panX = (box.width() - graph.width() * zoom) / 2; panY = (box.height() - graph.height() * zoom) / 2;
    }
    private int colour(Technology tech) { return tech.complete(mask()) ? GREEN : tech.available(mask()) ? AMBER : RED; }
    private String status(Technology tech) {
        return tech.complete(mask()) ? "completed" : ResearchState.selected(game.level) == tech.id ? "active" : tech.available(mask()) ? "available" : "locked";
    }
    private static int dark(int colour) { return 0xFF000000 | (colour >> 16 & 255) / 6 << 16 | (colour >> 8 & 255) / 6 << 8 | (colour & 255) / 6; }
    public void render(GuiGraphics graphics, int left, int top, int width, int mouseX, int mouseY) {
        int side = sidebar(width), right = left + side + 8, rightWidth = width - side - 12;
        FactoryWindowStyle.panel(graphics, left + 4, top + 4, side - 8, bodyHeight - 8);
        FactoryWindowStyle.panel(graphics, right, top + 4, rightWidth, bodyHeight - 8);
        graphics.drawString(game.font, Component.translatable("research.conveyance.cards"), left + 10, top + 10, FactoryWindowStyle.TEXT, false);
        renderCards(graphics, left, top, width);
        header(graphics, right, top, rightWidth);
        renderGraph(graphics, left, top, width);
        legend(graphics, left + 10, top + bodyHeight - 42);
    }
    public void renderTooltip(GuiGraphics graphics, int left, int top, int width, int mouseX, int mouseY) {
        ItemStack unlock = unlockAt(left, top, width, mouseX, mouseY);
        if (!unlock.isEmpty()) unlockTooltip(graphics, unlock, mouseX, mouseY);
        else {
            Technology hovered = cardAt(mouseX, mouseY, left, top, width);
            if (hovered == null) hovered = nodeAt(mouseX, mouseY, left, top, width);
            if (hovered != null) tooltip(graphics, hovered, mouseX, mouseY);
        }
    }
    private void renderCards(GuiGraphics graphics, int left, int top, int width) {
        var box = listBox(left, top, width); var cards = cards();
        listScroll = Mth.clamp(listScroll, 0, maxScroll());
        graphics.enableScissor(box.x(), box.y(), box.x() + box.width(), box.y() + box.height());
        int columnWidth = (box.width() - 7) / COLUMNS, cardWidth = columnWidth - 4;
        for (int index = 0; index < cards.size(); index++) {
            Technology tech = cards.get(index);
            int x = box.x() + index % COLUMNS * columnWidth, y = box.y() + index / COLUMNS * CARD_STEP - listScroll;
            if (y + CARD_HEIGHT <= box.y() || y >= box.y() + box.height()) continue;
            int state = colour(tech);
            graphics.fill(x, y, x + cardWidth, y + CARD_HEIGHT, tech.id == inspected ? FactoryWindowStyle.TEXT : state);
            graphics.fill(x + 1, y + 1, x + cardWidth - 1, y + CARD_HEIGHT - 1, dark(state));
            graphics.fill(x + 1, y + 1, x + 4, y + CARD_HEIGHT - 1, state);
            scaledIcon(graphics, tech.icon(), x + (cardWidth - 32) / 2, y + 6, 2);
            var title = game.font.split(tech.title(), cardWidth - 6);
            for (int line = 0; line < Math.min(2, title.size()); line++) graphics.drawString(game.font, title.get(line),
                    x + (cardWidth - game.font.width(title.get(line))) / 2, y + 41 + line * 10, FactoryWindowStyle.TEXT, false);
            String cost = Integer.toString(tech.target());
            int scienceX = x + (cardWidth - ((tech.triggered() ? 1 : Integer.bitCount(tech.scienceMask)) * 12 + 4 + game.font.width(cost))) / 2;
            if (tech.triggered()) { scaledIcon(graphics, tech.triggerIcon(), scienceX, y + 64, .75F); scienceX += 12; }
            for (int pack = 0; pack < Technology.SCIENCE_SLOTS; pack++) if (tech.needsPack(pack)) {
                scaledIcon(graphics, new ItemStack(Technology.packItem(pack)), scienceX, y + 64, .75F);
                scienceX += 12;
            }
            graphics.drawString(game.font, cost, scienceX + 4, y + 66, FactoryWindowStyle.TEXT, false);
        }
        graphics.disableScissor();
        if (maxScroll() > 0) {
            int thumb = Math.max(18, box.height() * box.height() / (cardRows() * CARD_STEP));
            int y = box.y() + listScroll * (box.height() - thumb) / maxScroll();
            graphics.fill(box.x() + box.width() - 4, box.y(), box.x() + box.width(), box.y() + box.height(), FactoryWindowStyle.SLOT_BACKGROUND);
            graphics.fill(box.x() + box.width() - 4, y, box.x() + box.width(), y + thumb, FactoryWindowStyle.BORDER);
        }
    }
    private void header(GuiGraphics graphics, int x, int top, int width) {
        var tech = selected();
        scaledIcon(graphics, tech.icon(), x + 8, top + 9, 2);
        graphics.drawString(game.font, game.font.plainSubstrByWidth(tech.title().getString(), width - 54), x + 48, top + 10, FactoryWindowStyle.TEXT, false);
        graphics.drawString(game.font, Component.translatable("research.conveyance.state." + status(tech)), x + 48, top + 24, colour(tech), false);
        var description = game.font.split(tech.description(), width - 16);
        for (int line = 0; line < Math.min(2, description.size()); line++) graphics.drawString(game.font, description.get(line), x + 8, top + 44 + line * 10, FactoryWindowStyle.MUTED, false);
        int scienceX = x + 8;
        if (tech.triggered()) { RecipePresentation.costIcon(graphics, tech.triggerIcon(), tech.target(), scienceX, top + 66, FactoryWindowStyle.TEXT); scienceX += 26; }
        for (int pack = 0; pack < Technology.SCIENCE_SLOTS; pack++) if (tech.needsPack(pack)) {
            RecipePresentation.costIcon(graphics, new ItemStack(Technology.packItem(pack)), tech.packs, scienceX, top + 66, FactoryWindowStyle.TEXT);
            scienceX += 26;
        }
        graphics.drawString(game.font, ResearchState.progress(game.level, tech.id) + " / " + tech.target(), scienceX + 8, top + 71, FactoryWindowStyle.TEXT, false);
        Component label = Component.translatable(tech.recipes.isEmpty() ? "research.conveyance.upgrades" : "research.conveyance.unlocks");
        graphics.drawString(game.font, label, x + 8, top + 95, FactoryWindowStyle.MUTED, false);
        for (int index = 0; index < tech.unlocks().size(); index++) graphics.renderItem(tech.unlocks().get(index), x + 16 + game.font.width(label) + index * 20, top + 90);
    }
    private void legend(GuiGraphics graphics, int x, int y) {
        String[] keys = {"unresearchable", "researchable", "researched"}; int[] colours = {RED, AMBER, GREEN};
        for (int index = 0; index < keys.length; index++) {
            graphics.fill(x, y + index * 11 + 1, x + 6, y + index * 11 + 7, colours[index]);
            graphics.drawString(game.font, Component.translatable("research.conveyance.legend." + keys[index]), x + 10, y + index * 11, FactoryWindowStyle.MUTED, false);
        }
    }
    private void renderGraph(GuiGraphics graphics, int left, int top, int width) {
        var box = chartBox(left, top, width); var graph = graph();
        graphics.fill(box.x(), box.y(), box.x() + box.width(), box.y() + box.height(), FactoryWindowStyle.SLOT_BACKGROUND);
        graphics.enableScissor(box.x(), box.y(), box.x() + box.width(), box.y() + box.height());
        graphics.pose().pushPose(); graphics.pose().translate(box.x() + panX, box.y() + panY, 0); graphics.pose().scale(zoom, zoom, 1);
        graphics.drawCenteredString(game.font, Component.translatable("research.conveyance.prerequisites"), NODE_W / 2, 4, FactoryWindowStyle.MUTED);
        graphics.drawCenteredString(game.font, Component.translatable("research.conveyance.selected"), SELECT_X + 43, 4, FactoryWindowStyle.MUTED);
        graphics.drawString(game.font, Component.translatable("research.conveyance.dependents"), CHILD_X, 4, FactoryWindowStyle.MUTED, false);
        Node centre = graph.nodes().get(0);
        for (Node node : graph.nodes()) if (node != centre) {
            if ((selected().prerequisiteMask & node.tech().bit) != 0) connection(graphics, node, centre);
            else connection(graphics, centre, node);
        }
        if (selected().prerequisites().isEmpty()) placeholder(graphics, "research.conveyance.no_prerequisites", 0, centre.y() + 20);
        if (graph.nodes().stream().noneMatch(node -> (node.tech().prerequisiteMask & selected().bit) != 0)) placeholder(graphics, "research.conveyance.no_dependents", CHILD_X, centre.y() + 20);
        for (Node node : graph.nodes()) node(graphics, node);
        graphics.pose().popPose(); graphics.disableScissor();
    }
    private void placeholder(GuiGraphics graphics, String key, int x, int y) {
        var lines = game.font.split(Component.translatable(key), NODE_W);
        for (int line = 0; line < lines.size(); line++) graphics.drawString(game.font, lines.get(line), x, y + line * 10, FactoryWindowStyle.MUTED, false);
    }
    private void connection(GuiGraphics graphics, Node from, Node to) {
        int x0 = from.x() + from.width(), y0 = from.y() + from.height() / 2, x1 = to.x(), y1 = to.y() + to.height() / 2;
        int elbow = x0 + 20, colour = colour(to.tech());
        graphics.fill(x0, y0 - 1, elbow + 1, y0 + 1, colour);
        if (to.x() > CHILD_X) {
            // Route extra dependent columns above the first column, not through its cards.
            int bus = x1 - 12, corridor = 20;
            graphics.fill(elbow - 1, Math.min(y0, corridor), elbow + 1, Math.max(y0, corridor) + 1, colour);
            graphics.fill(elbow, corridor - 1, bus + 1, corridor + 1, colour);
            graphics.fill(bus - 1, corridor, bus + 1, y1 + 1, colour);
            graphics.fill(bus, y1 - 1, x1, y1 + 1, colour);
        } else {
            graphics.fill(elbow - 1, Math.min(y0, y1), elbow + 1, Math.max(y0, y1) + 1, colour);
            graphics.fill(elbow, y1 - 1, x1, y1 + 1, colour);
        }
        graphics.fill(x1 - 5, y1 - 3, x1 - 3, y1 + 3, colour); graphics.fill(x1 - 3, y1 - 1, x1, y1 + 1, colour);
    }
    private void node(GuiGraphics graphics, Node node) {
        Technology tech = node.tech(); int x = node.x(), y = node.y(), state = colour(tech); boolean selected = tech.id == inspected;
        graphics.fill(x - 1, y - 1, x + node.width() + 1, y + node.height() + 1, selected ? FactoryWindowStyle.TEXT : state);
        graphics.fill(x, y, x + node.width(), y + node.height(), dark(state));
        graphics.fill(x, y, x + node.width(), y + 3, state);
        int size = selected ? 48 : zoom < .65F ? 40 : 32;
        scaledIcon(graphics, tech.icon(), x + (node.width() - size) / 2, y + 5, size / 16F);
        if (zoom >= .65F) graphics.drawCenteredString(game.font, game.font.plainSubstrByWidth(tech.shortName().getString(), node.width() - 4),
                x + node.width() / 2, y + node.height() - 15, FactoryWindowStyle.TEXT);
        FactoryWindowStyle.progress(graphics, x + 2, y + node.height() - 4, node.width() - 4, 2,
                ResearchState.progress(game.level, tech.id) / (float) Math.max(1, tech.target()), state);
    }
    private static void scaledIcon(GuiGraphics graphics, ItemStack icon, int x, int y, float scale) {
        graphics.pose().pushPose(); graphics.pose().translate(x, y, 0); graphics.pose().scale(scale, scale, 1);
        graphics.renderItem(icon, 0, 0); graphics.pose().popPose();
    }
    private Technology cardAt(double mouseX, double mouseY, int left, int top, int width) {
        var box = listBox(left, top, width);
        if (!box.contains(mouseX, mouseY) || mouseX >= box.x() + box.width() - 7) return null;
        int columnWidth = (box.width() - 7) / COLUMNS;
        int dx = (int) mouseX - box.x(), column = dx / columnWidth;
        if (column >= COLUMNS || dx % columnWidth >= columnWidth - 4) return null;
        int y = (int) mouseY - box.y() + listScroll, index = y / CARD_STEP * COLUMNS + column;
        var cards = cards(); return index >= 0 && index < cards.size() && y % CARD_STEP < CARD_HEIGHT ? cards.get(index) : null;
    }
    private Technology nodeAt(double mouseX, double mouseY, int left, int top, int width) {
        var box = chartBox(left, top, width); if (!box.contains(mouseX, mouseY)) return null;
        double x = (mouseX - box.x() - panX) / zoom, y = (mouseY - box.y() - panY) / zoom;
        for (Node node : graph().nodes()) if (x >= node.x() && x < node.x() + node.width() && y >= node.y() && y < node.y() + node.height()) return node.tech();
        return null;
    }
    private ItemStack unlockAt(int left, int top, int width, double mouseX, double mouseY) {
        var tech = selected();
        Component label = Component.translatable(tech.recipes.isEmpty() ? "research.conveyance.upgrades" : "research.conveyance.unlocks");
        int x = left + sidebar(width) + 24 + game.font.width(label), y = top + 90;
        for (int index = 0; index < tech.unlocks().size(); index++) if (new Box(x + index * 20, y, 16, 16).contains(mouseX, mouseY)) return tech.unlocks().get(index);
        return ItemStack.EMPTY;
    }
    private void unlockTooltip(GuiGraphics graphics, ItemStack item, int mouseX, int mouseY) {
        var lines = new ArrayList<Component>(net.minecraft.client.gui.screens.Screen.getTooltipFromItem(game, item));
        String extra = "tooltip.conveyance." + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem()).getPath();
        if (net.minecraft.client.resources.language.I18n.exists(extra) && !net.minecraft.client.resources.language.I18n.get(extra).contains("%s")) {
            Component description = Component.translatable(extra);
            if (lines.stream().noneMatch(line -> line.getString().equals(description.getString()))) lines.add(description.copy().withStyle(ChatFormatting.GRAY));
        }
        if (selected().recipes.isEmpty()) lines.add(selected().description().copy().withStyle(ChatFormatting.GREEN));
        IconTooltips.render(graphics, IconTooltips.itemLines(item, lines), mouseX, mouseY);
    }
    private void tooltip(GuiGraphics graphics, Technology tech, int mouseX, int mouseY) {
        var lines = new ArrayList<IconTooltips.Row>();
        lines.add(IconTooltips.item(tech.icon(), tech.title()));
        if (!tech.triggered()) lines.add(IconTooltips.text(tech.description()));
        lines.add(IconTooltips.text(Component.translatable("research.conveyance.state." + status(tech)).withStyle(ChatFormatting.GRAY)));
        lines.add(IconTooltips.text(Component.translatable(tech.triggered() ? "research.conveyance.production_progress" : "research.conveyance.science_cost",
                ResearchState.progress(game.level, tech.id), tech.target())));
        if (tech.triggered()) {
            if (tech.trigger.equals("crude_oil")) lines.add(IconTooltips.item(tech.triggerIcon(), tech.scienceCost().copy().withStyle(ChatFormatting.YELLOW)));
            else lines.add(IconTooltips.item(Component.translatable("research.conveyance.craft_trigger", tech.target(), Component.empty()).withStyle(ChatFormatting.YELLOW),
                    tech.triggerIcon(), tech.triggerIcon().getHoverName().copy().withStyle(ChatFormatting.YELLOW)));
        }
        else {
            for (int pack = 0; pack < Technology.SCIENCE_SLOTS; pack++) if (tech.needsPack(pack)) {
                ItemStack science = new ItemStack(Technology.packItem(pack));
                lines.add(IconTooltips.item(Component.literal(tech.packs + " × ").withStyle(ChatFormatting.YELLOW), science, science.getHoverName().copy().withStyle(ChatFormatting.YELLOW)));
            }
            lines.add(IconTooltips.item(new ItemStack(net.minecraft.world.item.Items.CLOCK), Component.translatable("research.conveyance.unit_time", tech.unitTicks / 20).withStyle(ChatFormatting.GRAY)));
        }
        for (Technology parent : tech.prerequisites()) lines.add(IconTooltips.item(Component.translatable("research.conveyance.requires", Component.empty()).withStyle(ChatFormatting.GRAY),
                parent.icon(), parent.title().copy().withStyle(parent.complete(mask()) ? ChatFormatting.GREEN : ChatFormatting.RED)));
        for (ItemStack stack : tech.unlocks()) lines.add(IconTooltips.item(Component.translatable(tech.recipes.isEmpty() ? "research.conveyance.upgrade_item" : "research.conveyance.unlock_item", Component.empty()), stack, stack.getHoverName()));
        IconTooltips.render(graphics, lines, mouseX, mouseY);
    }
    public boolean click(double mouseX, double mouseY, int button, int left, int top, int width) {
        if (button == 1 && chartBox(left, top, width).contains(mouseX, mouseY)) { dragging = true; return true; }
        if (button != 0) return false;
        var list = listBox(left, top, width);
        if (list.contains(mouseX, mouseY)) {
            if (mouseX >= list.x() + list.width() - 7 && maxScroll() > 0) {
                listScroll = Mth.clamp((int) ((mouseY - list.y()) / list.height() * maxScroll()), 0, maxScroll()); draggingList = true;
            } else { var tech = cardAt(mouseX, mouseY, left, top, width); if (tech != null) select(tech.id); }
            return true;
        }
        if (!chartBox(left, top, width).contains(mouseX, mouseY)) return false;
        var tech = nodeAt(mouseX, mouseY, left, top, width);
        if (tech != null) select(tech.id); else dragging = true;
        return true;
    }
    public boolean drag(double deltaX, double deltaY) {
        if (draggingList) {
            listScroll = Mth.clamp(listScroll + (int) (deltaY * cardRows() * CARD_STEP / listBox(0, 0, fittedWidth).height()), 0, maxScroll()); return true;
        }
        if (!dragging) return false; panX += (float) deltaX; panY += (float) deltaY; return true;
    }
    public boolean release() { boolean previous = dragging || draggingList; dragging = draggingList = false; return previous; }
    public boolean scroll(double mouseX, double mouseY, double amount, int left, int top, int width) {
        if (listBox(left, top, width).contains(mouseX, mouseY)) { listScroll = Mth.clamp(listScroll - (int) (amount * 40), 0, maxScroll()); return true; }
        var box = chartBox(left, top, width); if (!box.contains(mouseX, mouseY)) return false;
        float next = Mth.clamp(zoom * (amount > 0 ? 1.15F : 1 / 1.15F), .25F, 2F);
        float x = (float) (mouseX - box.x() - panX) / zoom, y = (float) (mouseY - box.y() - panY) / zoom;
        panX = (float) (mouseX - box.x()) - x * next; panY = (float) (mouseY - box.y()) - y * next; zoom = next; return true;
    }
}
