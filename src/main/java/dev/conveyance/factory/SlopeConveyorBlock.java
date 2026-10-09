package dev.conveyance.factory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The block occupies the lower level. FACING points uphill, independently of item flow. */
public final class SlopeConveyorBlock extends ConveyorBlock {
    public enum Flow implements StringRepresentable {
        UNSET, UP, DOWN, STUCK_UP, STUCK_DOWN;
        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
        public boolean moving() { return this == UP || this == DOWN; }
        public boolean down() { return this == DOWN || this == STUCK_DOWN; }
    }
    public static final EnumProperty<Flow> FLOW = EnumProperty.create("flow", Flow.class);
    public SlopeConveyorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FLOW, Flow.UNSET));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, FLOW); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection()).setValue(FLOW, Flow.UNSET);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction high = state.getValue(FACING);
        VoxelShape result = Shapes.empty();
        for (int i = 0; i < 8; i++) {
            double near = i * 2, far = near + 2, y = i * 2;
            VoxelShape slice = switch (high) {
                case NORTH -> Block.box(2, y, 16 - far, 14, y + 1, 16 - near);
                case SOUTH -> Block.box(2, y, near, 14, y + 1, far);
                case EAST -> Block.box(near, y, 2, far, y + 1, 14);
                default -> Block.box(16 - far, y, 2, 16 - near, y + 1, 14);
            };
            result = Shapes.or(result, slice);
        }
        return result;
    }
    private record Input(boolean active, Direction side, boolean high) {}
    public static BlockState update(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SlopeConveyorBlock)) return state;
        BlockState next = resolve(level, pos, state);
        if (!next.equals(state)) level.setBlockAndUpdate(pos, next);
        return next;
    }
    public static BlockState resolve(Level level, BlockPos pos, BlockState state) {
        Flow old = state.getValue(FLOW);
        Flow stopped = old == Flow.UNSET ? Flow.UNSET : old.down() ? Flow.STUCK_DOWN : Flow.STUCK_UP;
        BlockState next = state.setValue(FLOW, stopped);
        List<Input> inputs = new ArrayList<>();
        var seen = new HashSet<BlockPos>();
        for (Direction side : Direction.Plane.HORIZONTAL) for (int dy = -1; dy <= 1; dy++) {
            BlockPos neighbor = pos.relative(side).offset(0, dy, 0);
            ConveyorEntity belt = ConveyorEntity.at(level, neighbor);
            if (belt != null && belt.configured() && seen.add(belt.getBlockPos())) {
                BlockPos destination = belt.outputPosition();
                if (destination.equals(pos) || destination.equals(pos.above())) inputs.add(new Input(belt.moving(), side, destination.getY() > pos.getY()));
            }
            if (dy >= 0 && JunctionEntity.at(level, neighbor) != null
                    && JunctionBlock.port(level.getBlockState(neighbor), side.getOpposite()) == JunctionBlock.Port.OUTPUT)
                inputs.add(new Input(true, side, dy == 1));
        }
        if (inputs.size() == 1 && inputs.get(0).active && level.getBlockState(pos.above()).isAir()) {
            Input input = inputs.get(0);
            Direction high = input.high ? input.side : input.side.getOpposite();
            Direction travel = input.side.getOpposite();
            BlockPos exit = pos.relative(travel).offset(0, input.high ? 0 : 1, 0);
            // Compare physical receivers, not queue space. A full belt pauses cargo without changing direction.
            var outlets = new LinkedHashMap<BlockPos, HashSet<BlockPos>>();
            for (Direction side : Direction.Plane.HORIZONTAL) if (side != input.side) for (int dy = 0; dy <= 1; dy++) {
                BlockPos nominal = pos.relative(side).above(dy);
                for (int below = 0; below <= 1; below++) {
                    ConveyorEntity belt = ConveyorEntity.at(level, nominal.below(below));
                    if (belt != null && belt.potentialEntry(nominal, side.getOpposite()))
                        outlets.computeIfAbsent(belt.getBlockPos(), ignored -> new HashSet<>()).add(nominal);
                }
                if (JunctionEntity.at(level, nominal) != null) outlets.computeIfAbsent(nominal, ignored -> new HashSet<>()).add(nominal);
            }
            if (outlets.isEmpty() || outlets.size() == 1 && outlets.values().iterator().next().contains(exit))
                next = state.setValue(FACING, high).setValue(FLOW, input.high ? Flow.DOWN : Flow.UP);
        }
        return next;
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof BlockItem) return InteractionResult.PASS;
        if (player.getItemInHand(hand).isEmpty()) {
            if (!level.isClientSide) {
                Flow flow = update(level, pos).getValue(FLOW);
                player.displayClientMessage(Component.translatable("message.conveyance.slope_" + (flow.moving() ? flow.down() ? "down" : "up" : "stuck")), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.use(state, level, pos, player, hand, hit);
    }
}
