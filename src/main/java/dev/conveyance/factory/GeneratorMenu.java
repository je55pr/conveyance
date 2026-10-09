package dev.conveyance.factory;

import dev.conveyance.player.ResearchState;
import dev.conveyance.player.Technology;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class GeneratorMenu extends AbstractContainerMenu {
    @Override public void clicked(int slot, int button, net.minecraft.world.inventory.ClickType type, Player player) {
        dev.conveyance.player.InventoryAccess.click(slot, type, () -> super.clicked(slot, button, type, player));
    }
    private final Container fuel;
    private final boolean lab;
    private final ContainerData research;

    public GeneratorMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(1)); }
    public GeneratorMenu(int id, Inventory inventory, Container fuel) {
        this(id, inventory, fuel, false);
    }
    public GeneratorMenu(int id, Inventory inventory, Container fuel, boolean lab) {
        super(lab ? FactoryContent.LAB_MENU.get() : FactoryContent.GENERATOR_MENU.get(), id);
        int size = lab ? 2 : 1;
        checkContainerSize(fuel, size);
        this.fuel = fuel;
        this.lab = lab;
        fuel.startOpen(inventory.player);
        for (int slot = 0; slot < size; slot++) {
            final int input = slot;
            addSlot(new Slot(fuel, input, lab ? 62 + input * 36 : 80, 18) {
                @Override public boolean mayPlace(ItemStack stack) { return accepts(input, stack); }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 49 + row * 18));
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 107));
        research = lab && fuel instanceof MachineEntity machine ? new ContainerData() {
            @Override public int get(int index) {
                int selected = ResearchState.selected(machine.getLevel());
                Technology tech = Technology.byId(selected);
                return index == 0 ? tech == null ? 0 : tech.menuId() : index == 1 ? ResearchState.progress(machine.getLevel(), selected)
                        : index == 2 ? ResearchState.packCost(selected) : machine.progressPixels();
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 4; }
        } : inventory.player.level().isClientSide ? new SimpleContainerData(4) : new ContainerData() {
            @Override public int get(int index) {
                if (index == 0) return fuel instanceof GeneratorEntity generator ? generator.fuelRemaining()
                        : fuel instanceof MachineEntity machine ? machine.fuelRemaining()
                        : fuel instanceof BurnerInserterEntity arm ? arm.fuelRemaining() : 0;
                if (fuel instanceof MachineEntity machine && machine.kind() == MachineKind.BURNER_MINER)
                    return index == 1 ? machine.miningStatus().ordinal() + 1 : index == 2 ? machine.progressPixels() : 0;
                return 0;
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 4; }
        };
        addDataSlots(research);
    }
    private boolean accepts(int slot, ItemStack stack) { return lab ? stack.is(slot == 0 ? FactoryContent.RED_SCIENCE_ITEM.get() : FactoryContent.GREEN_SCIENCE_ITEM.get()) : GeneratorEntity.isFuel(stack); }
    public boolean lab() { return lab; }
    public int fuelRemaining() { return lab ? 0 : research.get(0); }
    public MachineEntity.MiningStatus minerStatus() {
        int code = lab ? 0 : research.get(1);
        return code > 0 && code <= MachineEntity.MiningStatus.values().length ? MachineEntity.MiningStatus.values()[code - 1] : null;
    }
    public int miningProgress() { return Math.max(0, Math.min(24, research.get(2))); }
    public int researchTarget() { Technology tech = Technology.byMenuId(research.get(0)); return tech == null ? 0 : tech.id; }
    public int scienceDone() { return research.get(1); }
    public int scienceRequired() { return research.get(2); }
    public int scienceProgress() { return Math.max(0, Math.min(24, research.get(3))); }
    @Override public boolean stillValid(Player player) { return fuel.stillValid(player); }
    @Override public void removed(Player player) { super.removed(player); fuel.stopOpen(player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        int size = lab ? 2 : 1, hotbar = size + 27, end = size + 36;
        if (index < size) {
            if (!moveItemStackTo(stack, size, end, true)) return ItemStack.EMPTY;
        } else if (accepts(0, stack) || lab && accepts(1, stack)) {
            if (!moveItemStackTo(stack, 0, size, false)) return ItemStack.EMPTY;
        } else if (index < hotbar) {
            if (!moveItemStackTo(stack, hotbar, end, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, size, hotbar, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
