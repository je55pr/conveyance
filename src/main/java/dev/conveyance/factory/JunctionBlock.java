package dev.conveyance.factory;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class JunctionBlock extends FactoryBlock {
    public enum Port implements StringRepresentable {
        NONE("none"), INPUT("input"), OUTPUT("output");
        private final String name;
        Port(String name) { this.name = name; }
        @Override public String getSerializedName() { return name; }
    }
    public static final EnumProperty<Port> NORTH = EnumProperty.create("north", Port.class);
    public static final EnumProperty<Port> EAST = EnumProperty.create("east", Port.class);
    public static final EnumProperty<Port> SOUTH = EnumProperty.create("south", Port.class);
    public static final EnumProperty<Port> WEST = EnumProperty.create("west", Port.class);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 8, 16);
    public JunctionBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(NORTH, Port.NONE).setValue(EAST, Port.NONE).setValue(SOUTH, Port.NONE).setValue(WEST, Port.NONE));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(NORTH, EAST, SOUTH, WEST); }
    private static EnumProperty<Port> property(Direction side) {
        return switch (side) { case NORTH -> NORTH; case EAST -> EAST; case SOUTH -> SOUTH; case WEST -> WEST; default -> throw new IllegalArgumentException("Horizontal port required"); };
    }
    static Port port(BlockState state, Direction side) { return state.getValue(property(side)); }
    private static Port detect(Level level, BlockPos pos, Direction side) {
        BlockPos neighbor = pos.relative(side);
        if (!level.hasChunkAt(neighbor)) return Port.NONE;
        BlockState state = level.getBlockState(neighbor);
        Direction facing;
        ConveyorEntity source = ConveyorEntity.outletAt(level, neighbor);
        if (source != null && source.configured() && source.outputPosition().equals(pos)) return Port.INPUT;
        for (int dy = 0; dy <= 1; dy++) {
            ConveyorEntity target = ConveyorEntity.at(level, neighbor.below(dy));
            if (target != null && target.potentialEntry(neighbor, side.getOpposite())) return Port.OUTPUT;
        }
        if (state.getBlock() instanceof ConveyorBlock) return Port.NONE;
        else if (state.getBlock() instanceof InserterBlock arm && arm.reach() == 1) facing = state.getValue(InserterBlock.FACING);
        else if (state.getBlock() instanceof MinerBlock && state.getValue(MinerBlock.PART) == 0) {
            return state.getValue(MinerBlock.FACING) == side.getOpposite() ? Port.INPUT : Port.NONE;
        } else if (state.getBlock() instanceof MachineBlock machine && machine.kind == MachineKind.BURNER_MINER && state.getValue(MachineBlock.PART) == 0) {
            return state.getValue(MachineBlock.FACING) == side.getOpposite() ? Port.INPUT : Port.NONE;
        }
        else {
            BlockPos distant = pos.relative(side, 2);
            if (!level.hasChunkAt(distant)) return Port.NONE;
            BlockState distantState = level.getBlockState(distant);
            if (!(distantState.getBlock() instanceof InserterBlock arm) || arm.reach() != 2) return Port.NONE;
            facing = distantState.getValue(InserterBlock.FACING);
        }
        return facing == side ? Port.OUTPUT : facing == side.getOpposite() ? Port.INPUT : Port.NONE;
    }
    static BlockState updatePorts(Level level, BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        if (!(current.getBlock() instanceof JunctionBlock)) return current;
        BlockState next = current;
        for (Direction side : JunctionEntity.SIDES) next = next.setValue(property(side), detect(level, pos, side));
        if (!next.equals(current)) level.setBlockAndUpdate(pos, next);
        return next;
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
        if (!level.isClientSide) updatePorts(level, pos);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            BlockState next = updatePorts(level, pos);
            level.sendBlockUpdated(pos, next, next, Block.UPDATE_CLIENTS);
        }
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.conveyance.junction"));
        super.appendHoverText(stack, level, tooltip, flag);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new JunctionEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, FactoryContent.JUNCTION_ENTITY.get(), JunctionEntity::tick);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof BlockItem) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof JunctionEntity junction) {
            if (held.isEmpty()) {
                BlockState current = updatePorts(level, pos);
                int inputs = 0, outputs = 0;
                for (Direction side : JunctionEntity.SIDES) {
                    if (port(current, side) == Port.INPUT) inputs++;
                    if (port(current, side) == Port.OUTPUT) outputs++;
                }
                player.displayClientMessage(Component.translatable("message.conveyance.junction_status", inputs, outputs, junction.storedCount()), true);
            } else {
                Direction side = hit.getDirection().getAxis().isHorizontal() ? hit.getDirection() : player.getDirection().getOpposite();
                if (junction.offer(held, side) && !player.getAbilities().instabuild) held.shrink(1);
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
        if (!(level.getBlockEntity(pos) instanceof JunctionEntity junction) || junction.recovered) return;
        junction.recovered = true;
        ItemStack packed = new ItemStack(FactoryContent.JUNCTION_ITEM.get());
        BlockItem.setBlockEntityData(packed, FactoryContent.JUNCTION_ENTITY.get(), junction.saveWithoutMetadata());
        Block.popResource(level, pos, packed);
    }
}
