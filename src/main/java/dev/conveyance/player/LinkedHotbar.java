package dev.conveyance.player;

import dev.conveyance.ItemPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Item references resolve to real inventory stacks; the hotbar never owns duplicate items. */
public final class LinkedHotbar {
    public static final String DATA_KEY = "ConveyanceHotbarLinks";
    private record Source(int selected, Item item, long time, int slot, ItemStack creative) {}
    private record Snapshot(List<ItemStack> extra, List<ResourceLocation> links) {}
    private static final Map<Player, Source> SOURCES = new WeakHashMap<>();
    private static final Map<Player, Snapshot> SENT = new WeakHashMap<>();
    public static boolean enabled(Player player) { return player.getPersistentData().contains(DATA_KEY, Tag.TAG_LIST); }
    public static void initialize(Player player) {
        if (enabled(player)) return;
        var links = new ListTag();
        for (int slot = 0; slot < 9; slot++) links.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(player.getInventory().getItem(slot).getItem()).toString()));
        player.getPersistentData().put(DATA_KEY, links);
    }
    public static ItemStack link(Player player, int slot) {
        if (slot < 0 || slot >= 9) return ItemStack.EMPTY;
        ListTag links = player.getPersistentData().getList(DATA_KEY, Tag.TAG_STRING);
        ResourceLocation id = slot < links.size() ? ResourceLocation.tryParse(links.getString(slot)) : null;
        Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
    public static void set(Player player, int slot, Item item) {
        if (slot < 0 || slot >= 9) return;
        initialize(player);
        ListTag links = player.getPersistentData().getList(DATA_KEY, Tag.TAG_STRING).copy();
        while (links.size() < 9) links.add(StringTag.valueOf("minecraft:air"));
        links.set(slot, StringTag.valueOf(BuiltInRegistries.ITEM.getKey(item).toString()));
        player.getPersistentData().put(DATA_KEY, links);
        SOURCES.remove(player);
    }
    public static int count(Player player, Item item) {
        int count = 0;
        for (int slot = 0; slot < 36; slot++) if (player.getInventory().getItem(slot).is(item)) count += player.getInventory().getItem(slot).getCount();
        var extra = ExtraInventory.of(player);
        for (int slot = 0; slot < ExtraInventory.SIZE; slot++) if (extra.getItem(slot).is(item)) count += extra.getItem(slot).getCount();
        return count;
    }
    public static void refreshHand(Player player) { SOURCES.remove(player); }
    private static ItemStack sourceStack(Player player, Source source) {
        return source.slot < 0 ? source.creative : source.slot < 36 ? player.getInventory().getItem(source.slot) : ExtraInventory.of(player).getItem(source.slot - 36);
    }
    public static ItemStack selectedStack(Player player) {
        int selected = player.getInventory().selected;
        ItemStack link = link(player, selected);
        if (link.isEmpty()) { SOURCES.remove(player); return ItemStack.EMPTY; }
        long now = player.level().getGameTime();
        Source cached = SOURCES.get(player);
        if (cached != null && cached.selected == selected && cached.item == link.getItem() && cached.time == now) {
            ItemStack stack = sourceStack(player, cached);
            if (stack.isEmpty() || stack.is(link.getItem())) return stack;
        }
        for (int slot = 0; slot < 36 + ExtraInventory.SIZE; slot++) {
            ItemStack stack = slot < 36 ? player.getInventory().getItem(slot) : ExtraInventory.of(player).getItem(slot - 36);
            if (!stack.isEmpty() && stack.is(link.getItem())) {
                SOURCES.put(player, new Source(selected, link.getItem(), now, slot, ItemStack.EMPTY));
                return stack;
            }
        }
        ItemStack creative = player.isCreative() ? link.copyWithCount(link.getMaxStackSize()) : ItemStack.EMPTY;
        SOURCES.put(player, new Source(selected, link.getItem(), now, -1, creative));
        return creative;
    }
    public static void replaceSelected(Player player, ItemStack replacement) {
        Source source = SOURCES.get(player);
        if (source == null || source.selected != player.getInventory().selected) { selectedStack(player); source = SOURCES.get(player); }
        if (source == null || source.slot < 0) {
            if (!replacement.isEmpty() && (source == null || replacement != source.creative)) {
                InventoryAccess.returnItem(player, replacement.copy());
                set(player, player.getInventory().selected, replacement.getItem());
            }
            return;
        }
        if (source.slot >= 36) ExtraInventory.of(player).setItem(source.slot - 36, replacement);
        else if (source.slot >= 0) player.getInventory().setItem(source.slot, replacement);
        player.getInventory().setChanged();
    }
    public static ItemStack removeSelected(Player player, boolean all) {
        ItemStack stack = selectedStack(player);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack thrown = stack.split(all ? stack.getCount() : 1);
        player.getInventory().setChanged(); ExtraInventory.of(player).setChanged();
        return thrown;
    }
    public static boolean allowed(Player player, ItemStack item) {
        if (item.isEmpty() || ItemPolicy.isDisabled(item)) return false;
        return player.isCreative() || count(player, item.getItem()) > 0 || player.containerMenu.getCarried().is(item.getItem())
                || HandCrafting.recipes(player.level(), ResearchState.mask(player.level())).stream()
                .anyMatch(recipe -> recipe.getResultItem(player.level().registryAccess()).is(item.getItem()));
    }
    public static void bind(ServerPlayer player, int containerId, int slot, ResourceLocation itemId) {
        if (slot < 0 || slot >= 9 || player.containerMenu.containerId != containerId || !player.containerMenu.stillValid(player)) return;
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item != Items.AIR && !allowed(player, new ItemStack(item))) return;
        set(player, slot, item);
        ItemStack carried = player.containerMenu.getCarried();
        if (item != Items.AIR && !carried.isEmpty() && carried.is(item) && InventoryAccess.give(player, carried.copy())) player.containerMenu.setCarried(ItemStack.EMPTY);
        sync(player, true);
        player.containerMenu.broadcastChanges();
    }
    public static void sync(ServerPlayer player, boolean force) {
        initialize(player);
        ExtraInventory extra = ExtraInventory.of(player);
        extra.setChanged(); // Hand-use may mutate a stack directly instead of calling Container.setItem.
        var items = new ArrayList<ItemStack>();
        for (int slot = 0; slot < ExtraInventory.SIZE; slot++) items.add(extra.getItem(slot).copy());
        var links = new ArrayList<ResourceLocation>();
        for (int slot = 0; slot < 9; slot++) links.add(BuiltInRegistries.ITEM.getKey(link(player, slot).getItem()));
        Snapshot previous = SENT.get(player);
        boolean changed = force || previous == null || !previous.links.equals(links);
        if (!changed) for (int slot = 0; slot < items.size(); slot++) if (!ItemStack.matches(items.get(slot), previous.extra.get(slot))) { changed = true; break; }
        if (changed) { SENT.put(player, new Snapshot(items, links)); InventoryNetwork.syncInventory(player, items, links); }
    }
    private LinkedHotbar() {}
}
