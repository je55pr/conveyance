package dev.conveyance.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.conveyance.Conveyance;
import dev.conveyance.factory.FactoryContent;
import dev.conveyance.factory.PowerPoleEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PowerPoleRenderer implements BlockEntityRenderer<PowerPoleEntity> {
    public PowerPoleRenderer(BlockEntityRendererProvider.Context context) {}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(FactoryContent.POWER_POLE_ENTITY.get(), PowerPoleRenderer::new);
    }
    @Override public void render(PowerPoleEntity pole, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockPos origin = pole.getBlockPos();
        VertexConsumer wire = buffers.getBuffer(RenderType.lines());
        for (BlockPos target : pole.connections()) {
            if (origin.asLong() >= target.asLong()) continue;
            double x = target.getX() - origin.getX(), y = target.getY() - origin.getY(), z = target.getZ() - origin.getZ();
            double sag = Math.min(0.75, Math.sqrt(x * x + z * z) * 0.04);
            for (int segment = 0; segment < 12; segment++) {
                Vec3 start = point(x, y, z, sag, segment / 12.0), end = point(x, y, z, sag, (segment + 1) / 12.0);
                Vec3 normal = end.subtract(start).normalize();
                vertex(wire, pose, start, normal);
                vertex(wire, pose, end, normal);
            }
        }
    }
    private static Vec3 point(double x, double y, double z, double sag, double t) {
        return new Vec3(0.5 + x * t, 3.95 + y * t - 4 * sag * t * (1 - t), 0.5 + z * t);
    }
    private static void vertex(VertexConsumer wire, PoseStack pose, Vec3 point, Vec3 normal) {
        wire.vertex(pose.last().pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(165, 100, 45, 255).normal(pose.last().normal(), (float) normal.x, (float) normal.y, (float) normal.z).endVertex();
    }
}
