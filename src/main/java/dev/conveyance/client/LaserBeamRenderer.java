package dev.conveyance.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.conveyance.Conveyance;
import dev.conveyance.combat.CombatNetwork;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Short-lived, full-bright beams are client graphics, never world entities. */
@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, value = Dist.CLIENT)
public final class LaserBeamRenderer {
    private record Beam(Level level, Vec3 from, Vec3 to, long started) {}
    private static final List<Beam> beams = new ArrayList<>();
    public static void accept(CombatNetwork.LaserBeam message) {
        var level = Minecraft.getInstance().level; if (level == null) return;
        if (beams.size() >= 512) beams.remove(0);
        beams.add(new Beam(level, message.from(), message.to(), level.getGameTime()));
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { beams.clear(); }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || beams.isEmpty()) return;
        var game = Minecraft.getInstance(); if (game.level == null) { beams.clear(); return; }
        long now = game.level.getGameTime(); beams.removeIf(beam -> beam.level() != game.level || now - beam.started() >= 5);
        if (beams.isEmpty()) return;
        var camera = event.getCamera().getPosition(); var pose = event.getPoseStack();
        pose.pushPose(); pose.translate(-camera.x, -camera.y, -camera.z);
        var buffers = game.renderBuffers().bufferSource(); var vertices = buffers.getBuffer(RenderType.lightning());
        for (Beam beam : beams) {
            Vec3 side = beam.to().subtract(beam.from()).cross(camera.subtract(beam.from())).normalize();
            if (side.lengthSqr() < .000001) side = new Vec3(1, 0, 0);
            float alpha = (5 - (now - beam.started())) / 5F;
            ribbon(pose, vertices, beam, side.scale(.12), 1, .05F, .05F, alpha * .35F);
            ribbon(pose, vertices, beam, side.scale(.045), 1, .55F, .45F, alpha);
        }
        buffers.endBatch(RenderType.lightning()); pose.popPose();
    }
    private static void ribbon(PoseStack pose, VertexConsumer vertices, Beam beam, Vec3 side, float r, float g, float b, float a) {
        for (Vec3 point : new Vec3[] {beam.from().subtract(side), beam.to().subtract(side), beam.to().add(side), beam.from().add(side)})
            vertices.vertex(pose.last().pose(), (float) point.x, (float) point.y, (float) point.z).color(r, g, b, a).endVertex();
    }
    private LaserBeamRenderer() {}
}
