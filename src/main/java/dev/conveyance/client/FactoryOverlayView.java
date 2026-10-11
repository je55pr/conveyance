package dev.conveyance.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.conveyance.Conveyance;
import dev.conveyance.factory.FactoryActions;
import dev.conveyance.factory.FactoryContent;
import dev.conveyance.factory.FactoryOverlayStatus;
import dev.conveyance.factory.InserterBlock;
import dev.conveyance.factory.PowerNetwork;
import dev.conveyance.factory.PowerPoleBlock;
import dev.conveyance.player.FactoryOverlayNetwork;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** B-mode layers share a bounded server snapshot; rendering never loads chunks. */
@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, value = Dist.CLIENT)
public final class FactoryOverlayView {
    public enum Layer {
        POWER(false), RECIPES(true), WARNINGS(true), ARROWS(false);
        private boolean enabled;
        Layer(boolean enabled) { this.enabled = enabled; }
        public void toggle() { enabled = !enabled; }
        public Component title() { return Component.translatable("overlay.conveyance.layer." + name().toLowerCase(java.util.Locale.ROOT)); }
        ItemStack icon() { return switch (this) {
            case POWER -> new ItemStack(FactoryContent.POWER_POLE_ITEM.get()); case RECIPES -> new ItemStack(FactoryContent.GEAR_ITEM.get());
            case ARROWS -> new ItemStack(FactoryContent.INSERTER_ITEM.get()); case WARNINGS -> ItemStack.EMPTY;
        }; }
    }
    private record Point(float x, float y) {}
    private record Badge(FactoryOverlayStatus.Entry entry, int x, int y, int size, boolean warning) {
        boolean contains(int mx, int my) { return mx >= x && mx < x + size && my >= y && my < y + size; }
    }
    private static final Minecraft GAME = Minecraft.getInstance();
    private static Level world;
    private static List<FactoryOverlayStatus.Entry> entries = List.of();
    private static long receivedAt;
    private static Matrix4f projection;
    private static Vec3 camera;
    private static List<BlockPos> cachedPoles = List.of();
    private static int coverageHeight = Integer.MIN_VALUE;
    private static Set<BlockPos> coverage = Set.of();
    private static List<Badge> badges = List.of();

    public static void accept(FactoryOverlayNetwork.Snapshot snapshot) {
        if (GAME.level == null || !GAME.level.dimension().location().equals(snapshot.dimension())) return;
        world = GAME.level; entries = snapshot.entries(); receivedAt = world.getGameTime();
    }
    private static boolean fresh() { return world != null && world == GAME.level && world.getGameTime() - receivedAt <= 20; }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        world = null; entries = List.of(); projection = null; camera = null; coverage = Set.of(); cachedPoles = List.of(); badges = List.of();
    }
    private static BlockPos hovered(OverheadBuildScreen screen) {
        BlockPos target = screen.target(); return target == null ? null : FactoryActions.root(GAME.level, target);
    }
    @SubscribeEvent public static void world(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (!(GAME.screen instanceof OverheadBuildScreen screen) || !OverheadView.active() || OverheadView.transitioning()) { projection = null; return; }
        camera = event.getCamera().getPosition();
        projection = new Matrix4f(event.getProjectionMatrix()).mul(event.getPoseStack().last().pose());
        var pose = event.getPoseStack(); var buffers = GAME.renderBuffers().bufferSource();
        pose.pushPose(); pose.translate(-camera.x, -camera.y, -camera.z);
        BlockPos hovered = hovered(screen);
        var preview = screen.plan();
        if (Layer.POWER.enabled && fresh()) {
            var poles = entries.stream().filter(entry -> entry.kind() == FactoryOverlayStatus.Kind.POLE).map(FactoryOverlayStatus.Entry::root).toList();
            if (coverageHeight != OverheadView.height || !poles.equals(cachedPoles)) {
                var tiles = new HashSet<BlockPos>();
                for (BlockPos pole : poles) tiles.addAll(coverage(pole));
                coverage = tiles; cachedPoles = poles; coverageHeight = OverheadView.height;
            }
            shade(pose, buffers.getBuffer(RenderType.debugQuads()), coverage, .15F, .65F, 1F, .16F);
        }
        if (fresh()) for (var entry : entries) {
            if (entry.kind() == FactoryOverlayStatus.Kind.POLE && entry.root().equals(hovered)) {
                var tiles = coverage(entry.root());
                if (!Layer.POWER.enabled) shade(pose, buffers.getBuffer(RenderType.debugQuads()), tiles, .15F, .65F, 1F, .22F);
                border(pose, buffers.getBuffer(RenderType.lines()), tiles, .3F, .8F, 1F);
            }
            if (entry.kind() == FactoryOverlayStatus.Kind.INSERTER && (Layer.ARROWS.enabled || entry.root().equals(hovered))) arrows(pose,
                    buffers.getBuffer(RenderType.lines()), entry.root(), entry.facing(), entry.reach());
        }
        if (preview != null && !preview.parts().isEmpty()) {
            var part = preview.parts().get(0);
            if (part.state().getBlock() instanceof PowerPoleBlock) {
                var tiles = coverage(part.pos());
                shade(pose, buffers.getBuffer(RenderType.debugQuads()), tiles, preview.valid() ? .2F : 1F, preview.valid() ? .9F : .25F, .3F, .22F);
                border(pose, buffers.getBuffer(RenderType.lines()), tiles, preview.valid() ? .3F : 1F, preview.valid() ? 1F : .3F, .35F);
            } else if (part.state().getBlock() instanceof InserterBlock arm) arrows(pose, buffers.getBuffer(RenderType.lines()), part.pos(), part.state().getValue(InserterBlock.FACING), arm.reach());
        }
        buffers.endBatch(RenderType.debugQuads()); buffers.endBatch(RenderType.lines()); pose.popPose();
    }
    private static Set<BlockPos> coverage(BlockPos pole) {
        var tiles = new HashSet<BlockPos>(); int radius = PowerNetwork.COVERAGE_RANGE;
        int dy = OverheadView.height - pole.getY();
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++)
            if (x * x + z * z + dy * dy <= radius * radius) tiles.add(new BlockPos(pole.getX() + x, OverheadView.height, pole.getZ() + z));
        return tiles;
    }
    private static void shade(PoseStack pose, VertexConsumer vertices, Set<BlockPos> tiles, float r, float g, float b, float a) {
        for (var pos : tiles) {
            float x = pos.getX(), y = pos.getY() + .015F, z = pos.getZ();
            vertices.vertex(pose.last().pose(), x, y, z).color(r, g, b, a).endVertex();
            vertices.vertex(pose.last().pose(), x, y, z + 1).color(r, g, b, a).endVertex();
            vertices.vertex(pose.last().pose(), x + 1, y, z + 1).color(r, g, b, a).endVertex();
            vertices.vertex(pose.last().pose(), x + 1, y, z).color(r, g, b, a).endVertex();
        }
    }
    private static void border(PoseStack pose, VertexConsumer vertices, Set<BlockPos> tiles, float r, float g, float b) {
        for (var pos : tiles) {
            double x = pos.getX(), y = pos.getY() + .025, z = pos.getZ();
            if (!tiles.contains(pos.north())) line(pose, vertices, new Vec3(x, y, z), new Vec3(x + 1, y, z), r, g, b);
            if (!tiles.contains(pos.south())) line(pose, vertices, new Vec3(x, y, z + 1), new Vec3(x + 1, y, z + 1), r, g, b);
            if (!tiles.contains(pos.west())) line(pose, vertices, new Vec3(x, y, z), new Vec3(x, y, z + 1), r, g, b);
            if (!tiles.contains(pos.east())) line(pose, vertices, new Vec3(x + 1, y, z), new Vec3(x + 1, y, z + 1), r, g, b);
        }
    }
    private static void line(PoseStack pose, VertexConsumer vertices, Vec3 from, Vec3 to, float r, float g, float b) {
        var normal = to.subtract(from).normalize();
        vertices.vertex(pose.last().pose(), (float) from.x, (float) from.y, (float) from.z).color(r, g, b, .95F)
                .normal(pose.last().normal(), (float) normal.x, (float) normal.y, (float) normal.z).endVertex();
        vertices.vertex(pose.last().pose(), (float) to.x, (float) to.y, (float) to.z).color(r, g, b, .95F)
                .normal(pose.last().normal(), (float) normal.x, (float) normal.y, (float) normal.z).endVertex();
    }
    private static void arrows(PoseStack pose, VertexConsumer vertices, BlockPos root, Direction facing, int reach) {
        Vec3 centre = Vec3.atCenterOf(root).add(0, .65, 0);
        Vec3 delta = new Vec3(facing.getStepX(), 0, facing.getStepZ());
        arrow(pose, vertices, centre.subtract(delta.scale(reach)), centre, .35F, .85F, 1F);
        arrow(pose, vertices, centre, centre.add(delta.scale(reach)), 1F, .65F, .2F);
    }
    private static void arrow(PoseStack pose, VertexConsumer vertices, Vec3 from, Vec3 to, float r, float g, float b) {
        line(pose, vertices, from, to, r, g, b);
        Vec3 delta = to.subtract(from).normalize(), side = new Vec3(-delta.z, 0, delta.x).scale(.18), back = to.subtract(delta.scale(.3));
        line(pose, vertices, back.add(side), to, r, g, b); line(pose, vertices, back.subtract(side), to, r, g, b);
    }
    private static Point project(Vec3 position, int width, int height) {
        if (projection == null || camera == null) return null;
        Vec3 relative = position.subtract(camera);
        var clip = new Vector4f((float) relative.x, (float) relative.y, (float) relative.z, 1).mul(projection);
        if (clip.w <= 0 || clip.z < -clip.w || clip.z > clip.w) return null;
        float x = (clip.x / clip.w + 1) * width / 2, y = (1 - clip.y / clip.w) * height / 2;
        return Float.isFinite(x) && Float.isFinite(y) ? new Point(x, y) : null;
    }
    public static void badges(GuiGraphics graphics, OverheadBuildScreen screen, int mouseX, int mouseY) {
        BlockPos hovered = fresh() ? hovered(screen) : null;
        var drawn = new ArrayList<Badge>(); badges = drawn;
        if (!fresh() || OverheadView.transitioning()) return;
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        var visible = new ArrayList<>(entries);
        visible.sort(Comparator.<FactoryOverlayStatus.Entry>comparingInt(entry -> entry.root().equals(hovered) ? 0 : entry.status().alert ? 1 : 2)
                .thenComparingDouble(entry -> entry.roof().distanceToSqr(camera == null ? Vec3.ZERO : camera)));
        var occupied = new HashSet<Long>();
        for (var entry : visible) {
            if (entry.kind() == FactoryOverlayStatus.Kind.POLE || !GAME.level.hasChunkAt(entry.root())) continue;
            if (!(entry.building().getItem() instanceof BlockItem item) || !GAME.level.getBlockState(entry.root()).is(item.getBlock())) continue;
            Point point = project(entry.roof(), width, height);
            if (point == null || point.x < 9 || point.x >= width - 9 || point.y < 64 || point.y > height - 60) continue;
            Point adjacent = project(entry.roof().add(1, 0, 0), width, height);
            float span = adjacent == null ? 0 : Math.abs(adjacent.x - point.x) * Math.min(entry.width(), entry.depth());
            boolean recipe = Layer.RECIPES.enabled && !entry.product().isEmpty() && (span >= 12 || entry.root().equals(hovered));
            boolean warning = Layer.WARNINGS.enabled && (entry.status().alert || entry.root().equals(hovered) && entry.status() != FactoryOverlayStatus.Status.IDLE && entry.status() != FactoryOverlayStatus.Status.WORKING);
            if (!recipe && !warning) continue;
            int x = (int) point.x - (recipe && warning ? 18 : 9), y = (int) point.y - 9;
            var cells = new ArrayList<Long>();
            for (int cx = (x - 1) / 20; cx <= (x + (recipe && warning ? 39 : 19)) / 20; cx++)
                for (int cy = (y - 1) / 20; cy <= (y + 19) / 20; cy++) cells.add((long) cx << 32 ^ (cy & 0xffffffffL));
            if (cells.stream().anyMatch(occupied::contains) && !entry.root().equals(hovered)) continue;
            occupied.addAll(cells);
            if (recipe) { Badge badge = new Badge(entry, x, y, 18, false); draw(graphics, badge); drawn.add(badge); x += 20; }
            if (warning) { Badge badge = new Badge(entry, x, y, 18, true); draw(graphics, badge); drawn.add(badge); }
        }
    }
    private static void draw(GuiGraphics graphics, Badge badge) {
        int x = badge.x(), y = badge.y(); int colour = !badge.warning() ? 0xFF899DAA
                : !badge.entry().status().alert ? 0xFFB1BCC5 : badge.entry().status() == FactoryOverlayStatus.Status.LOW_POWER ? 0xFFE3B44F : 0xFFFF7865;
        graphics.fill(x - 1, y - 1, x + 19, y + 19, colour); graphics.fill(x, y, x + 18, y + 18, 0xE0192530);
        if (!badge.warning()) graphics.renderItem(badge.entry().product(), x + 1, y + 1);
        else {
            ItemStack icon = statusIcon(badge.entry().status());
            if (!icon.isEmpty()) graphics.renderItem(icon, x + 1, y + 1);
            else lightning(graphics, x + 3, y + 2, colour);
        }
    }
    private static ItemStack statusIcon(FactoryOverlayStatus.Status status) { return switch (status) {
        case NO_FUEL -> new ItemStack(Items.COAL); case NO_AMMO -> new ItemStack(FactoryContent.FIREARM_MAGAZINE.get());
        case NO_RECIPE -> new ItemStack(FactoryContent.GEAR_ITEM.get()); case INPUTS -> new ItemStack(FactoryContent.WOOD_CRATE_ITEM.get());
        case OUTPUT_FULL -> new ItemStack(FactoryContent.CONVEYOR_ITEM.get()); case NO_RESOURCE -> new ItemStack(Items.IRON_PICKAXE);
        case NO_WATER -> new ItemStack(Items.WATER_BUCKET); case NO_STEAM -> new ItemStack(FactoryContent.STEAM_ENGINE_ITEM.get()); default -> ItemStack.EMPTY;
    }; }
    private static void lightning(GuiGraphics graphics, int x, int y, int colour) {
        graphics.fill(x + 6, y, x + 10, y + 5, colour); graphics.fill(x + 3, y + 4, x + 9, y + 8, colour);
        graphics.fill(x + 5, y + 7, x + 8, y + 11, colour); graphics.fill(x + 3, y + 10, x + 6, y + 14, colour);
    }
    public static BlockPos target(int mouseX, int mouseY) {
        if (!fresh()) return null;
        for (int index = badges.size() - 1; index >= 0; index--) {
            Badge badge = badges.get(index);
            if (badge.contains(mouseX, mouseY) && badge.entry().building().getItem() instanceof BlockItem item
                    && GAME.level.hasChunkAt(badge.entry().root()) && GAME.level.getBlockState(badge.entry().root()).is(item.getBlock())) return badge.entry().root();
        }
        return null;
    }
    public static void tooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        for (int index = badges.size() - 1; index >= 0; index--) {
            Badge badge = badges.get(index); if (!badge.contains(mouseX, mouseY)) continue;
            var entry = badge.entry(); var rows = new ArrayList<IconTooltips.Row>();
            rows.add(IconTooltips.item(entry.building(), entry.building().getHoverName()));
            if (!entry.product().isEmpty()) rows.add(IconTooltips.item(Component.translatable("overlay.conveyance.operation", Component.empty()), entry.product(), entry.operation()));
            rows.add(IconTooltips.text(entry.status().label().copy().withStyle(entry.status().alert ? ChatFormatting.GOLD : ChatFormatting.GRAY)));
            if (entry.status() == FactoryOverlayStatus.Status.NO_COVERAGE || entry.status() == FactoryOverlayStatus.Status.NO_POWER || entry.status() == FactoryOverlayStatus.Status.LOW_POWER)
                rows.add(IconTooltips.text(Component.translatable("overlay.conveyance.power_fraction", Math.round(entry.power() * 100))));
            IconTooltips.render(graphics, rows, mouseX, mouseY); return;
        }
    }
    public static void toolbar(GuiGraphics graphics, List<Button> buttons, int mouseX, int mouseY) {
        for (int index = 0; index < buttons.size(); index++) {
            var button = buttons.get(index); var layer = Layer.values()[index]; int x = button.getX(), y = button.getY();
            graphics.fill(x, y, x + 22, y + 20, layer.enabled ? 0xFF618A78 : 0xFF354353);
            graphics.fill(x + 1, y + 1, x + 21, y + 19, layer.enabled ? 0xFF375749 : 0xFF202A35);
            if (layer == Layer.WARNINGS) graphics.drawCenteredString(GAME.font, "!", x + 11, y + 5, 0xFFFFC16C);
            else graphics.renderItem(layer.icon(), x + 3, y + 2);
        }
    }
    public static void toolbarTooltip(GuiGraphics graphics, List<Button> buttons, int mouseX, int mouseY) {
        for (int index = 0; index < buttons.size(); index++) {
            var button = buttons.get(index); var layer = Layer.values()[index]; int x = button.getX(), y = button.getY();
            if (mouseX >= x && mouseX < x + 22 && mouseY >= y && mouseY < y + 20) {
                var rows = new ArrayList<IconTooltips.Row>();
                rows.add(IconTooltips.item(layer.icon(), layer.title()));
                rows.add(IconTooltips.text(Component.translatable(layer.enabled ? "overlay.conveyance.enabled" : "overlay.conveyance.disabled")));
                rows.add(IconTooltips.text(Component.translatable("overlay.conveyance.help." + layer.name().toLowerCase(java.util.Locale.ROOT))));
                IconTooltips.render(graphics, rows, mouseX, mouseY); return;
            }
        }
    }
    private FactoryOverlayView() {}
}
