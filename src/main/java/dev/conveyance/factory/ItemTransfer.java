package dev.conveyance.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;

final class ItemTransfer {
    static Container containerAt(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return null;
        var machine = MachineBlock.controller(level, pos);
        if (machine != null) return machine;
        return level.getBlockEntity(pos) instanceof Container container ? container : null;
    }

    static boolean canOutput(Level level, BlockPos pos, Direction incomingSide, ItemStack stack) {
        return canOutput(level, pos, incomingSide, stack, false);
    }
    static boolean canOutput(Level level, BlockPos pos, Direction incomingSide, ItemStack stack, boolean mined) {
        if (stack.isEmpty() || !level.hasChunkAt(pos)) return false;
        Container target = containerAt(level, pos);
        if (target != null) return insert(target, stack, true, coalDrillSlot(target, stack, mined)).isEmpty();
        ConveyorEntity belt = ConveyorEntity.at(level, pos);
        if (belt != null) return belt.canAccept(incomingSide);
        JunctionEntity junction = JunctionEntity.at(level, pos);
        return junction != null && junction.canAccept(incomingSide);
    }
    static boolean output(Level level, BlockPos pos, Direction incomingSide, ItemStack stack) {
        return output(level, pos, incomingSide, stack, false);
    }
    static boolean output(Level level, BlockPos pos, Direction incomingSide, ItemStack stack, boolean mined) {
        if (!canOutput(level, pos, incomingSide, stack, mined)) return false;
        Container target = containerAt(level, pos);
        if (target != null) return insert(target, stack, false, coalDrillSlot(target, stack, mined)).isEmpty();
        ConveyorEntity belt = ConveyorEntity.at(level, pos);
        if (belt != null) return belt.offer(stack, incomingSide);
        JunctionEntity junction = JunctionEntity.at(level, pos);
        return junction != null && junction.offer(stack, incomingSide);
    }
    static ItemStack peekSource(Level level, BlockPos pos, Direction outgoingSide) {
        Container source = containerAt(level, pos);
        if (source != null) {
            for (int slot = 0; slot < source.getContainerSize(); slot++) {
                ItemStack stack = source.getItem(slot);
                if (!stack.isEmpty() && source.canTakeItem(source, slot, stack)) return stack.copyWithCount(1);
            }
            return ItemStack.EMPTY;
        }
        ConveyorEntity belt = ConveyorEntity.at(level, pos);
        if (belt != null) return belt.peekFront();
        JunctionEntity junction = JunctionEntity.at(level, pos);
        return junction == null ? ItemStack.EMPTY : junction.peekOutput(outgoingSide);
    }
    static ItemStack peekForOutput(Level level, BlockPos from, BlockPos to, Direction facing) {
        Container source = containerAt(level, from);
        if (source != null) {
            for (int slot = 0; slot < source.getContainerSize(); slot++) {
                ItemStack stack = source.getItem(slot);
                if (stack.isEmpty() || !source.canTakeItem(source, slot, stack)) continue;
                ItemStack candidate = stack.copyWithCount(1);
                if (canOutput(level, to, facing.getOpposite(), candidate)) return candidate;
            }
            return ItemStack.EMPTY;
        }
        ItemStack candidate = peekSource(level, from, facing);
        return !candidate.isEmpty() && canOutput(level, to, facing.getOpposite(), candidate) ? candidate : ItemStack.EMPTY;
    }
    static ItemStack takeSource(Level level, BlockPos pos, Direction outgoingSide, boolean fuelOnly) {
        return takeSource(level, pos, outgoingSide, fuelOnly, ItemStack.EMPTY);
    }
    static ItemStack takeSource(Level level, BlockPos pos, Direction outgoingSide, ItemStack expected) {
        return takeSource(level, pos, outgoingSide, false, expected);
    }
    private static ItemStack takeSource(Level level, BlockPos pos, Direction outgoingSide, boolean fuelOnly, ItemStack expected) {
        Container source = containerAt(level, pos);
        if (source != null) {
            for (int slot = 0; slot < source.getContainerSize(); slot++) {
                ItemStack stack = source.getItem(slot);
                if (stack.isEmpty() || !source.canTakeItem(source, slot, stack) || fuelOnly && !GeneratorEntity.isFuel(stack)) continue;
                if (!expected.isEmpty() && !ItemStack.isSameItemSameTags(stack, expected)) continue;
                ItemStack result = source.removeItem(slot, 1);
                source.setChanged();
                return result;
            }
            return ItemStack.EMPTY;
        }
        ItemStack candidate = peekSource(level, pos, outgoingSide);
        if (fuelOnly && !GeneratorEntity.isFuel(candidate)
                || !expected.isEmpty() && !ItemStack.isSameItemSameTags(candidate, expected)) return ItemStack.EMPTY;
        ConveyorEntity belt = ConveyorEntity.at(level, pos);
        if (belt != null) return belt.takeFront();
        JunctionEntity junction = JunctionEntity.at(level, pos);
        return junction == null ? ItemStack.EMPTY : junction.takeOutput(outgoingSide);
    }

    static ItemStack insert(Container container, ItemStack input, boolean simulate) {
        return insert(container, input, simulate, false);
    }
    private static boolean coalDrillSlot(Container target, ItemStack stack, boolean mined) {
        // A pair of coal drills uses its fuel slots as the harvest storage. Applying
        // the inserter's five-item cap here permanently stalls both drills at five.
        return mined && stack.is(Items.COAL) && target instanceof MachineEntity machine && machine.kind() == MachineKind.BURNER_MINER;
    }
    private static ItemStack insert(Container container, ItemStack input, boolean simulate, boolean coalHarvest) {
        ItemStack remaining = input.copy();
        for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
            if (!container.canPlaceItem(slot, remaining)) continue;
            ItemStack existing = container.getItem(slot);
            if (!existing.isEmpty() && !ItemStack.isSameItemSameTags(existing, remaining)) continue;
            int limit = coalHarvest ? Math.min(container.getMaxStackSize(), remaining.getMaxStackSize()) : automaticLimit(container, slot, remaining);
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
    static int smallBuffer(ItemStack stack) { return Math.max(1, (stack.getMaxStackSize() + 19) / 20); }
    private static int automaticLimit(Container container, int slot, ItemStack stack) {
        int capacity = Math.min(container.getMaxStackSize(), stack.getMaxStackSize());
        int target = capacity;
        if (container instanceof MachineEntity machine) target = machine.automaticInputLimit(slot, stack);
        else if (container instanceof GeneratorEntity || container instanceof BurnerInserterEntity) target = smallBuffer(stack);
        else if (container instanceof AbstractFurnaceBlockEntity furnace) {
            if (slot == 0 && furnace.getLevel() != null) {
                var type = furnace instanceof BlastFurnaceBlockEntity ? RecipeType.BLASTING
                        : furnace instanceof SmokerBlockEntity ? RecipeType.SMOKING : RecipeType.SMELTING;
                target = furnace.getLevel().getRecipeManager().getRecipeFor(type, new SimpleContainer(stack), furnace.getLevel()).isPresent() ? 2 : 0;
            } else target = slot == 1 ? smallBuffer(stack) : 0;
        }
        return Math.min(capacity, target);
    }
    private ItemTransfer() {}
}
