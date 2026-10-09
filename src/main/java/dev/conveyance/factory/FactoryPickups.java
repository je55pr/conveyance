package dev.conveyance.factory;

import dev.conveyance.Conveyance;
import dev.conveyance.player.InventoryAccess;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class FactoryPickups {
    private static boolean factory(ItemStack item) {
        return item.getItem() instanceof BlockItem && BuiltInRegistries.ITEM.getKey(item.getItem()).getNamespace().equals(Conveyance.MOD_ID);
    }
    public static void add(List<ItemStack> loot, ItemStack item) { add(loot, item, 1, 0); }
    private static void add(List<ItemStack> loot, ItemStack item, int multiplier, int depth) {
        if (item.isEmpty()) return;
        if (factory(item)) {
            CompoundTag old = BlockItem.getBlockEntityData(item);
            if (old != null && depth < 8) contents(loot, old, item.getCount() * multiplier, depth + 1);
            loot.add(new ItemStack(item.getItem(), item.getCount() * multiplier));
        } else loot.add(item.copyWithCount(item.getCount() * multiplier));
    }
    private static void contents(List<ItemStack> loot, CompoundTag data, int multiplier, int depth) {
        var items = data.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) add(loot, ItemStack.of(items.getCompound(i)), multiplier, depth);
        add(loot, ItemStack.of(data.getCompound("Held")), multiplier, depth);
        add(loot, ItemStack.of(data.getCompound("Fuel")), multiplier, depth);
    }
    public static List<ItemStack> block(BlockEntity entity) {
        var loot = new ArrayList<ItemStack>();
        loot.add(new ItemStack(entity.getBlockState().getBlock()));
        if (entity instanceof Container container)
            for (int slot = 0; slot < container.getContainerSize(); slot++) add(loot, container.getItem(slot));
        if (entity instanceof ConveyorEntity belt)
            for (int slot = 0; slot < ConveyorEntity.CAPACITY; slot++) add(loot, belt.item(slot));
        if (entity instanceof JunctionEntity junction)
            for (int slot = 0; slot < JunctionEntity.CAPACITY; slot++) add(loot, junction.item(slot));
        if (entity instanceof InserterEntity arm) add(loot, arm.heldItem());
        if (entity instanceof BurnerInserterEntity arm) add(loot, arm.heldItem());
        return loot;
    }
    public static void normalizeInventory(Player player) {
        Container extra = InventoryAccess.extra(player);
        var refunds = new ArrayList<ItemStack>();
        boolean changed = false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) changed |= normalize(player.getInventory().getItem(slot), refunds);
        for (int slot = 0; slot < extra.getContainerSize(); slot++) changed |= normalize(extra.getItem(slot), refunds);
        changed |= normalize(player.containerMenu.getCarried(), refunds);
        if (!changed) return;
        player.getInventory().setChanged(); extra.setChanged();
        for (ItemStack refund : refunds) while (!refund.isEmpty()) InventoryAccess.returnItem(player, refund.split(refund.getMaxStackSize()));
    }
    private static boolean normalize(ItemStack item, List<ItemStack> refunds) {
        if (!factory(item) || !item.hasTag()) return false;
        CompoundTag old = BlockItem.getBlockEntityData(item);
        if (old != null) contents(refunds, old, item.getCount(), 0);
        item.setTag(null);
        return true;
    }
    private FactoryPickups() {}
}
