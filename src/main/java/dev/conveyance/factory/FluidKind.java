package dev.conveyance.factory;

import net.minecraft.network.chat.Component;

public enum FluidKind {
    EMPTY(0), WATER(1), STEAM(2), CRUDE_OIL(4), PETROLEUM(8), HEAVY_OIL(16), LIGHT_OIL(32), SULFURIC_ACID(64);
    public final int mask;
    FluidKind(int mask) { this.mask = mask; }
    public Component title() { return Component.translatable("fluid.conveyance." + name().toLowerCase(java.util.Locale.ROOT)); }
    public static FluidKind byId(int id) { return id >= 0 && id < values().length ? values()[id] : EMPTY; }
    public static FluidKind byMask(int mask) { for (var fluid : values()) if (fluid.mask == mask) return fluid; return EMPTY; }
}
