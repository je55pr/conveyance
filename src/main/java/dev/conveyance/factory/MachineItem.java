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

public final class MachineItem extends BlockItem {
    private final MachineKind kind;
    public MachineItem(Block block, Properties properties, MachineKind kind) { super(block, properties); this.kind = kind; }
    public BlockState previewState(BlockPlaceContext context) { return getPlacementState(context); }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        return getBlock().defaultBlockState().setValue(MachineBlock.FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override public InteractionResult place(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos root = context.getClickedPos();
        Direction facing = context.getHorizontalDirection().getOpposite();
        for (int part = 0; part < kind.parts(); part++) {
            BlockPos pos = MachineBlock.partPosition(root, facing, kind, part);
            BlockState state = (part == 0 ? getBlock() : kind.casing()).defaultBlockState().setValue(MachineBlock.PART, part).setValue(MachineBlock.FACING, facing);
            boolean supported = part >= kind.width * kind.width || level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
            if (level.isOutsideBuildHeight(pos) || !level.getBlockState(pos).canBeReplaced() || !supported
                    || !level.isUnobstructed(state, pos, context.getPlayer() == null ? CollisionContext.empty() : CollisionContext.of(context.getPlayer()))) {
                if (!level.isClientSide && context.getPlayer() != null) context.getPlayer().displayClientMessage(Component.translatable("message.conveyance.machine_space", kind.width, kind.width, kind.height), true);
                return InteractionResult.FAIL;
            }
        }
        InteractionResult result = super.place(context);
        if (result.consumesAction() && !level.isClientSide) {
            for (int part = 1; part < kind.parts(); part++) level.setBlockAndUpdate(MachineBlock.partPosition(root, facing, kind, part),
                    kind.casing().defaultBlockState().setValue(MachineBlock.PART, part).setValue(MachineBlock.FACING, facing));
        }
        return result;
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.conveyance." + kind.id));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
