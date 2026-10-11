package dev.conveyance.factory;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class MachineBlock extends FactoryBlock {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 49);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public final MachineKind kind;
    private record Removal(Level level, BlockPos root) {}
    private static final ThreadLocal<Set<Removal>> REMOVALS = ThreadLocal.withInitial(HashSet::new);
    public MachineBlock(Properties properties, MachineKind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(PART, 0).setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(PART, FACING, LIT); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FactoryGeometry.shape(kind.id, state.getValue(PART), state.getValue(FACING));
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return state.getValue(PART) == 0 ? new MachineEntity(pos, state) : null; }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, FactoryContent.MACHINE_ENTITY.get(), MachineEntity::tick);
    }
    static MachineKind kind(BlockState state) {
        if (state.getBlock() instanceof MachineBlock block) return block.kind;
        return state.getBlock() instanceof MachinePartBlock block ? block.kind : null;
    }
    static BlockPos partPosition(BlockPos root, Direction facing, MachineKind kind, int part) {
        int x = part % kind.width, z = part / kind.width % kind.depth, y = part / (kind.width * kind.depth);
        return localPosition(root, facing, x, y, z);
    }
    static BlockPos localPosition(BlockPos root, Direction facing, int x, int y, int z) {
        return root.offset(rotateX(x, z, facing), y, rotateZ(x, z, facing));
    }
    private static int rotateX(int x, int z, Direction facing) { return switch (facing) { case EAST -> -z; case SOUTH -> -x; case WEST -> z; default -> x; }; }
    private static int rotateZ(int x, int z, Direction facing) { return switch (facing) { case EAST -> x; case SOUTH -> -z; case WEST -> -x; default -> z; }; }
    private static BlockPos origin(BlockPos pos, BlockState state) {
        MachineKind kind = kind(state);
        int part = state.getValue(PART), x = part % kind.width, z = part / kind.width % kind.depth;
        Direction facing = state.getValue(FACING);
        return pos.offset(-rotateX(x, z, facing), -part / (kind.width * kind.depth), -rotateZ(x, z, facing));
    }
    static MachineEntity controller(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return null;
        BlockState state = level.getBlockState(pos);
        MachineKind kind = kind(state);
        if (kind == null) return null;
        BlockPos root = origin(pos, state);
        return level.hasChunkAt(root) && level.getBlockEntity(root) instanceof MachineEntity machine && machine.kind() == kind ? machine : null;
    }
    static InteractionResult interact(Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (player.getItemInHand(hand).getItem() instanceof BlockItem) return InteractionResult.PASS;
        MachineEntity machine = controller(level, pos);
        if (MachineModules.interact(machine, player, hand)) return InteractionResult.sidedSuccess(level.isClientSide);
        if (!level.isClientSide && machine != null) {
            if (machine.kind().utility()) { machine.utility().showStatus(player); return InteractionResult.SUCCESS; }
            if (machine.kind() == MachineKind.SOLAR_PANEL) {
                player.displayClientMessage(Component.translatable("message.conveyance.solar_output", FactoryTooltips.number(machine.outputPower())), true);
                return InteractionResult.SUCCESS;
            }
            if (machine.kind().assembler() || machine.kind() == MachineKind.LAB) PowerNetwork.showStatus(level, machine.getBlockPos(), player);
            player.openMenu(machine);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) { return interact(level, pos, player, hand); }
    @Override public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) dismantle(level, pos, state);
        super.playerWillDestroy(level, pos, state, player);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide) dismantle(level, pos, state);
        super.onRemove(state, level, pos, next, moving);
    }
    static void dismantle(Level level, BlockPos pos, BlockState state) {
        MachineKind kind = kind(state);
        BlockPos root = origin(pos, state);
        Removal removal = new Removal(level, root);
        if (!REMOVALS.get().add(removal)) return;
        try {
            ItemStack packed = new ItemStack(kind.item());
            if (level.getBlockEntity(root) instanceof MachineEntity machine) BlockItem.setBlockEntityData(packed, FactoryContent.MACHINE_ENTITY.get(), machine.saveWithoutMetadata());
            Direction facing = state.getValue(FACING);
            for (int part = kind.parts() - 1; part >= 0; part--) {
                BlockPos target = partPosition(root, facing, kind, part);
                BlockState current = level.getBlockState(target);
                if (kind(current) == kind && current.getValue(PART) == part && current.getValue(FACING) == facing) level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
            }
            Block.popResource(level, root, packed);
        } finally { REMOVALS.get().remove(removal); }
    }
}
