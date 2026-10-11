package dev.conveyance.client;

import dev.conveyance.Conveyance;
import dev.conveyance.combat.CombatNetwork;
import dev.conveyance.combat.PistolItem;
import dev.conveyance.factory.FactoryContent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, value = Dist.CLIENT)
public final class CombatClient {
    private static net.minecraft.core.BlockPos attackFrom;
    private static int attackCount;
    private static long alertUntil;
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void click(InputEvent.InteractionKeyMappingTriggered event) {
        var game = Minecraft.getInstance();
        if (game.player != null && game.screen == null && dev.conveyance.combat.PistolItem.gun(game.player.getMainHandItem()) && event.isAttack()) {
            event.setCanceled(true); event.setSwingHand(false); shoot();
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        var game = Minecraft.getInstance();
        if (event.phase == TickEvent.Phase.END && game.player != null && game.screen == null && game.options.keyAttack.isDown()) shoot();
    }
    private static long sentTick = Long.MIN_VALUE;
    private static void shoot() {
        var game = Minecraft.getInstance();
        if (game.player == null || !dev.conveyance.combat.PistolItem.gun(game.player.getMainHandItem())) return;
        long tick = game.player.level().getGameTime(); if (sentTick == tick) return; sentTick = tick;
        CombatNetwork.shoot(game.player.getEyePosition().add(game.player.getLookAngle().scale(PistolItem.RANGE)));
    }
    public static void tracer(CombatNetwork.Tracer tracer) {
        var game = Minecraft.getInstance(); if (game.level == null) return;
        var delta = tracer.to().subtract(tracer.from()); int steps = Math.max(1, (int) (delta.length() * 2));
        var dust = new DustParticleOptions(new Vector3f(1, .8F, .25F), .5F);
        for (int i = 0; i <= steps; i++) {
            var point = tracer.from().add(delta.scale((double) i / steps)); game.level.addParticle(dust, point.x, point.y, point.z, 0, 0, 0);
        }
    }
    public static void alert(CombatNetwork.Alert alert) {
        var game = Minecraft.getInstance(); if (game.level == null || game.player == null) return;
        attackFrom = alert.origin(); attackCount = alert.count(); alertUntil = game.level.getGameTime() + 200;
        game.player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.conveyance.attack_incoming", attackCount, attackFrom.getX(), attackFrom.getZ()), false);
    }
    @SubscribeEvent public static void hud(net.minecraftforge.client.event.RenderGuiEvent.Post event) {
        var game = Minecraft.getInstance(); if (game.level == null || attackFrom == null || game.level.getGameTime() >= alertUntil || game.options.hideGui) return;
        var title = net.minecraft.network.chat.Component.translatable("hud.conveyance.attack", attackCount);
        var detail = net.minecraft.network.chat.Component.translatable("hud.conveyance.attack_from", attackFrom.getX(), attackFrom.getZ());
        int width = Math.max(game.font.width(title), game.font.width(detail)) + 16;
        int top = game.screen instanceof OverheadBuildScreen ? 62 : 8;
        event.getGuiGraphics().fill(8, top, 8 + width, top + 31, 0xE02E1720);
        event.getGuiGraphics().drawString(game.font, title, 16, top + 5, 0xFFFF9585, false);
        event.getGuiGraphics().drawString(game.font, detail, 16, top + 17, 0xFFF1C8BC, false);
    }
    @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) { attackFrom = null; alertUntil = 0; }
    private CombatClient() {}
}
