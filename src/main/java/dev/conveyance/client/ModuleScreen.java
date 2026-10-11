package dev.conveyance.client;

import dev.conveyance.factory.ModuleMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ModuleScreen extends FactoryWindowScreen<ModuleMenu> {
    public ModuleScreen(ModuleMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @Override protected int compactMachineWidth() { return FactoryWindowStyle.FUEL_PANEL_WIDTH; }
    @Override protected Component machinePanelTitle() { return Component.translatable("menu.conveyance.modules"); }
    @Override protected void renderMachine(GuiGraphics graphics, float partialTick) {
        text(graphics, Component.translatable("menu.conveyance.module_effects"), RIGHT + 8, 50, FactoryWindowStyle.MUTED);
    }
}
