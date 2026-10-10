package dev.conveyance.player;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;

public final class InventoryAccess {
    public static final String RETURNS_KEY = "ConveyanceInventoryReturns";
    private static final ThreadLocal<Boolean> THROW_CLICK = new ThreadLocal<>();
    public static void click(int slot, ClickType type, Runnable action) {
        Boolean previous = THROW_CLICK.get();
        THROW_CLICK.set(type == ClickType.THROW || type == ClickType.PICKUP && slot == -999);
        try { action.run(); } finally { if (previous == null) THROW_CLICK.remove(); else THROW_CLICK.set(previous); }
    }
    public static boolean deliberateThrow() { return !Boolean.FALSE.equals(THROW_CLICK.get()); }
    public static Container extra(Player player) {
        return ExtraInventory.of(player);
    }
    public static List<ItemStack> snapshot(Player player, Container extra) {
        var result = new ArrayList<ItemStack>(72);
        for (int slot = 0; slot < 36; slot++) result.add(player.getInventory().getItem(slot).copy());
        for (int slot = 0; slot < 36; slot++) result.add(extra.getItem(slot).copy());
        return result;
    }
    public static boolean insert(List<ItemStack> items, ItemStack offered) {
        return remainder(items, offered).isEmpty();
    }
    public static ItemStack remainder(List<ItemStack> items, ItemStack offered) {
        ItemStack remaining = offered.copy();
        for (int pass = 0; pass < 2; pass++) {
            for (int slot = 0; slot < items.size() && !remaining.isEmpty(); slot++) {
                ItemStack stored = items.get(slot);
                if (pass == 0 && (stored.isEmpty() || !ItemStack.isSameItemSameTags(stored, remaining))) continue;
                if (pass == 1 && !stored.isEmpty()) continue;
                int count = Math.min(remaining.getCount(), remaining.getMaxStackSize() - stored.getCount());
                if (count <= 0) continue;
                items.set(slot, remaining.copyWithCount(stored.getCount() + count));
                remaining.shrink(count);
            }
        }
        return remaining;
    }
    public static void commit(Player player, Container extra, List<ItemStack> items) {
        for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, items.get(slot));
        player.getInventory().setChanged();
        if (extra instanceof ExtraInventory stored) stored.replace(items.subList(36, 72));
        else for (int slot = 0; slot < 36; slot++) extra.setItem(slot, items.get(slot + 36));
    }
    public static boolean give(Player player, ItemStack stack) {
        Container extra = extra(player);
        List<ItemStack> items = snapshot(player, extra);
        if (!insert(items, stack)) return false;
        commit(player, extra, items);
        return true;
    }
    /** Refunds from closing/swapping menu slots must never become ground loot. */
    public static void returnItem(Player player, ItemStack stack) {
        if (give(player, stack)) return;
        ListTag waiting = player.getPersistentData().getList(RETURNS_KEY, Tag.TAG_COMPOUND).copy();
        waiting.add(stack.save(new CompoundTag()));
        player.getPersistentData().put(RETURNS_KEY, waiting);
    }
    public static void flushReturns(Player player) {
        ListTag waiting = player.getPersistentData().getList(RETURNS_KEY, Tag.TAG_COMPOUND);
        if (waiting.isEmpty()) return;
        Container extra = extra(player);
        var inventory = snapshot(player, extra);
        var remaining = new ListTag();
        boolean changed = false;
        for (int i = 0; i < waiting.size(); i++) {
            ItemStack item = ItemStack.of(waiting.getCompound(i)), rest = remainder(inventory, item);
            changed |= rest.getCount() != item.getCount();
            if (!rest.isEmpty()) remaining.add(rest.save(new CompoundTag()));
        }
        if (changed) { commit(player, extra, inventory); player.getPersistentData().put(RETURNS_KEY, remaining); }
    }
    public static void restoreCursor(Player player, AbstractContainerMenu menu) {
        ListTag waiting = player.getPersistentData().getList(RETURNS_KEY, Tag.TAG_COMPOUND).copy();
        if (waiting.isEmpty() || !menu.getCarried().isEmpty()) return;
        menu.setCarried(ItemStack.of(waiting.getCompound(0)));
        waiting.remove(0);
        player.getPersistentData().put(RETURNS_KEY, waiting);
    }
    private InventoryAccess() {}
}
