package dev.conveyance.factory;

import dev.conveyance.player.ResearchState;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

final class IndustrialSmelting {
    static AbstractCookingRecipe find(Level level, ItemStack input) {
        if (level == null || input.isEmpty()) return null;
        var inventory = new SimpleContainer(input);
        var industrial = level.getRecipeManager().getRecipeFor(FactoryContent.BULK_SMELTING_TYPE.get(), inventory, level).orElse(null);
        if (industrial != null && ResearchState.recipeUnlocked(industrial.getId(), ResearchState.mask(level))) return industrial;
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, inventory, level).orElse(null);
    }
    static int inputCount(AbstractCookingRecipe recipe) { return recipe instanceof BulkSmeltingRecipe bulk ? bulk.inputCount() : 1; }
    static int cookingTime(AbstractCookingRecipe recipe) { return recipe instanceof BulkSmeltingRecipe ? recipe.getCookingTime() : 64; }
    private IndustrialSmelting() {}
}
