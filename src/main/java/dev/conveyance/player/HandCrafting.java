package dev.conveyance.player;

import dev.conveyance.Conveyance;
import dev.conveyance.ItemPolicy;
import dev.conveyance.factory.AssemblyRecipes;
import dev.conveyance.factory.FactoryCraftingRecipe;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import dev.conveyance.factory.FactoryContent;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public final class HandCrafting {
    public static List<CraftingRecipe> recipes(Level level, long unlocks) {
        return recipes(level, unlocks, false);
    }
    public static List<CraftingRecipe> recipes(Level level, long unlocks, boolean creative) {
        return level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
                .filter(recipe -> recipe.getId().getNamespace().equals(Conveyance.MOD_ID) && (creative || ResearchState.recipeUnlocked(recipe.getId(), unlocks)))
                .filter(recipe -> !recipe.getResultItem(level.registryAccess()).isEmpty() && !ItemPolicy.isDisabled(recipe.getResultItem(level.registryAccess())))
                .filter(recipe -> creative || !(recipe instanceof FactoryCraftingRecipe factory) || !factory.creativeOnly())
                .sorted(Comparator.comparing(recipe -> recipe.getResultItem(level.registryAccess()).getHoverName().getString())).toList();
    }
    public static int count(List<ItemStack> items, Ingredient ingredient) {
        return items.stream().filter(ingredient::test).mapToInt(ItemStack::getCount).sum();
    }
    static int consume(List<ItemStack> items, Ingredient ingredient, int count) {
        for (ItemStack stack : items) {
            if (!ingredient.test(stack)) continue;
            int taken = Math.min(count, stack.getCount());
            stack.shrink(taken);
            count -= taken;
            if (count == 0) break;
        }
        return count;
    }
    private static List<ItemStack> copy(List<ItemStack> items) {
        var result = new ArrayList<ItemStack>(items.size());
        items.forEach(stack -> result.add(stack.copy()));
        return result;
    }
    public static int craftingTicks(CraftingRecipe recipe) { return recipe instanceof FactoryCraftingRecipe factory ? factory.craftingTicks() : 10; }
    public record Operation(ResourceLocation recipe, List<AssemblyRecipes.Requirement> ingredients, ItemStack output, int ticks, int batches, boolean finished) {}
    public record QueuePlan(List<ItemStack> remaining, List<ItemStack> reserved, List<Operation> operations) {}
    /** Temporary intermediates need no inventory slots; only the finished transaction does. */
    public static final class Planner {
        private final Level level;
        private final List<CraftingRecipe> recipes;
        public Planner(Level level, long unlocks) { this.level = level; recipes = recipes(level, unlocks); }

        private boolean produce(List<ItemStack> items, CraftingRecipe recipe, int batches, Set<ResourceLocation> path, List<Operation> operations) {
            if (recipe instanceof FactoryCraftingRecipe factory && (factory.machineOnly() || factory.creativeOnly())) return false;
            if (path.size() >= 32 || !path.add(recipe.getId())) return false;
            try {
                var requirements = AssemblyRecipes.requirements(recipe);
                if (requirements.isEmpty()) return false;
                for (var requirement : requirements) {
                    int missing = consume(items, requirement.ingredient(), requirement.count() * batches);
                    if (missing == 0) continue;
                    boolean supplied = false;
                    for (CraftingRecipe producer : recipes) {
                        ItemStack output = producer.getResultItem(level.registryAccess());
                        if (path.contains(producer.getId()) || !requirement.ingredient().test(output)) continue;
                        var branch = copy(items);
                        var branchOperations = operations == null ? null : new ArrayList<Operation>();
                        int needed = (missing + output.getCount() - 1) / output.getCount();
                        if (produce(branch, producer, needed, path, branchOperations) && consume(branch, requirement.ingredient(), missing) == 0) {
                            items.clear(); items.addAll(branch);
                            if (operations != null) operations.addAll(branchOperations);
                            supplied = true; break;
                        }
                    }
                    if (!supplied) return false;
                }
                ItemStack output = recipe.getResultItem(level.registryAccess());
                items.add(output.copyWithCount(output.getCount() * batches));
                if (operations != null) operations.add(new Operation(recipe.getId(), requirements, output.copy(), craftingTicks(recipe), batches, path.size() == 1));
                return true;
            } finally { path.remove(recipe.getId()); }
        }
        private List<ItemStack> materials(List<ItemStack> original, CraftingRecipe recipe, int batches) {
            var working = copy(original);
            return produce(working, recipe, batches, new HashSet<>(), null) ? working : null;
        }
        private List<ItemStack> pack(List<ItemStack> working) {
            if (working == null) return null;
            // Keep existing slot positions. Append only completed outputs and batch leftovers.
            var packed = copy(working.subList(0, InventoryAccess.SIZE));
            for (int i = InventoryAccess.SIZE; i < working.size(); i++) if (!InventoryAccess.insert(packed, working.get(i))) return null;
            return packed;
        }
        public List<ItemStack> plan(List<ItemStack> original, CraftingRecipe recipe, int batches) {
            return batches < 1 || batches > 256 ? null : pack(materials(original, recipe, batches));
        }
        public QueuePlan queuePlan(List<ItemStack> original, CraftingRecipe recipe, int batches) {
            if (batches < 1 || batches > 256) return null;
            var working = copy(original); var operations = new ArrayList<Operation>();
            if (!produce(working, recipe, batches, new HashSet<>(), operations) || pack(working) == null) return null;
            var reserved = new ArrayList<ItemStack>();
            for (int slot = 0; slot < InventoryAccess.SIZE; slot++) {
                int used = original.get(slot).getCount() - working.get(slot).getCount();
                if (used > 0) reserved.add(original.get(slot).copyWithCount(used));
            }
            return new QueuePlan(copy(working.subList(0, InventoryAccess.SIZE)), reserved, List.copyOf(operations));
        }
        public int craftingTime(List<ItemStack> original, CraftingRecipe recipe) {
            var operations = new ArrayList<Operation>();
            if (!produce(copy(original), recipe, 1, new HashSet<>(), operations)) return craftingTicks(recipe);
            return operations.stream().mapToInt(operation -> operation.ticks * operation.batches).sum();
        }
        public int craftable(List<ItemStack> original, CraftingRecipe recipe) {
            if (materials(original, recipe, 1) == null) return 0;
            int low = 1, high = 256;
            while (low < high) {
                int middle = (low + high + 1) / 2;
                if (materials(original, recipe, middle) != null) low = middle; else high = middle - 1;
            }
            // Slot capacity can vary with batch leftovers, so check it after finding the material limit.
            for (int batches = low; batches > 0; batches--) if (plan(original, recipe, batches) != null) return batches;
            return 0;
        }
    }
    public static boolean craft(ServerPlayer player, Container extra, CraftingRecipe recipe, int requested) {
        List<ItemStack> original = InventoryAccess.snapshot(player, extra);
        ItemStack output = recipe.getResultItem(player.level().registryAccess());
        if (player.isCreative()) return InventoryAccess.give(player, output.copyWithCount(requested == 0 ? output.getMaxStackSize() : output.getCount() * requested));
        var planner = new Planner(player.level(), ResearchState.mask(player.level()));
        int batches = requested == 0 ? planner.craftable(original, recipe) : requested == 1 ? 1 : Math.min(requested, planner.craftable(original, recipe));
        if (batches == 0) return false;
        var candidate = planner.queuePlan(original, recipe, batches);
        if (candidate == null) return false;
        return HandCraftingQueue.enqueue(player, extra, recipe, batches, candidate);
    }
    public static List<AssemblyRecipes.Requirement> researchCost(int research) {
        Technology tech = Technology.byId(research);
        if (tech == null) return List.of();
        var red = new AssemblyRecipes.Requirement(Ingredient.of(FactoryContent.RED_SCIENCE_ITEM.get()), tech.packs);
        var costs = new ArrayList<AssemblyRecipes.Requirement>();
        for (int slot = 0; slot < 3; slot++) if (tech.needsPack(slot)) costs.add(new AssemblyRecipes.Requirement(Ingredient.of(Technology.packItem(slot)), tech.packs));
        return List.copyOf(costs);
    }
    public static boolean research(ServerPlayer player, Container extra, int research) {
        return ResearchState.select(player.level(), research);
    }
    private HandCrafting() {}
}
