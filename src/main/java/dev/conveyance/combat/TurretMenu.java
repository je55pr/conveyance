package dev.conveyance.combat;

import dev.conveyance.factory.FactoryContent;
import dev.conveyance.factory.FactoryMenu;
import dev.conveyance.factory.MachineEntity;
import dev.conveyance.player.PlayerStorageLayout;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class TurretMenu extends FactoryMenu {
    private final Container container;
    private final ContainerData data;
    public TurretMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(1)); }
    public TurretMenu(int id, Inventory inventory, Container container) {
        super(FactoryContent.TURRET_MENU.get(), id); this.container = container;
        addSlot(new Slot(container, 0, PlayerStorageLayout.MACHINE_X + 8, 70) {
            @Override public boolean mayPlace(ItemStack stack) { return dev.conveyance.combat.CombatShots.magazine(stack); }
        });
        addPlayerInventory(inventory);
        data = container instanceof MachineEntity machine ? new ContainerData() {
            @Override public int get(int index) { return machine.turret().rounds(); }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 1; }
        } : new SimpleContainerData(1);
        addDataSlots(data);
    }
    public int rounds() { return data.get(0); }
    @Override protected boolean moveToMachine(ItemStack stack) { return dev.conveyance.combat.CombatShots.magazine(stack) && moveItemStackTo(stack, 0, 1, false); }
    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
}
