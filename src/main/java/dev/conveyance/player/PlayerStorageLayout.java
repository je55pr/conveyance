package dev.conveyance.player;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Shared geometry and real-storage order for E and every factory menu. */
public final class PlayerStorageLayout {
    public static final int COLUMNS = 10, ROWS = 8, CELL = 18, SIZE = COLUMNS * ROWS;
    public static final int CONTENT_Y = 24, GRID_X = 8, GRID_Y = CONTENT_Y + 26;
    public static final int PANEL_WIDTH = COLUMNS * CELL + 14, MACHINE_X = PANEL_WIDTH + 8;
    public static final int HOTBAR_LABEL_Y = GRID_Y + ROWS * CELL + 1, HOTBAR_Y = GRID_Y + ROWS * CELL + 14;
    public static final int MIN_HEIGHT = HOTBAR_Y + CELL + 14, MIN_WIDTH = MACHINE_X + PANEL_WIDTH;
    public static int storageIndex(int cell) { return cell < 27 + ExtraInventory.SIZE ? cell + 9 : cell - 27 - ExtraInventory.SIZE; }
    public static void addSlots(Inventory inventory, Container extra, Consumer<Slot> add, BooleanSupplier visible) {
        for (int cell = 0; cell < SIZE; cell++) {
            int storage = storageIndex(cell);
            Container container = storage < 36 ? inventory : extra;
            int index = storage < 36 ? storage : storage - 36;
            add.accept(new Slot(container, index, GRID_X + cell % COLUMNS * CELL, GRID_Y + cell / COLUMNS * CELL) {
                @Override public boolean isActive() { return visible.getAsBoolean(); }
            });
        }
    }
    private PlayerStorageLayout() {}
}
