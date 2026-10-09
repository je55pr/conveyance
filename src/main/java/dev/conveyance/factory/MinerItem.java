package dev.conveyance.factory;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class MinerItem extends BlockItem {
    public MinerItem(Block block, Properties properties) { super(block, properties); }
    public BlockState previewState(BlockPlaceContext context) { return getPlacementState(context); }

    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        return getBlock().defaultBlockState().setValue(MinerBlock.FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override public InteractionResult place(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos root = context.getClickedPos();
        Direction facing = context.getHorizontalDirection().getOpposite();
        for (int part = 0; part < MinerBlock.PART_COUNT; part++) {
            BlockPos pos = MinerBlock.partPosition(root, facing, part);
            BlockState state = (part == 0 ? getBlock() : FactoryContent.MINER_PART.get()).defaultBlockState()
                    .setValue(MinerBlock.PART, part).setValue(MinerBlock.FACING, facing);
            boolean supported = part >= MinerBlock.WIDTH * MinerBlock.WIDTH
                    || level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
            if (level.isOutsideBuildHeight(pos) || !level.getBlockState(pos).canBeReplaced()
                    || !supported || !level.isUnobstructed(state, pos, context.getPlayer() == null
                            ? CollisionContext.empty() : CollisionContext.of(context.getPlayer()))) {
                if (context.getPlayer() != null && !level.isClientSide) {
                    context.getPlayer().displayClientMessage(Component.translatable("message.conveyance.miner_space"), true);
                }
                return InteractionResult.FAIL;
            }
        }
        InteractionResult result = super.place(context);
        if (result.consumesAction() && !level.isClientSide) {
            for (int part = 1; part < MinerBlock.PART_COUNT; part++) {
                level.setBlockAndUpdate(MinerBlock.partPosition(root, facing, part), FactoryContent.MINER_PART.get().defaultBlockState()
                        .setValue(MinerBlock.PART, part).setValue(MinerBlock.FACING, facing));
            }
        }
        return result;
    }

    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.conveyance.miner"));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
