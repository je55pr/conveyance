package dev.conveyance.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

final class ItemTransfer {
    static Container containerAt(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return null;
        var machine = MachineBlock.controller(level, pos);
        if (machine != null) return machine;
        return level.getBlockEntity(pos) instanceof Container container ? container : null;
    }

    static ItemStack insert(Container container, ItemStack input, boolean simulate) {
        ItemStack remaining = input.copy();
        for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
            if (!container.canPlaceItem(slot, remaining)) continue;
            ItemStack existing = container.getItem(slot);
            if (!existing.isEmpty() && !ItemStack.isSameItemSameTags(existing, remaining)) continue;
            int limit = Math.min(container.getMaxStackSize(), remaining.getMaxStackSize());
            int moved = Math.min(remaining.getCount(), limit - existing.getCount());
            if (moved <= 0) continue;
            if (!simulate) {
                ItemStack next = existing.isEmpty() ? remaining.copyWithCount(moved) : existing.copyWithCount(existing.getCount() + moved);
                container.setItem(slot, next);
                container.setChanged();
            }
            remaining.shrink(moved);
        }
        return remaining;
    }
    private ItemTransfer() {}
}
