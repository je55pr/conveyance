package dev.conveyance.mixin;

import dev.conveyance.client.OverheadBuildScreen;
import dev.conveyance.client.OverheadView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Minecraft.class)
public abstract class OverheadScreenMixin {
    // Forge's Opening event only fires for a non-null destination screen.
    // Replace the null destination before vanilla closes the menu and grabs the mouse.
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen conveyance$resumeBuilding(Screen screen) {
        return screen == null && OverheadView.active() ? new OverheadBuildScreen() : screen;
    }
}
