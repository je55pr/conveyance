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
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;

public final class ProcessingScreen extends AbstractContainerScreen<ProcessingMenu> {
    private static final ResourceLocation FURNACE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/furnace.png");
    private static final int RIGHT = 184, CELL = 26, ROW_HEIGHT = 42, GRID_Y = 73;
    private static String lastCategory = "intermediates";
    private final List<Button> recipeButtons = new ArrayList<>(), categoryButtons = new ArrayList<>();
    private final Map<ResourceLocation, List<ItemStack>> rawCosts = new HashMap<>();
    private List<CraftingRecipe> recipes = List.of();
    private List<RecipePresentation.Row> groups = List.of();
    private String category = lastCategory;
    private EditBox search;
    private int scroll, columns, rows, cachedMask = -1;
    private boolean cachedCreative;

    public ProcessingScreen(ProcessingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = menu.assembler() ? 175 : 166;
        inventoryLabelY = imageHeight - 94;
    }
    @Override protected void init() {
        if (menu.assembler()) {
            imageWidth = Math.max(332, width - 24);
            imageHeight = Math.max(190, height - 24);
        }
        super.init();
        if (!menu.assembler()) return;
        recipeButtons.clear();
        categoryButtons.clear();
        int panelWidth = imageWidth - RIGHT;
        int categoryWidth = (panelWidth - 16) / RecipePresentation.CATEGORIES.size();
        for (int index = 0; index < RecipePresentation.CATEGORIES.size(); index++) {
            String selected = RecipePresentation.CATEGORIES.get(index);
            Button button = addRenderableWidget(Button.builder(Component.empty(), clicked -> {
                category = selected; lastCategory = selected; scroll = 0; refresh();
            }).bounds(leftPos + RIGHT + 8 + index * categoryWidth, topPos + 25, categoryWidth - 1, 20).build());
            button.setTooltip(Tooltip.create(Component.translatable("menu.conveyance.category." + selected)));
            categoryButtons.add(button);
        }
        String query = search == null ? "" : search.getValue();
        search = new EditBox(font, leftPos + RIGHT + 8, topPos + 49, panelWidth - 16, 16, Component.translatable("menu.conveyance.search"));
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
                if (recipe != null) InventoryNetwork.requestAssemblyRecipe(menu.containerId, recipe.getId());
            }).bounds(leftPos + RIGHT + 8 + cell % columns * CELL, topPos + GRID_Y + cell / columns * ROW_HEIGHT + 12, 24, 24).build()));
        }
        refresh();
    }
    private void refresh() {
        int mask = ResearchState.mask(minecraft.level);
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
        for (int i = 0; i < categoryButtons.size(); i++) categoryButtons.get(i).active = !RecipePresentation.CATEGORIES.get(i).equals(category);
        for (int cell = 0; cell < recipeButtons.size(); cell++) {
            Button button = recipeButtons.get(cell);
            CraftingRecipe recipe = recipeAt(cell);
            button.visible = recipe != null;
            button.active = recipe != null;
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
    private static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF607080);
        graphics.fill(x, y, x + width, y + height, 0xFF202A35);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 20, 0xFF303D4A);
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (!menu.assembler()) {
            graphics.blit(FURNACE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
            if (menu.burning()) graphics.blit(FURNACE, leftPos + 56, topPos + 36, 176, 0, 14, 14);
            graphics.blit(FURNACE, leftPos + 79, topPos + 34, 176, 14, menu.progress() + 1, 16);
            return;
        }
        panel(graphics, leftPos, topPos, 176, imageHeight);
        panel(graphics, leftPos + RIGHT, topPos, imageWidth - RIGHT, imageHeight);
        for (var slot : menu.slots) {
            int x = leftPos + slot.x, y = topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF596674);
            graphics.fill(x, y, x + 16, y + 16, 0xFF111820);
        }
        graphics.fill(leftPos + 106, topPos + 60, leftPos + 130, topPos + 64, 0xFF596674);
        graphics.fill(leftPos + 106, topPos + 60, leftPos + 106 + menu.progress(), topPos + 64, 0xFF8AD8A0);
        var recipe = menu.recipe();
        if (recipe != null) {
            var requirements = AssemblyRecipes.requirements(recipe);
            for (int index = 0; index < requirements.size(); index++) {
                var slot = menu.slots.get(index);
                ItemStack[] choices = requirements.get(index).ingredient().getItems();
                if (slot.hasItem() || choices.length == 0) continue;
                ItemStack hint = choices[0].copyWithCount(requirements.get(index).count());
                graphics.renderItem(hint, leftPos + slot.x, topPos + slot.y);
                graphics.pose().pushPose(); graphics.pose().translate(0, 0, 250);
                graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 0xA0202A35);
                graphics.renderItemDecorations(font, hint, leftPos + slot.x, topPos + slot.y);
                graphics.pose().popPose();
            }
        }
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!menu.assembler()) { super.renderLabels(graphics, mouseX, mouseY); return; }
        graphics.drawString(font, title, 8, 7, 0xFFE7EEF6, false);
        graphics.drawString(font, playerInventoryTitle, 8, 82, 0xFFB5C5D2, false);
        graphics.drawString(font, Component.translatable("menu.conveyance.recipe_picker"), RIGHT + 8, 7, 0xFFE7EEF6, false);
        var recipe = menu.recipe();
        if (recipe != null) {
            ItemStack result = recipe.getResultItem(minecraft.level.registryAccess());
            String name = (result.getCount() > 1 ? result.getCount() + " × " : "") + result.getHoverName().getString();
            graphics.drawString(font, font.plainSubstrByWidth(name, 160), 8, 28, 0xFF8AD8A0, false);
        }
        graphics.drawString(font, Component.translatable("menu.conveyance.inputs"), 8, 42, 0xFFB5C5D2, false);
        graphics.drawString(font, Component.translatable("menu.conveyance.output"), 135, 42, 0xFFB5C5D2, false);
        if (groups.isEmpty()) graphics.drawString(font, Component.translatable("menu.conveyance.no_recipes"), RIGHT + 8, GRID_Y + 12, 0xFF9CABB8, false);
        if (maxScroll() > 0) graphics.drawString(font, (scroll + 1) + "–" + Math.min(groups.size(), scroll + rows) + " / " + groups.size(),
                RIGHT + 8, imageHeight - 12, 0xFF9CABB8, false);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (menu.assembler()) refresh();
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (menu.assembler()) renderPicker(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
        if (menu.assembler() && hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < 4 && menu.recipe() != null) {
            var requirements = AssemblyRecipes.requirements(menu.recipe());
            int index = hoveredSlot.index;
            if (index < requirements.size() && requirements.get(index).ingredient().getItems().length > 0) {
                graphics.renderTooltip(font, Component.translatable("menu.conveyance.requires", requirements.get(index).count(),
                        requirements.get(index).ingredient().getItems()[0].getHoverName()), mouseX, mouseY);
            }
        }
        if (menu.assembler()) {
            for (int cell = 0; cell < recipeButtons.size(); cell++) {
                if (over(recipeButtons.get(cell), mouseX, mouseY)) {
                    CraftingRecipe recipe = recipeAt(cell);
                    if (recipe != null) RecipePresentation.tooltip(graphics, recipe, rawCosts.getOrDefault(recipe.getId(), List.of()),
                            requirement -> 0xFFE7EEF6, width, height, mouseX, mouseY);
                }
            }
        }
    }
    private void renderPicker(GuiGraphics graphics, int mouseX, int mouseY) {
        for (int row = 0; row < rows && scroll + row < groups.size(); row++)
            graphics.drawString(font, Component.translatable("menu.conveyance.group." + groups.get(scroll + row).family()),
                    leftPos + RIGHT + 8, topPos + GRID_Y + row * ROW_HEIGHT, 0xFFB5C5D2, false);
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
        if (menu.assembler() && x >= leftPos + RIGHT && x < leftPos + imageWidth && y >= topPos + GRID_Y && y < topPos + imageHeight) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(amount)));
            refresh();
            return true;
        }
        return super.mouseScrolled(x, y, amount);
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (menu.assembler() && search.isFocused() && key != 256) return search.keyPressed(key, scan, modifiers);
        return super.keyPressed(key, scan, modifiers);
    }
}
