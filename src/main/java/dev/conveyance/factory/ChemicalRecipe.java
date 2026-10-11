package dev.conveyance.factory;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/** Item/fluid chemistry with independently typed ports and recipe-specific durations. */
public record ChemicalRecipe(ResourceLocation id, MachineKind machine, int ticks,
        List<AssemblyRecipes.Requirement> items, List<FluidCost> inputs, List<FluidCost> outputs, ItemStack result) implements Recipe<SimpleContainer> {
    public record FluidCost(int channel, FluidKind fluid, double amount) {}
    public static List<ChemicalRecipe> all(Level level, MachineKind kind) {
        return level.getRecipeManager().getAllRecipesFor(FactoryContent.CHEMICAL_TYPE.get()).stream()
                .filter(recipe -> recipe.machine == kind).sorted(java.util.Comparator.comparing(recipe -> recipe.id.toString())).toList();
    }
    public ItemStack icon() { return !result.isEmpty() ? result : new ItemStack(machine.item()); }
    @Override public ResourceLocation getId() { return id; }
    @Override public boolean matches(SimpleContainer inventory, Level level) { return false; }
    @Override public boolean canCraftInDimensions(int x, int y) { return false; }
    @Override public ItemStack assemble(SimpleContainer inventory, RegistryAccess access) { return result.copy(); }
    @Override public ItemStack getResultItem(RegistryAccess access) { return result; }
    @Override public RecipeType<?> getType() { return FactoryContent.CHEMICAL_TYPE.get(); }
    @Override public RecipeSerializer<?> getSerializer() { return FactoryContent.CHEMICAL_SERIALIZER.get(); }
    @Override public boolean isSpecial() { return true; }
    @Override public NonNullList<Ingredient> getIngredients() {
        var list = NonNullList.<Ingredient>create(); items.forEach(item -> list.add(item.ingredient())); return list;
    }
    public static final class Serializer implements RecipeSerializer<ChemicalRecipe> {
        private static List<FluidCost> fluids(JsonObject json, String key) {
            var costs = new ArrayList<FluidCost>();
            if (json.has(key)) for (var entry : json.getAsJsonArray(key)) {
                var fluid = entry.getAsJsonObject();
                costs.add(new FluidCost(GsonHelper.getAsInt(fluid, "channel"), FluidKind.valueOf(GsonHelper.getAsString(fluid, "fluid").toUpperCase(java.util.Locale.ROOT)), GsonHelper.getAsDouble(fluid, "amount")));
            }
            return List.copyOf(costs);
        }
        @Override public ChemicalRecipe fromJson(ResourceLocation id, JsonObject json) {
            var kind = java.util.Arrays.stream(MachineKind.values()).filter(value -> value.id.equals(GsonHelper.getAsString(json, "machine"))).findFirst().orElseThrow();
            var items = new ArrayList<AssemblyRecipes.Requirement>();
            if (json.has("items")) for (var entry : json.getAsJsonArray("items")) {
                var ingredient = entry.getAsJsonObject(); items.add(new AssemblyRecipes.Requirement(Ingredient.fromJson(ingredient), GsonHelper.getAsInt(ingredient, "count", 1)));
            }
            if (!kind.chemical() || items.size() > 2) throw new IllegalArgumentException("Invalid chemical machine or inputs: " + id);
            return new ChemicalRecipe(id, kind, Math.max(1, Math.round(GsonHelper.getAsFloat(json, "seconds") * 20)), List.copyOf(items), fluids(json, "inputs"), fluids(json, "outputs"),
                    json.has("result") ? ShapedRecipe.itemStackFromJson(json.getAsJsonObject("result")) : ItemStack.EMPTY);
        }
        private static void writeFluids(FriendlyByteBuf buffer, List<FluidCost> fluids) {
            buffer.writeVarInt(fluids.size()); for (var fluid : fluids) { buffer.writeVarInt(fluid.channel); buffer.writeVarInt(fluid.fluid.ordinal()); buffer.writeDouble(fluid.amount); }
        }
        private static List<FluidCost> readFluids(FriendlyByteBuf buffer) {
            int size = buffer.readVarInt(); if (size < 0 || size > 5) throw new IllegalArgumentException("Invalid fluid count");
            var result = new ArrayList<FluidCost>(); for (int i = 0; i < size; i++) result.add(new FluidCost(buffer.readVarInt(), FluidKind.byId(buffer.readVarInt()), buffer.readDouble())); return List.copyOf(result);
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, ChemicalRecipe recipe) {
            buffer.writeVarInt(recipe.machine.ordinal()); buffer.writeVarInt(recipe.ticks); buffer.writeVarInt(recipe.items.size());
            for (var item : recipe.items) { item.ingredient().toNetwork(buffer); buffer.writeVarInt(item.count()); }
            writeFluids(buffer, recipe.inputs); writeFluids(buffer, recipe.outputs); buffer.writeItem(recipe.result);
        }
        @Override public ChemicalRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            MachineKind kind = MachineKind.values()[buffer.readVarInt()]; int ticks = buffer.readVarInt(), size = buffer.readVarInt();
            if (size < 0 || size > 2) throw new IllegalArgumentException("Invalid input count");
            var items = new ArrayList<AssemblyRecipes.Requirement>(); for (int i = 0; i < size; i++) items.add(new AssemblyRecipes.Requirement(Ingredient.fromNetwork(buffer), buffer.readVarInt()));
            return new ChemicalRecipe(id, kind, ticks, List.copyOf(items), readFluids(buffer), readFluids(buffer), buffer.readItem());
        }
    }
}
