package dev.conveyance.factory;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class InserterEntity extends PoweredMachineEntity {
    private ItemStack held = ItemStack.EMPTY;
    public InserterEntity(BlockPos pos, BlockState state) {
        super(((InserterBlock) state.getBlock()).reach() == 2 ? FactoryContent.LONG_INSERTER_ENTITY.get() : FactoryContent.INSERTER_ENTITY.get(), pos, state,
                15, ((InserterBlock) state.getBlock()).reach() == 2 ? 20 : 10);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, InserterEntity inserter) {
        if (!inserter.advanceWork()) return;
        var facing = state.getValue(InserterBlock.FACING);
        int reach = ((InserterBlock) state.getBlock()).reach();
        BlockPos from = pos.relative(facing.getOpposite(), reach), to = pos.relative(facing, reach);
        if (inserter.held.isEmpty()) {
            ItemStack candidate = ItemTransfer.peekForOutput(level, from, to, facing);
            if (candidate.isEmpty()) return;
            inserter.held = ItemTransfer.takeSource(level, from, facing, candidate);
            if (inserter.held.isEmpty()) return;
            inserter.setChanged();
        }
        if (ItemTransfer.output(level, to, facing.getOpposite(), inserter.held)) {
            inserter.held = ItemStack.EMPTY;
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
