package dev.conveyance.client;

import dev.conveyance.factory.CrateMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CrateScreen extends FactoryWindowScreen<CrateMenu> {
    public CrateScreen(CrateMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @Override protected int compactMachineWidth() { return FactoryWindowStyle.STORAGE_PANEL_WIDTH; }
    @Override protected Component machinePanelTitle() { return Component.translatable("menu.conveyance.storage"); }
    @Override protected void renderMachine(GuiGraphics graphics, float partialTick) {
        text(graphics, Component.translatable("menu.conveyance.crate_capacity", menu.rows() * 8), RIGHT + 8, 50, FactoryWindowStyle.MUTED);
    }
}
