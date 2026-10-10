package dev.conveyance.player;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.lang.ref.WeakReference;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ExtraInventory extends SimpleContainer {
    public static final String DATA_KEY = "ConveyanceStorage";
    public static final int SIZE = PlayerStorageLayout.SIZE - 36;
    private static final Map<Player, ExtraInventory> CLIENT_STORES = new WeakHashMap<>(), SERVER_STORES = new WeakHashMap<>();
    private final WeakReference<Player> owner;
    private boolean loading = true;
    public ExtraInventory(Player owner) {
        super(SIZE);
        this.owner = new WeakReference<>(owner);
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(owner.getPersistentData().getCompound(DATA_KEY), items);
        for (int slot = 0; slot < SIZE; slot++) super.setItem(slot, items.get(slot));
        loading = false;
    }
    @Override public void setChanged() {
        super.setChanged();
        Player player = owner == null ? null : owner.get();
        if (loading || player == null) return;
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        for (int slot = 0; slot < SIZE; slot++) items.set(slot, getItem(slot));
        CompoundTag tag = new CompoundTag();
        ContainerHelper.saveAllItems(tag, items);
        player.getPersistentData().put(DATA_KEY, tag);
    }
    public static ExtraInventory of(Player player) { return (player.level().isClientSide ? CLIENT_STORES : SERVER_STORES).computeIfAbsent(player, ExtraInventory::new); }
    public void replace(List<ItemStack> items) {
        loading = true;
        for (int slot = 0; slot < SIZE; slot++) super.setItem(slot, items.get(slot));
        loading = false;
        setChanged();
    }
}
