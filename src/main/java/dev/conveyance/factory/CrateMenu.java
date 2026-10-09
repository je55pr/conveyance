package dev.conveyance.factory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class CrateMenu extends AbstractContainerMenu {
    @Override public void clicked(int slot, int button, net.minecraft.world.inventory.ClickType type, Player player) {
        dev.conveyance.player.InventoryAccess.click(slot, type, () -> super.clicked(slot, button, type, player));
    }
    private final Container crate;
    public CrateMenu(int id, Inventory inventory, int size) { this(id, inventory, new SimpleContainer(size)); }
    public CrateMenu(int id, Inventory inventory, Container crate) {
        super(crate.getContainerSize() == 16 ? FactoryContent.WOOD_CRATE_MENU.get() : FactoryContent.IRON_CRATE_MENU.get(), id);
        this.crate = crate;
        crate.startOpen(inventory.player);
        for (int slot = 0; slot < crate.getContainerSize(); slot++) addSlot(new Slot(crate, slot, 16 + slot % 8 * 18, 18 + slot / 8 * 18));
        int inventoryY = rows() * 18 + 40;
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, 9 + row * 9 + column, 8 + column * 18, inventoryY + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, inventoryY + 58));
    }
    public int rows() { return crate.getContainerSize() / 8; }
    @Override public boolean stillValid(Player player) { return crate.stillValid(player); }
    @Override public void removed(Player player) { super.removed(player); crate.stopOpen(player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        int size = crate.getContainerSize();
        if (index < size) { if (!moveItemStackTo(stack, size, size + 36, true)) return ItemStack.EMPTY; }
        else if (!moveItemStackTo(stack, 0, size, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
