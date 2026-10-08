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
import net.minecraft.world.phys.shapes.CollisionContext;

public final class PowerPoleItem extends BlockItem {
    public PowerPoleItem(Block block, Properties properties) { super(block, properties); }
    @Override public InteractionResult place(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos root = context.getClickedPos();
        boolean supported = level.getBlockState(root.below()).isFaceSturdy(level, root.below(), Direction.UP);
        for (int part = 0; part < PowerPoleBlock.HEIGHT; part++) {
            BlockPos pos = root.above(part);
            var state = (part == 0 ? getBlock() : FactoryContent.POWER_POLE_PART.get()).defaultBlockState().setValue(PowerPoleBlock.PART, part);
            if (!supported || level.isOutsideBuildHeight(pos) || !level.getBlockState(pos).canBeReplaced()
                    || !level.isUnobstructed(state, pos, context.getPlayer() == null ? CollisionContext.empty() : CollisionContext.of(context.getPlayer()))) {
                if (context.getPlayer() != null && !level.isClientSide) context.getPlayer().displayClientMessage(Component.translatable("message.conveyance.pole_space"), true);
                return InteractionResult.FAIL;
            }
        }
        InteractionResult result = super.place(context);
        if (result.consumesAction() && !level.isClientSide) {
            for (int part = 1; part < PowerPoleBlock.HEIGHT; part++) level.setBlockAndUpdate(root.above(part),
                    FactoryContent.POWER_POLE_PART.get().defaultBlockState().setValue(PowerPoleBlock.PART, part));
        }
        return result;
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.conveyance.power_pole", PowerNetwork.LINK_RANGE, PowerNetwork.COVERAGE_RANGE));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
