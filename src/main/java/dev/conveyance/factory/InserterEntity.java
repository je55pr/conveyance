package dev.conveyance.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class InserterEntity extends PoweredMachineEntity implements MovingInserter {
    public static final int DEMAND_KW = 15, SHORT_CYCLE_TICKS = 10, LONG_CYCLE_TICKS = 20;
    private ItemStack held = ItemStack.EMPTY;
    private boolean returning;
    private final InserterMotion motion = new InserterMotion(this);
    public ItemStack heldItem() { return held.copy(); }
    @Override public InserterMotion motion() { return motion; }
    private int cycleTicks() { return ((InserterBlock) getBlockState().getBlock()).reach() == 2 ? LONG_CYCLE_TICKS : SHORT_CYCLE_TICKS; }
    public InserterEntity(BlockPos pos, BlockState state) {
        super(((InserterBlock) state.getBlock()).reach() == 2 ? FactoryContent.LONG_INSERTER_ENTITY.get() : FactoryContent.INSERTER_ENTITY.get(), pos, state,
                DEMAND_KW, ((InserterBlock) state.getBlock()).reach() == 2 ? LONG_CYCLE_TICKS : SHORT_CYCLE_TICKS);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, InserterEntity inserter) {
        int cycle = inserter.cycleTicks();
        if (!inserter.hasWork() || PowerNetwork.fraction(level, pos) <= 0) {
            inserter.motion.capture(inserter.held.isEmpty() && !inserter.returning ? 0 : inserter.currentWork() / cycle, 0, inserter.returning, false);
            return;
        }
        var facing = state.getValue(InserterBlock.FACING);
        int reach = ((InserterBlock) state.getBlock()).reach();
        BlockPos from = pos.relative(facing.getOpposite(), reach), to = pos.relative(facing, reach);
        boolean changed = false;
        if (inserter.held.isEmpty() && !inserter.returning) {
            ItemStack candidate = ItemTransfer.peekForOutput(level, from, to, facing);
            if (candidate.isEmpty()) { inserter.motion.capture(0, 0, false, false); return; }
            inserter.held = ItemTransfer.takeSource(level, from, facing, candidate);
            if (inserter.held.isEmpty()) { inserter.motion.capture(0, 0, false, false); return; }
            changed = true;
        }
        double limit = inserter.returning ? cycle : cycle / 2.0;
        double rate = inserter.advanceInserterWork();
        if (inserter.currentWork() + 0.000001 >= limit) {
            if (inserter.returning) {
                inserter.returning = false; inserter.setWork(inserter.currentWork() - cycle); rate = 0; changed = true;
            } else if (ItemTransfer.inserterOutput(level, to, facing.getOpposite(), inserter.held)) {
                inserter.held = ItemStack.EMPTY; inserter.returning = true; changed = true;
            } else { inserter.setWork(limit); rate = 0; }
        }
        inserter.motion.capture(inserter.held.isEmpty() && !inserter.returning ? 0 : inserter.currentWork() / cycle, rate / cycle, inserter.returning, changed);
    }

    @Override protected boolean hasWork() {
        if (level == null) return false;
        if (returning) return true;
        var facing = getBlockState().getValue(InserterBlock.FACING);
        int reach = ((InserterBlock) getBlockState().getBlock()).reach();
        BlockPos from = worldPosition.relative(facing.getOpposite(), reach), to = worldPosition.relative(facing, reach);
        return held.isEmpty() ? !ItemTransfer.peekForOutput(level, from, to, facing).isEmpty()
                : currentWork() < cycleTicks() / 2.0 || ItemTransfer.canInserterOutput(level, to, facing.getOpposite(), held);
    }
    public void dropHeld() {
        if (level != null && !level.isClientSide && !held.isEmpty()) {
            level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, held));
            held = ItemStack.EMPTY;
        }
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.put("Held", held.save(new CompoundTag())); tag.putBoolean("Returning", returning);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); held = ItemStack.of(tag.getCompound("Held")); returning = tag.getBoolean("Returning");
        if (!tag.contains("Returning") && held.isEmpty()) resetWork();
        motion.read(tag, held.isEmpty() && !returning ? 0 : currentWork() / cycleTicks(), returning);
    }
    @Override public CompoundTag getUpdateTag() { var tag = saveWithoutMetadata(); motion.write(tag); return tag; }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).inflate(((InserterBlock) getBlockState().getBlock()).reach() + 0.5); }
}
