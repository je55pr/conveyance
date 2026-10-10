package dev.conveyance.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.conveyance.Conveyance;
import dev.conveyance.factory.ConveyorItem;
import dev.conveyance.factory.InserterBlock;
import dev.conveyance.factory.PlacementPreview;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.pipeline.VertexConsumerWrapper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, value = Dist.CLIENT)
public final class PlacementGhosts {
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft game = Minecraft.getInstance();
        if (game.level == null || game.player == null || game.screen != null && !(game.screen instanceof OverheadBuildScreen) || game.player.isSpectator()) return;
        PlacementPreview.Plan plan;
        if (game.screen instanceof OverheadBuildScreen overhead) plan = overhead.plan();
        else {
            InteractionHand hand = game.player.getMainHandItem().getItem() instanceof BlockItem ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            var item = game.player.getItemInHand(hand);
            if (!(item.getItem() instanceof BlockItem)) return;
            BlockHitResult hit = game.hitResult instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK ? block : null;
            if (hit == null && item.getItem() instanceof ConveyorItem) hit = ConveyorItem.extension(game.level, game.player, item);
            if (hit == null) return;
            plan = PlacementPreview.resolve(game.level, game.player, hand, hit);
        }
        var pose = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        var buffers = game.renderBuffers().bufferSource();
        MultiBufferSource ghost = type -> new VertexConsumerWrapper(buffers.getBuffer(RenderType.translucent())) {
            @Override public VertexConsumer color(int red, int green, int blue, int alpha) { return super.color(red, green, blue, Math.min(alpha, 85)); }
        };
        pose.pushPose(); pose.translate(-camera.x, -camera.y, -camera.z);
        if (game.screen instanceof OverheadBuildScreen overhead) {
            int x = game.player.chunkPosition().x * 16, z = game.player.chunkPosition().z * 16;
            LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()),
                    new AABB(x - 32, OverheadView.height + 0.002, z - 32, x + 48, OverheadView.height + 0.01, z + 48), 0.3F, 0.7F, 1F, 0.8F);
            if (overhead.target() != null) LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()),
                    new AABB(overhead.target()).inflate(0.003), 1F, 0.8F, 0.3F, 0.9F);
        }
        if (plan != null) for (var part : plan.parts()) {
            pose.pushPose(); pose.translate(part.pos().getX(), part.pos().getY(), part.pos().getZ());
            game.getBlockRenderer().renderSingleBlock(part.state(), pose, ghost, 15728880, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.translucent());
            if (part.state().getBlock() instanceof InserterBlock) InserterRenderer.preview(part.state(), pose, ghost, 15728880, OverlayTexture.NO_OVERLAY);
            MachineModelRenderer.preview(part.state(), pose, ghost, 15728880, OverlayTexture.NO_OVERLAY);
            pose.popPose();
            LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()), new AABB(part.pos()).inflate(0.002),
                    plan.valid() ? 0.35F : 1F, plan.valid() ? 0.9F : 0.25F, 0.35F, 0.8F);
        }
        buffers.endBatch(RenderType.translucent()); buffers.endBatch(RenderType.lines()); pose.popPose();
    }
    private PlacementGhosts() {}
}
