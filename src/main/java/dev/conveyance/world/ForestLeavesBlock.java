package dev.conveyance.world;

import dev.conveyance.factory.FactoryContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Decorative foliage: no decay ticks, saplings, fruit or inventory drops. */
public final class ForestLeavesBlock extends Block {
    // Offsets -3..3 are stored as 0..6; IntegerProperty forbids negative values.
    public static final IntegerProperty ROOT_X = IntegerProperty.create("root_x", 0, 6);
    public static final IntegerProperty ROOT_Z = IntegerProperty.create("root_z", 0, 6);
    public ForestLeavesBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ROOT_X, 3).setValue(ROOT_Z, 3).setValue(ForestLogBlock.LAYER, 0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(ROOT_X, ROOT_Z, ForestLogBlock.LAYER); }
    public static BlockState ownedBy(BlockPos leaf, BlockPos root) {
        return FactoryContent.TREE_LEAVES.get().defaultBlockState().setValue(ROOT_X, root.getX() - leaf.getX() + 3)
                .setValue(ROOT_Z, root.getZ() - leaf.getZ() + 3).setValue(ForestLogBlock.LAYER, leaf.getY() - root.getY());
    }
    public static BlockPos root(BlockPos leaf, BlockState state) { return leaf.offset(state.getValue(ROOT_X) - 3, -state.getValue(ForestLogBlock.LAYER), state.getValue(ROOT_Z) - 3); }
}
