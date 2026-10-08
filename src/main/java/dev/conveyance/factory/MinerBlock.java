package dev.conveyance.factory;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class MinerBlock extends BaseEntityBlock {
    public static final int WIDTH = 4, HEIGHT = 3, PART_COUNT = WIDTH * WIDTH * HEIGHT;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, PART_COUNT - 1);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private record Removal(Level level, BlockPos origin) {}
    private static final ThreadLocal<Set<Removal>> REMOVALS = ThreadLocal.withInitial(HashSet::new);

    public MinerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, 0).setValue(FACING, Direction.NORTH));
    }

    public static BlockPos partPosition(BlockPos origin, Direction facing, int part) {
        int x = part % WIDTH, z = (part / WIDTH) % WIDTH, y = part / (WIDTH * WIDTH);
        return origin.offset(rotateX(x, z, facing), y, rotateZ(x, z, facing));
    }

    public static BlockPos origin(BlockPos pos, BlockState state) {
        int part = state.getValue(PART), x = part % WIDTH, z = (part / WIDTH) % WIDTH;
        Direction facing = state.getValue(FACING);
        return pos.offset(-rotateX(x, z, facing), -part / (WIDTH * WIDTH), -rotateZ(x, z, facing));
    }

    private static int rotateX(int x, int z, Direction facing) {
        return switch (facing) { case EAST -> -z; case SOUTH -> -x; case WEST -> z; default -> x; };
    }
    private static int rotateZ(int x, int z, Direction facing) {
        return switch (facing) { case EAST -> x; case SOUTH -> -z; case WEST -> -x; default -> z; };
    }

    public static MinerEntity controller(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof MinerBlock) && !(state.getBlock() instanceof MinerPartBlock)) return null;
        BlockPos root = origin(pos, state);
        if (!level.hasChunkAt(root)) return null;
        return level.getBlockEntity(root) instanceof MinerEntity entity ? entity : null;
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0 ? new MinerEntity(pos, state) : null;
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(PART, FACING); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || state.getValue(PART) != 0 ? null
                : createTickerHelper(type, FactoryContent.MINER_ENTITY.get(), MinerEntity::tick);
    }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof BlockItem) return InteractionResult.PASS;
        var entity = controller(level, pos);
        if (entity != null && !level.isClientSide) {
            PowerNetwork.showStatus(level, entity.getBlockPos(), player);
        }
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
        var removal = new Removal(level, root);
        if (!REMOVALS.get().add(removal)) return;
        try {
            ItemStack packed = new ItemStack(FactoryContent.MINER_ITEM.get());
            if (level.getBlockEntity(root) instanceof MinerEntity entity) {
                BlockItem.setBlockEntityData(packed, FactoryContent.MINER_ENTITY.get(), entity.saveWithoutMetadata());
            }
            Direction facing = state.getValue(FACING);
            for (int part = PART_COUNT - 1; part >= 0; part--) {
                BlockPos target = partPosition(root, facing, part);
                BlockState current = level.getBlockState(target);
                if ((current.getBlock() instanceof MinerBlock || current.getBlock() instanceof MinerPartBlock)
                        && current.getValue(PART) == part
                        && current.getValue(FACING) == facing) {
                    level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
                }
            }
            var drop = new ItemEntity(level, root.getX() + 0.5, root.getY() + 0.5, root.getZ() + 0.5, packed);
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
        } finally { REMOVALS.get().remove(removal); }
    }
}
