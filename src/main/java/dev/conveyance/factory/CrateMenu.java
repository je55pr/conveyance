package dev.conveyance.factory;

import dev.conveyance.player.PlayerStorageLayout;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class CrateMenu extends FactoryMenu {
    private final Container crate;
    public CrateMenu(int id, Inventory inventory, int size) { this(id, inventory, new SimpleContainer(size)); }
    public CrateMenu(int id, Inventory inventory, Container crate) {
        super(crate.getContainerSize() == 16 ? FactoryContent.WOOD_CRATE_MENU.get() : FactoryContent.IRON_CRATE_MENU.get(), id);
        this.crate = crate;
        crate.startOpen(inventory.player);
        for (int slot = 0; slot < crate.getContainerSize(); slot++) addSlot(new Slot(crate, slot, PlayerStorageLayout.MACHINE_X + 8 + slot % 8 * PlayerStorageLayout.CELL, 70 + slot / 8 * PlayerStorageLayout.CELL));
        addPlayerInventory(inventory);
    }
    public int rows() { return crate.getContainerSize() / 8; }
    @Override public boolean stillValid(Player player) { return crate.stillValid(player); }
    @Override public void removed(Player player) { super.removed(player); crate.stopOpen(player); }
    @Override protected boolean moveToMachine(ItemStack stack) {
        return moveItemStackTo(stack, 0, crate.getContainerSize(), false);
    }
}
