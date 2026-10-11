package dev.conveyance.factory;

import dev.conveyance.player.ResearchState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Stored MJ, independent of the machine's consumption rate. */
public final class BurnerFuel {
    private double reserveMJ;
    public BurnerFuel(double initialMJ) { reserveMJ = initialMJ; }
    public double reserveMJ() { return reserveMJ; }
    public boolean available(ItemStack fuel) { return reserveMJ > 0.000000001 || FuelEnergy.mj(fuel) > 0; }
    public int remainingTicks(double powerKW) { return powerKW <= 0 ? 0 : (int) Math.ceil(reserveMJ * 20000 / powerKW); }
    /** Fraction of the requested work powered this tick, including the last fraction of a fuel item. */
    public double consume(Level level, ItemStack fuel, double powerKW) {
        double cost = Math.max(0, powerKW) / 20000;
        if (cost == 0) return 1;
        if (reserveMJ + 0.000000001 < cost && FuelEnergy.mj(fuel) > 0) {
            reserveMJ += FuelEnergy.mj(fuel) * ResearchState.fuelDuration(level); fuel.shrink(1);
        }
        double used = Math.min(reserveMJ, cost); reserveMJ = Math.max(0, reserveMJ - used);
        if (reserveMJ < 0.000000001) reserveMJ = 0;
        return Math.min(1, used / cost);
    }
    public void save(CompoundTag tag) { tag.putDouble("BurnerFuelMJ", reserveMJ); }
    public void load(CompoundTag tag) {
        // Legacy burners measured every fuel against the custom 120 kW generator.
        double saved = tag.contains("BurnerFuelMJ") ? tag.getDouble("BurnerFuelMJ")
                : tag.contains("BurnTicks") ? tag.getInt("BurnTicks") * GeneratorEntity.OUTPUT_KW / 20000.0 : reserveMJ;
        reserveMJ = Double.isFinite(saved) ? Math.max(0, Math.min(10, saved)) : 0;
    }
}
