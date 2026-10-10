package dev.conveyance.factory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import dev.conveyance.player.PlayerStorageLayout;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class ProcessingMenu extends FactoryMenu {
    private final Container container;
    private final MachineEntity machine;
    private final MachineKind kind;
    private final Level level;
    private final ContainerData data;
    private boolean processingVisible = true;
    public ProcessingMenu(int id, Inventory inventory, MachineKind kind) { this(id, inventory, new SimpleContainer(kind.slots), kind, null); }
    public ProcessingMenu(int id, Inventory inventory, MachineEntity machine) { this(id, inventory, machine, machine.kind(), machine); }
    private ProcessingMenu(int id, Inventory inventory, Container container, MachineKind kind, MachineEntity machine) {
        super(kind == MachineKind.ASSEMBLER ? FactoryContent.ASSEMBLER_MENU.get() : FactoryContent.FURNACE_MENU.get(), id);
        this.container = container; this.kind = kind; this.machine = machine; this.level = inventory.player.level();
        checkContainerSize(container, kind.slots);
        container.startOpen(inventory.player);
        if (kind == MachineKind.FURNACE) {
            slot(0, PlayerStorageLayout.MACHINE_X + 32, 60); slot(1, PlayerStorageLayout.MACHINE_X + 32, 100); slot(2, PlayerStorageLayout.MACHINE_X + 124, 80);
        } else {
            for (int index = 0; index < 4; index++) slot(index, PlayerStorageLayout.MACHINE_X + 8 + index * PlayerStorageLayout.CELL, 70);
            slot(4, PlayerStorageLayout.MACHINE_X + 152, 70);
        }
        addPlayerInventory(inventory);
        data = machine == null ? new SimpleContainerData(3) : new ContainerData() {
            @Override public int get(int index) { return index == 0 ? machine.fuelRemaining() : index == 1 ? machine.progressPixels() : kind == MachineKind.ASSEMBLER ? machine.selectedRecipeIndex() : -1; }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 3; }
        };
        if (machine == null) data.set(2, -1);
        addDataSlots(data);
    }
    public boolean assembler() { return kind == MachineKind.ASSEMBLER; }
    public void showProcessing(boolean visible) { processingVisible = visible; }
    public int progress() { return Math.max(0, Math.min(24, data.get(1))); }
    public boolean burning() { return data.get(0) > 0; }
    public int fuelRemaining() { return Math.max(0, data.get(0)); }
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
        addSlot(new Slot(container, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return accepts(index, stack); }
            @Override public boolean isActive() { return processingVisible; }
        });
    }
    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    @Override public void removed(Player player) { super.removed(player); container.stopOpen(player); }
    public boolean selectRecipe(Player player, ResourceLocation id) {
        if (machine == null || !assembler() || !stillValid(player) || player.isSpectator()) return false;
        if (!machine.selectRecipe(player, id)) return false;
        broadcastChanges();
        return true;
    }
    @Override protected boolean moveToMachine(ItemStack stack) {
        return moveItemStackTo(stack, 0, kind.slots - 1, false);
    }
}
