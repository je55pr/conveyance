package dev.conveyance.factory;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;

/** Industrial recipes may consume several material items per finished product. */
public final class BulkSmeltingRecipe extends SmeltingRecipe {
    private final int inputCount;
    public BulkSmeltingRecipe(ResourceLocation id, String group, Ingredient input, int inputCount, ItemStack result, int cookingTime) {
        super(id, group, CookingBookCategory.MISC, input, result, 0, cookingTime);
        if (inputCount < 1 || inputCount > 100) throw new IllegalArgumentException("Invalid smelting input quantity");
        this.inputCount = inputCount;
    }
    public int inputCount() { return inputCount; }
    @Override public RecipeType<?> getType() { return FactoryContent.BULK_SMELTING_TYPE.get(); }
    @Override public RecipeSerializer<?> getSerializer() { return FactoryContent.BULK_SMELTING_SERIALIZER.get(); }
    public static final class Serializer implements RecipeSerializer<BulkSmeltingRecipe> {
        @Override public BulkSmeltingRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new BulkSmeltingRecipe(id, GsonHelper.getAsString(json, "group", ""), Ingredient.fromJson(json.get("ingredient")),
                    GsonHelper.getAsInt(json, "input_count"), ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result")),
                    GsonHelper.getAsInt(json, "cookingtime", 320));
        }
        @Override public BulkSmeltingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new BulkSmeltingRecipe(id, buffer.readUtf(), Ingredient.fromNetwork(buffer), buffer.readVarInt(), buffer.readItem(), buffer.readVarInt());
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, BulkSmeltingRecipe recipe) {
            buffer.writeUtf(recipe.getGroup()); recipe.getIngredients().get(0).toNetwork(buffer);
            buffer.writeVarInt(recipe.inputCount); buffer.writeItem(recipe.getResultItem(RegistryAccess.EMPTY)); buffer.writeVarInt(recipe.getCookingTime());
        }
    }
}
