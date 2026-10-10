package dev.conveyance.factory;

import net.minecraft.world.item.ItemStack;

public interface MovingInserter {
    ItemStack heldItem();
    InserterMotion motion();
}
