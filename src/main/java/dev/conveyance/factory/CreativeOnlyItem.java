package dev.conveyance.factory;

import dev.conveyance.Conveyance;
import dev.conveyance.player.InventoryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID)
public final class CreativeOnlyItem extends BlockItem {
    public CreativeOnlyItem(Block block, Properties properties) { super(block, properties); }
    @Override public InteractionResult place(BlockPlaceContext context) {
        if (context.getPlayer() == null || !context.getPlayer().isCreative()) {
            if (context.getPlayer() != null) context.getPlayer().displayClientMessage(Component.translatable("message.conveyance.creative_only"), true);
            return InteractionResult.FAIL;
        }
        return super.place(context);
    }
    @SubscribeEvent public static void inventory(TickEvent.PlayerTickEvent event) {
        var player = event.player;
        if (event.phase != TickEvent.Phase.END || player.level().isClientSide || player.isCreative()) return;
        boolean changed = false;
        for (var container : new net.minecraft.world.Container[]{player.getInventory(), InventoryAccess.extra(player)})
            for (int slot = 0; slot < container.getContainerSize(); slot++)
                if (container.getItem(slot).getItem() instanceof CreativeOnlyItem) { container.setItem(slot, net.minecraft.world.item.ItemStack.EMPTY); changed = true; }
        if (player.containerMenu.getCarried().getItem() instanceof CreativeOnlyItem) { player.containerMenu.setCarried(net.minecraft.world.item.ItemStack.EMPTY); changed = true; }
        if (changed) player.containerMenu.broadcastChanges();
    }
}
