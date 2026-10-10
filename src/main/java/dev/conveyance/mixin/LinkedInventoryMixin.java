package dev.conveyance.mixin;

import dev.conveyance.player.LinkedHotbar;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
abstract class LinkedInventoryMixin {
    @Shadow @Final public Player player;
    @Inject(method = "getSelected", at = @At("HEAD"), cancellable = true)
    private void conveyance$selected(CallbackInfoReturnable<ItemStack> result) {
        if (LinkedHotbar.enabled(player)) result.setReturnValue(LinkedHotbar.selectedStack(player));
    }
    @Inject(method = "removeFromSelected", at = @At("HEAD"), cancellable = true)
    private void conveyance$remove(boolean all, CallbackInfoReturnable<ItemStack> result) {
        if (LinkedHotbar.enabled(player)) result.setReturnValue(LinkedHotbar.removeSelected(player, all));
    }
    @Inject(method = "swapPaint", at = @At("HEAD"), cancellable = true)
    private void conveyance$scroll(double amount, CallbackInfo callback) {
        if (LinkedHotbar.enabled(player) && !player.isSpectator()) { LinkedHotbar.scroll(player, amount); callback.cancel(); }
    }
}
