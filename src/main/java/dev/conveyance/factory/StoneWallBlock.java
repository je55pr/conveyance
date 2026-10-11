package dev.conveyance.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.HashSet;
import java.util.Set;

public final class StoneWallBlock extends WallBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    private final DoubleBlockHalf shapeHalf;
    private record Removal(Level level, BlockPos root) {}
    private static final ThreadLocal<Set<Removal>> REMOVALS = ThreadLocal.withInitial(HashSet::new);
    public StoneWallBlock(Properties properties) {
        super(properties); shapeHalf = defaultBlockState().getValue(HALF);
        registerDefaultState(defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); builder.add(HALF); }
    public static BlockPos origin(BlockPos pos, BlockState state) { return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos; }
    private static BlockState fullSides(BlockState state) {
        for (var side : java.util.List.of(NORTH_WALL, EAST_WALL, SOUTH_WALL, WEST_WALL))
            if (state.getValue(side) != WallSide.NONE) state = state.setValue(side, WallSide.TALL);
        return state;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return fullSides(super.getStateForPlacement(context).setValue(HALF, DoubleBlockHalf.LOWER)); }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos other) {
        return fullSides(super.updateShape(state, side, neighbor, level, pos, other));
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return super.getShape(state.setValue(HALF, shapeHalf), level, pos, context); }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.joinUnoptimized(super.getCollisionShape(state.setValue(HALF, shapeHalf), level, pos, context), Shapes.block(), BooleanOp.AND);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide) dismantle(level, pos, state);
        super.onRemove(state, level, pos, next, moving);
    }
    static void dismantle(Level level, BlockPos pos, BlockState state) {
        var root = origin(pos, state); var removal = new Removal(level, root);
        if (!REMOVALS.get().add(removal)) return;
        try {
            for (int height = 1; height >= 0; height--) {
                var target = root.above(height); if (!level.hasChunkAt(target)) continue;
                var current = level.getBlockState(target);
                if (current.getBlock() instanceof StoneWallBlock && origin(target, current).equals(root)) level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
            }
        } finally { REMOVALS.get().remove(removal); }
    }
    @Override public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) { return .1F; }
}
