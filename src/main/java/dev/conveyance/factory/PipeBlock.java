package dev.conveyance.factory;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class PipeBlock extends FactoryBlock {
    public static final IntegerProperty FLUID = IntegerProperty.create("fluid", 0, 6);
    public static final Map<Direction, BooleanProperty> CONNECTIONS = java.util.Arrays.stream(Direction.values())
            .collect(java.util.stream.Collectors.toMap(side -> side, side -> BooleanProperty.create(side.getName())));
    private static final VoxelShape CORE = box(5, 5, 5, 11, 11, 11);
    public PipeBlock(Properties properties) {
        super(properties); BlockState state = stateDefinition.any().setValue(FLUID, 0);
        for (var property : CONNECTIONS.values()) state = state.setValue(property, false);
        registerDefaultState(state);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FLUID); for (var property : CONNECTIONS.values()) builder.add(property);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!FluidNetwork.canConnect(context.getLevel(), PipeEntity.ports(context.getClickedPos(), new FluidTank(PipeEntity.CAPACITY, FluidKind.EMPTY)))) return null;
        return connections(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }
    private static BlockState connections(Level level, BlockPos pos, BlockState state) {
        for (var entry : CONNECTIONS.entrySet()) state = state.setValue(entry.getValue(),
                FluidNetwork.hasPort(level, pos.relative(entry.getKey()), entry.getKey().getOpposite()));
        return state;
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbour, BlockPos from, boolean moving) {
        BlockState next = connections(level, pos, state);
        if (next != state) { FluidNetwork.invalidate(level); level.setBlockAndUpdate(pos, next); }
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock())) FluidNetwork.invalidate(level);
        else if (level.isClientSide && state.getValue(FLUID) != next.getValue(FLUID)) FluidNetwork.invalidate(level);
        super.onRemove(state, level, pos, next, moving);
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PipeEntity(pos, state); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        for (var entry : CONNECTIONS.entrySet()) if (state.getValue(entry.getValue())) {
            Direction side = entry.getKey();
            shape = Shapes.or(shape, box(side == Direction.WEST ? 0 : 5, side == Direction.DOWN ? 0 : 5, side == Direction.NORTH ? 0 : 5,
                    side == Direction.EAST ? 16 : 11, side == Direction.UP ? 16 : 11, side == Direction.SOUTH ? 16 : 11));
        }
        return shape;
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof net.minecraft.world.item.BlockItem) return InteractionResult.PASS;
        if (!level.isClientSide) FluidNetwork.showStatus(level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
