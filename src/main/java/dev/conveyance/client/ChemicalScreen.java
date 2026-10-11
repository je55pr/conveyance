package dev.conveyance.client;

import dev.conveyance.factory.ChemicalMachine;
import dev.conveyance.factory.ChemicalMenu;
import dev.conveyance.factory.ChemicalRecipe;
import dev.conveyance.factory.FactoryContent;
import dev.conveyance.factory.FactoryTooltips;
import dev.conveyance.factory.FluidKind;
import dev.conveyance.factory.MachineKind;
import dev.conveyance.player.InventoryNetwork;
import dev.conveyance.player.ResearchState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class ChemicalScreen extends FactoryWindowScreen<ChemicalMenu> {
    private final List<FactoryIconButton> recipes = new ArrayList<>();
    private List<ChemicalRecipe> filtered = List.of();
    private EditBox search;
    private FactoryIconButton change, back;
    private boolean picker, manualPicker, rejected;
    private ResourceLocation pending;
    private int scroll;
    public ChemicalScreen(ChemicalMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @Override protected void init() {
        super.init(); recipes.clear();
        change = addRenderableWidget(new FactoryIconButton(leftPos + imageWidth - 28, topPos + 26, Component.translatable("menu.conveyance.change_recipe"), new ItemStack(FactoryContent.GEAR_ITEM.get()), button -> { manualPicker = true; refresh(); }));
        back = addRenderableWidget(new FactoryIconButton(leftPos + imageWidth - 28, topPos + 26, Component.translatable("menu.conveyance.return_to_processing"), "\u2190", button -> { manualPicker = false; refresh(); }));
        search = new EditBox(font, leftPos + RIGHT + 8, topPos + 54, machineWidth() - 16, 18, Component.translatable("menu.conveyance.search"));
        search.setHint(Component.translatable("menu.conveyance.search")); search.setResponder(value -> { scroll = 0; refresh(); }); addRenderableWidget(search);
        for (int cell = 0; cell < 18; cell++) {
            final int index = cell;
            recipes.add(addRenderableWidget(new FactoryIconButton(leftPos + RIGHT + 8 + cell % 6 * 28, topPos + 86 + cell / 6 * 30,
                    Component.translatable("menu.conveyance.recipe_picker"), ItemStack.EMPTY, button -> {
                        int selected = scroll * 6 + index;
                        if (selected < filtered.size() && pending == null) { pending = filtered.get(selected).id(); rejected = false; InventoryNetwork.requestAssemblyRecipe(menu.containerId, pending); }
                    })));
        }
        refresh();
    }
    void selectionConfirmed(ResourceLocation recipe, boolean accepted) {
        if (!recipe.equals(pending)) return;
        pending = null; rejected = !accepted; if (accepted) manualPicker = false; refresh();
    }
    private void refresh() {
        if (search == null) return;
        picker = menu.kind != MachineKind.PUMPJACK && (manualPicker || menu.recipe() == null);
        menu.showProcessing(!picker); search.setVisible(picker);
        change.visible = !picker && menu.kind != MachineKind.PUMPJACK;
        back.visible = picker && menu.recipe() != null; back.active = pending == null;
        String query = search.getValue().toLowerCase(java.util.Locale.ROOT);
        filtered = ChemicalRecipe.all(minecraft.level, menu.kind).stream()
                .filter(recipe -> minecraft.player.isCreative() || ResearchState.recipeUnlocked(recipe.id(), ResearchState.mask(minecraft.level)))
                .filter(recipe -> name(recipe).getString().toLowerCase(java.util.Locale.ROOT).contains(query)).toList();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, (filtered.size() + 5) / 6 - 3)));
        // Rebuild the icon widgets when their recipe changes; their icon is immutable.
        for (int cell = 0; cell < recipes.size(); cell++) {
            var old = recipes.get(cell); int index = scroll * 6 + cell;
            old.visible = picker && index < filtered.size(); old.active = old.visible && pending == null;
            old.setTooltip(null); if (index < filtered.size()) old.setMessage(name(filtered.get(index)));
        }
    }
    private static Component name(ChemicalRecipe recipe) { return Component.translatable("recipe.conveyance." + recipe.id().getPath()); }
    @Override protected void containerTick() { super.containerTick(); search.tick(); refresh(); }
    @Override protected Component machinePanelTitle() { return Component.translatable(picker ? "menu.conveyance.recipe_picker" : "menu.conveyance.production"); }
    @Override protected void renderMachine(GuiGraphics graphics, float partialTick) {
        if (picker) return;
        var recipe = menu.recipe();
        text(graphics, recipe == null ? Component.translatable("block.conveyance.pumpjack") : name(recipe), RIGHT + 8, 52, FactoryWindowStyle.TEXT);
        if (recipe != null) {
            for (int slot = 0; slot < recipe.items().size(); slot++) hint(graphics, slot, recipe.items().get(slot).ingredient().getItems()[0]);
            hint(graphics, 2, recipe.result());
        } else text(graphics, Component.translatable("menu.conveyance.oil_rate", FactoryTooltips.number(menu.flow())), RIGHT + 8, 80, FactoryWindowStyle.TEXT);
        text(graphics, Component.translatable("status.conveyance.chemical." + menu.status().name().toLowerCase(java.util.Locale.ROOT)), RIGHT + 8, 108,
                menu.status() == ChemicalMachine.Status.WORKING ? FactoryWindowStyle.ACCENT : FactoryWindowStyle.MUTED);
        var costs = new ArrayList<ChemicalRecipe.FluidCost>();
        if (recipe != null) { costs.addAll(recipe.inputs()); costs.addAll(recipe.outputs()); }
        else costs.add(new ChemicalRecipe.FluidCost(4, FluidKind.CRUDE_OIL, 0));
        int y = 130;
        for (var cost : costs) {
            String label = cost.fluid().title().getString() + " " + Math.round(menu.amount(cost.channel()));
            graphics.drawString(font, font.plainSubstrByWidth(label, 110), leftPos + RIGHT + 8, topPos + y, FactoryWindowStyle.MUTED, false);
            FactoryWindowStyle.progress(graphics, leftPos + RIGHT + 120, topPos + y + 2, machineWidth() - 128, 5,
                    (float) (menu.amount(cost.channel()) / ChemicalMachine.CAPACITY), cost.fluid() == FluidKind.WATER ? 0xFF579CCA : 0xFFBD8BDC);
            y += 16;
        }
        FactoryWindowStyle.progress(graphics, leftPos + RIGHT + 8, topPos + 218, machineWidth() - 16, 6, menu.progress(), FactoryWindowStyle.ACCENT);
    }
    @Override protected void renderMachineOverlay(GuiGraphics graphics, int x, int y, float partialTick) {
        if (!picker) return;
        for (int cell = 0; cell < recipes.size(); cell++) {
            int index = scroll * 6 + cell; var button = recipes.get(cell);
            if (button.visible && index < filtered.size()) graphics.renderItem(filtered.get(index).icon(), button.getX() + 2, button.getY() + 2);
        }
        if (rejected) text(graphics, Component.translatable("message.conveyance.chemical_selection_failed"), RIGHT + 8, 188, FactoryWindowStyle.WARNING);
    }
    @Override protected void renderMachineTooltip(GuiGraphics graphics, int x, int y) {
        if (!picker) return;
        for (int cell = 0; cell < recipes.size(); cell++) if (recipes.get(cell).visible && recipes.get(cell).isMouseOver(x, y)) {
            int index = scroll * 6 + cell; if (index >= filtered.size()) continue;
            var recipe = filtered.get(index);
            int boxWidth = Math.min(280, width - 8), boxHeight = 62 + recipe.inputs().size() * 12 + recipe.outputs().size() * 12
                    + (recipe.items().isEmpty() ? 0 : 24) + (recipe.result().isEmpty() ? 0 : 24);
            int left = Math.max(4, Math.min(x + 12, width - boxWidth - 4)), top = Math.max(4, Math.min(y + 12, height - boxHeight - 4));
            graphics.pose().pushPose(); graphics.pose().translate(0, 0, 450);
            graphics.fill(left - 1, top - 1, left + boxWidth + 1, top + boxHeight + 1, FactoryWindowStyle.SLOT_BORDER);
            graphics.fill(left, top, left + boxWidth, top + boxHeight, FactoryWindowStyle.BACKGROUND);
            graphics.drawString(font, name(recipe), left + 8, top + 7, FactoryWindowStyle.TEXT, false);
            graphics.drawString(font, Component.translatable("menu.conveyance.recipe_seconds", FactoryTooltips.number(recipe.ticks() / 20.0)), left + 8, top + 21, FactoryWindowStyle.MUTED, false);
            graphics.drawString(font, Component.translatable("menu.conveyance.ingredients"), left + 8, top + 35, FactoryWindowStyle.MUTED, false);
            int row = top + 48;
            for (int i = 0; i < recipe.items().size(); i++) {
                var item = recipe.items().get(i);
                RecipePresentation.costIcon(graphics, item.ingredient().getItems()[0], item.count(), left + 8 + i * 30, row, FactoryWindowStyle.TEXT);
            }
            if (!recipe.items().isEmpty()) row += 24;
            for (var fluid : recipe.inputs()) { fluidCost(graphics, fluid, left + 8, row); row += 12; }
            graphics.drawString(font, Component.translatable("menu.conveyance.recipe_products"), left + 8, row, FactoryWindowStyle.MUTED, false); row += 12;
            if (!recipe.result().isEmpty()) { RecipePresentation.costIcon(graphics, recipe.result(), recipe.result().getCount(), left + 8, row, FactoryWindowStyle.TEXT); row += 24; }
            for (var fluid : recipe.outputs()) { fluidCost(graphics, fluid, left + 8, row); row += 12; }
            graphics.pose().popPose();
        }
    }
    private void fluidCost(GuiGraphics graphics, ChemicalRecipe.FluidCost cost, int x, int y) {
        int[] colors = {0xFF7C8390,0xFF579CCA,0xFFECAF5A,0xFF22242D,0xFFBD8BDC,0xFF8D603A,0xFFE2C563};
        graphics.fill(x, y + 1, x + 8, y + 9, colors[cost.fluid().ordinal()]);
        graphics.drawString(font, Component.literal(FactoryTooltips.number(cost.amount()) + " ").append(cost.fluid().title()), x + 12, y, FactoryWindowStyle.TEXT, false);
    }
    @Override public boolean mouseScrolled(double x, double y, double amount) {
        if (picker) { scroll -= (int) Math.signum(amount); refresh(); return true; }
        return super.mouseScrolled(x, y, amount);
    }
    @Override public void render(GuiGraphics graphics, int x, int y, float partialTick) { refresh(); super.render(graphics, x, y, partialTick); }
}
