package dev.conveyance.player;

import dev.conveyance.Conveyance;
import dev.conveyance.factory.AssemblyRecipes;
import dev.conveyance.factory.BulkSmeltingRecipe;
import dev.conveyance.factory.FactoryContent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/** Full material cost from scratch, including smelting and reusable batch leftovers. */
public final class RecipeCosts {
    private final Level level;
    private final List<Recipe<?>> producers = new ArrayList<>();
    public RecipeCosts(Level level) {
        this.level = level;
        level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
                .filter(recipe -> recipe.getId().getNamespace().equals(Conveyance.MOD_ID)).forEach(producers::add);
        producers.addAll(level.getRecipeManager().getAllRecipesFor(FactoryContent.BULK_SMELTING_TYPE.get()));
        // Use the resources our miners harvest, rather than alternate vanilla recycling recipes.
        level.getRecipeManager().getAllRecipesFor(RecipeType.SMELTING).stream().filter(recipe -> {
            ItemStack output = recipe.getResultItem(level.registryAccess());
            return output.is(Items.IRON_INGOT) && recipe.getIngredients().get(0).test(new ItemStack(Items.RAW_IRON))
                    || output.is(Items.COPPER_INGOT) && recipe.getIngredients().get(0).test(new ItemStack(Items.RAW_COPPER));
        }).forEach(producers::add);
        producers.sort(java.util.Comparator.comparing(Recipe::getId));
    }
    public List<ItemStack> raw(CraftingRecipe recipe) {
        var totals = new LinkedHashMap<Item, Integer>();
        expand(recipe, 1, new ArrayList<>(), totals, new HashSet<>());
        return totals.entrySet().stream().map(entry -> new ItemStack(entry.getKey(), entry.getValue())).toList();
    }
    private void expand(Recipe<?> recipe, int batches, List<ItemStack> leftovers, LinkedHashMap<Item, Integer> totals, Set<ResourceLocation> path) {
        path.add(recipe.getId());
        try {
            List<AssemblyRecipes.Requirement> requirements = recipe instanceof CraftingRecipe crafting ? AssemblyRecipes.requirements(crafting)
                    : List.of(new AssemblyRecipes.Requirement(recipe.getIngredients().get(0), recipe instanceof BulkSmeltingRecipe bulk ? bulk.inputCount() : 1));
            for (var requirement : requirements) need(requirement.ingredient(), requirement.count() * batches, leftovers, totals, path);
            ItemStack output = recipe.getResultItem(level.registryAccess());
            leftovers.add(output.copyWithCount(output.getCount() * batches));
        } finally { path.remove(recipe.getId()); }
    }
    private void need(Ingredient ingredient, int amount, List<ItemStack> leftovers, LinkedHashMap<Item, Integer> totals, Set<ResourceLocation> path) {
        int missing = consume(leftovers, ingredient, amount);
        if (missing == 0) return;
        Recipe<?> producer = path.size() >= 32 ? null : producers.stream()
                .filter(recipe -> !path.contains(recipe.getId()) && ingredient.test(recipe.getResultItem(level.registryAccess())))
                .findFirst().orElse(null);
        if (producer == null) {
            ItemStack[] choices = ingredient.getItems();
            if (choices.length > 0) totals.merge(choices[0].getItem(), missing, Integer::sum);
            return;
        }
        int perBatch = producer.getResultItem(level.registryAccess()).getCount();
        expand(producer, (missing + perBatch - 1) / perBatch, leftovers, totals, path);
        consume(leftovers, ingredient, missing);
    }
    private static int consume(List<ItemStack> items, Ingredient ingredient, int amount) {
        for (ItemStack item : items) {
            if (!ingredient.test(item)) continue;
            int used = Math.min(item.getCount(), amount);
            item.shrink(used); amount -= used;
            if (amount == 0) break;
        }
        return amount;
    }
}
