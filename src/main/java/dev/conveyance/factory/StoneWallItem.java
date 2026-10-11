package dev.conveyance.factory;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.shapes.CollisionContext;

/** One wall item places both connected halves. */
public final class StoneWallItem extends BlockItem {
    public StoneWallItem(Block block, Properties properties) { super(block, properties); }
    @Override public InteractionResult place(BlockPlaceContext context) {
        var level = context.getLevel(); var root = context.getClickedPos(); var upper = root.above();
        var shape = getBlock().defaultBlockState().setValue(StoneWallBlock.HALF, DoubleBlockHalf.UPPER);
        if (level.isOutsideBuildHeight(upper) || !level.hasChunkAt(upper) || !level.getBlockState(upper).canBeReplaced()
                || !level.getBlockState(root.below()).isFaceSturdy(level, root.below(), Direction.UP)
                || !level.isUnobstructed(shape, upper, context.getPlayer() == null ? CollisionContext.empty() : CollisionContext.of(context.getPlayer()))) return InteractionResult.FAIL;
        var result = super.place(context);
        if (result.consumesAction() && !level.isClientSide) {
            var state = level.getBlockState(root).setValue(StoneWallBlock.HALF, DoubleBlockHalf.UPPER);
            level.setBlockAndUpdate(upper, state);
            for (Direction side : Direction.Plane.HORIZONTAL) state = state.updateShape(side, level.getBlockState(upper.relative(side)), level, upper, upper.relative(side));
            level.setBlockAndUpdate(upper, state);
        }
        return result;
    }
}
