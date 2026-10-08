package dev.conveyance;

import com.mojang.logging.LogUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Final filter after tags bind, including Forge's own vanilla recipe overrides. */
public final class RecipePolicy {
    private static final Set<String> DISABLED_SPECIAL_SERIALIZERS = Set.of(
            "crafting_special_tippedarrow", "crafting_special_suspiciousstew",
            "crafting_special_shulkerboxcoloring");

    private RecipePolicy() {}

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        filter(event.getServer());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            // Called after /reload, before recipes are sent to connected clients.
            filter(event.getPlayerList().getServer());
        }
    }

    private static void filter(MinecraftServer server) {
        var manager = server.getRecipeManager();
        List<Recipe<?>> retained = manager.getRecipes().stream()
                .filter(recipe -> !isDisabled(recipe, server.registryAccess())).toList();
        int removed = manager.getRecipes().size() - retained.size();
        manager.replaceRecipes(retained);
        LogUtils.getLogger().info("Conveyance filtered {} additional recipes; {} remain.", removed, retained.size());
    }

    private static boolean isDisabled(Recipe<?> recipe, RegistryAccess registries) {
        if (ItemPolicy.isDisabled(recipe.getResultItem(registries))) {
            return true;
        }
        var serializer = BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer());
        if (serializer.getNamespace().equals("minecraft")
                && DISABLED_SPECIAL_SERIALIZERS.contains(serializer.getPath())) {
            return true;
        }
        return recipe.getIngredients().stream().anyMatch(ingredient -> {
            var choices = ingredient.getItems();
            return choices.length > 0 && Arrays.stream(choices).allMatch(ItemPolicy::isDisabled);
        });
    }
}
