package dev.conveyance.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A single lane of four owned items. The server moves cargo; the client only draws it. */
public final class ConveyorEntity extends BlockEntity {
    public static final int CAPACITY = 4;
    public static final int MOVE_TICKS = 5;
    private final NonNullList<ItemStack> items = NonNullList.withSize(CAPACITY, ItemStack.EMPTY);
    private final long[] moveStart = new long[CAPACITY];
    private final float[] fromX = new float[CAPACITY], fromY = new float[CAPACITY], fromZ = new float[CAPACITY];
    boolean recovered;

    public ConveyorEntity(BlockPos pos, BlockState state) {
        super(FactoryContent.CONVEYOR_ENTITY.get(), pos, state);
    }

    public static ConveyorEntity at(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !(level.getBlockState(pos).getBlock() instanceof ConveyorBlock)) return null;
        return level.getBlockEntity(pos) instanceof ConveyorEntity belt ? belt : null;
    }
    public boolean slope() { return getBlockState().getBlock() instanceof SlopeConveyorBlock; }
    public boolean moving() { return !slope() || getBlockState().getValue(SlopeConveyorBlock.FLOW).moving(); }
    public boolean configured() { return !slope() || getBlockState().getValue(SlopeConveyorBlock.FLOW) != SlopeConveyorBlock.Flow.UNSET; }
    private boolean down() { return slope() && getBlockState().getValue(SlopeConveyorBlock.FLOW).down(); }
    public Direction travelDirection() { return down() ? getBlockState().getValue(ConveyorBlock.FACING).getOpposite() : getBlockState().getValue(ConveyorBlock.FACING); }
    private int inputY() { return worldPosition.getY() + (down() ? 1 : 0); }
    public int outputY() { return worldPosition.getY() + (slope() && !down() ? 1 : 0); }
    public BlockPos outputPosition() { return worldPosition.relative(travelDirection()).offset(0, outputY() - worldPosition.getY(), 0); }
    public boolean potentialEntry(BlockPos nominal, Direction side) {
        if (!slope()) return nominal.equals(worldPosition) && side != travelDirection();
        if (!configured()) return nominal.getX() == worldPosition.getX() && nominal.getZ() == worldPosition.getZ()
                && (nominal.getY() == worldPosition.getY() || nominal.getY() == worldPosition.getY() + 1);
        return nominal.equals(worldPosition.offset(0, inputY() - worldPosition.getY(), 0)) && side == travelDirection().getOpposite();
    }
    public static ConveyorEntity connection(Level level, BlockPos nominal, Direction side) {
        ConveyorEntity direct = at(level, nominal);
        if (direct != null && direct.moving() && direct.potentialEntry(nominal, side)) return direct;
        ConveyorEntity lower = at(level, nominal.below());
        return lower != null && lower.slope() && lower.moving() && lower.potentialEntry(nominal, side) ? lower : null;
    }
    public static ConveyorEntity outletAt(Level level, BlockPos nominal) {
        ConveyorEntity direct = at(level, nominal);
        if (direct != null && direct.outputY() == nominal.getY()) return direct;
        ConveyorEntity lower = at(level, nominal.below());
        return lower != null && lower.slope() && lower.outputY() == nominal.getY() ? lower : null;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ConveyorEntity belt) {
        if (belt.slope()) {
            if (level.getGameTime() % 5 == 0) SlopeConveyorBlock.update(level, pos);
            if (!belt.moving()) return;
        }
        boolean changed = false;
        int front = CAPACITY - 1;
        Direction facing = belt.travelDirection();
        if (!belt.items.get(front).isEmpty() && belt.ready(front)) {
            if (ItemTransfer.output(level, belt.outputPosition(), facing.getOpposite(), belt.items.get(front))) {
                belt.items.set(front, ItemStack.EMPTY);
                changed = true;
            }
        }
        // Work from the front so each item advances at most one position in this tick.
        for (int slot = front; slot > 0; slot--) {
            if (belt.items.get(slot).isEmpty() && !belt.items.get(slot - 1).isEmpty() && belt.ready(slot - 1)) {
                belt.fromX[slot] = belt.targetX(slot - 1);
                belt.fromY[slot] = belt.targetY(slot - 1);
                belt.fromZ[slot] = belt.targetZ(slot - 1);
                belt.moveStart[slot] = level.getGameTime();
                belt.items.set(slot, belt.items.get(slot - 1));
                belt.items.set(slot - 1, ItemStack.EMPTY);
                changed = true;
            }
        }
        if (changed) belt.changed();
    }

    /** Accept one item at the entrance. Callers retain ownership unless this succeeds. */
    public boolean offer(ItemStack stack, Direction incomingSide) {
        if (stack.isEmpty() || !canAccept(incomingSide)) return false;
        items.set(0, stack.copyWithCount(1));
        fromX[0] = 0.5F + incomingSide.getStepX() * 0.5F;
        fromZ[0] = 0.5F + incomingSide.getStepZ() * 0.5F;
        fromY[0] = 0.13F + inputY() - worldPosition.getY();
        moveStart[0] = level.getGameTime();
        changed();
        return true;
    }

    public boolean canAccept(Direction incomingSide) {
        return level != null && !level.isClientSide && moving() && items.get(0).isEmpty()
                && (slope() ? incomingSide == travelDirection().getOpposite() : incomingSide != travelDirection());
    }

    /** Only the item at the finished end of the lane is available to an inserter. */
    public ItemStack peekFront() {
        int front = CAPACITY - 1;
        return level != null && !level.isClientSide && moving() && ready(front) ? items.get(front).copy() : ItemStack.EMPTY;
    }
    public ItemStack takeFront() {
        int front = CAPACITY - 1;
        if (level == null || level.isClientSide || !moving() || !ready(front)) return ItemStack.EMPTY;
        ItemStack result = items.get(front);
        if (!result.isEmpty()) {
            items.set(front, ItemStack.EMPTY);
            changed();
        }
        return result;
    }

    private boolean ready(int slot) { return level.getGameTime() - moveStart[slot] >= MOVE_TICKS; }
    public ItemStack item(int slot) { return items.get(slot); }

    private float targetX(int slot) {
        return 0.5F + travelDirection().getStepX() * ((slot + 0.5F) / CAPACITY - 0.5F);
    }
    private float targetZ(int slot) {
        return 0.5F + travelDirection().getStepZ() * ((slot + 0.5F) / CAPACITY - 0.5F);
    }
    private float targetY(int slot) { float t = (slot + 0.5F) / CAPACITY; return 0.13F + (slope() ? down() ? 1 - t : t : 0); }
    private float progress(int slot, float partialTick) {
        return level == null ? 1 : Mth.clamp((level.getGameTime() - moveStart[slot] + partialTick) / MOVE_TICKS, 0, 1);
    }
    public float renderX(int slot, float partialTick) { return Mth.lerp(progress(slot, partialTick), fromX[slot], targetX(slot)); }
    public float renderZ(int slot, float partialTick) { return Mth.lerp(progress(slot, partialTick), fromZ[slot], targetZ(slot)); }
    public float renderY(int slot, float partialTick) { return Mth.lerp(progress(slot, partialTick), fromY[slot], targetY(slot)); }

    private void changed() {
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        items.clear();
        ContainerHelper.loadAllItems(tag, items);
        for (int slot = 0; slot < CAPACITY; slot++) {
            if (!items.get(slot).isEmpty()) items.get(slot).setCount(1);
            moveStart[slot] = -MOVE_TICKS;
            fromX[slot] = targetX(slot);
            fromY[slot] = targetY(slot);
            fromZ[slot] = targetZ(slot);
        }
        // Animation clocks are sent to clients, but not stored in saves or packed belt items.
        ListTag motion = tag.getList("Motion", Tag.TAG_COMPOUND);
        for (int slot = 0; slot < Math.min(CAPACITY, motion.size()); slot++) {
            CompoundTag entry = motion.getCompound(slot);
            moveStart[slot] = entry.getLong("Start");
            fromX[slot] = entry.getFloat("X");
            fromZ[slot] = entry.getFloat("Z");
            fromY[slot] = entry.contains("Y") ? entry.getFloat("Y") : targetY(slot);
        }
    }

    @Override public CompoundTag getUpdateTag() {
        CompoundTag tag = saveWithoutMetadata();
        ListTag motion = new ListTag();
        for (int slot = 0; slot < CAPACITY; slot++) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Start", moveStart[slot]);
            entry.putFloat("X", fromX[slot]);
            entry.putFloat("Z", fromZ[slot]);
            entry.putFloat("Y", fromY[slot]);
            motion.add(entry);
        }
        tag.put("Motion", motion);
        return tag;
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
