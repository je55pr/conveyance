package dev.conveyance.factory;

import dev.conveyance.player.InventoryAccess;
import dev.conveyance.player.LinkedHotbar;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

/** Tier-one Factorio module effects add together; idle power is unaffected. */
public final class MachineModules implements Container, MenuProvider {
    private final PoweredMachineEntity owner;
    private final NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private double bonus;
    private String bonusRecipe = "";
    MachineModules(PoweredMachineEntity owner) { this.owner = owner; }
    public int capacity() { return owner instanceof MinerEntity ? 3 : owner instanceof MachineEntity machine ? machine.kind().moduleSlots() : 0; }
    public static boolean module(ItemStack stack) { return stack.is(FactoryContent.SPEED_MODULE_ITEM.get()) || stack.is(FactoryContent.PRODUCTIVITY_MODULE_ITEM.get()) || stack.is(FactoryContent.EFFICIENCY_MODULE_ITEM.get()); }
    private int count(net.minecraft.world.item.Item item) { return (int) items.stream().filter(stack -> stack.is(item)).count(); }
    public double speed() { return Math.max(.2, 1 + .2 * count(FactoryContent.SPEED_MODULE_ITEM.get()) - .05 * count(FactoryContent.PRODUCTIVITY_MODULE_ITEM.get())); }
    public double consumption() { return Math.max(.2, 1 + .5 * count(FactoryContent.SPEED_MODULE_ITEM.get()) + .4 * count(FactoryContent.PRODUCTIVITY_MODULE_ITEM.get()) - .3 * count(FactoryContent.EFFICIENCY_MODULE_ITEM.get())); }
    public double pollution() { return consumption() * (1 + .05 * count(FactoryContent.PRODUCTIVITY_MODULE_ITEM.get())); }
    public double productivity(boolean allowed) { return allowed ? .04 * count(FactoryContent.PRODUCTIVITY_MODULE_ITEM.get()) : 0; }
    public int previewBonus(String recipe, double productivity) { return (int) Math.floor((recipe.equals(bonusRecipe) ? bonus : 0) + productivity + .000001); }
    public int produce(String recipe, double productivity) {
        if (!recipe.equals(bonusRecipe)) { bonusRecipe = recipe; bonus = 0; }
        bonus += productivity; int additional = (int) Math.floor(bonus + .000001); bonus = Math.max(0, bonus - additional); owner.setChanged(); return additional;
    }
    public static boolean productive(net.minecraft.world.item.crafting.CraftingRecipe recipe) {
        return recipe instanceof FactoryCraftingRecipe factory && (factory.tab().equals("intermediates") || factory.tab().equals("science"));
    }
    public static boolean interact(PoweredMachineEntity entity, Player player, InteractionHand hand) {
        if (entity == null || entity.modules.capacity() == 0 || hand != InteractionHand.MAIN_HAND) return false;
        ItemStack held = player.getItemInHand(hand);
        if (module(held)) {
            if (!player.level().isClientSide) {
                LinkedHotbar.refreshHand(player);
                for (int slot = 0; slot < entity.modules.capacity(); slot++) if (entity.modules.getItem(slot).isEmpty()) {
                    var extra = InventoryAccess.extra(player); var inventory = InventoryAccess.snapshot(player, extra);
                    boolean found = player.isCreative();
                    if (!found) for (ItemStack source : inventory) if (source.is(held.getItem()) && !source.isEmpty()) { source.shrink(1); found = true; break; }
                    if (found) { entity.modules.setItem(slot, held.copyWithCount(1)); InventoryAccess.commit(player, extra, inventory); }
                    break;
                }
            }
            return true;
        }
        if (held.isEmpty() && player.isShiftKeyDown()) { if (!player.level().isClientSide) player.openMenu(entity.modules); return true; }
        return false;
    }
    public List<ItemStack> contents() { return items.stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy).toList(); }
    public CompoundTag save() { var tag = new CompoundTag(); ContainerHelper.saveAllItems(tag, items); tag.putDouble("Bonus", bonus); tag.putString("Recipe", bonusRecipe); return tag; }
    public void load(CompoundTag tag) { ContainerHelper.loadAllItems(tag, items); double stored = tag.getDouble("Bonus"); bonus = Double.isFinite(stored) ? Math.max(0, Math.min(.999999, stored)) : 0; bonusRecipe = tag.getString("Recipe"); }
    @Override public int getContainerSize() { return 3; }
    @Override public int getMaxStackSize() { return 1; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int count) { ItemStack result = ContainerHelper.removeItem(items, slot, count); setChanged(); return result; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); setChanged(); }
    @Override public void setChanged() { owner.setChanged(); PowerNetwork.changed(owner.getLevel()); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot < capacity() && module(stack); }
    @Override public void clearContent() { items.clear(); setChanged(); }
    @Override public boolean stillValid(Player player) { return !owner.isRemoved() && (RemoteBuilding.canReach(player, owner.getBlockPos()) || player.distanceToSqr(owner.getBlockPos().getCenter()) < 100); }
    @Override public Component getDisplayName() { return Component.translatable("menu.conveyance.modules"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new ModuleMenu(id, inventory, this); }
}
