package dev.conveyance;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import dev.conveyance.world.WorldGeneration;

@Mod(Conveyance.MOD_ID)
public final class Conveyance {
    public static final String MOD_ID = "conveyance";

    public Conveyance(FMLJavaModLoadingContext context) {
        MinecraftForge.EVENT_BUS.addListener(HungerRemoval::onPlayerTick);
        MinecraftForge.EVENT_BUS.register(BaselineRules.class);
        MinecraftForge.EVENT_BUS.register(RecipePolicy.class);
        context.getModEventBus().addListener(ItemPolicy::onCreativeContents);
        WorldGeneration.register(context.getModEventBus());
        MinecraftForge.EVENT_BUS.register(WorldGeneration.class);
        LogUtils.getLogger().info("Conveyance loaded: stripped-back industrial baseline enabled.");
    }
}

