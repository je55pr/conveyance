package dev.conveyance.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import dev.conveyance.player.ResearchState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class MinerEntity extends PoweredMachineEntity {
    public static final int DEMAND_KW = 90, CYCLE_TICKS = 40;
    private int cursor;
    private ItemStack pendingOre = ItemStack.EMPTY;
    private boolean legacyFootprint;
    private final DrillMotion drill = new DrillMotion(this);
    public DrillMotion drillMotion() { return drill; }
    int footprintWidth() { return legacyFootprint ? 4 : MinerBlock.WIDTH; }
    public MinerEntity(BlockPos pos, BlockState state) { super(FactoryContent.MINER_ENTITY.get(), pos, state, DEMAND_KW, CYCLE_TICKS); }
    @Override protected boolean hasWork() {
        if (level == null || legacyFootprint || !pendingOre.isEmpty()) return false;
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
            if (!MinerBlock.resizeLegacy(level, pos, facing)) { miner.drill.capture(0); return; }
            miner.legacyFootprint = false;
            miner.setChanged();
            PowerNetwork.register(level, pos, PowerNetwork.Kind.MACHINE);
        }
        if (!miner.pendingOre.isEmpty()) {
            miner.drill.capture(0);
            if (ItemTransfer.output(level, pos.relative(facing), facing.getOpposite(), miner.pendingOre.copyWithCount(1))) { miner.pendingOre.shrink(1); miner.setChanged(); }
            return;
        }
        var resource = MiningResources.find(level, MinerBlock.FOUNDATION, miner.cursor, part -> MinerBlock.partPosition(pos, facing, part).below());
        if (resource.stack().isEmpty() || !ItemTransfer.canOutput(level, pos.relative(facing), facing.getOpposite(), resource.stack())) { miner.drill.capture(0); return; }
        miner.drill.capture(18 * PowerNetwork.fraction(level, pos) * ResearchState.miningSpeed(level) * miner.modules.speed());
        if (!miner.advanceWork()) return;
        if (ItemTransfer.output(level, pos.relative(facing), facing.getOpposite(), resource.stack())) {
            if (miner.modules.produce("mining", miner.modules.productivity(true) + ResearchState.miningProductivity(level)) > 0) miner.pendingOre = resource.stack().copy();
            miner.cursor = resource.nextCursor();
            miner.setChanged();
        }
    }

    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.put("PendingOre", pendingOre.save(new CompoundTag())); tag.putInt("Cursor", cursor % MinerBlock.FOUNDATION); tag.putInt("FootprintWidth", footprintWidth()); }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        pendingOre = ItemStack.of(tag.getCompound("PendingOre"));
        cursor = Math.floorMod(tag.getInt("Cursor"), MinerBlock.FOUNDATION);
        legacyFootprint = !tag.contains("FootprintWidth") || tag.getInt("FootprintWidth") == 4;
        drill.read(tag);
    }
    @Override public CompoundTag getUpdateTag() { var tag = saveWithoutMetadata(); drill.write(tag); return tag; }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public AABB getRenderBoundingBox() {
        int width = footprintWidth();
        BlockPos far = MinerBlock.partPosition(worldPosition, getBlockState().getValue(MinerBlock.FACING), width * width * MinerBlock.HEIGHT - 1, width);
        return new AABB(Math.min(worldPosition.getX(), far.getX()), worldPosition.getY(), Math.min(worldPosition.getZ(), far.getZ()),
                Math.max(worldPosition.getX(), far.getX()) + 1, worldPosition.getY() + MinerBlock.HEIGHT, Math.max(worldPosition.getZ(), far.getZ()) + 1);
    }
}
