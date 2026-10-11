package dev.conveyance.client;

import dev.conveyance.factory.FactoryTooltips;
import dev.conveyance.factory.FluidKind;
import dev.conveyance.factory.FluidMenu;
import dev.conveyance.factory.MachineKind;
import dev.conveyance.factory.SteamMachine;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class FluidScreen extends FactoryWindowScreen<FluidMenu> {
    public FluidScreen(FluidMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @Override protected Component machinePanelTitle() { return Component.translatable("menu.conveyance.fluids"); }
    private void buffer(GuiGraphics graphics, FluidKind type, double amount, int capacity, int y, int colour) {
        text(graphics, Component.translatable("menu.conveyance.fluid_buffer", type.title(), FactoryTooltips.number(amount), capacity), RIGHT + 8, y, FactoryWindowStyle.TEXT);
        FactoryWindowStyle.progress(graphics, leftPos + RIGHT + 8, topPos + y + 14, machineWidth() - 16, 6, (float) (amount / capacity), colour);
    }
    @Override protected void renderMachine(GuiGraphics graphics, float partialTick) {
        var status = menu.status();
        text(graphics, status.title(), RIGHT + 8, 50, status == SteamMachine.Status.WORKING ? FactoryWindowStyle.ACCENT : FactoryWindowStyle.MUTED);
        if (menu.kind == MachineKind.BOILER) {
            hint(graphics, 0, new ItemStack(Items.COAL));
            text(graphics, Component.translatable("menu.conveyance.fluid_fuel", FactoryTooltips.number(menu.fuelMJ())), RIGHT + 32, 74, FactoryWindowStyle.MUTED);
            buffer(graphics, FluidKind.WATER, menu.water(), 200, 100, 0xFF4D9ECC);
            buffer(graphics, FluidKind.STEAM, menu.steam(), 200, 136, 0xFFE9B561);
            text(graphics, Component.translatable("menu.conveyance.steam_rate", FactoryTooltips.number(menu.flow())), RIGHT + 8, 174, FactoryWindowStyle.MUTED);
            text(graphics, Component.translatable("menu.conveyance.fluid_boiler_ports"), RIGHT + 8, 198, FactoryWindowStyle.MUTED);
        } else if (menu.kind == MachineKind.STEAM_ENGINE) {
            buffer(graphics, FluidKind.STEAM, menu.steam(), 200, 82, 0xFFE9B561);
            text(graphics, Component.translatable("menu.conveyance.generator_output", FactoryTooltips.number(menu.outputKW())), RIGHT + 8, 124, FactoryWindowStyle.TEXT);
            text(graphics, Component.translatable("menu.conveyance.steam_consumption", FactoryTooltips.number(menu.flow())), RIGHT + 8, 150, FactoryWindowStyle.MUTED);
            text(graphics, Component.translatable("menu.conveyance.fluid_engine_ports"), RIGHT + 8, 190, FactoryWindowStyle.MUTED);
        } else {
            buffer(graphics, FluidKind.WATER, menu.water(), 100, 82, 0xFF4D9ECC);
            text(graphics, Component.translatable("menu.conveyance.water_rate", FactoryTooltips.number(menu.flow())), RIGHT + 8, 124, FactoryWindowStyle.TEXT);
            text(graphics, Component.translatable("menu.conveyance.fluid_pump_ports"), RIGHT + 8, 170, FactoryWindowStyle.MUTED);
        }
    }
}
