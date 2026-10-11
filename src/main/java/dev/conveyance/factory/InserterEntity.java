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
    public static final int SHORT_CYCLE_TICKS = 24, LONG_CYCLE_TICKS = 17;
    private ItemStack held = ItemStack.EMPTY;
    private boolean returning;
    private final InserterMotion motion = new InserterMotion(this);
    public ItemStack heldItem() { return held.copy(); }
    @Override public InserterMotion motion() { return motion; }
    private double cycleTicks() { return ((InserterBlock) getBlockState().getBlock()).fast() ? 26.0 / 3 : ((InserterBlock) getBlockState().getBlock()).reach() == 2 ? 50.0 / 3 : SHORT_CYCLE_TICKS; }
    public InserterEntity(BlockPos pos, BlockState state) {
        super(((InserterBlock) state.getBlock()).reach() == 2 ? FactoryContent.LONG_INSERTER_ENTITY.get() : FactoryContent.INSERTER_ENTITY.get(), pos, state,
                InserterPower.peakKW(((InserterBlock) state.getBlock()).reach(), false) - InserterPower.IDLE_KW,
                ((InserterBlock) state.getBlock()).reach() == 2 ? LONG_CYCLE_TICKS : SHORT_CYCLE_TICKS);
    }
    @Override protected double idlePower() { return ((InserterBlock) getBlockState().getBlock()).fast() ? .5 : InserterPower.IDLE_KW; }
    @Override protected double workingPower() {
        if (currentWork() >= (returning ? cycleTicks() : cycleTicks() / 2.0)) return 0;
        if (((InserterBlock) getBlockState().getBlock()).fast()) return InserterPower.fastMovementKW(currentWork(), cycleTicks());
        return InserterPower.movementKW(((InserterBlock) getBlockState().getBlock()).reach(), false,
                currentWork(), cycleTicks(), level == null ? 1 : dev.conveyance.player.ResearchState.inserterSpeed(level));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, InserterEntity inserter) {
        double cycle = inserter.cycleTicks();
        if (!inserter.hasWork() || PowerNetwork.fraction(level, pos) <= 0) {
            inserter.motion.capture(inserter.held.isEmpty() && !inserter.returning ? 0 : inserter.currentWork() / cycle, 0, inserter.returning, false);
            return;
        }
        var facing = state.getValue(InserterBlock.FACING);
        int reach = ((InserterBlock) state.getBlock()).reach();
        BlockPos from = pos.relative(facing.getOpposite(), reach), to = pos.relative(facing, reach);
        boolean changed = false;
        double budget = PowerNetwork.fraction(level, pos), rate = 0;
        // Carry fractional ticks through the pickup/drop/return boundaries.
        for (int transition = 0; transition < 4 && budget > .000001; transition++) {
            if (inserter.held.isEmpty() && !inserter.returning) {
                ItemStack candidate = ItemTransfer.peekForOutput(level, from, to, facing);
                if (candidate.isEmpty()) break;
                inserter.held = ItemTransfer.takeSource(level, from, facing, candidate);
                if (inserter.held.isEmpty()) break;
                changed = true;
            }
            double limit = inserter.returning ? cycle : cycle / 2;
            double step = Math.min(budget, Math.max(0, limit - inserter.currentWork()));
            inserter.setWork(inserter.currentWork() + step); budget -= step; rate += step;
            if (inserter.currentWork() + .000001 < limit) break;
            if (inserter.returning) {
                inserter.returning = false; inserter.setWork(0); changed = true;
            } else if (ItemTransfer.inserterOutput(level, to, facing.getOpposite(), inserter.held)) {
                inserter.held = ItemStack.EMPTY; inserter.returning = true; changed = true;
            } else { rate = 0; break; }
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
