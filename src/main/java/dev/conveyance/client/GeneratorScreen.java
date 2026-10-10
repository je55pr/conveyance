package dev.conveyance.client;

import dev.conveyance.Conveyance;
import dev.conveyance.factory.FactoryContent;
import dev.conveyance.factory.GeneratorMenu;
import dev.conveyance.factory.MachineEntity;
import dev.conveyance.player.Technology;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GeneratorScreen extends FactoryWindowScreen<GeneratorMenu> {
    public GeneratorScreen(GeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(FactoryContent.GENERATOR_MENU.get(), GeneratorScreen::new);
            MenuScreens.register(FactoryContent.ASSEMBLER_MENU.get(), ProcessingScreen::new);
            MenuScreens.register(FactoryContent.FURNACE_MENU.get(), ProcessingScreen::new);
            MenuScreens.register(FactoryContent.PLAYER_WORKBENCH_MENU.get(), PlayerWorkbenchScreen::new);
            MenuScreens.register(FactoryContent.LAB_MENU.get(), GeneratorScreen::new);
            MenuScreens.register(FactoryContent.WOOD_CRATE_MENU.get(), CrateScreen::new);
            MenuScreens.register(FactoryContent.IRON_CRATE_MENU.get(), CrateScreen::new);
        });
    }
    @Override protected int compactMachineWidth() { return FactoryWindowStyle.FUEL_PANEL_WIDTH; }
    @Override protected Component machinePanelTitle() {
        return Component.translatable(menu.lab() ? "menu.conveyance.science" : "menu.conveyance.fuel");
    }
    @Override protected void renderMachine(GuiGraphics graphics, float partialTick) {
        if (menu.lab()) {
            hint(graphics, 0, new ItemStack(FactoryContent.RED_SCIENCE_ITEM.get()));
            hint(graphics, 1, new ItemStack(FactoryContent.GREEN_SCIENCE_ITEM.get()));
            Technology tech = Technology.byId(menu.researchTarget());
            text(graphics, tech == null ? Component.translatable("research.conveyance.idle") : tech.shortName(), RIGHT + 8, 102, FactoryWindowStyle.TEXT);
            text(graphics, Component.literal(menu.scienceDone() + " / " + menu.scienceRequired()), RIGHT + 8, 130, FactoryWindowStyle.MUTED);
            FactoryWindowStyle.progress(graphics, leftPos + RIGHT + 8, topPos + 148, machineWidth() - 16, 6,
                    menu.scienceProgress() / 24F, FactoryWindowStyle.ACCENT);
        } else {
            hint(graphics, 0, new ItemStack(net.minecraft.world.item.Items.COAL));
            text(graphics, Component.translatable("menu.conveyance.fuel_seconds", (menu.fuelRemaining() + 19) / 20), RIGHT + 32, 74, FactoryWindowStyle.MUTED);
            var status = menu.minerStatus();
            if (status != null) {
                text(graphics, status.label(), RIGHT + 8, 112, status == MachineEntity.MiningStatus.MINING ? FactoryWindowStyle.ACCENT : FactoryWindowStyle.WARNING);
                FactoryWindowStyle.progress(graphics, leftPos + RIGHT + 8, topPos + 140, machineWidth() - 16, 6,
                        menu.miningProgress() / 24F, FactoryWindowStyle.ACCENT);
            } else if (menu.generator()) {
                text(graphics, menu.generatorOutput() == 0 ? Component.translatable("menu.conveyance.generator_idle")
                        : Component.translatable("menu.conveyance.generator_output", menu.generatorOutput()), RIGHT + 8, 112,
                        menu.generatorOutput() == 0 ? FactoryWindowStyle.MUTED : FactoryWindowStyle.ACCENT);
            }
        }
    }
    @Override protected void renderMachineTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        var status = menu.minerStatus();
        if (status != null && mouseX >= leftPos + RIGHT + 8 && mouseX < leftPos + imageWidth - 8
                && mouseY >= topPos + 108 && mouseY < topPos + 148)
            graphics.renderTooltip(font, font.split(status.description(), 220), mouseX, mouseY);
    }
}
