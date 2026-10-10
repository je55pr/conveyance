package dev.conveyance.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.conveyance.client.OverheadView;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
abstract class OverheadRendererMixin {
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void conveyance$hideHand(PoseStack pose, Camera camera, float partialTick, CallbackInfo callback) {
        if (OverheadView.controlsCamera()) callback.cancel();
    }
    @Inject(method = {"bobView", "bobHurt"}, at = @At("HEAD"), cancellable = true)
    private void conveyance$stableCamera(PoseStack pose, float partialTick, CallbackInfo callback) {
        if (OverheadView.controlsCamera()) callback.cancel();
    }
    @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void conveyance$hideNativeOutline(CallbackInfoReturnable<Boolean> callback) {
        if (OverheadView.controlsCamera()) callback.setReturnValue(false);
    }
}
