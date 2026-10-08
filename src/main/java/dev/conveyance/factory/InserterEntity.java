package dev.conveyance.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class InserterEntity extends PoweredMachineEntity {
    private ItemStack held = ItemStack.EMPTY;
    public InserterEntity(BlockPos pos, BlockState state) { super(FactoryContent.INSERTER_ENTITY.get(), pos, state, 15, 10); }

    public static void tick(Level level, BlockPos pos, BlockState state, InserterEntity inserter) {
        if (!inserter.advanceWork()) return;
        var facing = state.getValue(InserterBlock.FACING);
        BlockPos from = pos.relative(facing.getOpposite()), to = pos.relative(facing);
        if (inserter.held.isEmpty()) {
            var source = ItemTransfer.containerAt(level, from);
            if (source != null) {
                for (int slot = 0; slot < source.getContainerSize(); slot++) {
                    if (!source.getItem(slot).isEmpty() && source.canTakeItem(source, slot, source.getItem(slot))) {
                        inserter.held = source.removeItem(slot, 1);
                        source.setChanged();
                        break;
                    }
                }
            } else {
                ConveyorEntity belt = ConveyorEntity.at(level, from);
                if (belt != null) inserter.held = belt.takeFront();
            }
            if (!inserter.held.isEmpty()) inserter.setChanged();
        }
        if (!inserter.held.isEmpty()) {
            var target = ItemTransfer.containerAt(level, to);
            if (target != null) inserter.held = ItemTransfer.insert(target, inserter.held, false);
            else {
                ConveyorEntity belt = ConveyorEntity.at(level, to);
                if (belt != null && belt.offer(inserter.held, facing.getOpposite())) inserter.held.shrink(1);
            }
            inserter.setChanged();
        }
    }

    public void dropHeld() {
        if (level != null && !level.isClientSide && !held.isEmpty()) {
            level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, held));
            held = ItemStack.EMPTY;
        }
    }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.put("Held", held.save(new CompoundTag())); }
    @Override public void load(CompoundTag tag) { super.load(tag); held = ItemStack.of(tag.getCompound("Held")); }
}
