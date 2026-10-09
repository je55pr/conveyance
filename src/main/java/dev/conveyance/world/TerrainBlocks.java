package dev.conveyance.world;

import dev.conveyance.Conveyance;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Generated ground has no BlockItem. Construction resources retain ordinary Minecraft IDs. */
public final class TerrainBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Conveyance.MOD_ID);
    private static final Map<Block, RegistryObject<Block>> VARIANTS = new LinkedHashMap<>();
    public static final class TerrainBlock extends Block {
        final Block material;
        TerrainBlock(Block material) {
            super(BlockBehaviour.Properties.copy(material).strength(-1, 3_600_000).noLootTable());
            this.material = material;
        }
    }
    static {
        add("grass_block", Blocks.GRASS_BLOCK); add("dirt", Blocks.DIRT); add("coarse_dirt", Blocks.COARSE_DIRT);
        add("sand", Blocks.SAND); add("stone", Blocks.STONE); add("andesite", Blocks.ANDESITE);
        add("granite", Blocks.GRANITE); add("bedrock", Blocks.BEDROCK);
        add("iron_ore", Blocks.IRON_ORE); add("copper_ore", Blocks.COPPER_ORE); add("coal_ore", Blocks.COAL_ORE);
    }
    private static void add(String name, Block material) { VARIANTS.put(material, BLOCKS.register("terrain_" + name, () -> new TerrainBlock(material))); }
    public static void register(IEventBus bus) { BLOCKS.register(bus); }
    public static boolean natural(BlockState state) { return state.getBlock() instanceof TerrainBlock; }
    public static BlockState material(BlockState state) { return state.getBlock() instanceof TerrainBlock terrain ? terrain.material.defaultBlockState() : state; }
    public static BlockState generated(BlockState material) {
        var variant = VARIANTS.get(material.getBlock());
        return variant == null ? material : variant.get().defaultBlockState();
    }
    public static Block grass() { return VARIANTS.get(Blocks.GRASS_BLOCK).get(); }
    private TerrainBlocks() {}
}
