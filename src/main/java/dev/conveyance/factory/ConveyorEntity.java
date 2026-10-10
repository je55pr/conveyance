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
import net.minecraft.world.phys.Vec3;

/** Two independent four-item lanes, numbered left/right in the direction of travel. */
public final class ConveyorEntity extends BlockEntity {
    public static final int LEFT = 0, RIGHT = 1, LANES = 2, POSITIONS = 4, CAPACITY = LANES * POSITIONS;
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
    public Direction entrySide() {
        if (slope()) return travelDirection().getOpposite();
        return switch (getBlockState().getValue(ConveyorBlock.TURN)) {
            case LEFT -> travelDirection().getCounterClockWise();
            case RIGHT -> travelDirection().getClockWise();
            default -> travelDirection().getOpposite();
        };
    }
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
        } else if (level.getGameTime() % 5 == 0) ConveyorBlock.update(level, pos);
        boolean changed = false;
        for (int lane = 0; lane < LANES; lane++) {
            int first = lane * POSITIONS, front = first + POSITIONS - 1;
            if (!belt.items.get(front).isEmpty() && belt.ready(front)
                    && ItemTransfer.outputFromBelt(level, belt.outputPosition(), belt.travelDirection().getOpposite(), belt.items.get(front), lane)) {
                belt.items.set(front, ItemStack.EMPTY);
                changed = true;
            }
            // Each lane advances independently, from its front backwards.
            for (int slot = front; slot > first; slot--) {
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
        }
        if (changed) belt.changed();
    }

    /** Accept one item at the entrance. Callers retain ownership unless this succeeds. */
    public boolean offer(ItemStack stack, Direction incomingSide) {
        return offer(stack, incomingSide, nearestLane(incomingSide));
    }
    public boolean offer(ItemStack stack, Direction incomingSide, int lane) {
        if (stack.isEmpty() || !canAccept(incomingSide, lane)) return false;
        return arrive(stack, incomingSide, streamLane(incomingSide, lane), insertionPosition(incomingSide));
    }
    public boolean canAccept(Direction incomingSide) {
        return canAccept(incomingSide, nearestLane(incomingSide));
    }
    public boolean canAccept(Direction incomingSide, int lane) {
        return horizontal(incomingSide) && (slope() ? incomingSide == entrySide() : incomingSide != travelDirection())
                && free(streamLane(incomingSide, lane), insertionPosition(incomingSide));
    }
    private static boolean horizontal(Direction side) { return side.getAxis().isHorizontal(); }
    private int streamLane(Direction incomingSide, int lane) { return incomingSide == entrySide() ? lane : nearestLane(incomingSide); }
    private int insertionPosition(Direction side) {
        return side == entrySide() ? 0 : side == travelDirection() ? POSITIONS - 1 : POSITIONS / 2;
    }
    public int nearestLane(Direction side) { return side == travelDirection().getClockWise() ? RIGHT : LEFT; }
    public int inserterLane(Direction incomingSide) {
        if (incomingSide.getAxis() != travelDirection().getAxis()) return nearestLane(incomingSide);
        // Aligned belts use the inserter's left, even when that is the belt's right.
        return incomingSide.getOpposite() == travelDirection() ? LEFT : RIGHT;
    }
    public boolean canAcceptInserter(Direction incomingSide) {
        return horizontal(incomingSide) && free(inserterLane(incomingSide), insertionPosition(incomingSide));
    }
    public boolean offerInserter(ItemStack stack, Direction incomingSide) {
        return !stack.isEmpty() && canAcceptInserter(incomingSide)
                && arrive(stack, incomingSide, inserterLane(incomingSide), insertionPosition(incomingSide));
    }
    private boolean free(int lane, int position) {
        return lane >= 0 && lane < LANES && level != null && !level.isClientSide && moving()
                && items.get(lane * POSITIONS + position).isEmpty();
    }
    private boolean arrive(ItemStack stack, Direction side, int lane, int position) {
        int slot = lane * POSITIONS + position;
        items.set(slot, stack.copyWithCount(1));
        Direction left = side.getOpposite().getCounterClockWise();
        float offset = lane == LEFT ? 0.1875F : -0.1875F;
        fromX[slot] = 0.5F + side.getStepX() * 0.5F + left.getStepX() * offset;
        fromZ[slot] = 0.5F + side.getStepZ() * 0.5F + left.getStepZ() * offset;
        if (side != entrySide()) {
            fromX[slot] = side.getAxis() == Direction.Axis.X ? 0.5F + side.getStepX() * 0.5F : targetX(slot);
            fromZ[slot] = side.getAxis() == Direction.Axis.Z ? 0.5F + side.getStepZ() * 0.5F : targetZ(slot);
        }
        fromY[slot] = position == 0 ? 0.13F + inputY() - worldPosition.getY() : targetY(slot);
        moveStart[slot] = level.getGameTime();
        changed();
        return true;
    }
    public int nearestLane(Vec3 hit) {
        double best = Double.MAX_VALUE;
        int result = LEFT;
        for (int slot = 0; slot < CAPACITY; slot++) {
            double dx = hit.x - worldPosition.getX() - targetX(slot), dz = hit.z - worldPosition.getZ() - targetZ(slot);
            if (dx * dx + dz * dz < best) { best = dx * dx + dz * dz; result = slot / POSITIONS; }
        }
        return result;
    }

    /** Only the item at the finished end of the lane is available to an inserter. */
    public ItemStack peekFront(int lane) {
        int front = lane * POSITIONS + POSITIONS - 1;
        return level != null && !level.isClientSide && moving() && ready(front) ? items.get(front).copy() : ItemStack.EMPTY;
    }
    public ItemStack takeFront(int lane) {
        int front = lane * POSITIONS + POSITIONS - 1;
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
        return target(slot, true);
    }
    private float targetZ(int slot) {
        return target(slot, false);
    }
    private float target(int slot, boolean xAxis) {
        float t = (slot % POSITIONS + 0.5F) / POSITIONS;
        float offset = slot / POSITIONS == LEFT ? 0.1875F : -0.1875F;
        Direction travel = travelDirection(), left = travel.getCounterClockWise();
        if (entrySide() == travel.getOpposite())
            return 0.5F + (xAxis ? travel.getStepX() : travel.getStepZ()) * (t - 0.5F)
                    + (xAxis ? left.getStepX() : left.getStepZ()) * offset;
        // Quarter circles share a corner with the corresponding belt model.
        boolean fromLeft = entrySide() == left;
        float radius = 0.5F + (fromLeft ? -offset : offset);
        float angle = (1 - t) * (float) Math.PI / 2;
        float lateral = (fromLeft ? -0.5F : 0.5F) + (fromLeft ? 1 : -1) * radius * Mth.cos(angle);
        float forward = 0.5F - radius * Mth.sin(angle);
        return 0.5F + (xAxis ? left.getStepX() : left.getStepZ()) * -lateral
                + (xAxis ? travel.getStepX() : travel.getStepZ()) * forward;
    }
    private float targetY(int slot) { float t = (slot % POSITIONS + 0.5F) / POSITIONS; return 0.13F + (slope() ? down() ? 1 - t : t : 0); }
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
        tag.putInt("Lanes", LANES);
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
