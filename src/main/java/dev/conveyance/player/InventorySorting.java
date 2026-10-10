package dev.conveyance.player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Compact real storage in the order shown by E, independently of the nine item links. */
public final class InventorySorting {
    private record Order(int category, int family, String id) {}
    private static Order order(ItemStack stack) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        int category = 7, family = 0;
        switch (id.getPath()) {
            case "cobblestone", "stone", "andesite", "granite", "diorite" -> { category = 0; family = 0; }
            case "wood" -> { category = 0; family = 1; }
            case "coal", "charcoal" -> { category = 0; family = 2; }
            case "raw_iron", "iron_ore", "iron_ingot" -> { category = 0; family = 3; }
            case "raw_copper", "copper_ore", "copper_ingot" -> { category = 0; family = 4; }
            case "dirt", "grass_block", "sand", "red_sand", "gravel", "clay", "clay_ball" -> { category = 0; family = 5; }
            case "gear", "copper_wire", "electronic_circuit", "steel_ingot" -> category = 1;
            case "conveyor", "slope_conveyor", "junction" -> { category = 2; family = 0; }
            case "burner_inserter", "inserter", "long_inserter" -> { category = 2; family = 1; }
            case "wood_crate", "iron_crate" -> { category = 2; family = 2; }
            case "burner_miner", "miner" -> { category = 3; family = 0; }
            case "industrial_furnace" -> { category = 3; family = 1; }
            case "assembler" -> { category = 3; family = 2; }
            case "radar" -> { category = 3; family = 3; }
            case "generator", "solar_panel" -> { category = 4; family = 0; }
            case "power_pole" -> { category = 4; family = 1; }
            case "red_science_pack", "green_science_pack" -> { category = 5; family = 0; }
            case "research_lab" -> { category = 5; family = 1; }
            case "iron_pickaxe" -> category = 6;
            default -> {}
        }
        return new Order(category, family, id.toString());
    }
    private static final Comparator<ItemStack> ORDER = Comparator
            .comparingInt((ItemStack stack) -> order(stack).category)
            .thenComparingInt(stack -> order(stack).family)
            .thenComparing(stack -> order(stack).id)
            .thenComparing(Comparator.comparingInt(ItemStack::getDamageValue).reversed())
            .thenComparing(stack -> stack.getTag() == null ? "" : stack.getTag().toString())
            .thenComparing(Comparator.comparingInt(ItemStack::getCount).reversed());

    public static boolean sort(ServerPlayer player) {
        // Leave the grid stationary while a player holds or drag-distributes a stack.
        if (!player.containerMenu.getCarried().isEmpty()) return false;
        LinkedHotbar.initialize(player);
        var extra = InventoryAccess.extra(player);
        var original = InventoryAccess.snapshot(player, extra);
        var groups = new ArrayList<ItemStack>();
        for (ItemStack item : original) {
            if (item.isEmpty()) continue;
            ItemStack same = groups.stream().filter(group -> ItemStack.isSameItemSameTags(group, item)).findFirst().orElse(null);
            if (same == null) groups.add(item.copy()); else same.grow(item.getCount());
        }
        var packed = new ArrayList<ItemStack>();
        for (ItemStack group : groups) while (!group.isEmpty()) packed.add(group.split(group.getMaxStackSize()));
        if (packed.size() > original.size()) return false;
        packed.sort(ORDER);
        var sorted = new ArrayList<ItemStack>(original.size());
        for (int slot = 0; slot < original.size(); slot++) sorted.add(ItemStack.EMPTY);
        for (int cell = 0; cell < packed.size(); cell++) {
            // E shows main slots 9–35, extra storage, then former physical hotbar slots 0–8.
            int storage = cell < 27 ? cell + 9 : cell < 63 ? cell + 9 : cell - 63;
            sorted.set(storage, packed.get(cell));
        }
        boolean changed = false;
        for (int slot = 0; slot < original.size(); slot++) if (!ItemStack.matches(original.get(slot), sorted.get(slot))) { changed = true; break; }
        if (!changed) return false;
        InventoryAccess.commit(player, extra, sorted);
        LinkedHotbar.refreshHand(player);
        player.containerMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) player.inventoryMenu.broadcastChanges();
        return true;
    }
    private InventorySorting() {}
}
