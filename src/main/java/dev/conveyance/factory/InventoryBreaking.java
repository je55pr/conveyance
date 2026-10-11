package dev.conveyance.factory;

import dev.conveyance.Conveyance;
import dev.conveyance.player.InventoryAccess;
import dev.conveyance.world.ForestLogBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Preflight all loot before removing a block, tree or factory multiblock. */
public final class InventoryBreaking {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void breakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        var state = event.getState();
        WreckEntity wreck = WreckEntity.at(level, pos);
        if (wreck != null) {
            event.setCanceled(true);
            if (!wreck.salvage(player)) player.displayClientMessage(Component.translatable("message.conveyance.inventory_full"), true);
            return;
        }
        var machine = MachineBlock.controller(level, pos);
        var miner = MinerBlock.controller(level, pos);
        var radar = RadarBlock.controller(level, pos);
        boolean pole = state.getBlock() instanceof PowerPoleBlock || state.getBlock() instanceof PowerPolePartBlock;
        boolean tree = state.getBlock() instanceof ForestLogBlock;
        BlockEntity entity = level.getBlockEntity(pos);
        List<ItemStack> loot;
        if (tree) loot = List.of(new ItemStack(FactoryContent.WOOD_ITEM.get(), ForestLogBlock.logCount(level, pos, state)));
        else if (machine != null) loot = FactoryPickups.block(machine);
        else if (miner != null) loot = FactoryPickups.block(miner);
        else if (radar != null) loot = FactoryPickups.block(radar);
        else if (pole) loot = List.of(new ItemStack(FactoryContent.POWER_POLE_ITEM.get()));
        else if (entity != null && BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()).getNamespace().equals(Conveyance.MOD_ID)) loot = FactoryPickups.block(entity);
        else {
            loot = new ArrayList<>();
            FactoryPickups.add(loot, new ItemStack(state.getBlock()));
            if (entity instanceof Container container)
                for (int slot = 0; slot < container.getContainerSize(); slot++) FactoryPickups.add(loot, container.getItem(slot));
        }
        if (!player.isCreative()) loot = loot.stream().filter(item -> !(item.getItem() instanceof CreativeOnlyItem)).toList();
        var extra = InventoryAccess.extra(player);
        var inventory = InventoryAccess.snapshot(player, extra);
        event.setCanceled(true); // Our transaction replaces vanilla removal and entity drops.
        for (ItemStack item : loot) {
            if (!InventoryAccess.insert(inventory, item)) {
                player.displayClientMessage(Component.translatable("message.conveyance.inventory_full"), true);
                return;
            }
        }
        BuildingDamage.clear(level, FactoryActions.root(level, pos));
        if (tree) ForestLogBlock.removeTree(level, pos, state);
        else if (machine != null) MachineBlock.dismantle(level, pos, state);
        else if (miner != null) MinerBlock.dismantle(level, pos, state);
        else if (radar != null) RadarBlock.dismantle(level, pos, state);
        else if (pole) PowerPoleBlock.dismantle(level, pos, state);
        else if (!level.destroyBlock(pos, false, player)) return;
        InventoryAccess.commit(player, extra, inventory);
        if (!player.isCreative()) player.getMainHandItem().mineBlock(level, state, pos, player);
        player.awardStat(Stats.BLOCK_MINED.get(state.getBlock()));
    }
    private InventoryBreaking() {}
}
