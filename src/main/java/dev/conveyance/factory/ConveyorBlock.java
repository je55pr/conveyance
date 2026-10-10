package dev.conveyance.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import java.util.List;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

public class ConveyorBlock extends FactoryBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public enum Turn implements StringRepresentable {
        STRAIGHT, LEFT, RIGHT;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    public static final EnumProperty<Turn> TURN = EnumProperty.create("turn", Turn.class);
    private static final VoxelShape NORTH_SOUTH = Block.box(2, 0, 0, 14, 1, 16);
    private static final VoxelShape EAST_WEST = Block.box(0, 0, 2, 16, 1, 14);
    private static final VoxelShape[][] CORNERS = corners();
    public ConveyorBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any().setValue(FACING, Direction.NORTH);
        registerDefaultState(state.hasProperty(TURN) ? state.setValue(TURN, Turn.STRAIGHT) : state);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, TURN); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return resolve(context.getLevel(), context.getClickedPos(), defaultBlockState().setValue(FACING, context.getHorizontalDirection()));
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Turn turn = state.getValue(TURN);
        return turn == Turn.STRAIGHT ? state.getValue(FACING).getAxis() == Direction.Axis.Z ? NORTH_SOUTH : EAST_WEST
                : CORNERS[turn == Turn.LEFT ? 0 : 1][state.getValue(FACING).get2DDataValue()];
    }
    private static VoxelShape[][] corners() {
        VoxelShape[][] shapes = new VoxelShape[2][4];
        for (int turn = 0; turn < 2; turn++) for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            Direction left = facing.getCounterClockWise();
            for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                double radius = Math.hypot(x + 0.5, z + 0.5);
                if (radius < 2 || radius > 14) continue;
                double lateral = (turn == 0 ? x : 15 - x) - 7.5, forward = 7.5 - z;
                double px = 8 - left.getStepX() * lateral + facing.getStepX() * forward;
                double pz = 8 - left.getStepZ() * lateral + facing.getStepZ() * forward;
                shape = Shapes.or(shape, Block.box(px - 0.5, 0, pz - 0.5, px + 0.5, 1, pz + 0.5));
            }
            shapes[turn][facing.get2DDataValue()] = shape.optimize();
        }
        return shapes;
    }
    private static boolean incoming(Level level, BlockPos pos, Direction side) {
        if (!level.hasChunkAt(pos.relative(side))) return false;
        ConveyorEntity belt = ConveyorEntity.outletAt(level, pos.relative(side));
        if (belt != null && belt.configured() && belt.outputPosition().equals(pos)) return true;
        BlockState state = level.getBlockState(pos.relative(side));
        return state.getBlock() instanceof JunctionBlock && JunctionBlock.port(state, side.getOpposite()) == JunctionBlock.Port.OUTPUT;
    }
    private static BlockState resolve(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        boolean rear = incoming(level, pos, facing.getOpposite());
        boolean left = incoming(level, pos, facing.getCounterClockWise()), right = incoming(level, pos, facing.getClockWise());
        return state.setValue(TURN, !rear && left != right ? left ? Turn.LEFT : Turn.RIGHT : Turn.STRAIGHT);
    }
    public static BlockState update(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ConveyorBlock) || state.getBlock() instanceof SlopeConveyorBlock) return state;
        BlockState next = resolve(level, pos, state);
        if (!state.equals(next)) level.setBlockAndUpdate(pos, next);
        return next;
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
        if (!level.isClientSide && !(this instanceof SlopeConveyorBlock)) update(level, pos);
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public void appendHoverText(ItemStack item, BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(this instanceof SlopeConveyorBlock ? "tooltip.conveyance.slope_conveyor" : "tooltip.conveyance.conveyor"));
        super.appendHoverText(item, level, tooltip, flag);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ConveyorEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, FactoryContent.CONVEYOR_ENTITY.get(), ConveyorEntity::tick);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        // Packed cargo is loaded after block placement; send it even when a full queue cannot move.
        if (!level.isClientSide) {
            BlockState next = this instanceof SlopeConveyorBlock ? state : update(level, pos);
            level.sendBlockUpdated(pos, next, next, Block.UPDATE_CLIENTS);
        }
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            if (!level.isClientSide) level.setBlockAndUpdate(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()));
        } else {
            if (held.getItem() instanceof BlockItem) return InteractionResult.PASS;
            if (!level.isClientSide) {
                ConveyorEntity belt = ConveyorEntity.at(level, pos);
                if (belt != null && belt.offer(held, belt.entrySide(), belt.nearestLane(hit.getLocation())) && !player.getAbilities().instabuild) held.shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) recover(level, pos);
        super.playerWillDestroy(level, pos, state, player);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide) recover(level, pos);
        super.onRemove(state, level, pos, next, moving);
    }
    private static void recover(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof ConveyorEntity belt) || belt.recovered) return;
        belt.recovered = true;
        ItemStack packed = new ItemStack(FactoryContent.CONVEYOR_ITEM.get());
        boolean occupied = false;
        for (int slot = 0; slot < ConveyorEntity.CAPACITY; slot++) occupied |= !belt.item(slot).isEmpty();
        if (occupied) BlockItem.setBlockEntityData(packed, FactoryContent.CONVEYOR_ENTITY.get(), belt.saveWithoutMetadata());
        Block.popResource(level, pos, packed);
    }
}
