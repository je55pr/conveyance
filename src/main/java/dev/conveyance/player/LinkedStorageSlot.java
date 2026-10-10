package dev.conveyance.player;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Real storage remains accessible in E; factory menus draw item links over this row. */
public final class LinkedStorageSlot extends Slot {
    private final Inventory inventory;
    public LinkedStorageSlot(Inventory inventory, int index, int x, int y) { super(inventory, index, x, y); this.inventory = inventory; }
    @Override public boolean isActive() { return !LinkedHotbar.enabled(inventory.player); }
}
