package dev.conveyance.client;

import dev.conveyance.factory.AssemblyRecipes;
import dev.conveyance.factory.FactoryCraftingRecipe;
import dev.conveyance.factory.ProcessingMenu;
import dev.conveyance.player.InventoryNetwork;
import dev.conveyance.player.RecipeCosts;
import dev.conveyance.player.ResearchState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;

public final class ProcessingScreen extends FactoryWindowScreen<ProcessingMenu> {
    private static final int CELL = 26, ROW_HEIGHT = CELL, GRID_Y = 98;
    private static String lastCategory = "intermediates";
    private final List<Button> recipeButtons = new ArrayList<>(), categoryButtons = new ArrayList<>();
    private final Map<ResourceLocation, List<ItemStack>> rawCosts = new HashMap<>();
    private List<CraftingRecipe> recipes = List.of();
    private List<RecipePresentation.Row> groups = List.of();
    private String category = lastCategory;
    private EditBox search;
    private FactoryIconButton changeRecipe, backToProcessing;
    private boolean pickerOpen, manualPicker;
    private ResourceLocation pendingRecipe;
    private int scroll, columns, rows;
    private long cachedMask = -1;
    private boolean cachedCreative;

    public ProcessingScreen(ProcessingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        pickerOpen = menu.assembler() && menu.recipe() == null;
    }
    @Override protected int compactMachineWidth() {
        return menu.assembler() ? FactoryWindowStyle.RECIPE_PANEL_WIDTH : FactoryWindowStyle.STORAGE_PANEL_WIDTH;
    }
    @Override protected void init() {
        super.init();
        if (!menu.assembler()) return;
        changeRecipe = addRenderableWidget(new FactoryIconButton(leftPos + imageWidth - 28, topPos + 26,
                Component.translatable("menu.conveyance.change_recipe"), new ItemStack(dev.conveyance.factory.FactoryContent.GEAR_ITEM.get()), button -> {
                    manualPicker = true; pickerOpen = true; refresh();
                }));
        backToProcessing = addRenderableWidget(new FactoryIconButton(leftPos + imageWidth - 28, topPos + 26,
                Component.translatable("menu.conveyance.return_to_processing"), "\u2190", button -> {
                    manualPicker = false; pickerOpen = false; refresh();
                }));
        recipeButtons.clear();
        categoryButtons.clear();
        int panelWidth = imageWidth - RIGHT;
        int categoryWidth = (panelWidth - 16) / RecipePresentation.CATEGORIES.size();
        for (int index = 0; index < RecipePresentation.CATEGORIES.size(); index++) {
            String selected = RecipePresentation.CATEGORIES.get(index);
            Button button = addRenderableWidget(Button.builder(Component.empty(), clicked -> {
                category = selected; lastCategory = selected; scroll = 0; refresh();
            }).bounds(leftPos + RIGHT + 8 + index * categoryWidth, topPos + 50, categoryWidth - 1, 20).build());
            button.setTooltip(Tooltip.create(Component.translatable("menu.conveyance.category." + selected)));
            categoryButtons.add(button);
        }
        String query = search == null ? "" : search.getValue();
        search = new EditBox(font, leftPos + RIGHT + 8, topPos + 74, panelWidth - 16, 16, Component.translatable("menu.conveyance.search"));
        search.setHint(Component.translatable("menu.conveyance.search"));
        search.setValue(query);
        search.setResponder(value -> { scroll = 0; refresh(); });
        addRenderableWidget(search);
        columns = Math.max(1, (panelWidth - 16) / CELL);
        rows = Math.max(1, (imageHeight - GRID_Y - 12) / ROW_HEIGHT);
        for (int cell = 0; cell < rows * columns; cell++) {
            final int index = cell;
            recipeButtons.add(addRenderableWidget(Button.builder(Component.empty(), button -> {
                CraftingRecipe recipe = recipeAt(index);
                if (recipe != null && pendingRecipe == null) {
                    pendingRecipe = recipe.getId(); InventoryNetwork.requestAssemblyRecipe(menu.containerId, pendingRecipe); refresh();
                }
            }).bounds(leftPos + RIGHT + 8 + cell % columns * CELL, topPos + GRID_Y + cell / columns * ROW_HEIGHT, 24, 24).build()));
        }
        refresh();
    }
    void selectionConfirmed(ResourceLocation recipe, boolean accepted) {
        if (!recipe.equals(pendingRecipe)) return;
        pendingRecipe = null;
        if (accepted) { manualPicker = false; pickerOpen = false; }
        refresh();
    }
    private void refresh() {
        if (pendingRecipe == null) pickerOpen = manualPicker || menu.recipe() == null;
        menu.showProcessing(!pickerOpen);
        changeRecipe.visible = !pickerOpen;
        backToProcessing.visible = pickerOpen && menu.recipe() != null;
        backToProcessing.active = pendingRecipe == null;
        search.setVisible(pickerOpen);
        if (!pickerOpen) search.setFocused(false);
        long mask = ResearchState.mask(minecraft.level);
        boolean creative = minecraft.player.isCreative();
        if (mask != cachedMask || creative != cachedCreative) {
            cachedMask = mask; cachedCreative = creative;
            recipes = creative ? AssemblyRecipes.all(minecraft.level) : AssemblyRecipes.available(minecraft.level);
            rawCosts.clear();
            var costs = new RecipeCosts(minecraft.level);
            for (CraftingRecipe recipe : recipes) rawCosts.put(recipe.getId(), costs.raw(recipe));
        }
        String query = search.getValue().toLowerCase(Locale.ROOT);
        var filtered = recipes.stream()
                .filter(recipe -> !query.isBlank() || recipe instanceof FactoryCraftingRecipe factory && factory.tab().equals(category))
                .filter(recipe -> recipe.getResultItem(minecraft.level.registryAccess()).getHoverName().getString().toLowerCase(Locale.ROOT).contains(query))
                .toList();
        groups = RecipePresentation.grouped(filtered, minecraft.level, columns);
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        for (int i = 0; i < categoryButtons.size(); i++) {
            categoryButtons.get(i).visible = pickerOpen;
            categoryButtons.get(i).active = !RecipePresentation.CATEGORIES.get(i).equals(category);
        }
        for (int cell = 0; cell < recipeButtons.size(); cell++) {
            Button button = recipeButtons.get(cell);
            CraftingRecipe recipe = recipeAt(cell);
            button.visible = pickerOpen && recipe != null;
            button.active = button.visible && pendingRecipe == null;
        }
    }
    private int maxScroll() { return Math.max(0, groups.size() - rows); }
    private CraftingRecipe recipeAt(int cell) {
        int row = scroll + cell / columns, column = cell % columns;
        return row < groups.size() && column < groups.get(row).recipes().size() ? groups.get(row).recipes().get(column) : null;
    }
    @Override protected void containerTick() {
        super.containerTick();
        if (menu.assembler()) { search.tick(); refresh(); }
    }
    @Override protected Component machinePanelTitle() {
        return Component.translatable(menu.assembler() ? pickerOpen ? "menu.conveyance.recipe_picker" : "menu.conveyance.production" : "menu.conveyance.smelting");
    }
    @Override protected void renderMachine(GuiGraphics graphics, float partialTick) {
        if (!menu.assembler()) {
            text(graphics, Component.translatable("menu.conveyance.inputs"), RIGHT + 8, 50, FactoryWindowStyle.MUTED);
            text(graphics, Component.translatable("menu.conveyance.fuel"), RIGHT + 8, 116, FactoryWindowStyle.MUTED);
            text(graphics, Component.translatable("menu.conveyance.output"), RIGHT + 112, 64, FactoryWindowStyle.MUTED);
            hint(graphics, 0, new ItemStack(net.minecraft.world.item.Items.RAW_IRON));
            hint(graphics, 1, new ItemStack(net.minecraft.world.item.Items.COAL));
            text(graphics, Component.translatable("menu.conveyance.fuel_seconds", (menu.fuelRemaining() + 19) / 20), RIGHT + 8, 142, FactoryWindowStyle.MUTED);
            FactoryWindowStyle.progress(graphics, leftPos + RIGHT + 8, topPos + 164, machineWidth() - 16, 6, menu.progress() / 24F, FactoryWindowStyle.ACCENT);
            return;
        }
        if (pickerOpen) return;
        var recipe = menu.recipe();
        if (recipe != null) {
            ItemStack result = recipe.getResultItem(minecraft.level.registryAccess());
            String name = (result.getCount() > 1 ? result.getCount() + " × " : "") + result.getHoverName().getString();
            graphics.drawString(font, font.plainSubstrByWidth(name, machineWidth() - 16), leftPos + RIGHT + 8, topPos + 50, FactoryWindowStyle.ACCENT, false);
            var requirements = AssemblyRecipes.requirements(recipe);
            for (int index = 0; index < requirements.size(); index++) {
                ItemStack[] choices = requirements.get(index).ingredient().getItems();
                if (choices.length > 0) hint(graphics, index, choices[0].copyWithCount(requirements.get(index).count()));
            }
        } else text(graphics, Component.translatable("menu.conveyance.choose_recipe"), RIGHT + 8, 50, FactoryWindowStyle.MUTED);
        text(graphics, Component.translatable("menu.conveyance.inputs"), RIGHT + 8, 60, FactoryWindowStyle.MUTED);
        text(graphics, Component.translatable("menu.conveyance.output"), RIGHT + 146, 60, FactoryWindowStyle.MUTED);
        FactoryWindowStyle.progress(graphics, leftPos + RIGHT + 106, topPos + 76, 24, 4, menu.progress() / 24F, FactoryWindowStyle.ACCENT);
    }
    @Override protected void renderMachineOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (menu.assembler() && pickerOpen) renderPicker(graphics, mouseX, mouseY);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (menu.assembler()) refresh();
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    @Override protected void renderMachineTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!menu.assembler()) return;
        if (!pickerOpen && hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < 5 && menu.recipe() != null) {
            var requirements = AssemblyRecipes.requirements(menu.recipe());
            int index = hoveredSlot.index;
            if (index < requirements.size() && requirements.get(index).ingredient().getItems().length > 0)
                IconTooltips.render(graphics, List.of(IconTooltips.item(requirements.get(index).ingredient().getItems()[0],
                        Component.translatable("menu.conveyance.requires", requirements.get(index).count(), requirements.get(index).ingredient().getItems()[0].getHoverName()))), mouseX, mouseY);
        }
        if (pickerOpen) for (int cell = 0; cell < recipeButtons.size(); cell++) if (over(recipeButtons.get(cell), mouseX, mouseY)) {
            CraftingRecipe recipe = recipeAt(cell);
            if (recipe != null) RecipePresentation.tooltip(graphics, recipe, rawCosts.getOrDefault(recipe.getId(), List.of()),
                    requirement -> FactoryWindowStyle.TEXT, width, height, mouseX, mouseY);
        }
    }
    private void renderPicker(GuiGraphics graphics, int mouseX, int mouseY) {
        if (groups.isEmpty()) text(graphics, Component.translatable("menu.conveyance.no_recipes"), RIGHT + 8, GRID_Y, FactoryWindowStyle.MUTED);
        for (int i = 0; i < categoryButtons.size(); i++) {
            Button button = categoryButtons.get(i);
            boolean selected = RecipePresentation.CATEGORIES.get(i).equals(category);
            graphics.fill(button.getX(), button.getY(), button.getX() + button.getWidth(), button.getY() + 20, selected ? 0xFF618A78 : 0xFF354353);
            graphics.renderItem(RecipePresentation.categoryIcon(RecipePresentation.CATEGORIES.get(i)), button.getX() + (button.getWidth() - 16) / 2, button.getY() + 2);
        }
        CraftingRecipe current = menu.recipe();
        for (int cell = 0; cell < recipeButtons.size(); cell++) {
            CraftingRecipe recipe = recipeAt(cell);
            if (recipe == null) continue;
            Button button = recipeButtons.get(cell);
            int x = button.getX(), y = button.getY();
            boolean selected = current != null && current.getId().equals(recipe.getId());
            graphics.fill(x, y, x + 24, y + 24, selected ? 0xFF8AD8A0 : 0xFF596674);
            graphics.fill(x + 1, y + 1, x + 23, y + 23, selected ? 0xFF375749 : button.isHoveredOrFocused() ? 0xFF405061 : 0xFF293645);
            ItemStack output = recipe.getResultItem(minecraft.level.registryAccess());
            graphics.renderItem(output, x + 4, y + 4);
            graphics.renderItemDecorations(font, output, x + 4, y + 4);
        }
        if (maxScroll() > 0) {
            int trackHeight = rows * ROW_HEIGHT;
            int thumbHeight = Math.max(12, trackHeight * rows / groups.size());
            int x = leftPos + imageWidth - 5, y = topPos + GRID_Y;
            graphics.fill(x, y, x + 2, y + trackHeight, 0xFF111820);
            int thumbY = y + (trackHeight - thumbHeight) * scroll / maxScroll();
            graphics.fill(x, thumbY, x + 2, thumbY + thumbHeight, 0xFF88B9A2);
        }
    }
    private static boolean over(Button button, double x, double y) {
        return button.visible && x >= button.getX() && x < button.getX() + button.getWidth() && y >= button.getY() && y < button.getY() + button.getHeight();
    }
    @Override public boolean mouseScrolled(double x, double y, double amount) {
        if (menu.assembler() && pickerOpen && x >= leftPos + RIGHT && x < leftPos + imageWidth && y >= topPos + GRID_Y && y < topPos + imageHeight) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(amount)));
            refresh();
            return true;
        }
        return super.mouseScrolled(x, y, amount);
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (menu.assembler() && pickerOpen && search.isFocused() && key != 256) return search.keyPressed(key, scan, modifiers);
        return super.keyPressed(key, scan, modifiers);
    }
}
