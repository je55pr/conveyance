package dev.conveyance.factory;

import net.minecraft.core.BlockPos;
import dev.conveyance.player.ResearchState;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class GeneratorEntity extends BlockEntity implements Container, MenuProvider, PowerProducer {
    public static final int OUTPUT_KW = 120, FUEL_TICKS = 600;
    public static final int MAX_FUEL_TICKS = 750;
    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private int burnTicks;
    boolean recovered;
    public GeneratorEntity(BlockPos pos, BlockState state) { super(FactoryContent.GENERATOR_ENTITY.get(), pos, state); }

    public static void tick(Level level, BlockPos pos, BlockState state, GeneratorEntity generator) {
        boolean wasBurning = generator.burnTicks > 0;
        if (wasBurning) generator.burnTicks--;
        if (generator.burnTicks == 0) {
            for (ItemStack stack : generator.items) {
                if (isFuel(stack)) {
                    generator.burnTicks = fuelTicks(stack, level);
                    stack.shrink(1);
                    generator.setChanged();
                    break;
                }
            }
        }
        boolean burning = generator.burnTicks > 0;
        if (state.getValue(GeneratorBlock.LIT) != burning) level.setBlockAndUpdate(pos, state.setValue(GeneratorBlock.LIT, burning));
        if (wasBurning != burning) PowerNetwork.invalidateSupply(level);
        if (generator.burnTicks % 20 == 0 && (wasBurning || burning)) generator.setChanged();
    }
    public int outputPower() { return burnTicks > 0 ? OUTPUT_KW : 0; }
    public int fuelRemaining() { return burnTicks; }
    public static int fuelTicks(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        return stack.is(FactoryContent.WOOD_ITEM.get()) ? FUEL_TICKS / 2
                : stack.is(Items.COAL) || stack.is(Items.CHARCOAL) ? FUEL_TICKS : 0;
    }
    static boolean isFuel(ItemStack stack) { return fuelTicks(stack) > 0; }
    static int fuelTicks(ItemStack stack, Level level) { return (int) Math.round(fuelTicks(stack) * ResearchState.fuelDuration(level)); }
    @Override public void onLoad() {
        super.onLoad();
        PowerNetwork.register(level, worldPosition, PowerNetwork.Kind.GENERATOR);
    }
    @Override public void setRemoved() {
        PowerNetwork.unregister(level, worldPosition, PowerNetwork.Kind.GENERATOR);
        super.setRemoved();
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return isFuel(stack); }
    @Override public boolean canTakeItem(Container destination, int slot, ItemStack stack) { return false; }
    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int count) { ItemStack stack = ContainerHelper.removeItem(items, slot, count); setChanged(); return stack; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); setChanged(); }
    @Override public boolean stillValid(Player player) { return !isRemoved() && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) < 64; }
    @Override public void clearContent() { items.clear(); setChanged(); }
    @Override public Component getDisplayName() { return Component.translatable("block.conveyance.generator"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new GeneratorMenu(id, inventory, this); }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); ContainerHelper.saveAllItems(tag, items); tag.putInt("BurnTicks", burnTicks); }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        items = NonNullList.withSize(1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        if (!isFuel(items.get(0))) items.set(0, ItemStack.EMPTY);
        burnTicks = Math.max(0, Math.min(MAX_FUEL_TICKS, tag.getInt("BurnTicks")));
    }
}
