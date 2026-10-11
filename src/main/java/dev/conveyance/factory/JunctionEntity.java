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

/** Each port has separate left/right entrances and exits, with independent round-robin cursors. */
public final class JunctionEntity extends BlockEntity {
    static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    public static final int LANES = 2, PER_LANE = 8, CAPACITY = LANES * PER_LANE;
    public static final double MOVE_TICKS = 8.0 / 3;
    public double moveTicks() { return getBlockState().is(FactoryContent.FAST_JUNCTION.get()) ? MOVE_TICKS / 2 : MOVE_TICKS; }
    private double movementTime() { return Math.floor((level.getGameTime() + .000001) / moveTicks()) * moveTicks(); }
    private final NonNullList<ItemStack> items = NonNullList.withSize(CAPACITY, ItemStack.EMPTY);
    private final double[] moveStart = new double[CAPACITY];
    private final float[] fromX = new float[CAPACITY], fromZ = new float[CAPACITY];
    private final int[] inputCursor = new int[LANES], outputCursor = new int[LANES];
    boolean recovered;
    public JunctionEntity(BlockPos pos, BlockState state) { super(FactoryContent.JUNCTION_ENTITY.get(), pos, state); }
    public static JunctionEntity at(Level level, BlockPos pos) {
        return level.hasChunkAt(pos) && level.getBlockState(pos).getBlock() instanceof JunctionBlock
                && level.getBlockEntity(pos) instanceof JunctionEntity junction ? junction : null;
    }
    private static int index(Direction side) {
        return switch (side) { case NORTH -> 0; case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> -1; };
    }
    private static int entrance(int side, int lane) { return lane * PER_LANE + side; }
    private static int exit(int side, int lane) { return entrance(side, lane) + 4; }

    public static void tick(Level level, BlockPos pos, BlockState state, JunctionEntity junction) {
        state = JunctionBlock.updatePorts(level, pos);
        boolean changed = false;
        for (int lane = 0; lane < LANES; lane++) {
            for (int side = 0; side < 4; side++) {
                int exit = exit(side, lane), input = entrance(side, lane);
                if (junction.items.get(exit).isEmpty()) continue;
                if (JunctionBlock.port(state, SIDES[side]) != JunctionBlock.Port.OUTPUT) {
                    if (junction.items.get(input).isEmpty() && junction.ready(exit)) {
                        junction.move(exit, input);
                        changed = true;
                    }
                } else if (junction.ready(exit)) {
                    ConveyorEntity belt = ConveyorEntity.connection(level, pos.relative(SIDES[side]), SIDES[side].getOpposite());
                    if (belt != null && belt.offer(junction.items.get(exit), SIDES[side].getOpposite(), lane)) {
                        junction.items.set(exit, ItemStack.EMPTY);
                        changed = true;
                    }
                }
            }
            // Preserve the lane while fairly selecting both input and output ports.
            for (int transfer = 0; transfer < 4; transfer++) {
                int input = junction.nextInput(lane), output = junction.nextOutput(state, lane);
                if (input < 0 || output < 0) break;
                junction.move(entrance(input, lane), exit(output, lane));
                junction.inputCursor[lane] = (input + 1) % 4;
                junction.outputCursor[lane] = (output + 1) % 4;
                changed = true;
            }
            for (int side = 0; side < 4; side++) {
                if (JunctionBlock.port(state, SIDES[side]) != JunctionBlock.Port.INPUT || !junction.items.get(entrance(side, lane)).isEmpty()) continue;
                ConveyorEntity belt = ConveyorEntity.outletAt(level, pos.relative(SIDES[side]));
                if (belt == null || !belt.outputPosition().equals(pos)) continue;
                ItemStack stack = belt.takeFront(lane);
                if (!stack.isEmpty()) {
                    junction.arrive(stack, side, lane);
                    changed = true;
                }
            }
        }
        if (changed) junction.changed();
    }
    private int nextInput(int lane) {
        for (int offset = 0; offset < 4; offset++) {
            int side = (inputCursor[lane] + offset) % 4, slot = entrance(side, lane);
            if (!items.get(slot).isEmpty() && ready(slot)) return side;
        }
        return -1;
    }
    private int nextOutput(BlockState state, int lane) {
        for (int offset = 0; offset < 4; offset++) {
            int side = (outputCursor[lane] + offset) % 4;
            if (JunctionBlock.port(state, SIDES[side]) == JunctionBlock.Port.OUTPUT && items.get(exit(side, lane)).isEmpty()) return side;
        }
        return -1;
    }
    public boolean offer(ItemStack stack, Direction incomingSide) { return offer(stack, incomingSide, ConveyorEntity.LEFT); }
    public boolean offer(ItemStack stack, Direction incomingSide, int lane) {
        if (stack.isEmpty() || !canAccept(incomingSide, lane)) return false;
        arrive(stack, index(incomingSide), lane);
        changed();
        return true;
    }
    public boolean canAccept(Direction incomingSide) { return canAccept(incomingSide, ConveyorEntity.LEFT); }
    public boolean canAccept(Direction incomingSide, int lane) {
        int side = index(incomingSide);
        return level != null && !level.isClientSide && side >= 0 && lane >= 0 && lane < LANES
                && items.get(entrance(side, lane)).isEmpty()
                && JunctionBlock.port(getBlockState(), incomingSide) != JunctionBlock.Port.OUTPUT;
    }
    private void arrive(ItemStack stack, int side, int lane) {
        int slot = entrance(side, lane);
        items.set(slot, stack.copyWithCount(1));
        Direction left = SIDES[side].getOpposite().getCounterClockWise();
        float offset = lane == ConveyorEntity.LEFT ? 0.1875F : -0.1875F;
        fromX[slot] = 0.5F + SIDES[side].getStepX() * 0.5F + left.getStepX() * offset;
        fromZ[slot] = 0.5F + SIDES[side].getStepZ() * 0.5F + left.getStepZ() * offset;
        moveStart[slot] = movementTime();
    }
    public ItemStack peekOutput(Direction outputSide, int lane) {
        int side = index(outputSide);
        return level != null && !level.isClientSide && side >= 0 && ready(exit(side, lane))
                && JunctionBlock.port(getBlockState(), outputSide) == JunctionBlock.Port.OUTPUT
                ? items.get(exit(side, lane)).copy() : ItemStack.EMPTY;
    }
    public ItemStack takeOutput(Direction outputSide, int lane) {
        int side = index(outputSide);
        if (level == null || level.isClientSide || side < 0 || !ready(exit(side, lane))
                || JunctionBlock.port(getBlockState(), outputSide) != JunctionBlock.Port.OUTPUT) return ItemStack.EMPTY;
        int slot = exit(side, lane);
        ItemStack stack = items.get(slot);
        if (!stack.isEmpty()) {
            items.set(slot, ItemStack.EMPTY);
            changed();
        }
        return stack;
    }
    private void move(int source, int destination) {
        fromX[destination] = targetX(source);
        fromZ[destination] = targetZ(source);
        moveStart[destination] = movementTime();
        items.set(destination, items.get(source));
        items.set(source, ItemStack.EMPTY);
    }
    private boolean ready(int slot) { return movementTime() - moveStart[slot] + .000001 >= moveTicks(); }
    private float targetX(int slot) { return target(slot, true); }
    private float targetZ(int slot) { return target(slot, false); }
    private float target(int slot, boolean xAxis) {
        int position = slot % PER_LANE;
        Direction side = SIDES[position % 4], travel = position < 4 ? side.getOpposite() : side;
        Direction left = travel.getCounterClockWise();
        float offset = slot / PER_LANE == ConveyorEntity.LEFT ? 0.1875F : -0.1875F;
        return 0.5F + (xAxis ? side.getStepX() : side.getStepZ()) * (position < 4 ? 0.2F : 0.35F)
                + (xAxis ? left.getStepX() : left.getStepZ()) * offset;
    }
    private float progress(int slot, float partialTick) {
        return level == null ? 1 : (float) Mth.clamp((level.getGameTime() - moveStart[slot] + partialTick) / moveTicks(), 0, 1);
    }
    public float renderX(int slot, float partialTick) { return Mth.lerp(progress(slot, partialTick), fromX[slot], targetX(slot)); }
    public float renderZ(int slot, float partialTick) { return Mth.lerp(progress(slot, partialTick), fromZ[slot], targetZ(slot)); }
    public ItemStack item(int slot) { return items.get(slot); }
    public int storedCount() { return (int) items.stream().filter(stack -> !stack.isEmpty()).count(); }
    private void changed() {
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putIntArray("InputCursors", inputCursor); tag.putIntArray("OutputCursors", outputCursor);
        tag.putInt("Lanes", LANES);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        items.clear();
        ContainerHelper.loadAllItems(tag, items);
        int[] inputs = tag.getIntArray("InputCursors"), outputs = tag.getIntArray("OutputCursors");
        for (int lane = 0; lane < LANES; lane++) {
            inputCursor[lane] = Math.floorMod(lane < inputs.length ? inputs[lane] : lane == 0 ? tag.getInt("InputCursor") : 0, 4);
            outputCursor[lane] = Math.floorMod(lane < outputs.length ? outputs[lane] : lane == 0 ? tag.getInt("OutputCursor") : 0, 4);
        }
        for (int slot = 0; slot < CAPACITY; slot++) {
            if (!items.get(slot).isEmpty()) items.get(slot).setCount(1);
            moveStart[slot] = -MOVE_TICKS;
            fromX[slot] = targetX(slot); fromZ[slot] = targetZ(slot);
        }
        ListTag motion = tag.getList("Motion", Tag.TAG_COMPOUND);
        for (int slot = 0; slot < Math.min(CAPACITY, motion.size()); slot++) {
            CompoundTag entry = motion.getCompound(slot);
            moveStart[slot] = entry.getDouble("Start"); fromX[slot] = entry.getFloat("X"); fromZ[slot] = entry.getFloat("Z");
        }
    }
    @Override public CompoundTag getUpdateTag() {
        CompoundTag tag = saveWithoutMetadata();
        ListTag motion = new ListTag();
        for (int slot = 0; slot < CAPACITY; slot++) {
            CompoundTag entry = new CompoundTag();
            entry.putDouble("Start", moveStart[slot]); entry.putFloat("X", fromX[slot]); entry.putFloat("Z", fromZ[slot]);
            motion.add(entry);
        }
        tag.put("Motion", motion);
        return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
