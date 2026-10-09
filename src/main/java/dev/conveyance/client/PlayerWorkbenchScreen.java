package dev.conveyance.client;

import dev.conveyance.factory.AssemblyRecipes;
import dev.conveyance.factory.FactoryContent;
import dev.conveyance.factory.FactoryCraftingRecipe;
import dev.conveyance.player.HandCrafting;
import dev.conveyance.player.RecipeCosts;
import dev.conveyance.player.InventoryAccess;
import dev.conveyance.player.InventoryNetwork;
import dev.conveyance.player.PlayerWorkbenchMenu;
import dev.conveyance.player.ResearchState;
import dev.conveyance.player.Technology;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;

public final class PlayerWorkbenchScreen extends AbstractContainerScreen<PlayerWorkbenchMenu> {
    private static final int RIGHT = 184, ROWS = 4, CELL = 26, GRID_Y = 73, CONTENT = PlayerWorkbenchMenu.CONTENT_Y;
    private static final List<String> CATEGORIES = List.of("all", "intermediates", "logistics", "production", "power", "science");
    private final List<Button> recipeButtons = new ArrayList<>();
    private final List<Button> categoryButtons = new ArrayList<>();
    private final Map<ResourceLocation, Integer> amounts = new HashMap<>();
    private final Map<ResourceLocation, List<ItemStack>> rawCosts = new HashMap<>();
    private List<CraftingRecipe> availableRecipes = List.of();
    private List<ItemStack> cachedItems = List.of();
    private int cachedMask = -1;
    private String category = "all";
    private EditBox search;
    private Button inventoryTab, researchTab, mapTab, startResearch, fitGraph;
    private final ResearchGraphView graph = new ResearchGraphView();
    private final WorldMapView map = new WorldMapView();
    private boolean researchView, mapView;
    private int scroll, columns;
    public PlayerWorkbenchScreen(PlayerWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 396; imageHeight = 232;
    }
    @Override protected void init() {
        imageWidth = Math.min(396, Math.max(304, width - 12));
        super.init();
        recipeButtons.clear();
        categoryButtons.clear();
        int panelWidth = imageWidth - RIGHT;
        inventoryTab = addRenderableWidget(Button.builder(Component.translatable("menu.conveyance.inventory_tab"), button -> tab(0)).bounds(leftPos, topPos, 134, 20).build());
        researchTab = addRenderableWidget(Button.builder(Component.translatable("menu.conveyance.research_tab"), button -> tab(1)).bounds(leftPos + 138, topPos, 74, 20).build());
        mapTab = addRenderableWidget(Button.builder(Component.translatable("menu.conveyance.map_tab"), button -> tab(2)).bounds(leftPos + 216, topPos, 44, 20).build());
        fitGraph = addRenderableWidget(Button.builder(Component.translatable("menu.conveyance.tree_fit"), button -> { if (mapView) map.center(); else graph.fit(imageWidth); }).bounds(leftPos + imageWidth - 40, topPos, 40, 20).build());
        graph.fit(imageWidth);
        int categoryWidth = (panelWidth - 16) / CATEGORIES.size();
        for (int index = 0; index < CATEGORIES.size(); index++) {
            String selected = CATEGORIES.get(index);
            Button button = addRenderableWidget(Button.builder(Component.empty(), clicked -> { category = selected; scroll = 0; refresh(); })
                    .bounds(leftPos + RIGHT + 8 + index * categoryWidth, topPos + CONTENT + 25, categoryWidth - 1, 20).build());
            button.setTooltip(Tooltip.create(Component.translatable("menu.conveyance.category." + selected)));
            categoryButtons.add(button);
        }
        search = new EditBox(font, leftPos + RIGHT + 8, topPos + CONTENT + 49, panelWidth - 16, 16, Component.translatable("menu.conveyance.search"));
        search.setHint(Component.translatable("menu.conveyance.search"));
        search.setResponder(value -> scroll = 0);
        addRenderableWidget(search);
        columns = Math.max(1, (panelWidth - 16) / CELL);
        for (int cell = 0; cell < ROWS * columns; cell++) {
            final int index = cell;
            recipeButtons.add(addRenderableWidget(Button.builder(Component.empty(), button -> {
                var recipes = filtered();
                int selected = scroll * columns + index;
                if (selected < recipes.size()) InventoryNetwork.requestCraft(recipes.get(selected).getId(), hasShiftDown());
            }).bounds(leftPos + RIGHT + 8 + cell % columns * CELL, topPos + CONTENT + GRID_Y + cell / columns * CELL, 24, 24).build()));
        }
        startResearch = addRenderableWidget(Button.builder(Component.translatable("research.conveyance.start"), button -> research(graph.inspected()))
                .bounds(leftPos + imageWidth - 108, topPos + CONTENT + 184, 100, 18).build());
        refresh();
    }
    private void tab(int selected) {
        if (!menu.getCarried().isEmpty()) return;
        researchView = selected == 1;
        mapView = selected == 2;
        search.setFocused(false);
        graph.release();
        map.release();
        if (researchView) graph.open();
        if (mapView) { map.open(); map.tick(imageWidth); }
        refresh();
    }
    private void research(int id) {
        Technology tech = Technology.byId(id);
        if (minecraft.gameMode != null && tech != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, tech.menuId());
    }
    private List<CraftingRecipe> filtered() {
        if (minecraft.level == null) return List.of();
        String query = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT);
        return availableRecipes.stream()
                .filter(recipe -> category.equals("all") || recipe instanceof FactoryCraftingRecipe factory && factory.tab().equals(category))
                .filter(recipe -> recipe.getResultItem(minecraft.level.registryAccess()).getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)).toList();
    }
    private List<ItemStack> items() { return InventoryAccess.snapshot(minecraft.player, menu.extra()); }
    private void updateAmounts() {
        var current = items();
        int mask = menu.researchMask();
        boolean changed = mask != cachedMask || current.size() != cachedItems.size();
        if (!changed) for (int i = 0; i < current.size(); i++) if (!ItemStack.matches(current.get(i), cachedItems.get(i))) { changed = true; break; }
        if (!changed) return;
        boolean recipesChanged = mask != cachedMask;
        cachedMask = mask; cachedItems = current;
        availableRecipes = HandCrafting.recipes(minecraft.level, mask);
        if (recipesChanged) {
            rawCosts.clear();
            var costs = new RecipeCosts(minecraft.level);
            for (CraftingRecipe recipe : availableRecipes) rawCosts.put(recipe.getId(), costs.raw(recipe));
        }
        amounts.clear();
        var planner = new HandCrafting.Planner(minecraft.level, mask);
        for (CraftingRecipe recipe : availableRecipes) amounts.put(recipe.getId(), planner.craftable(current, recipe));
    }
    private void refresh() {
        boolean inventoryView = !researchView && !mapView;
        if (inventoryView) updateAmounts();
        menu.showStorage(inventoryView);
        inventoryTab.active = !inventoryView && menu.getCarried().isEmpty();
        researchTab.active = !researchView && menu.getCarried().isEmpty();
        mapTab.active = !mapView && menu.getCarried().isEmpty();
        search.setVisible(inventoryView);
        for (int i = 0; i < categoryButtons.size(); i++) {
            categoryButtons.get(i).visible = inventoryView;
            categoryButtons.get(i).active = !CATEGORIES.get(i).equals(category);
        }
        fitGraph.visible = researchView || mapView;
        fitGraph.setMessage(Component.translatable(mapView ? "map.conveyance.center" : "menu.conveyance.tree_fit"));
        startResearch.visible = researchView;
        Technology tech = Technology.byId(graph.inspected());
        startResearch.active = tech != null && tech.available(menu.researchMask()) && ResearchState.selected(minecraft.level) != tech.id;
        String action = tech == null || tech.complete(menu.researchMask()) ? "completed" : ResearchState.selected(minecraft.level) == tech.id ? "active" : !tech.available(menu.researchMask()) ? "locked" : "start";
        startResearch.setMessage(Component.translatable("research.conveyance." + action));
        var recipes = filtered();
        scroll = Math.min(scroll, maxScroll(recipes.size()));
        for (int cell = 0; cell < recipeButtons.size(); cell++) {
            Button button = recipeButtons.get(cell);
            int selected = scroll * columns + cell;
            button.visible = inventoryView && selected < recipes.size();
            button.active = button.visible && (minecraft.player.isCreative() || amounts.getOrDefault(recipes.get(selected).getId(), 0) > 0);
        }
    }
    private int maxScroll(int count) { return Math.max(0, (count + columns - 1) / columns - ROWS); }
    @Override protected void containerTick() { super.containerTick(); search.tick(); refresh(); if (mapView) map.tick(imageWidth); }
    @Override public void removed() { map.close(); super.removed(); }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 21, 0xFF202A35);
        int tabX = mapView ? 216 : researchView ? 138 : 0, tabWidth = mapView ? 44 : researchView ? 74 : 134;
        graphics.fill(leftPos + tabX, topPos + 19, leftPos + tabX + tabWidth, topPos + 22, 0xFF88B9A2);
        if (researchView || mapView) { panel(graphics, leftPos, topPos + CONTENT, imageWidth, 208); return; }
        panel(graphics, leftPos, topPos + CONTENT, 176, 208);
        panel(graphics, leftPos + RIGHT, topPos + CONTENT, imageWidth - RIGHT, 208);
        for (var slot : menu.slots) {
            int x = leftPos + slot.x, y = topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF596674);
            graphics.fill(x, y, x + 16, y + 16, 0xFF111820);
        }
    }
    private static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF607080);
        graphics.fill(x, y, x + width, y + height, 0xFF202A35);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 20, 0xFF303D4A);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (researchView || mapView) return;
        graphics.drawString(font, Component.translatable("menu.conveyance.inventory"), 8, CONTENT + 7, 0xFFE7EEF6, false);
        graphics.drawString(font, Component.translatable("menu.conveyance.handcrafting"), RIGHT + 8, CONTENT + 7, 0xFFE7EEF6, false);
        graphics.drawString(font, Component.translatable("menu.conveyance.hotbar"), 8, CONTENT + 155, 0xFF9CABB8, false);
        graphics.drawString(font, Component.translatable("menu.conveyance.storage_size"), 8, CONTENT + 193, 0xFF9CABB8, false);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        refresh();
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (mapView) { map.render(graphics, leftPos, topPos + CONTENT, imageWidth, mouseX, mouseY); return; }
        if (researchView) {
            graph.render(graphics, leftPos, topPos + CONTENT, imageWidth, mouseX, mouseY);
            startResearch.render(graphics, mouseX, mouseY, partialTick);
            return;
        }
        var recipes = filtered();
        for (int i = 0; i < categoryButtons.size(); i++) {
            Button button = categoryButtons.get(i);
            boolean selected = CATEGORIES.get(i).equals(category);
            graphics.fill(button.getX(), button.getY(), button.getX() + button.getWidth(), button.getY() + 20, selected ? 0xFF618A78 : 0xFF354353);
            graphics.renderItem(categoryIcon(CATEGORIES.get(i)), button.getX() + (button.getWidth() - 16) / 2, button.getY() + 2);
        }
        for (int cell = 0; cell < recipeButtons.size() && scroll * columns + cell < recipes.size(); cell++) {
            CraftingRecipe recipe = recipes.get(scroll * columns + cell);
            ItemStack output = recipe.getResultItem(minecraft.level.registryAccess());
            Button button = recipeButtons.get(cell);
            int x = button.getX(), y = button.getY();
            graphics.fill(x + 1, y + 1, x + 23, y + 23, button.isHoveredOrFocused() ? 0xFF405061 : 0xFF293645);
            graphics.renderItem(output, x + 4, y + 2);
            // Draw above the item renderer so missing-material recipes are visibly dimmed.
            if (!button.active) {
                graphics.pose().pushPose(); graphics.pose().translate(0, 0, 250);
                graphics.fill(x + 1, y + 1, x + 23, y + 23, 0xA510141B);
                graphics.pose().popPose();
            }
            String amount = minecraft.player.isCreative() ? "∞" : Integer.toString(amounts.getOrDefault(recipe.getId(), 0) * output.getCount());
            graphics.pose().pushPose();
            graphics.pose().translate(x + 22, y + 17, 300);
            graphics.pose().scale(0.65F, 0.65F, 1);
            graphics.drawString(font, amount, -font.width(amount), 0, button.active ? 0xFF8AD8A0 : 0xFF94A0AD, true);
            graphics.pose().popPose();
        }
        renderTooltip(graphics, mouseX, mouseY);
        for (int cell = 0; cell < recipeButtons.size() && scroll * columns + cell < recipes.size(); cell++) {
            if (over(recipeButtons.get(cell), mouseX, mouseY)) {
                CraftingRecipe recipe = recipes.get(scroll * columns + cell);
                ingredientTooltip(graphics, recipe, mouseX, mouseY);
            }
        }
    }
    private ItemStack categoryIcon(String tab) { return new ItemStack(switch (tab) {
        case "intermediates" -> FactoryContent.GEAR_ITEM.get();
        case "logistics" -> FactoryContent.CONVEYOR_ITEM.get();
        case "production" -> FactoryContent.BURNER_MINER_ITEM.get();
        case "power" -> FactoryContent.POWER_POLE_ITEM.get();
        case "science" -> FactoryContent.RED_SCIENCE_ITEM.get();
        default -> Items.PAPER;
    }); }
    private void ingredientTooltip(GuiGraphics graphics, CraftingRecipe recipe, int mouseX, int mouseY) {
        var requirements = AssemblyRecipes.requirements(recipe);
        ItemStack output = recipe.getResultItem(minecraft.level.registryAccess());
        Component title = Component.literal(output.getCount() > 1 ? output.getCount() + " × " : "").append(output.getHoverName());
        var raw = rawCosts.getOrDefault(recipe.getId(), List.of());
        Component rawTitle = Component.translatable("menu.conveyance.total_raw");
        int boxWidth = Math.min(width - 8, Math.max(160, Math.max(Math.max(font.width(title), font.width(rawTitle)) + 16, Math.max(requirements.size(), raw.size()) * 30 + 16)));
        int iconColumns = Math.max(1, (boxWidth - 16) / 30);
        int rawTitleY = 35 + 24 * Math.max(1, (requirements.size() + iconColumns - 1) / iconColumns) + 6;
        int rawIconsY = rawTitleY + 13;
        int boxHeight = rawIconsY + 24 * Math.max(1, (raw.size() + iconColumns - 1) / iconColumns) + 8;
        int x = Math.max(4, Math.min(mouseX + 12, width - boxWidth - 4));
        int y = Math.max(4, Math.min(mouseY + 12, height - boxHeight - 4));
        graphics.pose().pushPose(); graphics.pose().translate(0, 0, 450);
        graphics.fill(x - 1, y - 1, x + boxWidth + 1, y + boxHeight + 1, 0xFF718292);
        graphics.fill(x, y, x + boxWidth, y + boxHeight, 0xFA17212C);
        graphics.drawString(font, title, x + 8, y + 7, 0xFFE7EEF6, false);
        graphics.drawString(font, Component.translatable("menu.conveyance.ingredients"), x + 8, y + 23, 0xFFA9BBC9, false);
        boolean possible = minecraft.player.isCreative() || amounts.getOrDefault(recipe.getId(), 0) > 0;
        for (int i = 0; i < requirements.size(); i++) {
            var requirement = requirements.get(i);
            var choices = requirement.ingredient().getItems();
            if (choices.length == 0) continue;
            int iconX = x + 8 + i % iconColumns * 30, iconY = y + 35 + i / iconColumns * 24;
            boolean enough = HandCrafting.count(cachedItems, requirement.ingredient()) >= requirement.count();
            costIcon(graphics, choices[0], requirement.count(), iconX, iconY, enough ? 0xFF8AD8A0 : possible ? 0xFFE1BC76 : 0xFFEB8585);
        }
        graphics.drawString(font, rawTitle, x + 8, y + rawTitleY, 0xFFA9BBC9, false);
        for (int i = 0; i < raw.size(); i++) costIcon(graphics, raw.get(i), raw.get(i).getCount(), x + 8 + i % iconColumns * 30,
                y + rawIconsY + i / iconColumns * 24, 0xFFE7EEF6);
        graphics.pose().popPose();
    }
    private void costIcon(GuiGraphics graphics, ItemStack item, int count, int x, int y, int colour) {
        graphics.renderItem(item, x, y);
        graphics.pose().pushPose(); graphics.pose().translate(0, 0, 200);
        String text = Integer.toString(count);
        graphics.drawString(font, text, x + 20 - font.width(text), y + 11, colour, true);
        graphics.pose().popPose();
    }
    private static boolean over(Button button, double x, double y) { return button.visible && x >= button.getX() && x < button.getX() + button.getWidth() && y >= button.getY() && y < button.getY() + button.getHeight(); }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (mapView && map.click(x, y, button, leftPos, topPos + CONTENT, imageWidth)) return true;
        if (researchView && graph.click(x, y, button, leftPos, topPos + CONTENT, imageWidth)) { refresh(); return true; }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double deltaX, double deltaY) {
        return mapView && map.drag(deltaX, deltaY) || researchView && graph.drag(deltaX, deltaY) || super.mouseDragged(x, y, button, deltaX, deltaY);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        return mapView && map.release() || researchView && graph.release() || super.mouseReleased(x, y, button);
    }
    @Override public boolean mouseScrolled(double x, double y, double amount) {
        if (mapView) return map.scroll(x, y, amount, leftPos, topPos + CONTENT, imageWidth);
        if (researchView) return graph.scroll(x, y, amount, leftPos, topPos + CONTENT, imageWidth);
        if (x >= leftPos + RIGHT && y >= topPos + CONTENT + GRID_Y && y < topPos + CONTENT + GRID_Y + ROWS * CELL) {
            scroll = Math.max(0, Math.min(maxScroll(filtered().size()), scroll - (int) Math.signum(amount)));
            refresh();
            return true;
        }
        return super.mouseScrolled(x, y, amount);
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (!researchView && !mapView && search.isFocused() && key != 256) return search.keyPressed(key, scan, modifiers);
        return super.keyPressed(key, scan, modifiers);
    }
}
