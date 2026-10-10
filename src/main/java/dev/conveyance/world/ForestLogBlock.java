package dev.conveyance.world;

import dev.conveyance.factory.FactoryContent;
import dev.conveyance.pollution.PollutionData;
import net.minecraft.server.level.ServerLevel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Tree coordinates live in block states, so forests need no ticking block entities. */
public final class ForestLogBlock extends Block {
    public static final IntegerProperty LAYER = IntegerProperty.create("layer", 0, 11);
    public static final IntegerProperty HEIGHT = IntegerProperty.create("height", 7, 12);
    public static final IntegerProperty CLEARANCE = IntegerProperty.create("clearance", 2, 3);
    public ForestLogBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LAYER, 0).setValue(HEIGHT, 7).setValue(CLEARANCE, 3));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(LAYER, HEIGHT, CLEARANCE); }
    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) { return new ItemStack(FactoryContent.WOOD_ITEM.get()); }
    public static int logCount(Level level, BlockPos broken, BlockState state) {
        BlockPos root = broken.below(state.getValue(LAYER));
        int wood = 0;
        for (int layer = 0; layer < 12; layer++) {
            BlockPos pos = root.above(layer);
            if (!level.hasChunkAt(pos)) continue;
            BlockState log = level.getBlockState(pos);
            if (log.is(FactoryContent.TREE_LOG.get()) && log.getValue(LAYER) == layer) wood++;
        }
        return wood;
    }
    public static void removeTree(Level level, BlockPos broken, BlockState state) {
        BlockPos root = broken.below(state.getValue(LAYER));
        for (int layer = 0; layer < 12; layer++) {
            BlockPos pos = root.above(layer);
            if (!level.hasChunkAt(pos)) continue;
            BlockState log = level.getBlockState(pos);
            if (log.is(FactoryContent.TREE_LOG.get()) && log.getValue(LAYER) == layer) {
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
        }
        List<StandingTree> neighbours = new ArrayList<>();
        // Generated trees all start on the base plain. Only roots within six
        // blocks can have a canopy overlapping this one.
        for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) {
            BlockPos neighbour = root.offset(dx, 0, dz);
            if (neighbour.equals(root) || !level.hasChunkAt(neighbour)) continue;
            BlockState log = level.getBlockState(neighbour);
            if (log.is(FactoryContent.TREE_LOG.get()) && log.getValue(LAYER) == 0)
                neighbours.add(new StandingTree(neighbour, log.getValue(HEIGHT), log.getValue(CLEARANCE)));
        }
        for (BlockPos pos : BlockPos.betweenClosed(root.offset(-3, 0, -3), root.offset(3, 11, 3))) {
            if (!level.hasChunkAt(pos)) continue;
            BlockState leaf = level.getBlockState(pos);
            if (leaf.is(FactoryContent.TREE_LEAVES.get()) && ForestLeavesBlock.root(pos, leaf).equals(root)) {
                level.setBlock(pos, replacementLeaves(pos, neighbours), 3);
            }
        }
        if (level instanceof ServerLevel server && level.dimension() == Level.OVERWORLD) {
            var chunk = server.getChunkSource().getChunkNow(Math.floorDiv(root.getX(), 16), Math.floorDiv(root.getZ(), 16));
            if (chunk != null) PollutionData.get(server).observe(chunk, server.getGameTime());
        }
    }
    // A leaf shared by two canopies is reassigned to a standing neighbour when
    // its owner is felled. Nearby trees keep their complete crowns.
    private record StandingTree(BlockPos root, int height, int clearance) {}
    private static BlockState replacementLeaves(BlockPos leaf, List<StandingTree> neighbours) {
        for (StandingTree tree : neighbours) {
            int dx = leaf.getX() - tree.root.getX(), dz = leaf.getZ() - tree.root.getZ();
            if (Math.abs(dx) <= 3 && Math.abs(dz) <= 3 && ForestTerrain.inCanopy(dx,
                    leaf.getY() - tree.root.getY(), dz, tree.height, tree.clearance))
                return ForestLeavesBlock.ownedBy(leaf, tree.root);
        }
        return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    }
}
