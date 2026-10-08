package dev.conveyance.client;

import dev.conveyance.factory.AssemblyRecipes;
import dev.conveyance.factory.ProcessingMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class ProcessingScreen extends AbstractContainerScreen<ProcessingMenu> {
    private static final ResourceLocation FURNACE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/furnace.png");
    private static final ResourceLocation PANEL = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");
    public ProcessingScreen(ProcessingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = menu.assembler() ? 175 : 166;
        inventoryLabelY = imageHeight - 94;
    }
    @Override protected void init() {
        super.init();
        if (menu.assembler()) {
            addRenderableWidget(Button.builder(Component.literal("<"), button -> select(0)).bounds(leftPos + 8, topPos + 19, 20, 16).build());
            addRenderableWidget(Button.builder(Component.literal(">"), button -> select(1)).bounds(leftPos + 148, topPos + 19, 20, 16).build());
        }
    }
    private void select(int direction) { if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, direction); }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (!menu.assembler()) {
            graphics.blit(FURNACE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
            if (menu.burning()) graphics.blit(FURNACE, leftPos + 56, topPos + 36, 176, 0, 14, 14);
            graphics.blit(FURNACE, leftPos + 79, topPos + 34, 176, 14, menu.progress() + 1, 16);
            return;
        }
        graphics.blit(PANEL, leftPos, topPos, 0, 0, imageWidth, 79);
        graphics.fill(leftPos + 7, topPos + 17, leftPos + 169, topPos + 79, 0xFFC6C6C6);
        graphics.blit(PANEL, leftPos, topPos + 79, 0, 126, imageWidth, 96);
        for (int index = 0; index < 5; index++) {
            var slot = menu.slots.get(index);
            graphics.blit(PANEL, leftPos + slot.x - 1, topPos + slot.y - 1, 7, 17, 18, 18);
        }
        graphics.fill(leftPos + 106, topPos + 60, leftPos + 130, topPos + 64, 0xFF777777);
        graphics.fill(leftPos + 106, topPos + 60, leftPos + 106 + menu.progress(), topPos + 64, 0xFF338844);
        var recipe = menu.recipe();
        if (recipe != null) {
            var requirements = AssemblyRecipes.requirements(recipe);
            for (int index = 0; index < requirements.size(); index++) {
                var slot = menu.slots.get(index);
                ItemStack[] choices = requirements.get(index).ingredient().getItems();
                if (slot.hasItem() || choices.length == 0) continue;
                ItemStack hint = choices[0].copyWithCount(requirements.get(index).count());
                graphics.renderItem(hint, leftPos + slot.x, topPos + slot.y);
                graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 0x90C6C6C6);
                graphics.renderItemDecorations(font, hint, leftPos + slot.x, topPos + slot.y);
            }
        }
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        if (!menu.assembler()) return;
        var recipe = menu.recipe();
        if (recipe != null) {
            ItemStack result = recipe.getResultItem(minecraft.level.registryAccess());
            String name = result.getCount() + "x " + result.getHoverName().getString();
            graphics.drawCenteredString(font, font.plainSubstrByWidth(name, 112), 88, 23, 0x404040);
        }
        graphics.drawString(font, Component.translatable("menu.conveyance.inputs"), 8, 42, 0x404040, false);
        graphics.drawString(font, Component.translatable("menu.conveyance.output"), 135, 42, 0x404040, false);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (menu.assembler() && hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < 4 && menu.recipe() != null) {
            var requirements = AssemblyRecipes.requirements(menu.recipe());
            int index = hoveredSlot.index;
            if (index < requirements.size() && requirements.get(index).ingredient().getItems().length > 0) {
                graphics.renderTooltip(font, Component.translatable("menu.conveyance.requires", requirements.get(index).count(),
                        requirements.get(index).ingredient().getItems()[0].getHoverName()), mouseX, mouseY);
            }
        }
    }
}
