package dev.conveyance.mixin;

import dev.conveyance.client.OverheadView;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
abstract class OverheadCameraMixin {
    @Shadow private boolean detached;
    @Shadow protected abstract void setPosition(Vec3 position);
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Inject(method = "setup", at = @At("TAIL"))
    private void conveyance$overhead(BlockGetter level, Entity entity, boolean thirdPerson, boolean reverse, float partialTick, CallbackInfo callback) {
        if (!OverheadView.controlsCamera()) return;
        Camera camera = (Camera) (Object) this;
        var pose = OverheadView.cameraPose(new OverheadView.CameraPose(camera.getPosition(), camera.getYRot(), camera.getXRot()), partialTick);
        detached = pose.position().distanceToSqr(entity.getEyePosition(partialTick)) > 0.25;
        setRotation(pose.yaw(), pose.pitch());
        setPosition(pose.position());
    }
}
