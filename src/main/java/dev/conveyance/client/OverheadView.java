package dev.conveyance.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.conveyance.Conveyance;
import dev.conveyance.factory.RemoteBuilding;
import dev.conveyance.player.RemoteBuildNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, value = Dist.CLIENT)
public final class OverheadView {
    public static final float YAW = 180, PITCH = 68;
    public static final double FOV = 50;
    static final double ZOOM_STEP = 0.88;
    static final double MIN_DISTANCE = 4, MAX_DISTANCE = 110, DEFAULT_DISTANCE = 48 * Math.pow(ZOOM_STEP, 8);
    static final double PAN_SCALE = 0.04, MIN_PAN_PER_TICK = 0.6;
    private static final long TRANSITION_NANOS = 300_000_000L;
    public record CameraPose(Vec3 position, float yaw, float pitch) {}
    private record Transition(CameraPose from, double fov, double overhead, long start, Level level) {}
    private static Transition transition;
    private static double renderedFov = 70;
    private static boolean enabled;
    static double centerX, centerZ, distance = DEFAULT_DISTANCE;
    private static double previousCenterX, previousCenterZ;
    static int height = 1;
    static net.minecraft.core.Direction facing = net.minecraft.core.Direction.NORTH;
    @Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Keys {
        public static final KeyMapping TOGGLE = new KeyMapping("key.conveyance.overhead", KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.conveyance");
        @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) { event.register(TOGGLE); }
    }
    public static boolean active() {
        var game = Minecraft.getInstance();
        return enabled && game.level != null && game.player != null && game.player.isAlive() && !game.player.isSpectator();
    }
    private static boolean validWorld() {
        var game = Minecraft.getInstance();
        return game.level != null && game.player != null && game.player.isAlive() && !game.player.isSpectator();
    }
    public static boolean transitioning() {
        return validWorld() && transition != null && transition.level == Minecraft.getInstance().level
                && System.nanoTime() - transition.start < TRANSITION_NANOS;
    }
    public static boolean controlsCamera() { return active() || transitioning(); }
    private static double easedProgress() {
        double t = Mth.clamp((System.nanoTime() - transition.start) / (double) TRANSITION_NANOS, 0, 1);
        return t * t * (3 - 2 * t);
    }
    public static double overheadFraction() {
        if (!validWorld()) return 0;
        return transitioning() ? Mth.lerp(easedProgress(), transition.overhead, active() ? 1 : 0) : active() ? 1 : 0;
    }
    private static void beginTransition() {
        var game = Minecraft.getInstance(); var camera = game.gameRenderer.getMainCamera();
        CameraPose from = camera.isInitialized() ? new CameraPose(camera.getPosition(), camera.getYRot(), camera.getXRot())
                : new CameraPose(game.player.getEyePosition(), game.player.getYRot(), game.player.getXRot());
        transition = new Transition(from, renderedFov, overheadFraction(), System.nanoTime(), game.level);
    }
    public static CameraPose cameraPose(CameraPose normal, float partialTick) {
        CameraPose target = active() ? new CameraPose(cameraPosition(partialTick), YAW, PITCH) : normal;
        if (!transitioning()) return target;
        double t = easedProgress();
        return new CameraPose(transition.from.position.lerp(target.position, t),
                Mth.rotLerp((float) t, transition.from.yaw, target.yaw),
                (float) Mth.lerp(t, transition.from.pitch, target.pitch));
    }
    static void startPanTick() {
        previousCenterX = centerX; previousCenterZ = centerZ;
    }
    public static Vec3 cameraPosition(float partialTick) {
        double fraction = net.minecraft.util.Mth.clamp(partialTick, 0, 1);
        double x = net.minecraft.util.Mth.lerp(fraction, previousCenterX, centerX);
        double z = net.minecraft.util.Mth.lerp(fraction, previousCenterZ, centerZ);
        double yaw = Math.toRadians(YAW), pitch = Math.toRadians(PITCH);
        return new Vec3(x + Math.sin(yaw) * Math.cos(pitch) * distance,
                height + Math.sin(pitch) * distance, z - Math.cos(yaw) * Math.cos(pitch) * distance);
    }
    static void enter() {
        var game = Minecraft.getInstance();
        if (game.player == null || game.level == null || game.player.isSpectator()) return;
        beginTransition();
        enabled = true; height = 1; distance = DEFAULT_DISTANCE; facing = net.minecraft.core.Direction.NORTH;
        centerX = game.player.getX(); centerZ = game.player.getZ();
        startPanTick();
        RemoteBuilding.toggle(game.player, true); RemoteBuildNetwork.toggle(true);
        game.setScreen(new OverheadBuildScreen());
    }
    static void leave() {
        var game = Minecraft.getInstance();
        if (controlsCamera()) beginTransition();
        enabled = false;
        if (game.player != null) { RemoteBuilding.toggle(game.player, false); RemoteBuildNetwork.toggle(false); }
        game.setScreen(null);
    }
    static void clampCenter() {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        int x = player.chunkPosition().x * 16, z = player.chunkPosition().z * 16;
        centerX = Math.max(x - 32, Math.min(x + 48, centerX));
        centerZ = Math.max(z - 32, Math.min(z + 48, centerZ));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var game = Minecraft.getInstance();
        if (!validWorld()) { enabled = false; transition = null; if (game.player != null) RemoteBuilding.toggle(game.player, false); }
        if (transition != null && (!transitioning() || transition.level != game.level)) transition = null;
        while (Keys.TOGGLE.consumeClick()) if (game.screen == null) { if (active()) leave(); else enter(); }
    }
    @SubscribeEvent public static void opening(ScreenEvent.Opening event) {
        if (active() && event.getNewScreen() == null) event.setNewScreen(new OverheadBuildScreen());
    }
    @SubscribeEvent public static void key(ScreenEvent.KeyPressed.Pre event) {
        if (!active() || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                || screen.getFocused() instanceof EditBox edit && edit.isFocused()) return;
        if (Keys.TOGGLE.matches(event.getKeyCode(), event.getScanCode())) {
            event.setCanceled(true);
            Minecraft.getInstance().player.closeContainer(); leave();
        }
    }
    @SubscribeEvent public static void fov(ViewportEvent.ComputeFov event) {
        if (!event.usedConfiguredFov()) return;
        double target = active() ? FOV : event.getFOV();
        renderedFov = transitioning() ? Mth.lerp(easedProgress(), transition.fov, target) : target;
        event.setFOV(renderedFov);
    }
    @SubscribeEvent public static void overlay(RenderGuiOverlayEvent.Pre event) {
        if (controlsCamera() && event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())
                || active() && event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) event.setCanceled(true);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        enabled = false; transition = null; renderedFov = 70;
        if (event.getPlayer() != null) RemoteBuilding.toggle(event.getPlayer(), false);
    }
    private OverheadView() {}
}
