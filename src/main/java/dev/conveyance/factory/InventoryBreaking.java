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
    private static ItemStack packed(BlockEntity entity) {
        ItemStack item = new ItemStack(entity.getBlockState().getBlock());
        if (entity instanceof ConveyorEntity belt) {
            boolean occupied = false;
            for (int slot = 0; slot < ConveyorEntity.CAPACITY; slot++) occupied |= !belt.item(slot).isEmpty();
            if (!occupied) return item;
        }
        if (!(entity instanceof CrateEntity crate) || !crate.isEmpty())
            BlockItem.setBlockEntityData(item, entity.getType(), entity.saveWithoutMetadata());
        return item;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void breakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        var state = event.getState();
        var machine = MachineBlock.controller(level, pos);
        var miner = MinerBlock.controller(level, pos);
        boolean pole = state.getBlock() instanceof PowerPoleBlock || state.getBlock() instanceof PowerPolePartBlock;
        boolean tree = state.getBlock() instanceof ForestLogBlock;
        BlockEntity entity = level.getBlockEntity(pos);
        List<ItemStack> loot;
        if (tree) loot = List.of(new ItemStack(FactoryContent.WOOD_ITEM.get(), ForestLogBlock.logCount(level, pos, state)));
        else if (machine != null) loot = List.of(packed(machine));
        else if (miner != null) loot = List.of(packed(miner));
        else if (pole) loot = List.of(new ItemStack(FactoryContent.POWER_POLE_ITEM.get()));
        else if (entity != null && BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()).getNamespace().equals(Conveyance.MOD_ID)) loot = List.of(packed(entity));
        else {
            loot = new ArrayList<>();
            if (player.hasCorrectToolForDrops(state) || player.isCreative())
                loot.addAll(Block.getDrops(state, level, pos, entity, player, player.getMainHandItem()));
            if (entity instanceof Container container)
                for (int slot = 0; slot < container.getContainerSize(); slot++) loot.add(container.getItem(slot).copy());
        }
        var extra = InventoryAccess.extra(player);
        var inventory = InventoryAccess.snapshot(player, extra);
        event.setCanceled(true); // Our transaction replaces vanilla removal and entity drops.
        for (ItemStack item : loot) {
            if (!InventoryAccess.insert(inventory, item)) {
                player.displayClientMessage(Component.translatable("message.conveyance.inventory_full"), true);
                return;
            }
        }
        if (tree) ForestLogBlock.removeTree(level, pos, state);
        else if (machine != null) MachineBlock.dismantle(level, pos, state);
        else if (miner != null) MinerBlock.dismantle(level, pos, state);
        else if (pole) PowerPoleBlock.dismantle(level, pos, state);
        else if (!level.destroyBlock(pos, false, player)) return;
        InventoryAccess.commit(player, extra, inventory);
        if (!player.isCreative()) player.getMainHandItem().mineBlock(level, state, pos, player);
        player.awardStat(Stats.BLOCK_MINED.get(state.getBlock()));
    }
    private InventoryBreaking() {}
}
