package dev.conveyance.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.ForgeMod;

/** Air clicks can extend a visible belt endpoint, using the same placement transaction as block clicks. */
public final class ConveyorItem extends BlockItem {
    public ConveyorItem(Block block, Properties properties) { super(block, properties); }
    public static Direction incoming(Level level, BlockPos target) {
        Direction result = null;
        for (Direction side : Direction.Plane.HORIZONTAL) for (int dy = -1; dy <= 1; dy++) {
            ConveyorEntity belt = ConveyorEntity.at(level, target.relative(side).offset(0, dy, 0));
            if (belt == null || !belt.outputPosition().equals(target)) continue;
            if (result != null && result != belt.travelDirection()) return null;
            result = belt.travelDirection();
        }
        return result;
    }
    public static BlockHitResult extension(Level level, Player player) { return extension(level, player, false); }
    public static BlockHitResult extension(Level level, Player player, ItemStack item) {
        return extension(level, player, item.getItem() instanceof BlockItem block && block.getBlock() instanceof SlopeConveyorBlock);
    }
    private static BlockHitResult extension(Level level, Player player, boolean ramp) {
        if (player.isShiftKeyDown() && !ramp) return null;
        double reach = player.getAttributeValue(ForgeMod.BLOCK_REACH.get()) + (player.isCreative() ? 1.5 : 0);
        Vec3 eye = player.getEyePosition(), direction = player.getLookAngle(), end = eye.add(direction.scale(reach));
        BlockHitResult solid = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        double limit = solid.getType() == HitResult.Type.MISS ? reach : eye.distanceTo(solid.getLocation());
        BlockPos previous = null;
        for (double distance = 0.1; distance <= limit + 0.001; distance += 0.1) {
            Vec3 point = eye.add(direction.scale(distance));
            BlockPos pos = BlockPos.containing(point);
            if (pos.equals(previous)) continue;
            previous = pos;
            ConveyorEntity underneath = ConveyorEntity.at(level, pos.below());
            if (underneath != null && underneath.slope()) continue; // Preserve an existing ramp's upper inlet/headroom.
            if (level.hasChunkAt(pos) && level.getBlockState(pos).canBeReplaced() && incoming(level, pos) != null) {
                BlockPos target = ramp && player.isShiftKeyDown() ? pos.below() : pos;
                if (level.getBlockState(target).canBeReplaced()) return new BlockHitResult(point, Direction.UP, target, false);
            }
        }
        return null;
    }
    @Override public BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
        if (context.getPlayer() == null) return context;
        boolean ramp = getBlock() instanceof SlopeConveyorBlock;
        BlockPos clicked = context.replacingClickedOnBlock() ? context.getClickedPos() : context.getClickedPos().relative(context.getClickedFace().getOpposite());
        ConveyorEntity belt = ConveyorEntity.at(context.getLevel(), clicked);
        if (belt != null && (ramp || !context.getPlayer().isShiftKeyDown())) {
            // A construction chain can continue before its input is wired; transport remains stopped until resolved.
            BlockPos target = ramp && context.getPlayer().isShiftKeyDown() ? belt.outputPosition().below() : belt.outputPosition();
            if (!context.getLevel().getBlockState(target).canBeReplaced()) return null;
            var hit = new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false);
            return new BlockPlaceContext(context.getLevel(), context.getPlayer(), context.getHand(), context.getItemInHand(), hit);
        }
        // A floor click one level beneath an existing inlet is already a descending placement.
        if (ramp && incoming(context.getLevel(), context.getClickedPos().above()) != null) return context;
        BlockHitResult hit = extension(context.getLevel(), context.getPlayer(), ramp);
        return hit == null ? context : new BlockPlaceContext(context.getLevel(), context.getPlayer(), context.getHand(), context.getItemInHand(), hit);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        BlockHitResult hit = extension(level, player, getBlock() instanceof SlopeConveyorBlock);
        if (hit == null) return InteractionResultHolder.pass(player.getItemInHand(hand));
        if (!level.mayInteract(player, hit.getBlockPos())) return InteractionResultHolder.fail(player.getItemInHand(hand));
        UseOnContext context = new UseOnContext(level, player, hand, player.getItemInHand(hand), hit);
        var result = level.isClientSide ? useOn(context) : ForgeHooks.onPlaceItemIntoWorld(context);
        return new InteractionResultHolder<>(result, player.getItemInHand(hand));
    }
}
