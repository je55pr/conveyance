package dev.conveyance.factory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class RadarBlock extends FactoryBlock {
    public static final int WIDTH = 3, HEIGHT = 1, PART_COUNT = WIDTH * WIDTH;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty MULTIBLOCK = BooleanProperty.create("multiblock");
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, PART_COUNT - 1);
    private record Removal(Level level, BlockPos root) {}
    private static final ThreadLocal<Set<Removal>> REMOVALS = ThreadLocal.withInitial(HashSet::new);

    public RadarBlock(Properties properties) {
        super(properties);
        // A saved one-block radar without the new property remains visibly intact until it can expand.
        registerDefaultState(stateDefinition.any().setValue(LIT, false).setValue(PART, 0)
                .setValue(FACING, Direction.NORTH).setValue(MULTIBLOCK, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(LIT, PART, FACING, MULTIBLOCK); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(MULTIBLOCK, true);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(MULTIBLOCK) ? partShape(state.getValue(PART)) : Block.box(0, 0, 0, 16, 16, 16);
    }
    static VoxelShape partShape(int part) { return Block.box(0, 0, 0, 16, part == 4 ? 16 : 4, 16); }
    public static BlockPos partPosition(BlockPos root, Direction facing, int part) {
        int x = part % WIDTH, z = part / WIDTH;
        Direction side = facing.getClockWise();
        return root.offset(side.getStepX() * x - facing.getStepX() * z, 0, side.getStepZ() * x - facing.getStepZ() * z);
    }
    static BlockPos origin(BlockPos pos, BlockState state) {
        int part = state.getValue(PART), x = part % WIDTH, z = part / WIDTH;
        Direction facing = state.getValue(FACING), side = facing.getClockWise();
        return pos.offset(-side.getStepX() * x + facing.getStepX() * z, 0, -side.getStepZ() * x + facing.getStepZ() * z);
    }
    static RadarEntity controller(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof RadarBlock) && !(state.getBlock() instanceof RadarPartBlock)) return null;
        BlockPos root = origin(pos, state);
        return level.hasChunkAt(root) && level.getBlockEntity(root) instanceof RadarEntity radar ? radar : null;
    }
    private static boolean owned(BlockState state, int part, Direction facing) {
        return (part == 0 ? state.getBlock() instanceof RadarBlock : state.getBlock() instanceof RadarPartBlock)
                && state.getValue(PART) == part && state.getValue(FACING) == facing;
    }
    static void placeParts(Level level, BlockPos root, Direction facing, boolean lit) {
        for (int part = 1; part < PART_COUNT; part++) level.setBlockAndUpdate(partPosition(root, facing, part),
                FactoryContent.RADAR_PART.get().defaultBlockState().setValue(PART, part).setValue(FACING, facing).setValue(LIT, lit));
    }
    static boolean expandLegacy(Level level, BlockPos root, BlockState state) {
        if (state.getValue(MULTIBLOCK)) return false;
        Direction facing = state.getValue(FACING);
        for (int part = 1; part < PART_COUNT; part++) {
            BlockPos pos = partPosition(root, facing, part);
            BlockState replacement = FactoryContent.RADAR_PART.get().defaultBlockState().setValue(PART, part).setValue(FACING, facing);
            if (!level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos) || !level.getBlockState(pos).canBeReplaced()
                    || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                    || !level.isUnobstructed(replacement, pos, CollisionContext.empty())) return false;
        }
        placeParts(level, root, facing, state.getValue(LIT));
        level.setBlockAndUpdate(root, state.setValue(MULTIBLOCK, true));
        PowerNetwork.register(level, root, PowerNetwork.Kind.MACHINE);
        return true;
    }
    static void setPowered(Level level, BlockPos root, boolean lit) {
        BlockState state = level.getBlockState(root);
        Direction facing = state.getValue(FACING);
        int parts = state.getValue(MULTIBLOCK) ? PART_COUNT : 1;
        for (int part = 0; part < parts; part++) {
            BlockPos pos = partPosition(root, facing, part);
            BlockState current = level.getBlockState(pos);
            if (owned(current, part, facing) && current.getValue(LIT) != lit) level.setBlockAndUpdate(pos, current.setValue(LIT, lit));
        }
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return state.getValue(PART) == 0 ? new RadarEntity(pos, state) : null; }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || state.getValue(PART) != 0 ? null : createTickerHelper(type, FactoryContent.RADAR_ENTITY.get(), RadarEntity::tick);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return inspect(level, pos, player, hand);
    }
    static InteractionResult inspect(Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown() || player.getItemInHand(hand).getItem() instanceof BlockItem) return InteractionResult.PASS;
        RadarEntity radar = controller(level, pos);
        if (!level.isClientSide && radar != null)
            player.displayClientMessage(Component.translatable("message.conveyance.radar_status", radar.covered(), RadarEntity.total(),
                    Math.round(PowerNetwork.fraction(level, radar.getBlockPos()) * 100), RadarEntity.AREA_BLOCKS, RadarEntity.AREA_BLOCKS), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) dismantle(level, pos, state);
        super.playerWillDestroy(level, pos, state, player);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide) dismantle(level, pos, state);
        super.onRemove(state, level, pos, next, moving);
    }
    static void dismantle(Level level, BlockPos pos, BlockState state) {
        BlockPos root = origin(pos, state);
        Removal removal = new Removal(level, root);
        if (!REMOVALS.get().add(removal)) return;
        try {
            if (level.getBlockEntity(root) instanceof RadarEntity radar) radar.recover();
            Direction facing = state.getValue(FACING);
            for (int part = PART_COUNT - 1; part >= 0; part--) {
                BlockPos target = partPosition(root, facing, part);
                if (owned(level.getBlockState(target), part, facing)) level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
            }
        } finally { REMOVALS.get().remove(removal); }
    }
    @Override public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.conveyance.radar", RadarEntity.DEMAND_KW, RadarEntity.AREA_BLOCKS, RadarEntity.AREA_BLOCKS));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
