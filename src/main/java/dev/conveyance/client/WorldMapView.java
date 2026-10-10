package dev.conveyance.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.conveyance.factory.RadarEntity;
import dev.conveyance.factory.FactoryTooltips;
import dev.conveyance.map.MapNetwork;
import dev.conveyance.map.MapPalette;
import dev.conveyance.map.WorldMapData;
import dev.conveyance.pollution.PollutionData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

/** Distant views use larger overview pages with the same texture size. */
public final class WorldMapView {
    private int canvasHeight = 154, bodyHeight = 208, viewportWidth = 396;
    private record Page(int x, int z, int step) {}
    private record Texture(ResourceLocation id, long revision, long pollutionRevision, boolean pollution) {}
    private final Minecraft game = Minecraft.getInstance();
    private final LinkedHashMap<Page, Texture> textures = new LinkedHashMap<>(32, 0.75F, true);
    private double centerX, centerZ, zoom = 1;
    private boolean opened, dragging;
    private boolean pollution = WorkbenchPreferences.pollution();
    private long requestedAt = Long.MIN_VALUE;
    public void resize(int height) { bodyHeight = height; canvasHeight = Math.max(80, height - 54); requestedAt = Long.MIN_VALUE; }
    public void open() { if (!opened) { opened = true; center(); } }
    public Component pollutionLabel() { return Component.translatable(pollution ? "map.conveyance.pollution_on" : "map.conveyance.pollution_off"); }
    public void togglePollution() {
        pollution = !pollution; WorkbenchPreferences.pollution(pollution); requestedAt = Long.MIN_VALUE;
    }
    public void center() {
        if (game.player != null) { centerX = game.player.getX(); centerZ = game.player.getZ(); }
        requestedAt = Long.MIN_VALUE;
    }
    private static int canvasX(int left) { return left + 4; }
    private static int canvasY(int top) { return top + 23; }
    private int step() {
        int step = 1;
        while (step < 256) {
            double columns = (viewportWidth - 8) / (16 * step * zoom) + 2;
            double rows = canvasHeight / (16 * step * zoom) + 2;
            if (zoom * step >= 0.5 && columns < 62 && rows < 62 && columns * rows <= 1536) break;
            step *= 2;
        }
        return step;
    }
    private int minX(int width) { return (int) Math.floor((centerX - (width - 8) / (2 * zoom)) / (16 * step())); }
    private int minZ() { return (int) Math.floor((centerZ - canvasHeight / (2 * zoom)) / (16 * step())); }
    private int maxX(int width) { return (int) Math.floor((centerX + (width - 8) / (2 * zoom)) / (16 * step())); }
    private int maxZ() { return (int) Math.floor((centerZ + canvasHeight / (2 * zoom)) / (16 * step())); }
    public void tick(int width) {
        viewportWidth = width;
        if (game.level == null) return;
        long now = game.level.getGameTime();
        if (requestedAt != Long.MIN_VALUE && now - requestedAt < 20) return;
        requestedAt = now;
        MapNetwork.request(minX(width), minZ(), maxX(width), maxZ(), step(), pollution);
    }
    private ResourceLocation texture(WorldMapData.ViewTile tile) {
        Page key = new Page(tile.x(), tile.z(), tile.step());
        Texture cached = textures.get(key);
        if (cached != null && cached.revision == tile.revision() && cached.pollution == pollution
                && (!pollution || cached.pollutionRevision == tile.pollutionRevision())) return cached.id;
        if (cached != null) game.getTextureManager().release(cached.id);
        var image = new NativeImage(16, 16, false);
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            int rgb = MapPalette.color(tile.pixels()[z * 16 + x] & 255);
            if (pollution && tile.pixels()[z * 16 + x] != MapPalette.UNKNOWN) rgb = pollutionColor(rgb, tile.pollution()[z * 16 + x] & 255);
            image.setPixelRGBA(x, z, 0xFF000000 | (rgb & 255) << 16 | rgb & 0xFF00 | rgb >> 16 & 255);
        }
        var id = ResourceLocation.fromNamespaceAndPath("conveyance", "map/" + tile.step() + "/" + Long.toHexString(ChunkPos.asLong(tile.x(), tile.z())));
        game.getTextureManager().register(id, new DynamicTexture(image));
        textures.put(key, new Texture(id, tile.revision(), tile.pollutionRevision(), pollution));
        return id;
    }
    private static int pollutionColor(int rgb, int intensity) {
        if (intensity == 0) return rgb;
        double alpha = 0.16 + intensity / 255.0 * 0.62;
        int red = (int) ((rgb >> 16 & 255) * (1 - alpha) + 235 * alpha);
        int green = (int) ((rgb >> 8 & 255) * (1 - alpha) + 66 * alpha);
        int blue = (int) ((rgb & 255) * (1 - alpha) + 40 * alpha);
        return red << 16 | green << 8 | blue;
    }
    public void render(GuiGraphics graphics, int left, int top, int width, int mouseX, int mouseY) {
        int x = canvasX(left), y = canvasY(top), canvasWidth = width - 8;
        graphics.drawString(game.font, Component.translatable("menu.conveyance.map_tab"), left + 8, top + 7, 0xFFE7EEF6, false);
        Component count = Component.translatable("map.conveyance.explored", WorldMapData.clientCount());
        graphics.drawString(game.font, count, left + width - 8 - game.font.width(count), top + 7, 0xFF9CABB8, false);
        graphics.fill(x, y, x + canvasWidth, y + canvasHeight, 0xFF0C131B);
        graphics.enableScissor(x, y, x + canvasWidth, y + canvasHeight);
        int step = step(), pageBlocks = 16 * step;
        for (int cx = minX(width); cx <= maxX(width); cx++) for (int cz = minZ(); cz <= maxZ(); cz++) {
            int x1 = screenX(cx * (double) pageBlocks, x, canvasWidth), y1 = screenZ(cz * (double) pageBlocks, y);
            int x2 = screenX((cx + 1) * (double) pageBlocks, x, canvasWidth), y2 = screenZ((cz + 1) * (double) pageBlocks, y);
            var tile = WorldMapData.clientTile(cx, cz, step);
            if (tile != null) graphics.blit(texture(tile), x1, y1, x2 - x1, y2 - y1, 0, 0, 16, 16, 16, 16);
            if (zoom >= 1) {
                graphics.fill(x1, y1, x2, y1 + 1, 0x20374958);
                graphics.fill(x1, y1, x1 + 1, y2, 0x20374958);
            }
        }
        for (long packed : WorldMapData.clientRadars()) {
            BlockPos radar = BlockPos.of(packed);
            int sx = screenX(radar.getX() + 0.5, x, canvasWidth), sy = screenZ(radar.getZ() + 0.5, y);
            int x1 = screenX(RadarEntity.minChunk(radar.getX()) * 16.0, x, canvasWidth);
            int x2 = screenX(RadarEntity.maxChunkExclusive(radar.getX()) * 16.0, x, canvasWidth);
            int y1 = screenZ(RadarEntity.minChunk(radar.getZ()) * 16.0, y);
            int y2 = screenZ(RadarEntity.maxChunkExclusive(radar.getZ()) * 16.0, y);
            outline(graphics, x1, y1, x2, y2, 0xAA67BCE1);
            graphics.fill(sx - 3, sy - 3, sx + 4, sy + 4, 0xFF12252F);
            outline(graphics, sx - 2, sy - 2, sx + 3, sy + 3, 0xFF74D4FF);
        }
        if (game.player != null) {
            int px = screenX(game.player.getX(), x, canvasWidth), pz = screenZ(game.player.getZ(), y);
            graphics.fill(px - 4, pz - 4, px + 5, pz + 5, 0xFF182B21);
            graphics.fill(px - 1, pz - 3, px + 2, pz + 4, 0xFFB9FFB6);
            graphics.fill(px - 3, pz - 1, px + 4, pz + 2, 0xFFB9FFB6);
            double yaw = Math.toRadians(game.player.getYRot());
            int fx = px - (int) Math.round(Math.sin(yaw) * 6), fz = pz + (int) Math.round(Math.cos(yaw) * 6);
            graphics.fill(fx - 1, fz - 1, fx + 2, fz + 2, 0xFFFFFFFF);
        }
        graphics.drawString(game.font, "N ↑", x + 5, y + 5, 0xFFE7EEF6, true);
        graphics.disableScissor();
        int legendX = left + 7;
        for (int value : new int[] {MapPalette.IRON, MapPalette.COPPER, MapPalette.COAL, MapPalette.STONE}) {
            Component label = Component.translatable("map.conveyance." + MapPalette.name(value));
            graphics.fill(legendX, top + bodyHeight - 26, legendX + 5, top + bodyHeight - 20, 0xFF000000 | MapPalette.color(value));
            graphics.drawString(game.font, label, legendX + 7, top + bodyHeight - 27, 0xFFBCCAD6, false);
            legendX += 12 + game.font.width(label);
        }
        if (pollution) {
            Component low = Component.translatable("map.conveyance.pollution_low"), high = Component.translatable("map.conveyance.pollution_high");
            int total = game.font.width(low) + game.font.width(high) + 50;
            if (left + width - total - 8 > legendX) {
                int lx = left + width - total - 8, ly = top + bodyHeight - 27;
                graphics.drawString(game.font, low, lx, ly, 0xFFBCCAD6, false);
                lx += game.font.width(low) + 5;
                for (int i = 0; i < 32; i++) graphics.fill(lx + i, ly + 1, lx + i + 1, ly + 7, 0xFF000000 | pollutionColor(0x202A35, 1 + i * 254 / 31));
                graphics.drawString(game.font, high, lx + 37, ly, 0xFFBCCAD6, false);
            }
        }
        graphics.drawString(game.font, Component.translatable("map.conveyance.controls"), left + 8, top + bodyHeight - 13, 0xFF9CABB8, false);
        if (inside(mouseX, mouseY, left, top, width)) {
            int wx = (int) Math.floor(centerX + (mouseX - x - canvasWidth / 2.0) / zoom);
            int wz = (int) Math.floor(centerZ + (mouseY - y - canvasHeight / 2.0) / zoom);
            var tile = WorldMapData.clientTile(Math.floorDiv(wx, pageBlocks), Math.floorDiv(wz, pageBlocks), step);
            int pixel = Math.floorMod(wz, pageBlocks) / step * 16 + Math.floorMod(wx, pageBlocks) / step;
            int value = tile == null ? MapPalette.UNKNOWN : tile.pixels()[pixel] & 255;
            var tooltip = new ArrayList<Component>();
            if (MapPalette.isResource(value)) tooltip.add(Component.translatable("map.conveyance." + MapPalette.name(value)));
            int encoded = pollution && tile != null ? tile.pollution()[pixel] & 255 : 0;
            if (encoded > 0) tooltip.add(Component.translatable("map.conveyance.pollution_value", encoded == 255 ? "≥100" : "~" + FactoryTooltips.number(PollutionData.decode(encoded))));
            if (!tooltip.isEmpty()) graphics.renderComponentTooltip(game.font, tooltip, mouseX, mouseY);
        }
        while (textures.size() > 2048) {
            var first = textures.entrySet().iterator().next();
            game.getTextureManager().release(first.getValue().id);
            textures.remove(first.getKey());
        }
    }
    private int screenX(double worldX, int left, int width) { return (int) Math.floor(left + width / 2.0 + (worldX - centerX) * zoom); }
    private int screenZ(double worldZ, int top) { return (int) Math.floor(top + canvasHeight / 2.0 + (worldZ - centerZ) * zoom); }
    private static void outline(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y1 + 1, color); graphics.fill(x1, y2 - 1, x2, y2, color);
        graphics.fill(x1, y1, x1 + 1, y2, color); graphics.fill(x2 - 1, y1, x2, y2, color);
    }
    private boolean inside(double x, double y, int left, int top, int width) { return x >= canvasX(left) && x < left + width - 4 && y >= canvasY(top) && y < canvasY(top) + canvasHeight; }
    public boolean click(double x, double y, int button, int left, int top, int width) {
        if (!inside(x, y, left, top, width)) return false;
        if (button == 0 || button == 1) dragging = true;
        return true;
    }
    public boolean drag(double dx, double dy) {
        if (!dragging) return false;
        centerX = Math.max(-29999000, Math.min(29999000, centerX - dx / zoom));
        centerZ = Math.max(-29999000, Math.min(29999000, centerZ - dy / zoom));
        return true;
    }
    public boolean release() { boolean result = dragging; dragging = false; return result; }
    public boolean scroll(double x, double y, double amount, int left, int top, int width) {
        if (!inside(x, y, left, top, width)) return false;
        double wx = centerX + (x - canvasX(left) - (width - 8) / 2.0) / zoom;
        double wz = centerZ + (y - canvasY(top) - canvasHeight / 2.0) / zoom;
        int previousStep = step();
        zoom = Math.max(0.0625, Math.min(6, zoom * Math.pow(1.25, amount)));
        if (step() != previousStep) requestedAt = Long.MIN_VALUE;
        centerX = wx - (x - canvasX(left) - (width - 8) / 2.0) / zoom;
        centerZ = wz - (y - canvasY(top) - canvasHeight / 2.0) / zoom;
        return true;
    }
    public void close() { for (var texture : textures.values()) game.getTextureManager().release(texture.id); textures.clear(); }
}
