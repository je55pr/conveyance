package dev.conveyance.factory;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/** Counted materials shared by hand crafting and assemblers, without a crafting grid. */
public final class FactoryCraftingRecipe implements CraftingRecipe {
    private final ResourceLocation id;
    private final String tab;
    private final List<AssemblyRecipes.Requirement> requirements;
    private final ItemStack result;

    public FactoryCraftingRecipe(ResourceLocation id, String tab, List<AssemblyRecipes.Requirement> requirements, ItemStack result) {
        if (requirements.isEmpty() || requirements.stream().anyMatch(r -> r.count() < 1 || r.count() > 100 || r.ingredient().isEmpty()))
            throw new IllegalArgumentException("Invalid factory ingredients: " + id);
        this.id = id; this.tab = tab; this.requirements = List.copyOf(requirements); this.result = result;
    }
    public String tab() { return tab; }
    public List<AssemblyRecipes.Requirement> requirements() { return requirements; }
    @Override public ResourceLocation getId() { return id; }
    @Override public boolean matches(CraftingContainer container, Level level) { return false; }
    @Override public boolean canCraftInDimensions(int width, int height) { return false; }
    @Override public ItemStack assemble(CraftingContainer container, RegistryAccess access) { return result.copy(); }
    @Override public ItemStack getResultItem(RegistryAccess access) { return result; }
    @Override public NonNullList<Ingredient> getIngredients() {
        var result = NonNullList.<Ingredient>create();
        requirements.forEach(r -> result.add(r.ingredient()));
        return result;
    }
    @Override public CraftingBookCategory category() { return CraftingBookCategory.MISC; }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeSerializer<?> getSerializer() { return FactoryContent.FACTORY_CRAFTING_SERIALIZER.get(); }

    public static final class Serializer implements RecipeSerializer<FactoryCraftingRecipe> {
        @Override public FactoryCraftingRecipe fromJson(ResourceLocation id, JsonObject json) {
            var requirements = new ArrayList<AssemblyRecipes.Requirement>();
            for (var entry : GsonHelper.getAsJsonArray(json, "ingredients")) {
                JsonObject ingredient = entry.getAsJsonObject();
                requirements.add(new AssemblyRecipes.Requirement(Ingredient.fromJson(ingredient), GsonHelper.getAsInt(ingredient, "count", 1)));
            }
            return new FactoryCraftingRecipe(id, GsonHelper.getAsString(json, "tab", "production"), requirements,
                    ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result")));
        }
        @Override public FactoryCraftingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            String tab = buffer.readUtf();
            int size = buffer.readVarInt();
            if (size < 1 || size > 32) throw new IllegalArgumentException("Invalid factory ingredient count");
            var requirements = new ArrayList<AssemblyRecipes.Requirement>();
            for (int i = 0; i < size; i++) requirements.add(new AssemblyRecipes.Requirement(Ingredient.fromNetwork(buffer), buffer.readVarInt()));
            return new FactoryCraftingRecipe(id, tab, requirements, buffer.readItem());
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, FactoryCraftingRecipe recipe) {
            buffer.writeUtf(recipe.tab); buffer.writeVarInt(recipe.requirements.size());
            recipe.requirements.forEach(r -> { r.ingredient().toNetwork(buffer); buffer.writeVarInt(r.count()); });
            buffer.writeItem(recipe.result);
        }
    }
}
