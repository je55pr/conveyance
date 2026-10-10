package dev.conveyance.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class RadarPartBlock extends FactoryPartBlock {
    public RadarPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(RadarBlock.PART, 1).setValue(RadarBlock.FACING, Direction.NORTH).setValue(RadarBlock.LIT, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(RadarBlock.PART, RadarBlock.FACING, RadarBlock.LIT); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return RadarBlock.partShape(state.getValue(RadarBlock.PART)); }
    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) { return new ItemStack(FactoryContent.RADAR_ITEM.get()); }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) { return RadarBlock.inspect(level, pos, player, hand); }
    @Override public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) RadarBlock.dismantle(level, pos, state);
        super.playerWillDestroy(level, pos, state, player);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && !level.isClientSide) RadarBlock.dismantle(level, pos, state);
        super.onRemove(state, level, pos, next, moving);
    }
}
