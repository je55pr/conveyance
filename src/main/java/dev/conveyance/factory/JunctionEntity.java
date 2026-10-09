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

/** Each port has a one-item entrance and exit. Independent cursors balance both ends. */
public final class JunctionEntity extends BlockEntity {
    static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    public static final int CAPACITY = 8;
    private static final int MOVE_TICKS = 5;
    private final NonNullList<ItemStack> items = NonNullList.withSize(CAPACITY, ItemStack.EMPTY);
    private final long[] moveStart = new long[CAPACITY];
    private final float[] fromX = new float[CAPACITY], fromZ = new float[CAPACITY];
    private int inputCursor, outputCursor;
    boolean recovered;
    public JunctionEntity(BlockPos pos, BlockState state) { super(FactoryContent.JUNCTION_ENTITY.get(), pos, state); }
    public static JunctionEntity at(Level level, BlockPos pos) {
        return level.hasChunkAt(pos) && level.getBlockState(pos).getBlock() instanceof JunctionBlock
                && level.getBlockEntity(pos) instanceof JunctionEntity junction ? junction : null;
    }
    private static int index(Direction side) {
        return switch (side) { case NORTH -> 0; case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> -1; };
    }
    public static void tick(Level level, BlockPos pos, BlockState state, JunctionEntity junction) {
        state = JunctionBlock.updatePorts(level, pos);
        boolean changed = false;
        // Empty completed exit positions into belts. Inserters collect from these positions directly.
        for (int side = 0; side < 4; side++) {
            int exit = side + 4;
            if (junction.items.get(exit).isEmpty()) continue;
            if (JunctionBlock.port(state, SIDES[side]) != JunctionBlock.Port.OUTPUT) {
                // Retain and reroute cargo when a connected belt is removed or rotated.
                if (junction.items.get(side).isEmpty() && junction.ready(exit)) {
                    junction.move(exit, side);
                    changed = true;
                }
            } else if (junction.ready(exit)) {
                ConveyorEntity belt = ConveyorEntity.at(level, pos.relative(SIDES[side]));
                if (belt != null && belt.offer(junction.items.get(exit), SIDES[side].getOpposite())) {
                    junction.items.set(exit, ItemStack.EMPTY);
                    changed = true;
                }
            }
        }
        // Route each ready input once, choosing the next available output independently.
        for (int transfer = 0; transfer < 4; transfer++) {
            int input = junction.nextInput(), output = junction.nextOutput(state);
            if (input < 0 || output < 0) break;
            junction.move(input, output + 4);
            junction.inputCursor = (input + 1) % 4;
            junction.outputCursor = (output + 1) % 4;
            changed = true;
        }
        // Reserved entrance positions prevent a fast lane from taking another lane's space.
        for (int side = 0; side < 4; side++) {
            if (JunctionBlock.port(state, SIDES[side]) != JunctionBlock.Port.INPUT || !junction.items.get(side).isEmpty()) continue;
            ConveyorEntity belt = ConveyorEntity.at(level, pos.relative(SIDES[side]));
            if (belt == null) continue;
            ItemStack stack = belt.takeFront();
            if (!stack.isEmpty()) {
                junction.arrive(stack, side);
                changed = true;
            }
        }
        if (changed) junction.changed();
    }
    private int nextInput() {
        for (int offset = 0; offset < 4; offset++) {
            int side = (inputCursor + offset) % 4;
            if (!items.get(side).isEmpty() && ready(side)) return side;
        }
        return -1;
    }
    private int nextOutput(BlockState state) {
        for (int offset = 0; offset < 4; offset++) {
            int side = (outputCursor + offset) % 4;
            if (JunctionBlock.port(state, SIDES[side]) == JunctionBlock.Port.OUTPUT && items.get(side + 4).isEmpty()) return side;
        }
        return -1;
    }
    public boolean offer(ItemStack stack, Direction incomingSide) {
        int side = index(incomingSide);
        if (stack.isEmpty() || !canAccept(incomingSide)) return false;
        arrive(stack, side);
        changed();
        return true;
    }
    public boolean canAccept(Direction incomingSide) {
        int side = index(incomingSide);
        return level != null && !level.isClientSide && side >= 0 && items.get(side).isEmpty()
                && JunctionBlock.port(getBlockState(), incomingSide) != JunctionBlock.Port.OUTPUT;
    }
    private void arrive(ItemStack stack, int side) {
        items.set(side, stack.copyWithCount(1));
        fromX[side] = 0.5F + SIDES[side].getStepX() * 0.5F;
        fromZ[side] = 0.5F + SIDES[side].getStepZ() * 0.5F;
        moveStart[side] = level.getGameTime();
    }
    public ItemStack peekOutput(Direction outputSide) {
        int side = index(outputSide);
        return level != null && !level.isClientSide && side >= 0 && ready(side + 4)
                && JunctionBlock.port(getBlockState(), outputSide) == JunctionBlock.Port.OUTPUT ? items.get(side + 4).copy() : ItemStack.EMPTY;
    }
    public ItemStack takeOutput(Direction outputSide) {
        int side = index(outputSide);
        if (level == null || level.isClientSide || side < 0 || !ready(side + 4)
                || JunctionBlock.port(getBlockState(), outputSide) != JunctionBlock.Port.OUTPUT) return ItemStack.EMPTY;
        ItemStack stack = items.get(side + 4);
        if (!stack.isEmpty()) {
            items.set(side + 4, ItemStack.EMPTY);
            changed();
        }
        return stack;
    }
    private void move(int source, int destination) {
        fromX[destination] = targetX(source);
        fromZ[destination] = targetZ(source);
        moveStart[destination] = level.getGameTime();
        items.set(destination, items.get(source));
        items.set(source, ItemStack.EMPTY);
    }
    private boolean ready(int slot) { return level.getGameTime() - moveStart[slot] >= MOVE_TICKS; }
    private float targetX(int slot) { return 0.5F + SIDES[slot % 4].getStepX() * (slot < 4 ? 0.2F : 0.35F); }
    private float targetZ(int slot) { return 0.5F + SIDES[slot % 4].getStepZ() * (slot < 4 ? 0.2F : 0.35F); }
    private float progress(int slot, float partialTick) {
        return level == null ? 1 : Mth.clamp((level.getGameTime() - moveStart[slot] + partialTick) / MOVE_TICKS, 0, 1);
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
        tag.putInt("InputCursor", inputCursor); tag.putInt("OutputCursor", outputCursor);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        items.clear();
        ContainerHelper.loadAllItems(tag, items);
        inputCursor = Math.floorMod(tag.getInt("InputCursor"), 4);
        outputCursor = Math.floorMod(tag.getInt("OutputCursor"), 4);
        for (int slot = 0; slot < CAPACITY; slot++) {
            if (!items.get(slot).isEmpty()) items.get(slot).setCount(1);
            moveStart[slot] = -MOVE_TICKS;
            fromX[slot] = targetX(slot); fromZ[slot] = targetZ(slot);
        }
        ListTag motion = tag.getList("Motion", Tag.TAG_COMPOUND);
        for (int slot = 0; slot < Math.min(CAPACITY, motion.size()); slot++) {
            CompoundTag entry = motion.getCompound(slot);
            moveStart[slot] = entry.getLong("Start");
            fromX[slot] = entry.getFloat("X"); fromZ[slot] = entry.getFloat("Z");
        }
    }
    @Override public CompoundTag getUpdateTag() {
        CompoundTag tag = saveWithoutMetadata();
        ListTag motion = new ListTag();
        for (int slot = 0; slot < CAPACITY; slot++) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Start", moveStart[slot]); entry.putFloat("X", fromX[slot]); entry.putFloat("Z", fromZ[slot]);
            motion.add(entry);
        }
        tag.put("Motion", motion);
        return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
