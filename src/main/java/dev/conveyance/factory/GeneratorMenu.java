package dev.conveyance.factory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class GeneratorMenu extends AbstractContainerMenu {
    private final Container fuel;

    public GeneratorMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(1)); }
    public GeneratorMenu(int id, Inventory inventory, Container fuel) {
        super(FactoryContent.GENERATOR_MENU.get(), id);
        checkContainerSize(fuel, 1);
        this.fuel = fuel;
        fuel.startOpen(inventory.player);
        addSlot(new Slot(fuel, 0, 80, 18) {
            @Override public boolean mayPlace(ItemStack stack) { return GeneratorEntity.isFuel(stack); }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 49 + row * 18));
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 107));
    }
    @Override public boolean stillValid(Player player) { return fuel.stillValid(player); }
    @Override public void removed(Player player) { super.removed(player); fuel.stopOpen(player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, 37, true)) return ItemStack.EMPTY;
        } else if (GeneratorEntity.isFuel(stack)) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (index < 28) {
            if (!moveItemStackTo(stack, 28, 37, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 1, 28, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
