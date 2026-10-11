package dev.conveyance.factory;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Uses the placement item's target and orientation, without changing the world. */
public final class PlacementPreview {
    public record Part(BlockPos pos, BlockState state) {}
    public record Plan(List<Part> parts, boolean valid) {}
    public static Plan resolve(Level level, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack item = player.getItemInHand(hand);
        if (!(item.getItem() instanceof BlockItem blockItem)) return null;
        BlockPlaceContext context = new BlockPlaceContext(level, player, hand, item, hit);
        return resolve(context);
    }
    public static Plan resolve(BlockPlaceContext context) {
        Level level = context.getLevel(); Player player = context.getPlayer();
        if (player == null || !(context.getItemInHand().getItem() instanceof BlockItem blockItem)) return null;
        context = blockItem.updatePlacementContext(context);
        if (context == null) return null;
        BlockPos root = context.getClickedPos();
        BlockState state = blockItem instanceof MachineItem machine ? machine.previewState(context)
                : blockItem instanceof MinerItem miner ? miner.previewState(context)
                : blockItem instanceof RadarItem radar ? radar.previewState(context) : blockItem.getBlock().getStateForPlacement(context);
        boolean valid = state != null && context.canPlace() && player.getAbilities().mayBuild && level.mayInteract(player, root);
        if (state == null) state = blockItem.getBlock().defaultBlockState();
        if (state.getBlock() instanceof SlopeConveyorBlock) state = SlopeConveyorBlock.resolve(level, root, state);
        var parts = new ArrayList<Part>();
        int foundation = 0;
        if (state.getBlock() instanceof MachineBlock machine) {
            MachineKind kind = machine.kind; foundation = kind.width * kind.depth;
            Direction facing = state.getValue(MachineBlock.FACING);
            if (kind == MachineKind.OFFSHORE_PUMP && !SteamMachine.waterAtIntake(level, root, facing)) valid = false;
            if (kind.fluids() && !FluidNetwork.canConnect(level, kind == MachineKind.PUMPJACK ? ChemicalMachine.pumpPreview(root, facing) : SteamMachine.previewPorts(root, facing, kind))) valid = false;
            for (int part = 0; part < kind.parts(); part++) parts.add(new Part(MachineBlock.partPosition(root, facing, kind, part), part == 0 ? state
                    : kind.casing().defaultBlockState().setValue(MachineBlock.PART, part).setValue(MachineBlock.FACING, facing)));
        } else if (state.getBlock() instanceof MinerBlock) {
            foundation = MinerBlock.WIDTH * MinerBlock.WIDTH;
            Direction facing = state.getValue(MinerBlock.FACING);
            for (int part = 0; part < MinerBlock.PART_COUNT; part++) parts.add(new Part(MinerBlock.partPosition(root, facing, part), part == 0 ? state
                    : FactoryContent.MINER_PART.get().defaultBlockState().setValue(MinerBlock.PART, part).setValue(MinerBlock.FACING, facing)));
        } else if (state.getBlock() instanceof RadarBlock) {
            foundation = RadarBlock.PART_COUNT;
            Direction facing = state.getValue(RadarBlock.FACING);
            for (int part = 0; part < RadarBlock.PART_COUNT; part++) parts.add(new Part(RadarBlock.partPosition(root, facing, part), part == 0 ? state
                    : FactoryContent.RADAR_PART.get().defaultBlockState().setValue(RadarBlock.PART, part).setValue(RadarBlock.FACING, facing)));
        } else if (state.getBlock() instanceof PowerPoleBlock) {
            foundation = 1;
            for (int part = 0; part < PowerPoleBlock.HEIGHT; part++) parts.add(new Part(root.above(part), part == 0 ? state
                    : FactoryContent.POWER_POLE_PART.get().defaultBlockState().setValue(PowerPoleBlock.PART, part)));
        } else if (state.getBlock() instanceof StoneWallBlock) {
            foundation = 1; parts.add(new Part(root, state));
            parts.add(new Part(root.above(), state.setValue(StoneWallBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER)));
        } else parts.add(new Part(root, state));
        for (int index = 0; index < parts.size(); index++) {
            Part part = parts.get(index);
            if (level.isOutsideBuildHeight(part.pos) || !level.hasChunkAt(part.pos) || !level.getBlockState(part.pos).canBeReplaced()
                    || !part.state.canSurvive(level, part.pos) || !level.isUnobstructed(part.state, part.pos, CollisionContext.of(player))) valid = false;
            if (index < foundation && (!level.hasChunkAt(part.pos.below())
                    || !level.getBlockState(part.pos.below()).isFaceSturdy(level, part.pos.below(), Direction.UP))) valid = false;
        }
        return new Plan(List.copyOf(parts), valid);
    }
    private PlacementPreview() {}
}
