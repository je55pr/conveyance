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
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public final class PlayerWorkbenchMenu extends AbstractContainerMenu {
    public static final int CONTENT_Y = PlayerStorageLayout.CONTENT_Y;
    private final Player owner;
    private final Container extra;
    private boolean storageVisible = true;
    private int openingTab;
    private final ContainerData opening;
    public PlayerWorkbenchMenu(int id, Inventory inventory) {
        this(id, inventory, 0);
    }
    public PlayerWorkbenchMenu(int id, Inventory inventory, int tab) {
        super(FactoryContent.PLAYER_WORKBENCH_MENU.get(), id);
        owner = inventory.player;
        openingTab = tab;
        opening = owner instanceof ServerPlayer ? new ContainerData() {
            @Override public int get(int index) { return openingTab + 1; }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 1; }
        } : new SimpleContainerData(1);
        addDataSlots(opening);
        extra = ExtraInventory.of(owner);
        PlayerStorageLayout.addSlots(inventory, extra, this::addSlot, () -> storageVisible);
    }
    public Container extra() { return extra; }
    public int openingTab() { return opening.get(0) - 1; }
    public void openTab(int tab) { openingTab = tab; broadcastChanges(); }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (type == ClickType.SWAP && slot >= 0 && slot < slots.size() && button >= 0 && button < 9) {
            ItemStack item = slots.get(slot).getItem();
            if (!item.isEmpty()) LinkedHotbar.set(player, button + 1, item.getItem());
            return;
        }
        InventoryAccess.click(slot, type, () -> super.clicked(slot, button, type, player));
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
        ItemStack stack = slot.getItem();
        int target = LinkedHotbar.bindingSlot(player);
        for (int link = 1; link < LinkedHotbar.SIZE; link++) {
            if (LinkedHotbar.link(player, link).is(stack.getItem())) { target = link; break; }
            if (LinkedHotbar.link(player, link).isEmpty()) { target = link; break; }
        }
        LinkedHotbar.set(player, target, stack.getItem());
        return ItemStack.EMPTY;
    }
    @Override public void removed(Player player) {
        if (player instanceof ServerPlayer && player.isAlive() && !getCarried().isEmpty() && InventoryAccess.give(player, getCarried().copy())) setCarried(ItemStack.EMPTY);
        super.removed(player);
    }
}
