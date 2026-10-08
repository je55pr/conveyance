package dev.conveyance.world;

import com.mojang.serialization.Codec;
import dev.conveyance.Conveyance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.DeferredRegister;

public final class WorldGeneration {
    public static final ResourceKey<WorldPreset> PRESET = ResourceKey.create(Registries.WORLD_PRESET,
            ResourceLocation.fromNamespaceAndPath(Conveyance.MOD_ID, "terraced"));
    private static final DeferredRegister<Codec<? extends ChunkGenerator>> GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, Conveyance.MOD_ID);

    static { GENERATORS.register("terraced", () -> TerracedChunkGenerator.CODEC); }
    private WorldGeneration() {}

    public static void register(IEventBus bus) { GENERATORS.register(bus); }

    @SubscribeEvent
    public static void onCreateSpawn(LevelEvent.CreateSpawnPosition event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD
                && level.getChunkSource().getGenerator() instanceof TerracedChunkGenerator) {
            level.setDefaultSpawnPos(new BlockPos(0, TerracedTerrain.START_HEIGHT, 0), 0);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        var server = event.getServer();
        // Existing saves retain their saved generator. Minecraft's isolated
        // GameTest world also intentionally uses its own fixture generator.
        if (server instanceof GameTestServer || server.getWorldData().overworldData().isInitialized()) return;
        var overworld = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM).get(LevelStem.OVERWORLD);
        if (overworld == null || !(overworld.generator() instanceof TerracedChunkGenerator)) {
            throw new IllegalStateException("New Conveyance worlds require the Conveyance terraced preset. "
                    + "Use level-type=conveyance:terraced when creating a dedicated server world.");
        }
    }
}
