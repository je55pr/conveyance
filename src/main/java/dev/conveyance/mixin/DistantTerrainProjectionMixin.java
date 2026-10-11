package dev.conveyance.mixin;

import dev.conveyance.client.DistantTerrainRenderer;
import dev.conveyance.client.OverheadView;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
abstract class DistantTerrainProjectionMixin {
    @Inject(method = "getDepthFar", at = @At("RETURN"), cancellable = true)
    private void conveyance$distantProjection(CallbackInfoReturnable<Float> callback) {
        if (DistantTerrainRenderer.active()) callback.setReturnValue(Math.max(callback.getReturnValue(), DistantTerrainRenderer.DISTANCE + 256F + OverheadView.cameraDistance()));
        else if (OverheadView.controlsCamera()) callback.setReturnValue(Math.max(callback.getReturnValue(), 256F + OverheadView.cameraDistance()));
    }
    @ModifyArg(method = "getProjectionMatrix", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;setPerspective(FFFF)Lorg/joml/Matrix4f;", remap = false), index = 2)
    private float conveyance$overviewNearPlane(float near) {
        // A first-person near plane loses depth precision when the ground is thousands of blocks away.
        return Math.max(near, (OverheadView.cameraDistance() - 256F) * .25F);
    }
}
