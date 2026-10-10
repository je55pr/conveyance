package dev.conveyance.factory;

import dev.conveyance.Conveyance;
import dev.conveyance.ItemPolicy;
import dev.conveyance.player.ResearchState;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public final class AssemblyRecipes {
    public record Requirement(Ingredient ingredient, int count) {}
    public static List<Requirement> requirements(CraftingRecipe recipe) {
        if (recipe instanceof FactoryCraftingRecipe factory) return factory.requirements();
        var grouped = new LinkedHashMap<String, Requirement>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty()) continue;
            String key = ingredient.toJson().toString();
            grouped.compute(key, (ignored, previous) -> new Requirement(ingredient, previous == null ? 1 : previous.count + 1));
        }
        return List.copyOf(grouped.values());
    }
    public static List<CraftingRecipe> available(Level level) {
        return all(level).stream().filter(recipe -> ResearchState.recipeUnlocked(recipe.getId(), ResearchState.mask(level))).toList();
    }
    public static List<CraftingRecipe> all(Level level) {
        return level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
                .filter(recipe -> recipe.getId().getNamespace().equals(Conveyance.MOD_ID))
                .filter(recipe -> !recipe.getResultItem(level.registryAccess()).isEmpty() && !ItemPolicy.isDisabled(recipe.getResultItem(level.registryAccess())))
                .filter(recipe -> !requirements(recipe).isEmpty() && requirements(recipe).size() <= 4)
                .sorted(Comparator.comparing(recipe -> recipe.getId().toString())).toList();
    }
    private AssemblyRecipes() {}
}
