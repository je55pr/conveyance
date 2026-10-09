package dev.conveyance.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CrateEntity extends BlockEntity implements Container, MenuProvider {
    private NonNullList<ItemStack> items;
    boolean recovered;
    public CrateEntity(BlockPos pos, BlockState state) {
        super(FactoryContent.CRATE_ENTITY.get(), pos, state);
        items = NonNullList.withSize(((CrateBlock) state.getBlock()).slots, ItemStack.EMPTY);
    }
    @Override public int getContainerSize() { return items.size(); }
    @Override public int getMaxStackSize() { return 100; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int count) { ItemStack result = ContainerHelper.removeItem(items, slot, count); setChanged(); return result; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); setChanged(); }
    @Override public void clearContent() { items.clear(); setChanged(); }
    @Override public boolean stillValid(Player player) { return !isRemoved() && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) < 64; }
    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new CrateMenu(id, inventory, this); }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); ContainerHelper.saveAllItems(tag, items); }
    @Override public void load(CompoundTag tag) { super.load(tag); items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY); ContainerHelper.loadAllItems(tag, items); }
}
