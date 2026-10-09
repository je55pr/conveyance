package dev.conveyance.client;

import dev.conveyance.Conveyance;
import dev.conveyance.factory.FactoryContent;
import dev.conveyance.factory.GeneratorMenu;
import dev.conveyance.factory.MachineEntity;
import dev.conveyance.player.ResearchState;
import dev.conveyance.player.Technology;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GeneratorScreen extends AbstractContainerScreen<GeneratorMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");
    public GeneratorScreen(GeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 131;
        inventoryLabelY = 37;
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
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, 35);
        graphics.fill(leftPos + 7, topPos + 17, leftPos + 169, topPos + 35, 0xFFC6C6C6);
        if (menu.lab()) {
            graphics.blit(TEXTURE, leftPos + 61, topPos + 17, 7, 17, 18, 18);
            graphics.blit(TEXTURE, leftPos + 97, topPos + 17, 7, 17, 18, 18);
            if (!menu.getSlot(0).hasItem()) graphics.drawString(font, "R", leftPos + 67, topPos + 22, 0xFFAA4444, false);
            if (!menu.getSlot(1).hasItem()) graphics.drawString(font, "G", leftPos + 103, topPos + 22, 0xFF448844, false);
        } else graphics.blit(TEXTURE, leftPos + 79, topPos + 17, 7, 17, 18, 18);
        graphics.blit(TEXTURE, leftPos, topPos + 35, 0, 126, imageWidth, 96);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        var status = menu.minerStatus();
        if (status != null && mouseX >= leftPos + 104 && mouseX < leftPos + 170 && mouseY >= topPos + 17 && mouseY < topPos + 35)
            graphics.renderTooltip(font, font.split(status.description(), 220), mouseX, mouseY);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        if (!menu.lab()) {
            graphics.drawString(font, Component.translatable("menu.conveyance.fuel_seconds", (menu.fuelRemaining() + 19) / 20), 8, 20, 0x404040, false);
            var status = menu.minerStatus();
            if (status != null) {
                graphics.drawString(font, status.label(), 108, 20, status == MachineEntity.MiningStatus.MINING ? 0x226633 : 0x995522, false);
                graphics.fill(108, 31, 166, 34, 0xFF777777);
                graphics.fill(108, 31, 108 + 58 * menu.miningProgress() / 24, 34, 0xFF338844);
            }
            return;
        }
        Technology tech = Technology.byId(menu.researchTarget());
        graphics.drawString(font, tech == null ? Component.translatable("research.conveyance.idle") : tech.shortName(), 8, 20, 0x404040, false);
        graphics.drawString(font, menu.scienceDone() + "/" + menu.scienceRequired(), 124, 20, 0x404040, false);
        graphics.fill(8, 31, 57, 34, 0xFF666666);
        graphics.fill(8, 31, 8 + 49 * menu.scienceProgress() / 24, 34, tech != null && tech.green ? 0xFF448844 : 0xFFBB4444);
    }
}
