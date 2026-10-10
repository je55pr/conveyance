package dev.conveyance.mixin;

import dev.conveyance.player.LinkedHotbar;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
abstract class LinkedPlayerMixin {
    @Inject(method = "setItemSlot", at = @At("HEAD"), cancellable = true)
    private void conveyance$replace(EquipmentSlot slot, ItemStack stack, CallbackInfo callback) {
        Player player = (Player) (Object) this;
        if (slot == EquipmentSlot.MAINHAND && LinkedHotbar.enabled(player)) { LinkedHotbar.replaceSelected(player, stack); callback.cancel(); }
    }
}
