package dev.conveyance.client;

import dev.conveyance.combat.TurretMenu;
import dev.conveyance.factory.FactoryContent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class TurretScreen extends FactoryWindowScreen<TurretMenu> {
    public TurretScreen(TurretMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @Override protected int compactMachineWidth() { return FactoryWindowStyle.FUEL_PANEL_WIDTH; }
    @Override protected Component machinePanelTitle() { return Component.translatable("menu.conveyance.ammunition"); }
    @Override protected void renderMachine(GuiGraphics graphics, float partialTick) {
        hint(graphics, 0, new ItemStack(FactoryContent.FIREARM_MAGAZINE.get()));
        text(graphics, Component.translatable("menu.conveyance.loaded_rounds", menu.rounds()), RIGHT + 8, 98, FactoryWindowStyle.TEXT);
        text(graphics, Component.translatable("tooltip.conveyance.gun_turret"), RIGHT + 8, 123, FactoryWindowStyle.MUTED);
    }
}
