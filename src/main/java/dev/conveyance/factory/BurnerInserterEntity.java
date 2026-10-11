package dev.conveyance.factory;

import net.minecraft.core.BlockPos;
import dev.conveyance.player.ResearchState;
import dev.conveyance.pollution.PollutionData;
import dev.conveyance.pollution.PollutionRates;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Fuel and cargo are separate: available coal or Wood can bootstrap an empty arm. */
public final class BurnerInserterEntity extends BlockEntity implements Container, MenuProvider, MovingInserter {
    public static final int CYCLE_TICKS = 20;
    private ItemStack fuel = ItemStack.EMPTY, held = ItemStack.EMPTY;
    public ItemStack heldItem() { return held.copy(); }
    private final BurnerFuel burner = new BurnerFuel(0.5);
    private double work;
    private boolean returning;
    private final InserterMotion motion = new InserterMotion(this);
    @Override public InserterMotion motion() { return motion; }
    private boolean recovered;
    public BurnerInserterEntity(BlockPos pos, BlockState state) { super(FactoryContent.BURNER_INSERTER_ENTITY.get(), pos, state); }
    public int fuelRemaining() { return burner.remainingTicks(InserterPower.BURNER_PEAK_KW); }
    public double fuelReserveMJ() { return burner.reserveMJ(); }

    public static void tick(Level level, BlockPos pos, BlockState state, BurnerInserterEntity arm) {
        var facing = state.getValue(InserterBlock.FACING);
        BlockPos from = pos.relative(facing.getOpposite()), to = pos.relative(facing);
        boolean changed = false;
        if (!arm.burner.available(arm.fuel)) {
            ItemStack picked = ItemTransfer.takeSource(level, from, facing, true);
            if (picked.isEmpty() && GeneratorEntity.isFuel(arm.held)) {
                picked = arm.held;
                arm.held = ItemStack.EMPTY;
                arm.work = CYCLE_TICKS - Math.min(CYCLE_TICKS / 2.0, arm.work);
                arm.returning = true;
            }
            if (!picked.isEmpty()) { arm.fuel = picked; changed = true; }
        }
        if (arm.held.isEmpty() && !arm.returning) {
            ItemStack candidate = ItemTransfer.peekForOutput(level, from, to, facing);
            if (candidate.isEmpty() || !arm.burner.available(arm.fuel)) { arm.sync(0, changed); return; }
            arm.held = ItemTransfer.takeSource(level, from, facing, candidate);
            if (arm.held.isEmpty()) { arm.sync(0, changed); return; }
            changed = true;
        }
        if (!arm.returning && arm.work >= CYCLE_TICKS / 2.0 && !ItemTransfer.canInserterOutput(level, to, facing.getOpposite(), arm.held)) { arm.sync(0, changed); return; }
        double limit = arm.returning ? CYCLE_TICKS : CYCLE_TICKS / 2.0;
        double speed = ResearchState.inserterSpeed(level), step = Math.min(speed, Math.max(0, limit - arm.work));
        double fraction = arm.burner.consume(level, arm.fuel, step == 0 ? 0 : InserterPower.movementKW(1, true, arm.work, CYCLE_TICKS, speed));
        if (fraction <= 0) { arm.sync(0, changed); return; }
        PollutionData.emit(level, pos, PollutionRates.of(state.getBlock()) * fraction);
        if (step > 0) arm.setChanged();
        double rate = step * fraction;
        arm.work += rate;
        if (arm.work + 0.000001 >= limit) {
            if (arm.returning) {
                arm.returning = false; arm.work = Math.max(0, arm.work - CYCLE_TICKS); rate = 0; changed = true;
            } else if (ItemTransfer.inserterOutput(level, to, facing.getOpposite(), arm.held)) {
                arm.held = ItemStack.EMPTY; arm.returning = true; changed = true;
            } else { arm.work = limit; rate = 0; }
        }
        arm.sync(rate, changed);
    }
    private void sync(double rate, boolean force) { motion.capture(held.isEmpty() && !returning ? 0 : work / CYCLE_TICKS, rate / CYCLE_TICKS, returning, force); }
    public void recover() {
        if (level == null || level.isClientSide || recovered) return;
        recovered = true;
        ItemStack packed = new ItemStack(FactoryContent.BURNER_INSERTER_ITEM.get());
        BlockItem.setBlockEntityData(packed, FactoryContent.BURNER_INSERTER_ENTITY.get(), saveWithoutMetadata());
        Block.popResource(level, worldPosition, packed);
    }
    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return fuel.isEmpty(); }
    @Override public ItemStack getItem(int slot) { return fuel; }
    @Override public ItemStack removeItem(int slot, int count) { ItemStack stack = fuel.split(count); setChanged(); return stack; }
    @Override public ItemStack removeItemNoUpdate(int slot) { ItemStack stack = fuel; fuel = ItemStack.EMPTY; setChanged(); return stack; }
    @Override public void setItem(int slot, ItemStack stack) { fuel = stack; setChanged(); }
    @Override public void clearContent() { fuel = ItemStack.EMPTY; setChanged(); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return GeneratorEntity.isFuel(stack); }
    @Override public boolean canTakeItem(Container destination, int slot, ItemStack stack) { return false; }
    @Override public boolean stillValid(Player player) { return !isRemoved() && (RemoteBuilding.canReach(player, worldPosition) || player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) < 64); }
    @Override public Component getDisplayName() { return Component.translatable("block.conveyance.burner_inserter"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new GeneratorMenu(id, inventory, this); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Fuel", fuel.save(new CompoundTag())); tag.put("Held", held.save(new CompoundTag()));
        burner.save(tag); tag.putInt("BurnTicks", fuelRemaining()); tag.putDouble("Work", work);
        tag.putBoolean("Returning", returning);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        fuel = ItemStack.of(tag.getCompound("Fuel"));
        if (!GeneratorEntity.isFuel(fuel)) fuel = ItemStack.EMPTY;
        held = ItemStack.of(tag.getCompound("Held"));
        if (!held.isEmpty()) held.setCount(1);
        burner.load(tag);
        double savedWork = tag.getDouble("Work");
        work = Double.isFinite(savedWork) ? Math.max(0, Math.min(CYCLE_TICKS, savedWork)) : 0;
        returning = tag.getBoolean("Returning");
        motion.read(tag, held.isEmpty() && !returning ? 0 : work / CYCLE_TICKS, returning);
    }
    @Override public CompoundTag getUpdateTag() { var tag = saveWithoutMetadata(); motion.write(tag); return tag; }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).inflate(1.5); }
}
