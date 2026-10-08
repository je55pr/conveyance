package dev.conveyance.client;

import dev.conveyance.Conveyance;
import dev.conveyance.factory.FactoryContent;
import dev.conveyance.factory.GeneratorMenu;
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
        });
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, 35);
        graphics.fill(leftPos + 7, topPos + 17, leftPos + 169, topPos + 35, 0xFFC6C6C6);
        graphics.blit(TEXTURE, leftPos + 79, topPos + 17, 7, 17, 18, 18);
        graphics.blit(TEXTURE, leftPos, topPos + 35, 0, 126, imageWidth, 96);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
