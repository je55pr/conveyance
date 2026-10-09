package dev.conveyance.player;

import dev.conveyance.factory.FactoryContent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

public final class PlayerWorkbenchMenu extends AbstractContainerMenu {
    public static final int CONTENT_Y = 24;
    private final Player owner;
    private final Container extra;
    private boolean storageVisible = true;
    public PlayerWorkbenchMenu(int id, Inventory inventory) {
        super(FactoryContent.PLAYER_WORKBENCH_MENU.get(), id);
        owner = inventory.player;
        extra = owner instanceof ServerPlayer ? new ExtraInventory(owner) : new SimpleContainer(36);
        for (int index = 0; index < 63; index++) {
            int x = 8 + index % 9 * 18, y = CONTENT_Y + 28 + index / 9 * 18;
            storageSlot(index < 27 ? inventory : extra, index < 27 ? index + 9 : index - 27, x, y);
        }
        for (int slot = 0; slot < 9; slot++) storageSlot(inventory, slot, 8 + slot * 18, CONTENT_Y + 166);
    }
    public Container extra() { return extra; }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        InventoryAccess.click(slot, type, () -> super.clicked(slot, button, type, player));
    }
    private void storageSlot(Container container, int index, int x, int y) {
        addSlot(new Slot(container, index, x, y) { @Override public boolean isActive() { return storageVisible; } });
    }
    public void showStorage(boolean visible) { storageVisible = visible; }
    public int researchMask() { return ResearchState.mask(owner.level()); }
    @Override public boolean stillValid(Player player) { return player == owner && player.isAlive(); }
    @Override public boolean clickMenuButton(Player player, int id) {
        Technology tech = Technology.byMenuId(id);
        if (!(player instanceof ServerPlayer server) || !stillValid(player) || tech == null) return false;
        return HandCrafting.research(server, extra, tech.id);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index >= 63) {
            if (!moveItemStackTo(stack, 0, 63, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 63, 72, false)) {
            if (index < 27) { if (!moveItemStackTo(stack, 27, 63, false)) return ItemStack.EMPTY; }
            else if (!moveItemStackTo(stack, 0, 27, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
    @Override public void removed(Player player) {
        if (player instanceof ServerPlayer && player.isAlive() && !getCarried().isEmpty() && InventoryAccess.give(player, getCarried().copy())) setCarried(ItemStack.EMPTY);
        super.removed(player);
    }
}
