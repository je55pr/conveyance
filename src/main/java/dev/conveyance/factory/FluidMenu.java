package dev.conveyance.factory;

import dev.conveyance.player.PlayerStorageLayout;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class FluidMenu extends FactoryMenu {
    public final MachineKind kind;
    private final Container machine;
    private final ContainerData data;
    public FluidMenu(int id, Inventory inventory, MachineKind kind) { this(id, inventory, new SimpleContainer(kind.slots), kind); }
    public FluidMenu(int id, Inventory inventory, MachineEntity machine) { this(id, inventory, machine, machine.kind()); }
    private static MenuType<FluidMenu> type(MachineKind kind) {
        return switch (kind) {
            case BOILER -> FactoryContent.BOILER_MENU.get();
            case STEAM_ENGINE -> FactoryContent.STEAM_ENGINE_MENU.get();
            case OFFSHORE_PUMP -> FactoryContent.OFFSHORE_PUMP_MENU.get();
            default -> throw new IllegalArgumentException("Not a fluid machine");
        };
    }
    private FluidMenu(int id, Inventory inventory, Container machine, MachineKind kind) {
        super(type(kind), id); this.machine = machine; this.kind = kind;
        if (kind == MachineKind.BOILER) addSlot(new Slot(machine, 0, PlayerStorageLayout.MACHINE_X + 8, 70) {
            @Override public boolean mayPlace(ItemStack stack) { return GeneratorEntity.isFuel(stack); }
        });
        addPlayerInventory(inventory);
        data = machine instanceof MachineEntity entity ? new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> (int) Math.round(entity.steam().fuelMJ() * 1000);
                    case 1, 2 -> (int) Math.round(FluidNetwork.localAmount(entity.getLevel(), entity.getBlockPos(), index - 1) * 10);
                    case 3 -> (int) Math.round(entity.steam().outputKW() * 10);
                    case 4 -> entity.steam().status().ordinal();
                    case 5 -> (int) Math.round(entity.steam().flow() * 10);
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 6; }
        } : new SimpleContainerData(6);
        addDataSlots(data);
    }
    public double fuelMJ() { return data.get(0) / 1000.0; }
    public double water() { return data.get(1) / 10.0; }
    public double steam() { return data.get(2) / 10.0; }
    public double outputKW() { return data.get(3) / 10.0; }
    public double flow() { return data.get(5) / 10.0; }
    public SteamMachine.Status status() { return SteamMachine.Status.values()[Math.max(0, Math.min(SteamMachine.Status.values().length - 1, data.get(4)))]; }
    @Override public boolean stillValid(Player player) { return machine.stillValid(player); }
    @Override protected boolean moveToMachine(ItemStack stack) { return kind.slots > 0 && moveItemStackTo(stack, 0, kind.slots, false); }
}
