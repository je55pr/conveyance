package dev.conveyance.factory;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** MJ per item, shared by fuel tooltips and steam production. */
public final class FuelEnergy {
    public static double mj(ItemStack stack) {
        return stack.isEmpty() ? 0 : stack.is(FactoryContent.SOLID_FUEL_ITEM.get()) ? 12 : stack.is(FactoryContent.WOOD_ITEM.get()) ? 2
                : stack.is(Items.COAL) || stack.is(Items.CHARCOAL) ? 4 : 0;
    }
    private FuelEnergy() {}
}
