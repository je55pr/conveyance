package dev.conveyance.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.conveyance.Conveyance;
import dev.conveyance.factory.FactoryContent;
import dev.conveyance.factory.JunctionEntity;
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
public final class JunctionRenderer implements BlockEntityRenderer<JunctionEntity> {
    private final ItemRenderer items;
    public JunctionRenderer(BlockEntityRendererProvider.Context context) { items = context.getItemRenderer(); }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(FactoryContent.JUNCTION_ENTITY.get(), JunctionRenderer::new);
    }
    @Override public void render(JunctionEntity junction, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        for (int slot = 0; slot < JunctionEntity.CAPACITY; slot++) {
            if (junction.item(slot).isEmpty()) continue;
            pose.pushPose();
            pose.translate(junction.renderX(slot, partialTick), 0.54, junction.renderZ(slot, partialTick));
            pose.scale(0.38F, 0.38F, 0.38F);
            items.renderStatic(junction.item(slot), ItemDisplayContext.GROUND, light, overlay, pose, buffers, junction.getLevel(), slot);
            pose.popPose();
        }
    }
}
