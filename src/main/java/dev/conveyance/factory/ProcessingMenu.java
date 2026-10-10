package dev.conveyance.factory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;
import dev.conveyance.player.InventoryAccess;
import dev.conveyance.player.LinkedStorageSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class ProcessingMenu extends AbstractContainerMenu {
    private final Container container;
    private final MachineEntity machine;
    private final MachineKind kind;
    private final Level level;
    private final ContainerData data;
    public ProcessingMenu(int id, Inventory inventory, MachineKind kind) { this(id, inventory, new SimpleContainer(kind.slots), kind, null); }
    public ProcessingMenu(int id, Inventory inventory, MachineEntity machine) { this(id, inventory, machine, machine.kind(), machine); }
    private ProcessingMenu(int id, Inventory inventory, Container container, MachineKind kind, MachineEntity machine) {
        super(kind == MachineKind.ASSEMBLER ? FactoryContent.ASSEMBLER_MENU.get() : FactoryContent.FURNACE_MENU.get(), id);
        this.container = container; this.kind = kind; this.machine = machine; this.level = inventory.player.level();
        checkContainerSize(container, kind.slots);
        container.startOpen(inventory.player);
        if (kind == MachineKind.FURNACE) {
            slot(0, 56, 17); slot(1, 56, 53); slot(2, 116, 35);
        } else {
            for (int index = 0; index < 4; index++) slot(index, 8 + index * 18, 55);
            slot(4, 142, 55);
        }
        int inventoryY = kind == MachineKind.ASSEMBLER ? 93 : 84;
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, inventoryY + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new LinkedStorageSlot(inventory, column, 8 + column * 18, inventoryY + 58));
        data = machine == null ? new SimpleContainerData(3) : new ContainerData() {
            @Override public int get(int index) { return index == 0 ? machine.fuelRemaining() : index == 1 ? machine.progressPixels() : kind == MachineKind.ASSEMBLER ? machine.selectedRecipeIndex() : -1; }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 3; }
        };
        addDataSlots(data);
    }
    public boolean assembler() { return kind == MachineKind.ASSEMBLER; }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        InventoryAccess.click(slot, type, () -> super.clicked(slot, button, type, player));
    }
    public int progress() { return Math.max(0, Math.min(24, data.get(1))); }
    public boolean burning() { return data.get(0) > 0; }
    public CraftingRecipe recipe() {
        var recipes = AssemblyRecipes.all(level);
        int selected = data.get(2);
        return selected < 0 || selected >= recipes.size() ? null : recipes.get(selected);
    }
    private boolean accepts(int index, ItemStack stack) {
        if (machine != null) return machine.canPlaceItem(index, stack);
        if (kind == MachineKind.FURNACE) return index == 1 ? GeneratorEntity.isFuel(stack)
                : index == 0 && IndustrialSmelting.find(level, stack) != null;
        CraftingRecipe recipe = recipe();
        var requirements = recipe == null ? java.util.List.<AssemblyRecipes.Requirement>of() : AssemblyRecipes.requirements(recipe);
        return index < requirements.size() && requirements.get(index).ingredient().test(stack);
    }
    private void slot(int index, int x, int y) {
        addSlot(new Slot(container, index, x, y) { @Override public boolean mayPlace(ItemStack stack) { return accepts(index, stack); } });
    }
    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    @Override public void removed(Player player) { super.removed(player); container.stopOpen(player); }
    public boolean selectRecipe(Player player, ResourceLocation id) {
        if (machine == null || !assembler() || !stillValid(player) || player.isSpectator()) return false;
        if (!machine.selectRecipe(player, id)) return false;
        broadcastChanges();
        return true;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot source = slots.get(index);
        if (!source.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = source.getItem(), original = stack.copy();
        int firstPlayerSlot = kind.slots, hotbar = kind.slots + 27, end = kind.slots + 36;
        if (index < firstPlayerSlot) {
            if (!moveItemStackTo(stack, firstPlayerSlot, end, true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, kind.slots - 1, false)) {
            if (index < hotbar) { if (!moveItemStackTo(stack, hotbar, end, false)) return ItemStack.EMPTY; }
            else if (!moveItemStackTo(stack, firstPlayerSlot, hotbar, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) source.set(ItemStack.EMPTY); else source.setChanged();
        source.onTake(player, stack);
        return original;
    }
}
