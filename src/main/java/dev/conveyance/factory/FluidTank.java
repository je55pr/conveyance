package dev.conveyance.factory;

import net.minecraft.nbt.CompoundTag;

/** Local share of a connected fluid network; saved with its owning block. */
public final class FluidTank {
    public final double capacity;
    public final FluidKind required;
    private FluidKind fluid = FluidKind.EMPTY;
    private double amount;
    public FluidTank(double capacity, FluidKind required) { this.capacity = capacity; this.required = required; }
    public FluidKind fluid() { return required != FluidKind.EMPTY ? required : fluid; }
    public double amount() { return amount; }
    public boolean store(FluidKind kind, double quantity) {
        double next = Math.max(0, Math.min(capacity, quantity));
        FluidKind type = next > 0.00000001 ? kind : FluidKind.EMPTY;
        boolean changed = Math.abs(next - amount) > 0.00000001 || type != fluid;
        amount = next; fluid = type;
        return changed;
    }
    public CompoundTag save() {
        var tag = new CompoundTag(); tag.putInt("Fluid", fluid.ordinal()); tag.putDouble("Amount", amount); return tag;
    }
    public void load(CompoundTag tag) {
        FluidKind type = FluidKind.byId(tag.getInt("Fluid"));
        double saved = tag.getDouble("Amount");
        store(type, !Double.isFinite(saved) || type == FluidKind.EMPTY || required != FluidKind.EMPTY && required != type ? 0 : saved);
    }
}
