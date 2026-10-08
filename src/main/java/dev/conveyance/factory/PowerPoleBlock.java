package dev.conveyance.factory;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class PowerPoleBlock extends BaseEntityBlock {
    public static final int HEIGHT = 4;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, HEIGHT - 1);
    private record Removal(Level level, BlockPos origin) {}
    private static final ThreadLocal<Set<Removal>> REMOVALS = ThreadLocal.withInitial(HashSet::new);
    private static final VoxelShape SHAFT = Block.box(6, 0, 6, 10, 16, 10);
    private static final VoxelShape BASE = Shapes.or(SHAFT, Block.box(2, 0, 2, 14, 4, 14));
    private static final VoxelShape TOP = Shapes.or(SHAFT, Block.box(0, 10, 5, 16, 16, 11));

    public PowerPoleBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(PART, 0)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(PART); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return state.getValue(PART) == 0 ? new PowerPoleEntity(pos, state) : null; }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, FactoryContent.POWER_POLE_ENTITY.get(), PowerPoleEntity::tick);
    }
    static VoxelShape shape(int part) { return part == 0 ? BASE : part == HEIGHT - 1 ? TOP : SHAFT; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return shape(state.getValue(PART)); }
    static BlockPos origin(BlockPos pos, BlockState state) { return pos.below(state.getValue(PART)); }

    static InteractionResult inspect(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (player.getItemInHand(hand).getItem() instanceof BlockItem) return InteractionResult.PASS;
        if (!level.isClientSide) PowerNetwork.showStatus(level, origin(pos, state), player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return inspect(state, level, pos, player, hand);
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
            for (int part = HEIGHT - 1; part >= 0; part--) {
                BlockPos target = root.above(part);
                BlockState current = level.getBlockState(target);
                if ((current.getBlock() instanceof PowerPoleBlock || current.getBlock() instanceof PowerPolePartBlock)
                        && current.getValue(PART) == part) level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
            }
            Block.popResource(level, root, new ItemStack(FactoryContent.POWER_POLE_ITEM.get()));
        } finally { REMOVALS.get().remove(removal); }
    }
}
