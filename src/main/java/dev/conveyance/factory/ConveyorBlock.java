package dev.conveyance.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ConveyorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);
    public ConveyorBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection()); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ConveyorEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, FactoryContent.CONVEYOR_ENTITY.get(), ConveyorEntity::tick);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        // Packed cargo is loaded after block placement; send it even when a full queue cannot move.
        if (!level.isClientSide) level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            if (!level.isClientSide) level.setBlockAndUpdate(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()));
        } else {
            if (held.getItem() instanceof BlockItem) return InteractionResult.PASS;
            if (!level.isClientSide) {
                ConveyorEntity belt = ConveyorEntity.at(level, pos);
                if (belt != null && belt.offer(held, state.getValue(FACING).getOpposite()) && !player.getAbilities().instabuild) held.shrink(1);
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
