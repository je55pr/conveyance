package dev.conveyance.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.conveyance.Conveyance;
import dev.conveyance.factory.FactoryActions;
import dev.conveyance.player.InventoryNetwork;
import dev.conveyance.player.LinkedHotbar;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, value = Dist.CLIENT)
public final class FactoryControls {
    private static final Set<BlockPos> PASTED = new HashSet<>(), TRANSFERRED = new HashSet<>();
    private static int transferMode = -1;
    @Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Keys {
        public static final KeyMapping COPY = new KeyMapping("key.conveyance.copy", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, "key.categories.conveyance");
        public static final KeyMapping PASTE = new KeyMapping("key.conveyance.paste", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.conveyance");
        @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) { event.register(COPY); event.register(PASTE); }
    }
    private static BlockPos target(Minecraft game) {
        return game.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : null;
    }
    private static int rightAction(Minecraft game, BlockPos pos) {
        if (Screen.hasControlDown()) return FactoryActions.COLLECT;
        ItemStack held = game.player.getMainHandItem();
        if (held.isEmpty() && transferMode == FactoryActions.FUEL && LinkedHotbar.enabled(game.player)) held = LinkedHotbar.handReference(game.player);
        return FactoryActions.fuelTarget(game.level, pos, held) ? FactoryActions.FUEL : -1;
    }
    private static void transfer(Minecraft game, BlockPos pos, int action) {
        if (action != transferMode) { TRANSFERRED.clear(); transferMode = action; }
        if (action == FactoryActions.COLLECT && !FactoryActions.collectionTarget(game.level, pos)) return;
        BlockPos root = FactoryActions.root(game.level, pos);
        if (TRANSFERRED.add(root.immutable())) InventoryNetwork.requestAction(pos, action);
    }
    @SubscribeEvent public static void interact(InputEvent.InteractionKeyMappingTriggered event) {
        var game = Minecraft.getInstance();
        if (game.player == null || game.level == null || game.screen != null) return;
        BlockPos pos = target(game);
        if (event.isUseItem() && Screen.hasControlDown()) {
            event.setCanceled(true); event.setSwingHand(false);
            if (pos != null && event.getHand() == InteractionHand.MAIN_HAND) transfer(game, pos, FactoryActions.COLLECT);
            return;
        }
        if (pos == null) return;
        if (event.isPickBlock() && LinkedHotbar.enabled(game.player)) {
            ItemStack clone = game.level.getBlockState(pos).getCloneItemStack(targetHit(game), game.level, pos, game.player);
            if (!clone.isEmpty()) InventoryNetwork.requestLink(game.player.containerMenu.containerId, LinkedHotbar.bindingSlot(game.player), BuiltInRegistries.ITEM.getKey(clone.getItem()));
            event.setCanceled(true); event.setSwingHand(false);
        } else if (event.isUseItem()) {
            int action = rightAction(game, pos);
            if (action < 0) return;
            event.setCanceled(true); event.setSwingHand(false);
            if (event.getHand() == InteractionHand.MAIN_HAND) transfer(game, pos, action);
        }
    }
    private static BlockHitResult targetHit(Minecraft game) { return (BlockHitResult) game.hitResult; }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var game = Minecraft.getInstance();
        if (game.player == null || game.level == null || game.screen != null) {
            PASTED.clear(); TRANSFERRED.clear(); transferMode = -1;
            while (Keys.COPY.consumeClick()) {}
            return;
        }
        BlockPos pos = target(game);
        while (Keys.COPY.consumeClick()) if (pos != null) InventoryNetwork.requestAction(pos, FactoryActions.COPY);
        if (!Keys.PASTE.isDown()) PASTED.clear();
        else if (pos != null && FactoryActions.assembler(game.level, pos) && PASTED.add(FactoryActions.root(game.level, pos).immutable())) InventoryNetwork.requestAction(pos, FactoryActions.PASTE);
        if (!game.options.keyUse.isDown()) { TRANSFERRED.clear(); transferMode = -1; }
        else if (pos != null) {
            int action = rightAction(game, pos);
            if (action >= 0) transfer(game, pos, action);
        }
    }
    private FactoryControls() {}
}
