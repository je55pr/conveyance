package dev.conveyance.client;

import dev.conveyance.factory.FactoryCraftingRecipe;
import dev.conveyance.player.HandCrafting;
import dev.conveyance.player.HandCraftingQueue;
import dev.conveyance.factory.FactoryTooltips;
import dev.conveyance.player.RecipeCosts;
import dev.conveyance.player.InventoryAccess;
import dev.conveyance.player.InventoryNetwork;
import dev.conveyance.player.LinkedHotbar;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;
import dev.conveyance.player.PlayerWorkbenchMenu;
import dev.conveyance.player.PlayerStorageLayout;
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
import net.minecraft.world.item.crafting.CraftingRecipe;

public final class PlayerWorkbenchScreen extends AbstractContainerScreen<PlayerWorkbenchMenu> {
    private static final int RIGHT = PlayerStorageLayout.MACHINE_X, CELL = 26, ROW_HEIGHT = CELL, GRID_Y = 73, CONTENT = PlayerWorkbenchMenu.CONTENT_Y;
    private static final List<String> CATEGORIES = RecipePresentation.CATEGORIES;
    private final List<Button> recipeButtons = new ArrayList<>();
    private final List<Button> categoryButtons = new ArrayList<>();
    private final List<Button> hotbarLinks = new ArrayList<>();
    private final Map<ResourceLocation, Integer> amounts = new HashMap<>();
    private final Map<ResourceLocation, Integer> craftingTimes = new HashMap<>();
    private final Map<ResourceLocation, List<ItemStack>> rawCosts = new HashMap<>();
    private List<CraftingRecipe> availableRecipes = List.of();
    private List<ItemStack> cachedItems = List.of();
    private long cachedMask = -1;
    private boolean cachedCreative;
    private String category;
    private EditBox search;
    private Button inventoryTab, researchTab, mapTab, startResearch, fitGraph, pollutionToggle, revealMap;
    private final ResearchGraphView graph = new ResearchGraphView();
    private final WorldMapView map = new WorldMapView();
    private boolean researchView, mapView;
    private int scroll, columns, rows;
    private int queueScroll;
    private boolean linkPressed, resizePending;
    private int appliedOpeningTab = -1;
    public PlayerWorkbenchScreen(PlayerWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        category = CATEGORIES.contains(WorkbenchPreferences.category()) ? WorkbenchPreferences.category() : "production";
        int openingTab = WorkbenchNavigation.takeOpeningTab();
        researchView = openingTab == 1;
        mapView = openingTab == 2;
        imageWidth = PlayerStorageLayout.MIN_WIDTH; imageHeight = PlayerStorageLayout.MIN_HEIGHT;
    }
    @Override protected void init() {
        imageWidth = researchView || mapView ? Math.max(FactoryWindowStyle.COMPACT_WIDTH, width - 24) : FactoryWindowStyle.COMPACT_WIDTH;
        imageHeight = researchView || mapView ? Math.max(FactoryWindowStyle.COMPACT_HEIGHT, height - 24) : FactoryWindowStyle.COMPACT_HEIGHT;
        super.init();
        recipeButtons.clear();
        categoryButtons.clear();
        hotbarLinks.clear();
        for (int slot = 0; slot < LinkedHotbar.SIZE; slot++) {
            final int link = slot;
            hotbarLinks.add(addRenderableWidget(Button.builder(Component.empty(), button -> {
                linkPressed = true;
                var carried = menu.getCarried();
                if (link > 0 && !carried.isEmpty()) InventoryNetwork.requestLink(menu.containerId, link, BuiltInRegistries.ITEM.getKey(carried.getItem()));
                else HotbarControls.select(link);
            }).bounds(leftPos + PlayerStorageLayout.GRID_X + slot * PlayerStorageLayout.CELL, topPos + PlayerStorageLayout.HOTBAR_Y, PlayerStorageLayout.CELL, PlayerStorageLayout.CELL).build()));
        }
        int panelWidth = imageWidth - RIGHT;
        inventoryTab = addRenderableWidget(Button.builder(Component.translatable("menu.conveyance.inventory_tab"), button -> tab(0)).bounds(leftPos, topPos, 134, 20).build());
        researchTab = addRenderableWidget(Button.builder(Component.translatable("menu.conveyance.research_tab"), button -> tab(1)).bounds(leftPos + 138, topPos, 74, 20).build());
        mapTab = addRenderableWidget(Button.builder(Component.translatable("menu.conveyance.map_tab"), button -> tab(2)).bounds(leftPos + 216, topPos, 44, 20).build());
        fitGraph = addRenderableWidget(Button.builder(Component.translatable("menu.conveyance.tree_fit"), button -> { if (mapView) map.center(); else graph.fit(imageWidth); }).bounds(leftPos + imageWidth - 40, topPos, 40, 20).build());
        if (researchView) graph.resize(imageWidth, bodyHeight());
        pollutionToggle = addRenderableWidget(Button.builder(map.pollutionLabel(), button -> {
            map.togglePollution(); button.setMessage(map.pollutionLabel());
        }).bounds(leftPos + 48, topPos + CONTENT + 3, 88, 16).build());
        pollutionToggle.setTooltip(Tooltip.create(Component.translatable("map.conveyance.pollution_toggle")));
        revealMap = addRenderableWidget(Button.builder(Component.translatable("map.conveyance.reveal_area"), button -> {
            if (minecraft.player != null && minecraft.player.isCreative()) {
                dev.conveyance.map.MapNetwork.reveal();
                map.showArea(minecraft.player.getX(), minecraft.player.getZ(), dev.conveyance.map.CreativeMapReveal.SIDE);
                refresh();
            }
        }).bounds(leftPos + 144, topPos + CONTENT + 3, 116, 16).build());
        revealMap.setTooltip(Tooltip.create(Component.translatable("map.conveyance.reveal_tooltip")));
        int categoryWidth = (panelWidth - 16) / CATEGORIES.size();
        for (int index = 0; index < CATEGORIES.size(); index++) {
            String selected = CATEGORIES.get(index);
            Button button = addRenderableWidget(Button.builder(Component.empty(), clicked -> { category = selected; scroll = 0; remember(); refresh(); })
                    .bounds(leftPos + RIGHT + 8 + index * categoryWidth, topPos + CONTENT + 25, categoryWidth - 1, 20).build());
            button.setTooltip(Tooltip.create(Component.translatable("menu.conveyance.category." + selected)));
            categoryButtons.add(button);
        }
        String query = search == null ? "" : search.getValue();
        search = new EditBox(font, leftPos + RIGHT + 8, topPos + CONTENT + 49, panelWidth - 16, 16, Component.translatable("menu.conveyance.search"));
        search.setHint(Component.translatable("menu.conveyance.search"));
        search.setValue(query);
        search.setResponder(value -> scroll = 0);
        addRenderableWidget(search);
        columns = Math.max(1, (panelWidth - 16) / CELL);
        rows = Math.max(1, (bodyHeight() - GRID_Y - 12) / ROW_HEIGHT);
        for (int cell = 0; cell < rows * columns; cell++) {
            final int index = cell;
            recipeButtons.add(addRenderableWidget(Button.builder(Component.empty(), button -> {
                CraftingRecipe recipe = recipeAt(index, grouped());
                if (recipe != null) InventoryNetwork.requestCraft(recipe.getId(), hasShiftDown());
            }).bounds(leftPos + RIGHT + 8 + cell % columns * CELL, topPos + CONTENT + GRID_Y + cell / columns * ROW_HEIGHT, 24, 24).build()));
        }
        startResearch = addRenderableWidget(Button.builder(Component.translatable("research.conveyance.start"), button -> research(graph.inspected()))
                .bounds(leftPos + imageWidth - 108, topPos + imageHeight - 24, 100, 18).build());
        if (researchView) graph.open();
        if (mapView) { map.resize(bodyHeight()); map.open(); map.tick(imageWidth); }
        refresh();
    }
    private int bodyHeight() { return imageHeight - CONTENT; }
    private void remember() { WorkbenchPreferences.remember(category); }
    public int selectedTab() { return mapView ? 2 : researchView ? 1 : 0; }
    public void openTab(int selected) { tab(selected); }
    private void tab(int selected) {
        if (!menu.getCarried().isEmpty()) return;
        boolean wasLarge = researchView || mapView;
        researchView = selected == 1;
        mapView = selected == 2;
        remember();
        search.setFocused(false);
        graph.release();
        map.release();
        if (wasLarge != (researchView || mapView)) { rebuildWidgets(); return; }
        if (researchView) { graph.resize(imageWidth, bodyHeight()); graph.open(); }
        if (mapView) { map.resize(bodyHeight()); map.open(); map.tick(imageWidth); }
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
                .filter(recipe -> !query.isBlank() || recipe instanceof FactoryCraftingRecipe factory && factory.tab().equals(category))
                .filter(recipe -> recipe.getResultItem(minecraft.level.registryAccess()).getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)).toList();
    }
    private List<ItemStack> items() { return InventoryAccess.snapshot(minecraft.player, menu.extra()); }
    private List<RecipePresentation.Row> grouped() {
        return RecipePresentation.grouped(filtered(), minecraft.level, columns);
    }
    private CraftingRecipe recipeAt(int cell, List<RecipePresentation.Row> groups) {
        int row = scroll + cell / columns, column = cell % columns;
        return row < groups.size() && column < groups.get(row).recipes().size() ? groups.get(row).recipes().get(column) : null;
    }
    private void updateAmounts() {
        var current = items();
        long mask = menu.researchMask();
        boolean creative = minecraft.player.isCreative();
        boolean changed = mask != cachedMask || creative != cachedCreative || current.size() != cachedItems.size();
        if (!changed) for (int i = 0; i < current.size(); i++) if (!ItemStack.matches(current.get(i), cachedItems.get(i))) { changed = true; break; }
        if (!changed) return;
        boolean recipesChanged = mask != cachedMask || creative != cachedCreative;
        cachedMask = mask; cachedCreative = creative; cachedItems = current;
        availableRecipes = HandCrafting.recipes(minecraft.level, mask, creative);
        if (recipesChanged) {
            rawCosts.clear();
            var costs = new RecipeCosts(minecraft.level);
            for (CraftingRecipe recipe : availableRecipes) rawCosts.put(recipe.getId(), costs.raw(recipe));
        }
        amounts.clear();
        var planner = new HandCrafting.Planner(minecraft.level, mask);
        craftingTimes.clear();
        for (CraftingRecipe recipe : availableRecipes) {
            amounts.put(recipe.getId(), planner.craftable(current, recipe));
            craftingTimes.put(recipe.getId(), planner.craftingTime(current, recipe));
        }
    }
    private void refresh() {
        if (!menu.getCarried().isEmpty() && (researchView || mapView)) { researchView = false; mapView = false; resizePending = true; }
        boolean inventoryView = !researchView && !mapView;
        if (inventoryView) updateAmounts();
        menu.showStorage(inventoryView);
        inventoryTab.active = !inventoryView && menu.getCarried().isEmpty();
        researchTab.active = !researchView && menu.getCarried().isEmpty();
        mapTab.active = !mapView && menu.getCarried().isEmpty();
        search.setVisible(inventoryView);
        hotbarLinks.forEach(button -> button.visible = inventoryView);
        for (int i = 0; i < categoryButtons.size(); i++) {
            categoryButtons.get(i).visible = inventoryView;
            categoryButtons.get(i).active = !CATEGORIES.get(i).equals(category);
        }
        fitGraph.visible = researchView || mapView;
        pollutionToggle.visible = mapView;
        revealMap.visible = mapView && minecraft.player.isCreative();
        revealMap.active = !dev.conveyance.map.WorldMapData.revealing();
        revealMap.setMessage(Component.translatable(dev.conveyance.map.WorldMapData.revealing() ? "map.conveyance.reveal_progress" : "map.conveyance.reveal_area", dev.conveyance.map.WorldMapData.revealPercent()));
        fitGraph.setMessage(Component.translatable(mapView ? "map.conveyance.center" : "menu.conveyance.tree_fit"));
        startResearch.visible = researchView;
        Technology tech = Technology.byId(graph.inspected());
        startResearch.active = tech != null && !tech.triggered() && tech.available(menu.researchMask()) && ResearchState.selected(minecraft.level) != tech.id;
        String action = tech != null && tech.triggered() && !tech.complete(menu.researchMask()) ? "trigger_button" : tech == null || tech.complete(menu.researchMask()) ? "completed" : ResearchState.selected(minecraft.level) == tech.id ? "active" : !tech.available(menu.researchMask()) ? "locked" : "start";
        startResearch.setMessage(Component.translatable("research.conveyance." + action));
        var groups = grouped();
        scroll = Math.min(scroll, maxScroll(groups.size()));
        for (int cell = 0; cell < recipeButtons.size(); cell++) {
            Button button = recipeButtons.get(cell);
            CraftingRecipe recipe = recipeAt(cell, groups);
            button.visible = inventoryView && recipe != null;
            button.active = button.visible && (minecraft.player.isCreative() || amounts.getOrDefault(recipe.getId(), 0) > 0);
        }
    }
    private int maxScroll(int count) { return Math.max(0, count - rows); }
    @Override protected void containerTick() {
        super.containerTick(); search.tick();
        if (resizePending) { resizePending = false; rebuildWidgets(); }
        int openingTab = menu.openingTab();
        if (openingTab >= 0 && openingTab != appliedOpeningTab && menu.getCarried().isEmpty()) {
            appliedOpeningTab = openingTab;
            if (openingTab != selectedTab()) tab(openingTab);
        }
        refresh(); if (mapView) map.tick(imageWidth);
    }
    @Override public void removed() { map.close(); super.removed(); }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 21, FactoryWindowStyle.BACKGROUND);
        int tabX = mapView ? 216 : researchView ? 138 : 0, tabWidth = mapView ? 44 : researchView ? 74 : 134;
        graphics.fill(leftPos + tabX, topPos + 19, leftPos + tabX + tabWidth, topPos + 22, 0xFF88B9A2);
        if (researchView || mapView) { panel(graphics, leftPos, topPos + CONTENT, imageWidth, bodyHeight()); return; }
        panel(graphics, leftPos, topPos + CONTENT, PlayerStorageLayout.PANEL_WIDTH, bodyHeight());
        panel(graphics, leftPos + RIGHT, topPos + CONTENT, imageWidth - RIGHT, bodyHeight());
        for (var slot : menu.slots) {
            int x = leftPos + slot.x, y = topPos + slot.y;
            FactoryWindowStyle.slot(graphics, x, y);
        }
    }
    private static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        FactoryWindowStyle.panel(graphics, x, y, width, height);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (researchView || mapView) return;
        graphics.drawString(font, Component.translatable("menu.conveyance.inventory"), 8, CONTENT + 7, FactoryWindowStyle.TEXT, false);
        graphics.drawString(font, Component.translatable("menu.conveyance.handcrafting"), RIGHT + 8, CONTENT + 7, FactoryWindowStyle.TEXT, false);
        graphics.drawString(font, Component.translatable("menu.conveyance.hotbar_links"), 8, PlayerStorageLayout.HOTBAR_LABEL_Y, 0xFF9CABB8, false);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        refresh();
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (mapView) { map.render(graphics, leftPos, topPos + CONTENT, imageWidth, mouseX, mouseY); return; }
        if (researchView) {
            graph.render(graphics, leftPos, topPos + CONTENT, imageWidth, mouseX, mouseY);
            startResearch.render(graphics, mouseX, mouseY, partialTick);
            graph.renderTooltip(graphics, leftPos, topPos + CONTENT, imageWidth, mouseX, mouseY);
            return;
        }
        var groups = grouped();
        for (int i = 0; i < categoryButtons.size(); i++) {
            Button button = categoryButtons.get(i);
            boolean selected = CATEGORIES.get(i).equals(category);
            graphics.fill(button.getX(), button.getY(), button.getX() + button.getWidth(), button.getY() + 20, selected ? 0xFF618A78 : 0xFF354353);
            graphics.renderItem(RecipePresentation.categoryIcon(CATEGORIES.get(i)), button.getX() + (button.getWidth() - 16) / 2, button.getY() + 2);
        }
        for (int cell = 0; cell < recipeButtons.size(); cell++) {
            CraftingRecipe recipe = recipeAt(cell, groups);
            if (recipe == null) continue;
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
            graphics.drawString(font, amount, -font.width(amount), 0, button.active ? FactoryWindowStyle.ACCENT : 0xFF94A0AD, true);
            graphics.pose().popPose();
        }
        for (int slot = 0; slot < hotbarLinks.size(); slot++) {
            Button button = hotbarLinks.get(slot);
            HotbarLinksView.cell(graphics, slot, button.getX(), button.getY(), PlayerStorageLayout.CELL);
        }
        renderCraftingQueue(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (menu.getCarried().isEmpty()) for (int slot = 0; slot < hotbarLinks.size(); slot++) if (over(hotbarLinks.get(slot), mouseX, mouseY)) HotbarLinksView.tooltip(graphics, slot, mouseX, mouseY);
        for (int cell = 0; cell < recipeButtons.size(); cell++) {
            if (over(recipeButtons.get(cell), mouseX, mouseY)) {
                CraftingRecipe recipe = recipeAt(cell, groups);
                if (recipe != null) ingredientTooltip(graphics, recipe, mouseX, mouseY);
            }
        }
    }
    private void ingredientTooltip(GuiGraphics graphics, CraftingRecipe recipe, int mouseX, int mouseY) {
        boolean possible = minecraft.player.isCreative() || amounts.getOrDefault(recipe.getId(), 0) > 0;
        int baseTime = HandCrafting.craftingTicks(recipe), totalTime = craftingTimes.getOrDefault(recipe.getId(), baseTime);
        var timing = new ArrayList<Component>();
        timing.add(Component.translatable("menu.conveyance.crafting_time", FactoryTooltips.number(baseTime / 20.0)));
        if (totalTime > baseTime) timing.add(Component.translatable("menu.conveyance.crafting_time_recursive", FactoryTooltips.number(totalTime / 20.0)));
        if (minecraft.player.isCreative()) timing.add(Component.translatable("menu.conveyance.crafting_creative"));
        RecipePresentation.tooltip(graphics, recipe, rawCosts.getOrDefault(recipe.getId(), List.of()),
                requirement -> HandCrafting.count(cachedItems, requirement.ingredient()) >= requirement.count()
                        ? FactoryWindowStyle.ACCENT : possible ? 0xFFE1BC76 : 0xFFEB8585,
                width, height, mouseX, mouseY, timing);
    }
    private int queueX() { return leftPos + (imageHeight >= 278 ? 8 : 64); }
    private int queueY() { return topPos + (imageHeight >= 278 ? CONTENT + 225 : CONTENT + 1); }
    private int queueCells() { return imageHeight >= 278 ? 8 : 5; }
    private int queueCell(double x, double y) {
        if (mapView || researchView || y < queueY() || y >= queueY() + 20 || x < queueX() || x >= queueX() + queueCells() * 20 - 2) return -1;
        int index = queueScroll + (int) (x - queueX()) / 20;
        return index < HandCraftingQueue.client().orders().size() ? index : -1;
    }
    private void renderCraftingQueue(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var queue = HandCraftingQueue.client();
        queueScroll = Math.min(queueScroll, Math.max(0, queue.orders().size() - queueCells()));
        if (queue.orders().isEmpty()) return;
        if (imageHeight >= 278) {
            graphics.drawString(font, Component.translatable("menu.conveyance.crafting_queue"), leftPos + 8, topPos + CONTENT + 211, FactoryWindowStyle.MUTED, false);
            if (imageHeight >= 314) graphics.drawString(font, CraftingHud.eta(partialTick), leftPos + 8, queueY() + 25, 0xFF9CABB8, false);
        }
        CraftingHud.cells(graphics, queueX(), queueY(), queueCells(), queueScroll, partialTick);
        int hovered = queueCell(mouseX, mouseY);
        if (hovered >= 0 && menu.getCarried().isEmpty()) CraftingHud.tooltip(graphics, queue.orders().get(hovered), mouseX, mouseY);
    }
    private static boolean over(Button button, double x, double y) { return button.visible && x >= button.getX() && x < button.getX() + button.getWidth() && y >= button.getY() && y < button.getY() + button.getHeight(); }
    @Override public boolean mouseClicked(double x, double y, int button) {
        linkPressed = false;
        int queued = queueCell(x, y);
        if (queued >= 0) {
            if (button == 1) InventoryNetwork.requestCancelCraft(HandCraftingQueue.client().orders().get(queued).id());
            return true;
        }
        if (!mapView && !researchView && button == 1) {
            for (int slot = 0; slot < hotbarLinks.size(); slot++) if (over(hotbarLinks.get(slot), x, y)) {
                if (slot == 0) HotbarControls.select(0);
                else InventoryNetwork.requestLink(menu.containerId, slot, BuiltInRegistries.ITEM.getKey(Items.AIR));
                return true;
            }
            var groups = grouped();
            for (int cell = 0; cell < recipeButtons.size(); cell++) if (over(recipeButtons.get(cell), x, y)) {
                CraftingRecipe recipe = recipeAt(cell, groups);
                if (recipe != null && recipeButtons.get(cell).active) InventoryNetwork.requestFive(recipe.getId());
                return true;
            }
        }
        if (!mapView && !researchView && button == 2) {
            var groups = grouped();
            for (int cell = 0; cell < recipeButtons.size(); cell++) if (over(recipeButtons.get(cell), x, y)) {
                CraftingRecipe recipe = recipeAt(cell, groups);
                if (recipe != null) InventoryNetwork.requestLink(menu.containerId, LinkedHotbar.bindingSlot(minecraft.player),
                        BuiltInRegistries.ITEM.getKey(recipe.getResultItem(minecraft.level.registryAccess()).getItem()));
                return true;
            }
        }
        if (mapView && map.click(x, y, button, leftPos, topPos + CONTENT, imageWidth)) return true;
        if (researchView && graph.click(x, y, button, leftPos, topPos + CONTENT, imageWidth)) { refresh(); return true; }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double deltaX, double deltaY) {
        return mapView && map.drag(deltaX, deltaY) || researchView && graph.drag(deltaX, deltaY) || super.mouseDragged(x, y, button, deltaX, deltaY);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        if (!mapView && !researchView && button == 0 && !linkPressed && !menu.getCarried().isEmpty()) {
            for (int slot = 0; slot < hotbarLinks.size(); slot++) if (over(hotbarLinks.get(slot), x, y)) {
                if (slot > 0) InventoryNetwork.requestLink(menu.containerId, slot, BuiltInRegistries.ITEM.getKey(menu.getCarried().getItem()));
                super.mouseReleased(x, y, button);
                return true;
            }
        }
        linkPressed = false;
        return mapView && map.release() || researchView && graph.release() || super.mouseReleased(x, y, button);
    }
    @Override public boolean mouseScrolled(double x, double y, double amount) {
        if (queueCell(x, y) >= 0) {
            queueScroll = Math.max(0, Math.min(Math.max(0, HandCraftingQueue.client().orders().size() - queueCells()), queueScroll - (int) Math.signum(amount)));
            return true;
        }
        if (mapView) return map.scroll(x, y, amount, leftPos, topPos + CONTENT, imageWidth);
        if (researchView) return graph.scroll(x, y, amount, leftPos, topPos + CONTENT, imageWidth);
        if (x >= leftPos + RIGHT && y >= topPos + CONTENT + GRID_Y && y < topPos + CONTENT + GRID_Y + rows * ROW_HEIGHT) {
            scroll = Math.max(0, Math.min(maxScroll(grouped().size()), scroll - (int) Math.signum(amount)));
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
