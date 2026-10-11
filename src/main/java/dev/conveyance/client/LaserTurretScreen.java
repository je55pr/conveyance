package dev.conveyance.client;

import dev.conveyance.combat.LaserTurretLogic;
import dev.conveyance.combat.LaserTurretMenu;
import dev.conveyance.factory.FactoryTooltips;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class LaserTurretScreen extends FactoryWindowScreen<LaserTurretMenu> {
    public LaserTurretScreen(LaserTurretMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @Override protected int compactMachineWidth() { return FactoryWindowStyle.STORAGE_PANEL_WIDTH; }
    @Override protected Component machinePanelTitle() { return Component.translatable("menu.conveyance.laser_power"); }
    @Override protected void renderMachine(GuiGraphics graphics, float partialTick) {
        text(graphics, menu.status().label(), RIGHT + 8, 50, FactoryWindowStyle.ACCENT);
        text(graphics, Component.translatable("menu.conveyance.laser_charge", menu.chargeKJ(), 801), RIGHT + 8, 74, FactoryWindowStyle.TEXT);
        FactoryWindowStyle.progress(graphics, leftPos + RIGHT + 8, topPos + 92, machineWidth() - 16, 6, menu.chargeKJ() / 801F, FactoryWindowStyle.ACCENT);
        text(graphics, Component.translatable("menu.conveyance.laser_input", menu.inputKW(), menu.satisfactionPercent()), RIGHT + 8, 112, FactoryWindowStyle.MUTED);
        text(graphics, Component.translatable("tooltip.conveyance.stat.weapon_range", LaserTurretLogic.RANGE), RIGHT + 8, 136, FactoryWindowStyle.TEXT);
        text(graphics, Component.translatable("tooltip.conveyance.stat.shot_damage", FactoryTooltips.number(menu.damage())), RIGHT + 8, 156, FactoryWindowStyle.TEXT);
        text(graphics, Component.translatable("tooltip.conveyance.stat.shots", FactoryTooltips.number(menu.rate())), RIGHT + 8, 176, FactoryWindowStyle.TEXT);
        text(graphics, Component.translatable("tooltip.conveyance.stat.shot_energy", 800), RIGHT + 8, 196, FactoryWindowStyle.MUTED);
    }
}
