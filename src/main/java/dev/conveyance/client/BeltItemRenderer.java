package dev.conveyance.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;

/** Render belt cargo like inventory icons, with their front always facing the live camera. */
final class BeltItemRenderer {
    private static final Quaternionf FRONT = Axis.YP.rotationDegrees(180);
    private static final float SIZE = 0.19F;
    static void render(ItemRenderer renderer, ItemStack item, Level level, int seed, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0, 0.07, 0);
        pose.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        pose.mulPose(FRONT);
        pose.scale(SIZE, SIZE, SIZE);
        renderer.renderStatic(item, ItemDisplayContext.GUI, light, overlay, pose, buffers, level, seed);
        pose.popPose();
    }
    private BeltItemRenderer() {}
}
