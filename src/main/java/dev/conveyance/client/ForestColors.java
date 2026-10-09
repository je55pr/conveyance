package dev.conveyance.client;

import dev.conveyance.Conveyance;
import dev.conveyance.factory.FactoryContent;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ForestColors {
    @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> level == null || pos == null ? 0x929B59
                : BiomeColors.getAverageFoliageColor(level, pos), FactoryContent.TREE_LEAVES.get());
    }
    private ForestColors() {}
}
