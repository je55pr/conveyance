package dev.conveyance.client;

import dev.conveyance.Conveyance;
import dev.conveyance.world.WorldGeneration;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, value = Dist.CLIENT)
public final class WorldCreationScreen {
    private static final Set<WorldCreationUiState> ATTACHED = Collections.newSetFromMap(new WeakHashMap<>());
    private WorldCreationScreen() {}

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof CreateWorldScreen screen) {
            var state = screen.getUiState();
            if (ATTACHED.add(state)) state.addListener(WorldCreationScreen::lockPreset);
            lockPreset(state);
        }
    }

    private static void lockPreset(WorldCreationUiState state) {
        var preset = state.getSettings().worldgenLoadContext()
                .registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldGeneration.PRESET);
        if (state.getWorldType().preset() == null || !state.getWorldType().preset().is(WorldGeneration.PRESET)) {
            state.setWorldType(new WorldCreationUiState.WorldTypeEntry(preset));
        }
        if (state.isGenerateStructures()) state.setGenerateStructures(false);
        if (state.isBonusChest()) state.setBonusChest(false);
    }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Pre event) {
        if (event.getScreen() instanceof CreateWorldScreen screen) {
            // Tab widgets are attached lazily as the player switches tabs.
            screen.children().forEach(WorldCreationScreen::disableGenerationOptions);
        }
    }

    private static void disableGenerationOptions(GuiEventListener listener) {
        if (listener instanceof AbstractWidget widget) {
            String label = widget.getMessage().getString();
            for (String key : new String[] {"selectWorld.mapType", "selectWorld.mapFeatures",
                    "selectWorld.bonusItems", "selectWorld.customizeType"}) {
                if (label.startsWith(Component.translatable(key).getString())) widget.active = false;
            }
        }
        if (listener instanceof ContainerEventHandler container) {
            container.children().forEach(WorldCreationScreen::disableGenerationOptions);
        }
    }
}
