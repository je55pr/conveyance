package dev.conveyance.combat;

import dev.conveyance.player.InventoryAccess;
import dev.conveyance.player.LinkedHotbar;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class GrenadeItem extends Item {
    public GrenadeItem(Properties properties) { super(properties); }
    private static void launch(ServerPlayer player, Vec3 target) {
        LinkedHotbar.refreshHand(player);
        var item = dev.conveyance.factory.FactoryContent.GRENADE_ITEM.get();
        if (!player.getMainHandItem().is(item) || player.getCooldowns().isOnCooldown(item)) return;
        if (!player.isCreative()) {
            var extra = InventoryAccess.extra(player); var inventory = InventoryAccess.snapshot(player, extra); boolean found = false;
            for (var stack : inventory) if (stack.is(item) && !stack.isEmpty()) { stack.shrink(1); found = true; break; }
            if (!found) return;
            InventoryAccess.commit(player, extra, inventory);
        }
        var grenade = new GrenadeEntity(player.serverLevel(), player);
        Vec3 direction = target.subtract(player.getEyePosition());
        double distance = Math.min(15, direction.length());
        Vec3 velocity = direction.normalize().scale(.7).add(0, .1 + distance * .018, 0);
        grenade.shoot(velocity.x, velocity.y, velocity.z, (float) velocity.length(), 0);
        player.serverLevel().addFreshEntity(grenade); player.getCooldowns().addCooldown(item, 10);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(player.getItemInHand(hand));
        if (player instanceof ServerPlayer server) launch(server, player.getEyePosition().add(player.getLookAngle().scale(15)));
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() instanceof ServerPlayer player) launch(player, context.getClickLocation());
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
