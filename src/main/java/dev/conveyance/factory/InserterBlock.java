package dev.conveyance.factory;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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

public final class InserterBlock extends FactoryBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final boolean burner;
    private final int reach;
    public InserterBlock(Properties properties) { this(properties, false); }
    public InserterBlock(Properties properties, boolean burner) { this(properties, burner, 1); }
    public InserterBlock(Properties properties, int reach) { this(properties, false, reach); }
    private InserterBlock(Properties properties, boolean burner, int reach) { super(properties); this.burner = burner; this.reach = reach; registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)); }
    public boolean fast() { return this == FactoryContent.FAST_INSERTER.get(); }
    public int reach() { return reach; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getHorizontalDirection()); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return burner ? new BurnerInserterEntity(pos, state) : new InserterEntity(pos, state); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Block.box(3, 0, 3, 13, 12, 13); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return burner ? createTickerHelper(type, FactoryContent.BURNER_INSERTER_ENTITY.get(), BurnerInserterEntity::tick)
                : createTickerHelper(type, reach == 2 ? FactoryContent.LONG_INSERTER_ENTITY.get() : FactoryContent.INSERTER_ENTITY.get(), InserterEntity::tick);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (burner) {
            if (held.getItem() instanceof BlockItem) return InteractionResult.PASS;
            if (!level.isClientSide) {
                if (held.isEmpty() && player.isShiftKeyDown()) level.setBlockAndUpdate(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()));
                else if (level.getBlockEntity(pos) instanceof BurnerInserterEntity entity) player.openMenu(entity);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!held.isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide) {
            level.setBlockAndUpdate(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()));
            PowerNetwork.showStatus(level, pos, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof BurnerInserterEntity arm) arm.recover();
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof InserterEntity inserter) inserter.dropHeld();
        super.onRemove(state, level, pos, next, moving);
    }
    @Override public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BurnerInserterEntity arm) arm.recover();
        super.playerWillDestroy(level, pos, state, player);
    }
    @Override public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        if (burner) tooltip.add(Component.translatable("tooltip.conveyance.burner_inserter"));
        else if (reach == 2) tooltip.add(Component.translatable("tooltip.conveyance.long_inserter"));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
