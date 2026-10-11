package dev.conveyance.factory;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

public final class PipeItem extends BlockItem {
    public PipeItem(Block block, Properties properties) { super(block, properties); }
    @Override public InteractionResult place(BlockPlaceContext context) {
        if (!FluidNetwork.canConnect(context.getLevel(), PipeEntity.ports(context.getClickedPos(), new FluidTank(PipeEntity.CAPACITY, FluidKind.EMPTY)))) {
            if (!context.getLevel().isClientSide && context.getPlayer() != null)
                context.getPlayer().displayClientMessage(Component.translatable("message.conveyance.fluid_mixing"), true);
            return InteractionResult.FAIL;
        }
        return super.place(context);
    }
}
