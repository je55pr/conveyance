package dev.conveyance.factory;

import dev.conveyance.player.PlayerStorageLayout;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ModuleMenu extends FactoryMenu {
    private final Container modules;
    public ModuleMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(3)); }
    public ModuleMenu(int id, Inventory inventory, Container modules) {
        super(FactoryContent.MODULE_MENU.get(), id); this.modules = modules;
        for (int index = 0; index < 3; index++) {
            final int slot = index;
            addSlot(new Slot(modules, index, PlayerStorageLayout.MACHINE_X + 16 + index * 24, 74) {
                @Override public boolean mayPlace(ItemStack stack) { return MachineModules.module(stack) && modules.canPlaceItem(slot, stack); }
                @Override public int getMaxStackSize() { return 1; }
            });
        }
        addPlayerInventory(inventory);
    }
    @Override public boolean stillValid(Player player) { return modules.stillValid(player); }
    @Override protected boolean moveToMachine(ItemStack stack) { return MachineModules.module(stack) && moveItemStackTo(stack, 0, 3, false); }
}
