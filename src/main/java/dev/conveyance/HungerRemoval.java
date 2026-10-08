package dev.conveyance;

import net.minecraft.world.food.FoodData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.LogicalSide;

final class HungerRemoval {
    private HungerRemoval() {}

    static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side != LogicalSide.SERVER) {
            return;
        }

        // Restore before vanilla's food tick to prevent starvation in existing saves,
        // and after it so movement and regeneration never drain the food meter.
        FoodData food = event.player.getFoodData();
        food.setFoodLevel(20);
        food.setExhaustion(0.0F);
        // Keep ordinary slow natural healing, without saturation's rapid healing.
        food.setSaturation(0.0F);
    }
}

