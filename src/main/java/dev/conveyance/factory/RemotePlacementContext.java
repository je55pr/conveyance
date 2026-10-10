package dev.conveyance.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** An explicit build plane and rotation, independent of the character's eyes. */
public final class RemotePlacementContext extends BlockPlaceContext {
    private final BlockPos target;
    private final Direction facing;
    public RemotePlacementContext(Player player, BlockPos target, Direction facing) {
        super(player.level(), player, InteractionHand.MAIN_HAND, player.getMainHandItem(),
                new BlockHitResult(Vec3.atBottomCenterOf(target), Direction.UP, target, false));
        this.target = target.immutable(); this.facing = facing; replaceClicked = true;
    }
    @Override public BlockPos getClickedPos() { return target == null ? super.getClickedPos() : target; }
    @Override public boolean canPlace() { return getLevel().getBlockState(getClickedPos()).canBeReplaced(this); }
    @Override public Direction getHorizontalDirection() { return facing == null ? Direction.NORTH : facing; }
    @Override public float getRotation() { return getHorizontalDirection().toYRot(); }
    @Override public Direction getNearestLookingDirection() { return Direction.DOWN; }
    @Override public Direction getNearestLookingVerticalDirection() { return Direction.DOWN; }
    @Override public Direction[] getNearestLookingDirections() {
        return new Direction[]{Direction.DOWN, getHorizontalDirection(), getHorizontalDirection().getClockWise(),
                getHorizontalDirection().getCounterClockWise(), getHorizontalDirection().getOpposite(), Direction.UP};
    }
}
