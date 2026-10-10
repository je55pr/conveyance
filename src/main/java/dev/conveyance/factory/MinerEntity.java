package dev.conveyance.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class MinerEntity extends PoweredMachineEntity {
    public static final int DEMAND_KW = 90, CYCLE_TICKS = 20;
    private int cursor;
    private boolean legacyFootprint;
    int footprintWidth() { return legacyFootprint ? 4 : MinerBlock.WIDTH; }
    public MinerEntity(BlockPos pos, BlockState state) { super(FactoryContent.MINER_ENTITY.get(), pos, state, DEMAND_KW, CYCLE_TICKS); }
    @Override protected boolean hasWork() {
        if (level == null || legacyFootprint) return false;
        var facing = getBlockState().getValue(MinerBlock.FACING);
        var resource = MiningResources.find(level, MinerBlock.FOUNDATION, cursor, part -> MinerBlock.partPosition(worldPosition, facing, part).below());
        return !resource.stack().isEmpty() && ItemTransfer.canOutput(level, worldPosition.relative(facing), facing.getOpposite(), resource.stack());
    }

    @Override public double powerDistanceSquared(BlockPos pole) {
        int width = footprintWidth();
        BlockPos far = MinerBlock.partPosition(worldPosition, getBlockState().getValue(MinerBlock.FACING), width * width * MinerBlock.HEIGHT - 1, width);
        int x = Math.max(Math.min(worldPosition.getX(), far.getX()), Math.min(Math.max(worldPosition.getX(), far.getX()), pole.getX()));
        int y = Math.max(worldPosition.getY(), Math.min(far.getY(), pole.getY()));
        int z = Math.max(Math.min(worldPosition.getZ(), far.getZ()), Math.min(Math.max(worldPosition.getZ(), far.getZ()), pole.getZ()));
        return pole.distSqr(new BlockPos(x, y, z));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MinerEntity miner) {
        var facing = state.getValue(MinerBlock.FACING);
        if (miner.legacyFootprint) {
            if (!MinerBlock.resizeLegacy(level, pos, facing)) return;
            miner.legacyFootprint = false;
            miner.setChanged();
            PowerNetwork.register(level, pos, PowerNetwork.Kind.MACHINE);
        }
        var resource = MiningResources.find(level, MinerBlock.FOUNDATION, miner.cursor, part -> MinerBlock.partPosition(pos, facing, part).below());
        if (resource.stack().isEmpty() || !ItemTransfer.canOutput(level, pos.relative(facing), facing.getOpposite(), resource.stack())) return;
        if (!miner.advanceWork()) return;
        if (ItemTransfer.output(level, pos.relative(facing), facing.getOpposite(), resource.stack())) {
            miner.cursor = resource.nextCursor();
            miner.setChanged();
        }
    }

    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putInt("Cursor", cursor % MinerBlock.FOUNDATION); tag.putInt("FootprintWidth", footprintWidth()); }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        cursor = Math.floorMod(tag.getInt("Cursor"), MinerBlock.FOUNDATION);
        legacyFootprint = !tag.contains("FootprintWidth") || tag.getInt("FootprintWidth") == 4;
    }
}
