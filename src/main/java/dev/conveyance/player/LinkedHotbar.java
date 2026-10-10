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
    public static final String EMPTY_KEY = "ConveyanceEmptyHand", TEMP_KEY = "ConveyanceTemporaryHand";
    public static final int SIZE = 10;
    private record Source(int selected, Item item, long time, int slot, ItemStack creative) {}
    private record Snapshot(List<ItemStack> extra, List<ResourceLocation> links, int selected, ResourceLocation temporary) {}
    private static final Map<Player, Source> CLIENT_SOURCES = new WeakHashMap<>(), SERVER_SOURCES = new WeakHashMap<>();
    private static Map<Player, Source> sources(Player player) { return player.level().isClientSide ? CLIENT_SOURCES : SERVER_SOURCES; }
    private static final Map<Player, Snapshot> SENT = new WeakHashMap<>();
    public static boolean enabled(Player player) { return player.getPersistentData().contains(DATA_KEY, Tag.TAG_LIST); }
    public static void initialize(Player player) {
        if (!player.getPersistentData().contains(EMPTY_KEY)) player.getPersistentData().putBoolean(EMPTY_KEY, true);
        if (enabled(player)) return;
        var links = new ListTag();
        for (int slot = 0; slot < 9; slot++) links.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(player.getInventory().getItem(slot).getItem()).toString()));
        player.getPersistentData().put(DATA_KEY, links);
    }
    public static ItemStack link(Player player, int slot) {
        if (slot <= 0 || slot >= SIZE) return ItemStack.EMPTY;
        ListTag links = player.getPersistentData().getList(DATA_KEY, Tag.TAG_STRING);
        ResourceLocation id = slot - 1 < links.size() ? ResourceLocation.tryParse(links.getString(slot - 1)) : null;
        Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
    public static void set(Player player, int slot, Item item) {
        if (slot <= 0 || slot >= SIZE) return;
        initialize(player);
        ListTag links = player.getPersistentData().getList(DATA_KEY, Tag.TAG_STRING).copy();
        while (links.size() < 9) links.add(StringTag.valueOf("minecraft:air"));
        links.set(slot - 1, StringTag.valueOf(BuiltInRegistries.ITEM.getKey(item).toString()));
        player.getPersistentData().put(DATA_KEY, links);
        refreshHand(player);
    }
    public static int selectedSlot(Player player) { return player.getPersistentData().getBoolean(EMPTY_KEY) ? 0 : player.getInventory().selected + 1; }
    public static int bindingSlot(Player player) { return Math.max(1, selectedSlot(player)); }
    public static ItemStack temporary(Player player) {
        ResourceLocation id = ResourceLocation.tryParse(player.getPersistentData().getString(TEMP_KEY));
        Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
    public static ItemStack handReference(Player player) { return selectedSlot(player) == 0 ? temporary(player) : link(player, selectedSlot(player)); }
    public static void select(Player player, int slot) {
        if (slot < 0 || slot >= SIZE) return;
        initialize(player);
        player.getInventory().selected = Math.max(0, slot - 1);
        player.getPersistentData().putBoolean(EMPTY_KEY, slot == 0);
        player.getPersistentData().remove(TEMP_KEY);
        refreshHand(player);
    }
    public static void selectTemporary(Player player, ItemStack item) {
        select(player, 0);
        if (!item.isEmpty() && count(player, item.getItem()) > 0)
            player.getPersistentData().putString(TEMP_KEY, BuiltInRegistries.ITEM.getKey(item.getItem()).toString());
    }
    public static ItemStack building(Player player, net.minecraft.core.BlockPos pos) {
        if (pos == null || !player.level().hasChunkAt(pos)) return ItemStack.EMPTY;
        var root = dev.conveyance.factory.FactoryActions.root(player.level(), pos);
        var state = player.level().getBlockState(root);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(root), net.minecraft.core.Direction.UP, root, false);
        ItemStack item = state.getCloneItemStack(hit, player.level(), root, player);
        return item.getItem() instanceof net.minecraft.world.item.BlockItem && !ItemPolicy.isDisabled(item)
                && BuiltInRegistries.ITEM.getKey(item.getItem()).getNamespace().equals("conveyance") ? new ItemStack(item.getItem()) : ItemStack.EMPTY;
    }
    public static void pipette(Player player, net.minecraft.core.BlockPos pos) {
        ItemStack target = building(player, pos);
        if (selectedSlot(player) != 0 || !target.isEmpty() && handReference(player).is(target.getItem())) select(player, 0);
        else selectTemporary(player, target);
    }
    public static void scroll(Player player, double amount) {
        if (amount == 0) return;
        int slot = Math.floorMod(selectedSlot(player) - (int) Math.signum(amount), SIZE);
        select(player, slot);
        if (player.level().isClientSide) InventoryNetwork.requestSelection(slot);
    }
    public static int count(Player player, Item item) {
        int count = 0;
        for (int slot = 0; slot < 36; slot++) if (player.getInventory().getItem(slot).is(item)) count += player.getInventory().getItem(slot).getCount();
        var extra = ExtraInventory.of(player);
        for (int slot = 0; slot < ExtraInventory.SIZE; slot++) if (extra.getItem(slot).is(item)) count += extra.getItem(slot).getCount();
        return count;
    }
    public static void refreshHand(Player player) { sources(player).remove(player); }
    private static ItemStack sourceStack(Player player, Source source) {
        return source.slot < 0 ? source.creative : source.slot < 36 ? player.getInventory().getItem(source.slot) : ExtraInventory.of(player).getItem(source.slot - 36);
    }
    public static ItemStack selectedStack(Player player) {
        int selected = selectedSlot(player);
        ItemStack link = handReference(player);
        if (link.isEmpty()) { refreshHand(player); return ItemStack.EMPTY; }
        long now = player.level().getGameTime();
        Source cached = sources(player).get(player);
        if (cached != null && cached.selected == selected && cached.item == link.getItem() && cached.time == now) {
            ItemStack stack = sourceStack(player, cached);
            if (stack.isEmpty() || stack.is(link.getItem())) return stack;
        }
        for (int slot = 0; slot < 36 + ExtraInventory.SIZE; slot++) {
            ItemStack stack = slot < 36 ? player.getInventory().getItem(slot) : ExtraInventory.of(player).getItem(slot - 36);
            if (!stack.isEmpty() && stack.is(link.getItem())) {
                sources(player).put(player, new Source(selected, link.getItem(), now, slot, ItemStack.EMPTY));
                return stack;
            }
        }
        ItemStack creative = player.isCreative() ? link.copyWithCount(link.getMaxStackSize()) : ItemStack.EMPTY;
        sources(player).put(player, new Source(selected, link.getItem(), now, -1, creative));
        return creative;
    }
    public static void replaceSelected(Player player, ItemStack replacement) {
        Source source = sources(player).get(player);
        if (source == null || source.selected != selectedSlot(player)) { selectedStack(player); source = sources(player).get(player); }
        if (source == null || source.slot < 0) {
            if (!replacement.isEmpty() && (source == null || replacement != source.creative)) {
                InventoryAccess.returnItem(player, replacement.copy());
                if (selectedSlot(player) > 0) set(player, selectedSlot(player), replacement.getItem());
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
        if (slot <= 0 || slot >= SIZE || player.containerMenu.containerId != containerId || !player.containerMenu.stillValid(player)) return;
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
        ItemStack picked = temporary(player);
        if (!picked.isEmpty() && count(player, picked.getItem()) == 0) select(player, 0);
        ExtraInventory extra = ExtraInventory.of(player);
        extra.setChanged(); // Hand-use may mutate a stack directly instead of calling Container.setItem.
        var items = new ArrayList<ItemStack>();
        for (int slot = 0; slot < ExtraInventory.SIZE; slot++) items.add(extra.getItem(slot).copy());
        var links = new ArrayList<ResourceLocation>();
        for (int slot = 0; slot < SIZE; slot++) links.add(BuiltInRegistries.ITEM.getKey(link(player, slot).getItem()));
        int selected = selectedSlot(player);
        ResourceLocation temporary = BuiltInRegistries.ITEM.getKey(temporary(player).getItem());
        Snapshot previous = SENT.get(player);
        boolean changed = force || previous == null || !previous.links.equals(links) || previous.selected != selected || !previous.temporary.equals(temporary);
        if (!changed) for (int slot = 0; slot < items.size(); slot++) if (!ItemStack.matches(items.get(slot), previous.extra.get(slot))) { changed = true; break; }
        if (changed) { SENT.put(player, new Snapshot(items, links, selected, temporary)); InventoryNetwork.syncInventory(player, items, links, selected, temporary); }
    }
    private LinkedHotbar() {}
}
