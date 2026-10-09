package dev.conveyance.player;

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ExtraInventory extends SimpleContainer {
    public static final String DATA_KEY = "ConveyanceStorage";
    public static final int SIZE = 36;
    private final Player owner;
    private boolean loading = true;
    public ExtraInventory(Player owner) {
        super(SIZE);
        this.owner = owner;
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(owner.getPersistentData().getCompound(DATA_KEY), items);
        for (int slot = 0; slot < SIZE; slot++) super.setItem(slot, items.get(slot));
        loading = false;
    }
    @Override public void setChanged() {
        super.setChanged();
        if (loading || owner == null) return;
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        for (int slot = 0; slot < SIZE; slot++) items.set(slot, getItem(slot));
        CompoundTag tag = new CompoundTag();
        ContainerHelper.saveAllItems(tag, items);
        owner.getPersistentData().put(DATA_KEY, tag);
    }
    void replace(List<ItemStack> items) {
        loading = true;
        for (int slot = 0; slot < SIZE; slot++) super.setItem(slot, items.get(slot));
        loading = false;
        setChanged();
    }
}
