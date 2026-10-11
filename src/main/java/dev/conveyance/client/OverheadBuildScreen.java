package dev.conveyance.client;

import dev.conveyance.factory.FactoryActions;
import dev.conveyance.factory.PlacementPreview;
import dev.conveyance.factory.RemoteBuilding;
import dev.conveyance.player.InventoryNetwork;
import dev.conveyance.player.LinkedHotbar;
import dev.conveyance.player.RemoteBuildNetwork;
import dev.conveyance.player.TerrainHarvest;
import dev.conveyance.world.ForestLeavesBlock;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

/** Transparent, non-pausing screen: the mouse belongs to construction, WASD to the camera. */
public final class OverheadBuildScreen extends Screen {
    private Direction facing = OverheadView.facing;
    private double mouseX, mouseY;
    private boolean leftHeld, rightHeld, pasteHeld;
    private final Set<Integer> keys = new HashSet<>();
    private final Set<BlockPos> placed = new HashSet<>(), transferred = new HashSet<>(), pasted = new HashSet<>();
    private BlockPos floor, breaking;
    private BlockHitResult hit;
    private Vec3 combatAim;
    private int work;
    private final java.util.List<Button> overlayButtons = new java.util.ArrayList<>();
    public OverheadBuildScreen() { super(Component.translatable("screen.conveyance.overhead")); }
    @Override protected void init() {
        KeyMapping.releaseAll();
        OverheadView.startPanTick();
        mouseX = width / 2.0; mouseY = height / 2.0;
        overlayButtons.clear();
        for (int index = 0; index < FactoryOverlayView.Layer.values().length; index++) {
            var layer = FactoryOverlayView.Layer.values()[index];
            overlayButtons.add(addRenderableWidget(Button.builder(Component.empty(), button -> layer.toggle()).bounds(width - 102 + index * 24, 7, 22, 20).build()));
        }
        addRenderableWidget(Button.builder(Component.literal("−"), b -> changeHeight(-1)).bounds(12, 28, 22, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> changeHeight(1)).bounds(122, 28, 22, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.conveyance.base"), b -> { OverheadView.height = 1; resetWork(); }).bounds(149, 28, 68, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.conveyance.exit_view"), b -> OverheadView.leave()).bounds(222, 28, 72, 20).build());
    }
    private void changeHeight(int amount) {
        OverheadView.height = Math.max(minecraft.level.getMinBuildHeight(), Math.min(minecraft.level.getMaxBuildHeight() - 1, OverheadView.height + amount));
        placed.clear(); resetWork();
    }
    private boolean overUI(double x, double y) { return y < 55 || y >= height - 52; }
    private int hotbarSlot(double x, double y) {
        int barWidth = LinkedHotbar.SIZE * 22, left = (width - barWidth) / 2;
        return y >= height - 28 && y < height - 6 && x >= left && x < left + barWidth ? (int) (x - left) / 22 : -1;
    }
    private void select(int slot) {
        HotbarControls.select(slot);
        placed.clear(); resetWork();
    }
    private static Vec3 vector(Vector3f vector) { return new Vec3(vector.x(), vector.y(), vector.z()); }
    private void targets() {
        mouseX = minecraft.mouseHandler.xpos() * width / minecraft.getWindow().getScreenWidth();
        mouseY = minecraft.mouseHandler.ypos() * height / minecraft.getWindow().getScreenHeight();
        var camera = minecraft.gameRenderer.getMainCamera();
        double aspect = (double) minecraft.getWindow().getWidth() / minecraft.getWindow().getHeight();
        double tangent = Math.tan(Math.toRadians(OverheadView.FOV / 2));
        Vec3 ray = vector(camera.getLookVector()).add(vector(camera.getLeftVector()).scale(-(mouseX * 2 / width - 1) * tangent * aspect))
                .add(vector(camera.getUpVector()).scale((1 - mouseY * 2 / height) * tangent)).normalize();
        Vec3 origin = camera.getPosition();
        double t = (OverheadView.height - origin.y) / ray.y;
        Vec3 point = origin.add(ray.scale(t));
        combatAim = point.add(0, .35, 0);
        floor = ray.y < -0.0001 && t > 0 ? new BlockPos(net.minecraft.util.Mth.floor(point.x), OverheadView.height, net.minecraft.util.Mth.floor(point.z)) : null;
        if (floor == null) { hit = null; return; }
        // Trace only through the world's height, skipping the thousands of air blocks above it.
        double start = Math.max(0, (minecraft.level.getMaxBuildHeight() + 1.0 - origin.y) / ray.y);
        double end = (minecraft.level.getMinBuildHeight() - 1.0 - origin.y) / ray.y;
        Vec3 rayStart = origin.add(ray.scale(start)), rayEnd = origin.add(ray.scale(end));
        double nearest = Double.POSITIVE_INFINITY;
        if (dev.conveyance.combat.PistolItem.gun(minecraft.player.getMainHandItem())) for (var enemy : minecraft.level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, new net.minecraft.world.phys.AABB(rayStart, rayEnd).inflate(1), entity -> entity.isAlive() && dev.conveyance.combat.CombatContent.enemy(entity))) {
            var intersection = enemy.getBoundingBox().inflate(.15).clip(rayStart, rayEnd);
            if (intersection.isPresent() && origin.distanceToSqr(intersection.get()) < nearest) { nearest = origin.distanceToSqr(intersection.get()); combatAim = enemy.getBoundingBox().getCenter(); }
        }
        var found = minecraft.level.clip(new ClipContext(rayStart, rayEnd, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, minecraft.player) {
            @Override public VoxelShape getBlockShape(BlockState state, BlockGetter level, BlockPos pos) {
                return state.getBlock() instanceof ForestLeavesBlock ? Shapes.empty() : super.getBlockShape(state, level, pos);
            }
        });
        hit = found.getType() == HitResult.Type.BLOCK && RemoteBuilding.inArea(minecraft.player, found.getBlockPos()) ? found : null;
    }
    PlacementPreview.Plan plan() {
        targets();
        if (OverheadView.transitioning() || floor == null || overUI(mouseX, mouseY) || hasShiftDown()) return null;
        return RemoteBuilding.preview(minecraft.player, floor, facing);
    }
    BlockPos target() {
        if (OverheadView.transitioning() || overUI(mouseX, mouseY)) return null;
        BlockPos badge = FactoryOverlayView.target((int) mouseX, (int) mouseY);
        return badge != null && RemoteBuilding.canReach(minecraft.player, badge) ? badge : hit == null ? null : hit.getBlockPos();
    }
    private void action(int action, BlockPos pos) {
        RemoteBuildNetwork.action(action, pos, facing, LinkedHotbar.selectedSlot(minecraft.player), hasShiftDown());
    }
    private void resetWork() {
        if (breaking != null && minecraft != null && minecraft.level != null) {
            minecraft.level.destroyBlockProgress(minecraft.player.getId(), breaking, -1);
            action(RemoteBuilding.CANCEL_BREAK, breaking);
        }
        breaking = null; work = 0;
    }
    @Override public void tick() {
        if (!OverheadView.active()) { onClose(); return; }
        OverheadView.startPanTick();
        if (OverheadView.transitioning()) { leftHeld = false; rightHeld = false; resetWork(); return; }
        double forward = (keys.contains(GLFW.GLFW_KEY_W) ? 1 : 0) - (keys.contains(GLFW.GLFW_KEY_S) ? 1 : 0);
        double right = (keys.contains(GLFW.GLFW_KEY_D) ? 1 : 0) - (keys.contains(GLFW.GLFW_KEY_A) ? 1 : 0);
        double yaw = Math.toRadians(OverheadView.YAW), speed = Math.max(OverheadView.MIN_PAN_PER_TICK, Math.min(OverheadView.MAX_PAN_PER_TICK, OverheadView.distance * OverheadView.PAN_SCALE));
        if (forward != 0 && right != 0) speed /= Math.sqrt(2);
        OverheadView.centerX += (-Math.sin(yaw) * forward - Math.cos(yaw) * right) * speed;
        OverheadView.centerZ += (Math.cos(yaw) * forward - Math.sin(yaw) * right) * speed;
        targets();
        BlockPos target = target();
        if (pasteHeld && target != null && FactoryActions.assembler(minecraft.level, target)
                && pasted.add(FactoryActions.root(minecraft.level, target))) InventoryNetwork.requestAction(target, FactoryActions.PASTE);
        if (rightHeld && target != null) transfer(target);
        if (!leftHeld || overUI(mouseX, mouseY)) { resetWork(); return; }
        if (dev.conveyance.combat.PistolItem.gun(minecraft.player.getMainHandItem())) {
            resetWork();
            if (combatAim != null && minecraft.level.getGameTime() % dev.conveyance.combat.PistolItem.COOLDOWN == 0) dev.conveyance.combat.CombatNetwork.shoot(combatAim);
            return;
        }
        boolean building = !hasShiftDown() && minecraft.player.getMainHandItem().getItem() instanceof BlockItem;
        if (building) {
            resetWork(); var plan = plan();
            if (floor != null && plan != null && plan.valid() && placed.add(floor.immutable())) action(RemoteBuilding.PLACE, floor);
            return;
        }
        if (target == null) { resetWork(); return; }
        if (!target.equals(breaking)) { resetWork(); breaking = target.immutable(); action(RemoteBuilding.START_BREAK, target); }
        boolean terrain = TerrainHarvest.terrain(minecraft.level.getBlockState(target));
        int duration = terrain ? 20 : 10;
        minecraft.level.destroyBlockProgress(minecraft.player.getId(), target, Math.min(9, work * 10 / duration));
        if (++work >= duration + 1) {
            if (terrain) InventoryNetwork.requestHarvest(target); else action(RemoteBuilding.FINISH_BREAK, target);
            minecraft.level.destroyBlockProgress(minecraft.player.getId(), target, -1);
            breaking = null; work = 0;
        }
    }
    private boolean transfer(BlockPos target) {
        int action = hasControlDown() ? FactoryActions.COLLECT
                : FactoryActions.fuelTarget(minecraft.level, target, minecraft.player.getMainHandItem()) ? FactoryActions.FUEL : -1;
        if (action < 0) return false;
        if (transferred.add(FactoryActions.root(minecraft.level, target))) InventoryNetwork.requestAction(target, action);
        return true;
    }
    @Override public void render(GuiGraphics graphics, int x, int y, float partialTick) {
        mouseX = x; mouseY = y; targets();
        FactoryOverlayView.badges(graphics, this, x, y);
        graphics.fill(6, 5, 301, 54, 0xD91A2530);
        graphics.drawString(font, title, 12, 11, 0xFFE1E8EF);
        graphics.drawCenteredString(font, Component.translatable("screen.conveyance.height", OverheadView.height), 78, 34, 0xFFFFFFFF);
        var held = LinkedHotbar.handReference(minecraft.player);
        Component selection = held.isEmpty() ? Component.translatable("screen.conveyance.empty_hand") : held.getHoverName();
        graphics.drawCenteredString(font, selection, width / 2, height - 46, 0xFFFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.conveyance.controls"), width / 2, height - 37, 0xFFD2DCE5);
        int left = (width - LinkedHotbar.SIZE * 22) / 2;
        for (int slot = 0; slot < LinkedHotbar.SIZE; slot++) HotbarLinksView.cell(graphics, slot, left + slot * 22, height - 28, 22);
        if (floor != null && !overUI(x, y)) {
            boolean inside = RemoteBuilding.canReach(minecraft.player, floor);
            String label = inside ? floor.getX() + ", " + floor.getY() + ", " + floor.getZ()
                    : Component.translatable("screen.conveyance.outside").getString();
            graphics.drawString(font, label, x + 12, y + 12, inside ? 0xFFFFFFFF : 0xFFFF8888);
        }
        super.render(graphics, x, y, partialTick);
        FactoryOverlayView.toolbar(graphics, overlayButtons, x, y);
        int slot = hotbarSlot(x, y); if (slot >= 0) HotbarLinksView.tooltip(graphics, slot, x, y);
        else FactoryOverlayView.tooltip(graphics, x, y);
        FactoryOverlayView.toolbarTooltip(graphics, overlayButtons, x, y);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        mouseX = x; mouseY = y;
        if (super.mouseClicked(x, y, button)) return true;
        int slot = hotbarSlot(x, y); if (slot >= 0) { select(slot); return true; }
        if (overUI(x, y)) return false;
        targets();
        if (button == 0) { leftHeld = true; placed.clear(); return true; }
        if (button == 1) {
            rightHeld = true; transferred.clear(); BlockPos target = target();
            if (target != null && !transfer(target)) { action(RemoteBuilding.USE, target); rightHeld = false; }
            return true;
        }
        if (button == 2 && hit != null) {
            var clone = minecraft.level.getBlockState(hit.getBlockPos()).getCloneItemStack(hit, minecraft.level, hit.getBlockPos(), minecraft.player);
            if (!clone.isEmpty()) InventoryNetwork.requestLink(minecraft.player.containerMenu.containerId,
                    LinkedHotbar.bindingSlot(minecraft.player), BuiltInRegistries.ITEM.getKey(clone.getItem()));
            return true;
        }
        return false;
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        if (button == 0) { leftHeld = false; placed.clear(); resetWork(); }
        if (button == 1) { rightHeld = false; transferred.clear(); }
        return super.mouseReleased(x, y, button);
    }
    @Override public void mouseMoved(double x, double y) { mouseX = x; mouseY = y; }
    @Override public boolean mouseScrolled(double x, double y, double amount) {
        OverheadView.distance = Math.max(OverheadView.MIN_DISTANCE, Math.min(OverheadView.MAX_DISTANCE, OverheadView.distance * Math.pow(OverheadView.ZOOM_STEP, amount))); return true;
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (OverheadView.Keys.TOGGLE.matches(key, scan) || key == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        if (minecraft.options.keyInventory.matches(key, scan)) { WorkbenchNavigation.open(0); return true; }
        if (WorkbenchNavigation.Keys.MAP.matches(key, scan)) { WorkbenchNavigation.open(2); return true; }
        if (WorkbenchNavigation.Keys.TECHNOLOGY.matches(key, scan)) { WorkbenchNavigation.open(1); return true; }
        if (HotbarControls.Keys.EMPTY.matches(key, scan)) {
            if (keys.add(key)) {
                leftHeld = false; rightHeld = false; resetWork(); placed.clear(); HotbarControls.pipette(target());
            }
            return true;
        }
        for (int slot = 0; slot < 9; slot++) if (minecraft.options.keyHotbarSlots[slot].matches(key, scan)) { select(slot + 1); return true; }
        if (key == GLFW.GLFW_KEY_PAGE_UP) { changeHeight(1); return true; }
        if (key == GLFW.GLFW_KEY_PAGE_DOWN) { changeHeight(-1); return true; }
        if (key == GLFW.GLFW_KEY_R) {
            if (hasShiftDown() && target() != null) action(RemoteBuilding.ROTATE, target());
            else { facing = facing.getClockWise(); OverheadView.facing = facing; }
            placed.clear(); return true;
        }
        if (FactoryControls.Keys.COPY.matches(key, scan)) { if (target() != null) InventoryNetwork.requestAction(target(), FactoryActions.COPY); return true; }
        if (FactoryControls.Keys.PASTE.matches(key, scan)) { pasteHeld = true; return true; }
        keys.add(key); return true;
    }
    @Override public boolean keyReleased(int key, int scan, int modifiers) {
        keys.remove(key);
        if (FactoryControls.Keys.PASTE.matches(key, scan)) { pasteHeld = false; pasted.clear(); }
        return true;
    }
    @Override public void removed() { resetWork(); OverheadView.startPanTick(); keys.clear(); KeyMapping.releaseAll(); }
    @Override public void onClose() { resetWork(); OverheadView.leave(); }
    @Override public boolean isPauseScreen() { return false; }
}
