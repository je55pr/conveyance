package dev.conveyance.client;

import com.mojang.blaze3d.shaders.FogShape;
import dev.conveyance.Conveyance;
import dev.conveyance.pollution.PollutionData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Cosmetic smog uses the same server field as the map and future enemy AI. */
@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, value = Dist.CLIENT)
public final class PollutionAtmosphere {
    private static float strength;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var game = Minecraft.getInstance();
        if (game.level == null || game.level.dimension() != Level.OVERWORLD) { strength = 0; return; }
        var pos = game.gameRenderer.getMainCamera().getPosition();
        double amount = PollutionData.clientSample(pos.x, pos.z);
        double target = Math.max(0, Math.min(1, (Math.log1p(amount) - Math.log1p(5)) / (Math.log1p(80) - Math.log1p(5))));
        strength += (target - strength) * 0.08F;
    }
    public static Vec3 tint(Vec3 color) {
        if (strength < 0.001F) return color;
        double brightness = Math.max(color.x, Math.max(color.y, color.z)), mix = strength * 0.7;
        return color.scale(1 - mix).add(brightness * 0.42 * mix, brightness * 0.68 * mix, brightness * 0.24 * mix);
    }
    @SubscribeEvent public static void color(ViewportEvent.ComputeFogColor event) {
        if (event.getCamera().getFluidInCamera() != FogType.NONE) return;
        Vec3 color = tint(new Vec3(event.getRed(), event.getGreen(), event.getBlue()));
        event.setRed((float) color.x); event.setGreen((float) color.y); event.setBlue((float) color.z);
    }
    @SubscribeEvent public static void fog(ViewportEvent.RenderFog event) {
        if (strength < 0.001F || event.getType() != FogType.NONE) return;
        float original = event.getFarPlaneDistance(), target = Math.min(original, 40);
        float far = original + (target - original) * strength;
        event.setFarPlaneDistance(far);
        event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), far * (0.75F - 0.6F * strength)));
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { strength = 0; PollutionData.resetClient(); }
    private PollutionAtmosphere() {}
}
