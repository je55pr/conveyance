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
    @Override public BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
        if (kind != MachineKind.OFFSHORE_PUMP || context.getPlayer() == null) return context;
        Level level = context.getLevel(); BlockPos target = context.getClickedPos(), bank = target.below();
        if (!level.getBlockState(target.below()).isFaceSturdy(level, target.below(), Direction.UP)
                && level.getBlockState(bank).canBeReplaced() && level.getBlockState(bank.below()).isFaceSturdy(level, bank.below(), Direction.UP)) {
            boolean water = false;
            for (Direction side : Direction.Plane.HORIZONTAL) water |= SteamMachine.waterAtIntake(level, bank, side);
            if (water) return context instanceof RemotePlacementContext
                    ? new RemotePlacementContext(context.getPlayer(), bank, context.getHorizontalDirection())
                    : new BlockPlaceContext(level, context.getPlayer(), context.getHand(), context.getItemInHand(),
                        new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(bank), Direction.UP, bank, false));
        }
        return context;
    }
    public BlockState previewState(BlockPlaceContext context) { return getPlacementState(context); }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (kind == MachineKind.OFFSHORE_PUMP) facing = SteamMachine.pumpFacing(context.getLevel(), context.getClickedPos(), facing);
        return getBlock().defaultBlockState().setValue(MachineBlock.FACING, facing);
    }
    @Override public InteractionResult place(BlockPlaceContext context) {
        context = updatePlacementContext(context);
        Level level = context.getLevel();
        BlockPos root = context.getClickedPos();
        Direction facing = getPlacementState(context).getValue(MachineBlock.FACING);
        if (kind == MachineKind.OFFSHORE_PUMP && !SteamMachine.waterAtIntake(level, root, facing)) {
            if (!level.isClientSide && context.getPlayer() != null) context.getPlayer().displayClientMessage(Component.translatable("message.conveyance.pump_shore"), true);
            return InteractionResult.FAIL;
        }
        if (kind.fluids() && !FluidNetwork.canConnect(level, kind == MachineKind.PUMPJACK ? ChemicalMachine.pumpPreview(root, facing) : SteamMachine.previewPorts(root, facing, kind))) {
            if (!level.isClientSide && context.getPlayer() != null) context.getPlayer().displayClientMessage(Component.translatable("message.conveyance.fluid_mixing"), true);
            return InteractionResult.FAIL;
        }
        for (int part = 0; part < kind.parts(); part++) {
            BlockPos pos = MachineBlock.partPosition(root, facing, kind, part);
            BlockState state = (part == 0 ? getBlock() : kind.casing()).defaultBlockState().setValue(MachineBlock.PART, part).setValue(MachineBlock.FACING, facing);
            boolean supported = part >= kind.width * kind.depth || level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
            if (level.isOutsideBuildHeight(pos) || !level.getBlockState(pos).canBeReplaced() || !supported
                    || !level.isUnobstructed(state, pos, context.getPlayer() == null ? CollisionContext.empty() : CollisionContext.of(context.getPlayer()))) {
                if (!level.isClientSide && context.getPlayer() != null) context.getPlayer().displayClientMessage(Component.translatable("message.conveyance.machine_space", kind.width, kind.depth, kind.height), true);
                return InteractionResult.FAIL;
            }
        }
        InteractionResult result = super.place(context);
        if (result.consumesAction() && !level.isClientSide) {
            for (int part = 1; part < kind.parts(); part++) level.setBlockAndUpdate(MachineBlock.partPosition(root, facing, kind, part),
                    kind.casing().defaultBlockState().setValue(MachineBlock.PART, part).setValue(MachineBlock.FACING, facing));
            if (kind.fluids()) FluidNetwork.invalidate(level);
        }
        return result;
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.conveyance." + kind.id));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
