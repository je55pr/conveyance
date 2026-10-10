package dev.conveyance;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import dev.conveyance.world.WorldGeneration;
import dev.conveyance.factory.FactoryContent;
import dev.conveyance.factory.InventoryBreaking;
import dev.conveyance.factory.FactoryTooltips;
import dev.conveyance.player.InventoryNetwork;
import dev.conveyance.player.PlayerInventoryEvents;
import dev.conveyance.player.StarterSupplies;
import dev.conveyance.player.TerrainHarvest;
import dev.conveyance.player.ResearchState;
import dev.conveyance.map.MapExploration;
import dev.conveyance.map.MapNetwork;

@Mod(Conveyance.MOD_ID)
public final class Conveyance {
    public static final String MOD_ID = "conveyance";

    public Conveyance(FMLJavaModLoadingContext context) {
        MinecraftForge.EVENT_BUS.addListener(HungerRemoval::onPlayerTick);
        MinecraftForge.EVENT_BUS.register(BaselineRules.class);
        MinecraftForge.EVENT_BUS.register(RecipePolicy.class);
        context.getModEventBus().addListener(ItemPolicy::onCreativeContents);
        WorldGeneration.register(context.getModEventBus());
        FactoryContent.register(context.getModEventBus());
        InventoryNetwork.register();
        MapNetwork.register();
        MinecraftForge.EVENT_BUS.register(MapExploration.class);
        MinecraftForge.EVENT_BUS.addListener(ResearchState::registerCommands);
        MinecraftForge.EVENT_BUS.register(PlayerInventoryEvents.class);
        MinecraftForge.EVENT_BUS.register(StarterSupplies.class);
        MinecraftForge.EVENT_BUS.register(TerrainHarvest.class);
        MinecraftForge.EVENT_BUS.register(InventoryBreaking.class);
        MinecraftForge.EVENT_BUS.register(FactoryTooltips.class);
        MinecraftForge.EVENT_BUS.register(WorldGeneration.class);
        LogUtils.getLogger().info("Conveyance loaded: stripped-back industrial baseline enabled.");
    }
}

