package dev.conveyance.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class RadarItem extends BlockItem {
    public RadarItem(Block block, Properties properties) { super(block, properties); }
    public BlockState previewState(BlockPlaceContext context) { return getPlacementState(context); }
    @Override public InteractionResult place(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos root = context.getClickedPos();
        Direction facing = context.getHorizontalDirection().getOpposite();
        for (int part = 0; part < RadarBlock.PART_COUNT; part++) {
            BlockPos pos = RadarBlock.partPosition(root, facing, part);
            BlockState state = (part == 0 ? getBlock() : FactoryContent.RADAR_PART.get()).defaultBlockState()
                    .setValue(RadarBlock.PART, part).setValue(RadarBlock.FACING, facing);
            if (part == 0) state = state.setValue(RadarBlock.MULTIBLOCK, true);
            if (!level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos) || !level.getBlockState(pos).canBeReplaced()
                    || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                    || !level.isUnobstructed(state, pos, context.getPlayer() == null ? CollisionContext.empty() : CollisionContext.of(context.getPlayer()))) {
                if (!level.isClientSide && context.getPlayer() != null) context.getPlayer().displayClientMessage(Component.translatable("message.conveyance.machine_space", RadarBlock.WIDTH, RadarBlock.WIDTH, RadarBlock.HEIGHT), true);
                return InteractionResult.FAIL;
            }
        }
        InteractionResult result = super.place(context);
        if (result.consumesAction() && !level.isClientSide) RadarBlock.placeParts(level, root, facing, false);
        return result;
    }
}
