package dev.conveyance.factory;

import net.minecraft.core.BlockPos;
import dev.conveyance.player.ResearchState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

/** Fuel and cargo are separate: available coal or Wood can bootstrap an empty arm. */
public final class BurnerInserterEntity extends BlockEntity implements Container, MenuProvider {
    public static final int CYCLE_TICKS = 20;
    private ItemStack fuel = ItemStack.EMPTY, held = ItemStack.EMPTY;
    public ItemStack heldItem() { return held.copy(); }
    private int burnTicks;
    private double work;
    private boolean recovered;
    public BurnerInserterEntity(BlockPos pos, BlockState state) { super(FactoryContent.BURNER_INSERTER_ENTITY.get(), pos, state); }
    public int fuelRemaining() { return burnTicks; }

    public static void tick(Level level, BlockPos pos, BlockState state, BurnerInserterEntity arm) {
        var facing = state.getValue(InserterBlock.FACING);
        BlockPos from = pos.relative(facing.getOpposite()), to = pos.relative(facing);
        if (arm.burnTicks == 0 && arm.fuel.isEmpty()) {
            ItemStack picked = ItemTransfer.takeSource(level, from, facing, true);
            if (picked.isEmpty() && GeneratorEntity.isFuel(arm.held)) {
                picked = arm.held;
                arm.held = ItemStack.EMPTY;
                arm.work = 0;
            }
            if (!picked.isEmpty()) { arm.fuel = picked; arm.setChanged(); }
        }
        if (arm.held.isEmpty()) {
            ItemStack candidate = ItemTransfer.peekForOutput(level, from, to, facing);
            if (candidate.isEmpty() || arm.burnTicks == 0 && arm.fuel.isEmpty()) return;
            arm.held = ItemTransfer.takeSource(level, from, facing, candidate);
            if (arm.held.isEmpty()) return;
            arm.setChanged();
        }
        if (!ItemTransfer.canInserterOutput(level, to, facing.getOpposite(), arm.held)) return;
        if (arm.burnTicks == 0) {
            if (!GeneratorEntity.isFuel(arm.fuel)) return;
            arm.burnTicks = GeneratorEntity.fuelTicks(arm.fuel, level);
            arm.fuel.shrink(1);
            arm.setChanged();
        }
        arm.burnTicks--;
        if (level.getGameTime() % 20 == 0) arm.setChanged();
        arm.work += ResearchState.inserterSpeed(level);
        if (arm.work + 0.000001 < CYCLE_TICKS) return;
        if (ItemTransfer.inserterOutput(level, to, facing.getOpposite(), arm.held)) {
            arm.held = ItemStack.EMPTY;
            arm.work = Math.max(0, arm.work - CYCLE_TICKS);
            arm.setChanged();
        }
    }
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
    @Override public boolean stillValid(Player player) { return !isRemoved() && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) < 64; }
    @Override public Component getDisplayName() { return Component.translatable("block.conveyance.burner_inserter"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new GeneratorMenu(id, inventory, this); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Fuel", fuel.save(new CompoundTag())); tag.put("Held", held.save(new CompoundTag()));
        tag.putInt("BurnTicks", burnTicks); tag.putDouble("Work", work);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        fuel = ItemStack.of(tag.getCompound("Fuel"));
        if (!GeneratorEntity.isFuel(fuel)) fuel = ItemStack.EMPTY;
        held = ItemStack.of(tag.getCompound("Held"));
        if (!held.isEmpty()) held.setCount(1);
        burnTicks = Math.max(0, Math.min(GeneratorEntity.MAX_FUEL_TICKS, tag.getInt("BurnTicks")));
        work = Math.max(0, Math.min(CYCLE_TICKS - 1, tag.getDouble("Work")));
    }
}
