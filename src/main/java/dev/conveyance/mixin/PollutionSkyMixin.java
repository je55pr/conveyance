package dev.conveyance.mixin;

import dev.conveyance.client.PollutionAtmosphere;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
abstract class PollutionSkyMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void conveyance$pollutionSky(Vec3 position, float partialTick, CallbackInfoReturnable<Vec3> callback) {
        callback.setReturnValue(PollutionAtmosphere.tint(callback.getReturnValue()));
    }
}
