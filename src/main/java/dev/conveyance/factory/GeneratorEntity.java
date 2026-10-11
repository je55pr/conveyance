package dev.conveyance.factory;

import net.minecraft.core.BlockPos;
import dev.conveyance.player.ResearchState;
import dev.conveyance.pollution.PollutionData;
import dev.conveyance.pollution.PollutionRates;
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
    public static final int OUTPUT_KW = 120;
    public static final int MAX_FUEL_TICKS = 1000;
    private NonNullList<ItemStack> items = NonNullList.withSize(0, ItemStack.EMPTY);
    // Remaining fuel energy measured in ticks at the generator's full 120 kW output.
    private double fuelReserve;
    private double producingKW;
    boolean recovered;
    public GeneratorEntity(BlockPos pos, BlockState state) { super(FactoryContent.GENERATOR_ENTITY.get(), pos, state); }

    public static void tick(Level level, BlockPos pos, BlockState state, GeneratorEntity generator) {
        if (!state.getValue(GeneratorBlock.LIT)) level.setBlockAndUpdate(pos, state.setValue(GeneratorBlock.LIT, true));
    }
    @Override public double availablePower() {
        return Double.POSITIVE_INFINITY;
    }
    @Override public double outputPower() { return Double.POSITIVE_INFINITY; }
    @Override public boolean consumesFuelPower() { return false; }
    public int fuelRemaining() { return 0; }
    static boolean isFuel(ItemStack stack) { return FuelEnergy.mj(stack) > 0; }
    @Override public void onLoad() {
        super.onLoad();
        PowerNetwork.register(level, worldPosition, PowerNetwork.Kind.GENERATOR);
    }
    @Override public void setRemoved() {
        PowerNetwork.unregister(level, worldPosition, PowerNetwork.Kind.GENERATOR);
        super.setRemoved();
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return false; }
    @Override public boolean canTakeItem(Container destination, int slot, ItemStack stack) { return false; }
    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int count) { ItemStack stack = ContainerHelper.removeItem(items, slot, count); setChanged(); return stack; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); setChanged(); }
    @Override public boolean stillValid(Player player) { return !isRemoved() && (RemoteBuilding.canReach(player, worldPosition) || player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) < 64); }
    @Override public void clearContent() { items.clear(); setChanged(); }
    @Override public Component getDisplayName() { return Component.translatable("block.conveyance.generator"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return null; }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        items = NonNullList.withSize(0, ItemStack.EMPTY);
    }
}
