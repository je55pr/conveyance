package dev.conveyance.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class MinerEntity extends PoweredMachineEntity {
    private int cursor;
    public MinerEntity(BlockPos pos, BlockState state) { super(FactoryContent.MINER_ENTITY.get(), pos, state, 90, 20); }

    @Override public double powerDistanceSquared(BlockPos pole) {
        BlockPos far = MinerBlock.partPosition(worldPosition, getBlockState().getValue(MinerBlock.FACING), MinerBlock.PART_COUNT - 1);
        int x = Math.max(Math.min(worldPosition.getX(), far.getX()), Math.min(Math.max(worldPosition.getX(), far.getX()), pole.getX()));
        int y = Math.max(worldPosition.getY(), Math.min(far.getY(), pole.getY()));
        int z = Math.max(Math.min(worldPosition.getZ(), far.getZ()), Math.min(Math.max(worldPosition.getZ(), far.getZ()), pole.getZ()));
        return pole.distSqr(new BlockPos(x, y, z));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MinerEntity miner) {
        var facing = state.getValue(MinerBlock.FACING);
        var resource = MiningResources.find(level, 16, miner.cursor, part -> MinerBlock.partPosition(pos, facing, part).below());
        if (resource.stack().isEmpty() || !ItemTransfer.canOutput(level, pos.relative(facing), facing.getOpposite(), resource.stack())) return;
        if (!miner.advanceWork()) return;
        if (ItemTransfer.output(level, pos.relative(facing), facing.getOpposite(), resource.stack())) {
            miner.cursor = resource.nextCursor();
            miner.setChanged();
        }
    }

    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putInt("Cursor", cursor % 16); }
    @Override public void load(CompoundTag tag) { super.load(tag); cursor = Math.floorMod(tag.getInt("Cursor"), 16); }
}
