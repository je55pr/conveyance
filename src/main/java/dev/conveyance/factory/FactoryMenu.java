package dev.conveyance.factory;

import dev.conveyance.player.ExtraInventory;
import dev.conveyance.player.InventoryAccess;
import dev.conveyance.player.LinkedHotbar;
import dev.conveyance.player.PlayerStorageLayout;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/** One full player inventory, transfer policy and cursor-return path for all machines. */
public abstract class FactoryMenu extends AbstractContainerMenu {
    private int playerStart, playerEnd;
    protected FactoryMenu(MenuType<?> type, int id) { super(type, id); }
    protected final void addPlayerInventory(Inventory inventory) {
        playerStart = slots.size();
        PlayerStorageLayout.addSlots(inventory, ExtraInventory.of(inventory.player), this::addSlot, () -> true);
        playerEnd = slots.size();
    }
    @Override public final void clicked(int slot, int button, ClickType type, Player player) {
        if (type == ClickType.SWAP && slot >= 0 && slot < slots.size() && button >= 0 && button < 9) {
            ItemStack item = slots.get(slot).getItem();
            if (!item.isEmpty()) LinkedHotbar.set(player, button + 1, item.getItem());
            if (player instanceof ServerPlayer server) LinkedHotbar.sync(server, true);
            return;
        }
        InventoryAccess.click(slot, type, () -> super.clicked(slot, button, type, player));
    }
    protected abstract boolean moveToMachine(ItemStack stack);
    @Override public final ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var source = slots.get(index);
        if (!source.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = source.getItem(), original = stack.copy();
        boolean moved = index < playerStart ? moveItemStackTo(stack, playerStart, playerEnd, false) : moveToMachine(stack);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) source.set(ItemStack.EMPTY); else source.setChanged();
        source.onTake(player, stack);
        LinkedHotbar.refreshHand(player);
        return original;
    }
    @Override public void removed(Player player) {
        if (player instanceof ServerPlayer && player.isAlive() && !getCarried().isEmpty()) {
            InventoryAccess.returnItem(player, getCarried().copy()); setCarried(ItemStack.EMPTY);
        }
        super.removed(player);
    }
}
