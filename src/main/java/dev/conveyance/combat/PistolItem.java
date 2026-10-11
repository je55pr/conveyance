package dev.conveyance.combat;

import dev.conveyance.factory.FactoryContent;
import dev.conveyance.player.InventoryAccess;
import dev.conveyance.player.LinkedHotbar;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class PistolItem extends Item {
    public static final int RANGE = 15, COOLDOWN = 5;
    private static final String ROUNDS = "ConveyancePistolRounds", LAST = "ConveyancePistolLastShot";
    public PistolItem(Properties properties) { super(properties); }
    public static boolean gun(ItemStack stack) { return stack.getItem() instanceof PistolItem; }
    public static boolean shoot(ServerPlayer player, Vec3 aim) {
        if (!player.isAlive() || player.isSpectator() || player.containerMenu != player.inventoryMenu
                || !Double.isFinite(aim.x) || !Double.isFinite(aim.y) || !Double.isFinite(aim.z)) return false;
        LinkedHotbar.refreshHand(player);
        ItemStack gun = player.getMainHandItem();
        if (!gun(gun)) return false;
        boolean shotgun = gun.is(FactoryContent.SHOTGUN_ITEM.get());
        double cooldown = (shotgun ? 20 : gun.is(FactoryContent.SUBMACHINE_GUN_ITEM.get()) ? 2 : COOLDOWN) / dev.conveyance.player.ResearchState.shootingSpeed(player.level());
        String suffix = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(gun.getItem()).getPath();
        String roundKey = ROUNDS + suffix, lastKey = LAST + suffix, damageKey = "ConveyanceAmmoDamage" + suffix;
        var data = player.getPersistentData(); long now = player.level().getGameTime();
        if (data.contains(lastKey) && now + .000001 < data.getDouble(lastKey)) return false;
        Vec3 origin = player.getEyePosition(), direction = aim.subtract(origin);
        if (direction.lengthSqr() < .00001) return false;
        int rounds = Math.min(CombatShots.ROUNDS_PER_MAGAZINE, Math.max(0, data.getInt(roundKey)));
        if (!player.isCreative() && rounds == 0) {
            var extra = InventoryAccess.extra(player); var inventory = InventoryAccess.snapshot(player, extra);
            for (int priority = 0; priority < 2 && rounds == 0; priority++) for (var stack : inventory) {
                boolean valid = shotgun ? stack.is(FactoryContent.SHOTGUN_SHELL_ITEM.get()) : priority == 0 ? stack.is(FactoryContent.PIERCING_MAGAZINE_ITEM.get()) : stack.is(FactoryContent.FIREARM_MAGAZINE.get());
                if (valid && !stack.isEmpty()) { data.putInt(damageKey, shotgun ? 8 : stack.is(FactoryContent.PIERCING_MAGAZINE_ITEM.get()) ? 8 : 5); stack.shrink(1); rounds = CombatShots.ROUNDS_PER_MAGAZINE; break; }
            }
            if (rounds == 0) { player.displayClientMessage(Component.translatable("message.conveyance.no_ammo"), true); return false; }
            InventoryAccess.commit(player, extra, inventory);
        }
        if (!player.isCreative()) data.putInt(roundKey, rounds - 1);
        double previous = data.getDouble(lastKey);
        data.putDouble(lastKey, Math.max(previous, now - 1) + cooldown);
        float damage = player.isCreative() ? shotgun ? 8 : 5 : Math.max(1, data.getInt(damageKey));
        if (shotgun) for (int pellet = 0; pellet < 12; pellet++) {
            Vec3 scatter = direction.normalize().add(player.getRandom().nextGaussian() * .09, player.getRandom().nextGaussian() * .04, player.getRandom().nextGaussian() * .09).normalize();
            CombatShots.fire(player.serverLevel(), origin, origin.add(scatter.scale(RANGE)), player, false, null, damage);
        } else CombatShots.fire(player.serverLevel(), origin, origin.add(direction.normalize().scale(RANGE)), player, false, null, damage);
        player.getCooldowns().addCooldown(gun.getItem(), Math.max(1, (int) Math.ceil(cooldown))); return true;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(player.getItemInHand(hand));
        if (player instanceof ServerPlayer server) shoot(server, player.getEyePosition().add(player.getLookAngle().scale(RANGE)));
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
