package dev.conveyance.factory;

import dev.conveyance.player.LinkedHotbar;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class CombatRepair {
    public static boolean repair(Player player, BlockPos pos) {
        if (!(player instanceof ServerPlayer server) || !player.isAlive() || player.isSpectator() || !player.getAbilities().mayBuild
                || !player.level().hasChunkAt(pos) || !player.level().mayInteract(player, pos)
                || !RemoteBuilding.canReach(player, pos) && pos.distToCenterSqr(player.getEyePosition()) > 64) return false;
        LinkedHotbar.refreshHand(player); var held = player.getMainHandItem();
        if (!held.is(FactoryContent.REPAIR_PACK.get())) return false;
        WreckEntity wreck = WreckEntity.at(player.level(), pos);
        if (wreck != null) {
            if (!wreck.restore(server)) { player.displayClientMessage(Component.translatable("message.conveyance.repair_blocked"), true); return false; }
        } else {
            var root = FactoryActions.root(player.level(), pos); var state = player.level().getBlockState(root);
            if (!BuildingDamage.building(state) || BuildingDamage.current(server.serverLevel(), root) >= BuildingDamage.maximum(state)) return false;
            BuildingDamage.clear(server.serverLevel(), root);
        }
        if (!player.isCreative()) held.shrink(1);
        player.getInventory().setChanged(); dev.conveyance.player.InventoryAccess.extra(player).setChanged(); LinkedHotbar.refreshHand(player);
        player.displayClientMessage(Component.translatable("message.conveyance.repaired"), true); return true;
    }
    private CombatRepair() {}
}
