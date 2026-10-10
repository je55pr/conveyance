package dev.conveyance.client;

import dev.conveyance.factory.FactoryMenu;
import dev.conveyance.player.InventoryNetwork;
import dev.conveyance.player.LinkedHotbar;
import dev.conveyance.player.PlayerStorageLayout;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Common content-sized shell, full inventory and linked hotbar. Subclasses supply machine controls. */
public abstract class FactoryWindowScreen<M extends FactoryMenu> extends AbstractContainerScreen<M> {
    protected static final int RIGHT = PlayerStorageLayout.MACHINE_X;
    private final List<Button> links = new ArrayList<>();
    private boolean linkPressed;
    protected FactoryWindowScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = PlayerStorageLayout.MIN_WIDTH; imageHeight = PlayerStorageLayout.MIN_HEIGHT;
    }
    @Override protected void init() {
        imageWidth = RIGHT + compactMachineWidth();
        imageHeight = FactoryWindowStyle.COMPACT_HEIGHT;
        super.init(); links.clear();
        addRenderableWidget(Button.builder(Component.literal("×"), button -> onClose()).bounds(leftPos + imageWidth - 20, topPos + 1, 20, 20).build());
        for (int slot = 0; slot < LinkedHotbar.SIZE; slot++) {
            final int index = slot;
            links.add(addRenderableWidget(Button.builder(Component.empty(), button -> {
                linkPressed = true;
                ItemStack carried = menu.getCarried();
                if (index > 0 && !carried.isEmpty()) InventoryNetwork.requestLink(menu.containerId, index, BuiltInRegistries.ITEM.getKey(carried.getItem()));
                else HotbarControls.select(index);
            }).bounds(leftPos + PlayerStorageLayout.GRID_X + slot * PlayerStorageLayout.CELL,
                    topPos + PlayerStorageLayout.HOTBAR_Y, PlayerStorageLayout.CELL, PlayerStorageLayout.CELL).build()));
        }
    }
    protected int machineWidth() { return imageWidth - RIGHT; }
    protected int compactMachineWidth() { return FactoryWindowStyle.RECIPE_PANEL_WIDTH; }
    protected Component machinePanelTitle() { return Component.translatable("menu.conveyance.machine"); }
    protected abstract void renderMachine(GuiGraphics graphics, float partialTick);
    protected void renderMachineOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}
    protected void renderMachineTooltip(GuiGraphics graphics, int mouseX, int mouseY) {}
    protected void text(GuiGraphics graphics, Component label, int x, int y, int color) {
        graphics.drawWordWrap(font, label, leftPos + x, topPos + y, Math.max(20, imageWidth - x - 8), color);
    }
    protected void hint(GuiGraphics graphics, int index, ItemStack item) {
        var slot = menu.getSlot(index); if (slot.hasItem() || item.isEmpty()) return;
        int x = leftPos + slot.x, y = topPos + slot.y;
        graphics.renderItem(item, x, y);
        graphics.pose().pushPose(); graphics.pose().translate(0, 0, 250);
        graphics.fill(x, y, x + 16, y + 16, 0xA0202A35);
        graphics.renderItemDecorations(font, item, x, y); graphics.pose().popPose();
    }
    @Override protected final void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 21, FactoryWindowStyle.HEADER);
        FactoryWindowStyle.panel(graphics, leftPos, topPos + PlayerStorageLayout.CONTENT_Y, PlayerStorageLayout.PANEL_WIDTH, imageHeight - PlayerStorageLayout.CONTENT_Y);
        FactoryWindowStyle.panel(graphics, leftPos + RIGHT, topPos + PlayerStorageLayout.CONTENT_Y, machineWidth(), imageHeight - PlayerStorageLayout.CONTENT_Y);
        for (var slot : menu.slots) if (slot.isActive()) FactoryWindowStyle.slot(graphics, leftPos + slot.x, topPos + slot.y);
        renderMachine(graphics, partialTick);
    }
    @Override protected final void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, font.plainSubstrByWidth(title.getString(), imageWidth - 32), 8, 7, FactoryWindowStyle.TEXT, false);
        graphics.drawString(font, playerInventoryTitle, 8, PlayerStorageLayout.CONTENT_Y + 7, FactoryWindowStyle.TEXT, false);
        graphics.drawString(font, Component.translatable("menu.conveyance.hotbar_links"), 8, PlayerStorageLayout.HOTBAR_LABEL_Y, FactoryWindowStyle.MUTED, false);
        graphics.drawString(font, font.plainSubstrByWidth(machinePanelTitle().getString(), machineWidth() - 16), RIGHT + 8, PlayerStorageLayout.CONTENT_Y + 7, FactoryWindowStyle.TEXT, false);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics); super.render(graphics, mouseX, mouseY, partialTick);
        renderMachineOverlay(graphics, mouseX, mouseY, partialTick);
        for (int slot = 0; slot < links.size(); slot++) {
            var button = links.get(slot); HotbarLinksView.cell(graphics, slot, button.getX(), button.getY(), PlayerStorageLayout.CELL);
        }
        renderTooltip(graphics, mouseX, mouseY); renderMachineTooltip(graphics, mouseX, mouseY);
        int hovered = linkAt(mouseX, mouseY);
        if (hovered >= 0 && menu.getCarried().isEmpty()) HotbarLinksView.tooltip(graphics, hovered, mouseX, mouseY);
    }
    private int linkAt(double x, double y) {
        for (int index = 0; index < links.size(); index++) {
            var button = links.get(index);
            if (x >= button.getX() && x < button.getX() + button.getWidth() && y >= button.getY() && y < button.getY() + button.getHeight()) return index;
        }
        return -1;
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        linkPressed = false; int link = linkAt(x, y);
        if (link >= 0 && button == 1) {
            if (link == 0) HotbarControls.select(0);
            else InventoryNetwork.requestLink(menu.containerId, link, BuiltInRegistries.ITEM.getKey(Items.AIR));
            return true;
        }
        return super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        int link = linkAt(x, y);
        if (button == 0 && link > 0 && !linkPressed && !menu.getCarried().isEmpty())
            InventoryNetwork.requestLink(menu.containerId, link, BuiltInRegistries.ITEM.getKey(menu.getCarried().getItem()));
        linkPressed = false; return super.mouseReleased(x, y, button);
    }
}
