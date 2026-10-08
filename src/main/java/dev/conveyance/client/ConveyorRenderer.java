package dev.conveyance.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.conveyance.Conveyance;
import dev.conveyance.factory.ConveyorEntity;
import dev.conveyance.factory.FactoryContent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ConveyorRenderer implements BlockEntityRenderer<ConveyorEntity> {
    private final ItemRenderer items;
    public ConveyorRenderer(BlockEntityRendererProvider.Context context) { items = context.getItemRenderer(); }

    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(FactoryContent.CONVEYOR_ENTITY.get(), ConveyorRenderer::new);
    }

    @Override public void render(ConveyorEntity belt, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        for (int slot = 0; slot < ConveyorEntity.CAPACITY; slot++) {
            if (belt.item(slot).isEmpty()) continue;
            pose.pushPose();
            pose.translate(belt.renderX(slot, partialTick), 0.16, belt.renderZ(slot, partialTick));
            pose.scale(0.45F, 0.45F, 0.45F);
            items.renderStatic(belt.item(slot), ItemDisplayContext.GROUND, light, overlay, pose, buffers,
                    belt.getLevel(), slot);
            pose.popPose();
        }
    }
}
