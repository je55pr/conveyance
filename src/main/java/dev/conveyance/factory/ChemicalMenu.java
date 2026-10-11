package dev.conveyance.factory;

import dev.conveyance.player.InventoryAccess;
import dev.conveyance.player.PlayerStorageLayout;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ChemicalMenu extends FactoryMenu {
    public final MachineKind kind;
    private final Container container;
    private final MachineEntity machine;
    private final net.minecraft.world.level.Level level;
    private final ContainerData data;
    private boolean processing = true;
    private static MenuType<ChemicalMenu> type(MachineKind kind) { return kind == MachineKind.PUMPJACK ? FactoryContent.PUMPJACK_MENU.get() : kind == MachineKind.REFINERY ? FactoryContent.REFINERY_MENU.get() : FactoryContent.CHEMICAL_MENU.get(); }
    public ChemicalMenu(int id, Inventory inventory, MachineKind kind) { this(id, inventory, new SimpleContainer(kind.slots), kind, null); }
    public ChemicalMenu(int id, Inventory inventory, MachineEntity machine) { this(id, inventory, machine, machine.kind(), machine); }
    private ChemicalMenu(int id, Inventory inventory, Container container, MachineKind kind, MachineEntity machine) {
        super(type(kind), id); this.kind = kind; this.container = container; this.machine = machine; level = inventory.player.level();
        for (int slot = 0; slot < kind.slots; slot++) {
            final int index = slot;
            addSlot(new Slot(container, slot, PlayerStorageLayout.MACHINE_X + (slot == 2 ? 152 : 8 + slot * 20), 80) {
                @Override public boolean mayPlace(ItemStack stack) {
                    if (machine != null) return machine.canPlaceItem(index, stack);
                    var recipe = recipe(); return recipe != null && index < recipe.items().size() && recipe.items().get(index).ingredient().test(stack);
                }
                @Override public boolean isActive() { return processing; }
            });
        }
        addPlayerInventory(inventory);
        data = machine == null ? new SimpleContainerData(9) : new ContainerData() {
            @Override public int get(int index) {
                if (index == 0) return machine.chemical().recipeIndex();
                if (index == 1) return machine.chemical().progress();
                if (index == 2) return machine.chemical().status().ordinal();
                if (index == 8) return (int) Math.round(machine.chemical().flow() * 10);
                return (int) Math.round(FluidNetwork.localAmount(level, machine.getBlockPos(), index - 3) * 10);
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 9; }
        };
        if (machine == null) data.set(0, -1);
        addDataSlots(data);
    }
    public ChemicalRecipe recipe() { var recipes = ChemicalRecipe.all(level, kind); int index = data == null ? -1 : data.get(0); return index < 0 || index >= recipes.size() ? null : recipes.get(index); }
    public float progress() { return data.get(1) / 1000F; }
    public ChemicalMachine.Status status() { return ChemicalMachine.Status.values()[Math.max(0, Math.min(ChemicalMachine.Status.values().length - 1, data.get(2)))]; }
    public double amount(int channel) { return data.get(channel + 3) / 10.0; }
    public double flow() { return data.get(8) / 10.0; }
    public void showProcessing(boolean value) { processing = value; }
    public boolean selectRecipe(Player player, ResourceLocation id) {
        if (machine == null || !stillValid(player) || player.isSpectator()) return false;
        if (machine.chemical().recipe() != null && machine.chemical().recipe().id().equals(id)) return true;
        var extra = InventoryAccess.extra(player); var inventory = InventoryAccess.snapshot(player, extra);
        for (int slot = 0; slot < container.getContainerSize(); slot++) if (!InventoryAccess.insert(inventory, container.getItem(slot))) return false;
        if (!machine.chemical().select(id, player.isCreative())) return false;
        InventoryAccess.commit(player, extra, inventory); container.clearContent(); broadcastChanges(); return true;
    }
    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    @Override protected boolean moveToMachine(ItemStack stack) { return kind.slots > 0 && moveItemStackTo(stack, 0, 2, false); }
}
