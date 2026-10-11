package dev.conveyance.combat;

import dev.conveyance.factory.FactoryContent;
import dev.conveyance.factory.FactoryMenu;
import dev.conveyance.factory.MachineEntity;
import dev.conveyance.factory.PowerNetwork;
import dev.conveyance.player.ResearchState;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/** A powered weapon has status and charge, without an ammunition inventory. */
public final class LaserTurretMenu extends FactoryMenu {
    private final MachineEntity machine;
    private final ContainerData data;
    public LaserTurretMenu(int id, Inventory inventory) { this(id, inventory, null); }
    public LaserTurretMenu(int id, Inventory inventory, MachineEntity machine) {
        super(FactoryContent.LASER_TURRET_MENU.get(), id); this.machine = machine;
        addPlayerInventory(inventory);
        data = machine == null ? new SimpleContainerData(6) : new ContainerData() {
            @Override public int get(int index) { return switch (index) {
                case 0 -> (int) Math.round(machine.laser().energyMJ() * 1000);
                case 1 -> machine.laser().status().ordinal();
                case 2 -> (int) Math.round(PowerNetwork.deliveredPower(machine.getLevel(), machine.getBlockPos()));
                case 3 -> (int) Math.round(PowerNetwork.satisfaction(machine.getLevel(), machine.getBlockPos()) * 1000);
                case 4 -> (int) Math.round(LaserTurretLogic.DAMAGE * ResearchState.laserDamage(machine.getLevel()) * 100);
                default -> (int) Math.round(LaserTurretLogic.SHOTS_PER_SECOND * ResearchState.laserSpeed(machine.getLevel()) * 100);
            }; }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 6; }
        };
        addDataSlots(data);
    }
    public int chargeKJ() { return data.get(0); }
    public int inputKW() { return data.get(2); }
    public int satisfactionPercent() { return data.get(3) / 10; }
    public double damage() { return data.get(4) / 100.0; }
    public double rate() { return data.get(5) / 100.0; }
    public LaserTurretLogic.Status status() { return LaserTurretLogic.Status.values()[Math.max(0, Math.min(LaserTurretLogic.Status.values().length - 1, data.get(1)))]; }
    @Override protected boolean moveToMachine(ItemStack stack) { return false; }
    @Override public boolean stillValid(Player player) { return machine == null || machine.stillValid(player); }
}
