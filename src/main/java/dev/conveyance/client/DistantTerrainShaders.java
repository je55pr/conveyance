package dev.conveyance.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.conveyance.Conveyance;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DistantTerrainShaders {
    @SubscribeEvent public static void shaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Conveyance.MOD_ID, "distant_terrain"),
                DefaultVertexFormat.POSITION_TEX_COLOR), loaded -> {
            DistantTerrainRenderer.shader = loaded;
            Minecraft.getInstance().execute(DistantTerrainRenderer::reload);
        });
    }
    private DistantTerrainShaders() {}
}
