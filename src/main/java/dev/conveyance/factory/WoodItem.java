package dev.conveyance.factory;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public final class WoodItem extends Item {
    public WoodItem(Properties properties) { super(properties); }
    // Vanilla furnaces burn coal for 1,600 ticks; our industrial machines use
    // their shorter prototype scale. Wood is half of coal in both systems.
    @Override public int getBurnTime(ItemStack stack, RecipeType<?> recipeType) { return 800; }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.conveyance.wood"));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
