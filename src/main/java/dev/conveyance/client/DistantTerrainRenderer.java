package dev.conveyance.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import dev.conveyance.Conveyance;
import dev.conveyance.world.DistantTerrainNetwork;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/** Bounded GPU tiles plus one low-priority CPU worker; never loads distant world chunks. */
@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, value = Dist.CLIENT)
public final class DistantTerrainRenderer {
    public static final int DISTANCE = 1024, PAGE = 64;
    private static final String[] SPRITES = {"grass_block_top", "dirt", "sand", "granite", "stone", "water_still",
            "iron_ore", "copper_ore", "coal_ore", "emerald_ore", "black_concrete", "oak_log", "acacia_leaves"};
    private record Request(int x, int z, double distance) { long key() { return ChunkPos.asLong(x, z); } }
    private static final class Session implements AutoCloseable {
        final PriorityBlockingQueue<Request> requests = new PriorityBlockingQueue<>(64, Comparator.comparingDouble(Request::distance));
        final ArrayBlockingQueue<DistantTerrainMesh.Data> complete = new ArrayBlockingQueue<>(8);
        final Set<Long> scheduled = ConcurrentHashMap.newKeySet();
        final Thread worker;
        volatile boolean closed;
        volatile int centreX, centreZ;
        Session(DistantTerrainNetwork.Source source) {
            worker = new Thread(() -> {
                try {
                    var builder = new DistantTerrainMesh(source);
                    int built = 0;
                    while (!closed) {
                        var request = requests.take();
                        var mesh = builder.build(request.x(), request.z());
                        if (++built % 32 == 0) builder.trim(centreX, centreZ);
                        if (!closed) complete.put(mesh);
                    }
                } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                catch (Throwable error) { closed = true; LogUtils.getLogger().error("Distant terrain worker stopped", error); }
            }, "Conveyance distant terrain");
            worker.setDaemon(true); worker.setPriority(Thread.MIN_PRIORITY + 1); worker.start();
        }
        @Override public void close() { closed = true; worker.interrupt(); requests.clear(); complete.clear(); }
    }
    private static final class Tile implements AutoCloseable {
        final int x, z;
        final VertexBuffer vertices;
        final VertexBuffer water;
        final AABB bounds;
        Tile(int x, int z, VertexBuffer vertices, VertexBuffer water) {
            this.x = x; this.z = z; this.vertices = vertices; this.water = water;
            bounds = new AABB(x * PAGE - 4, -32, z * PAGE - 4, x * PAGE + PAGE + 4, 32, z * PAGE + PAGE + 4);
        }
        @Override public void close() { if (vertices != null) vertices.close(); if (water != null) water.close(); }
    }
    private static final Map<Long, Tile> tiles = new HashMap<>();
    private static final BufferBuilder uploadBuilder = new BufferBuilder(262144);
    private static DistantTerrainNetwork.Source source;
    private static Session session;
    private static TextureAtlasSprite[] sprites;
    private static float[][] spriteBounds;
    private static DynamicTexture coverage;
    private static int coverageSize;
    private static int plannedX = Integer.MIN_VALUE, plannedZ = Integer.MIN_VALUE;
    static ShaderInstance shader;
    public static boolean active() { return source != null && source.enabled() && Minecraft.getInstance().level != null && Minecraft.getInstance().level.dimension() == Level.OVERWORLD; }
    public static void configure(DistantTerrainNetwork.Source next) {
        if (next.equals(source)) return;
        reset(); source = next;
        if (next.enabled()) {
            session = new Session(next);
            LogUtils.getLogger().info("Distant terrain enabled: {} blocks, generation {}", DISTANCE, next.generation());
        }
    }
    public static void reload() {
        var previous = source;
        reset(); source = previous;
        if (previous != null && previous.enabled()) session = new Session(previous);
    }
    private static void reset() {
        if (session != null) session.close(); session = null;
        tiles.values().forEach(Tile::close); tiles.clear(); sprites = null;
        spriteBounds = null;
        if (coverage != null) coverage.close(); coverage = null; coverageSize = 0;
        plannedX = plannedZ = Integer.MIN_VALUE;
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { reset(); source = null; }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        boolean waterPass = event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS;
        if ((event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS && !waterPass) || !active() || shader == null || session == null || session.closed) return;
        var game = Minecraft.getInstance(); var camera = event.getCamera().getPosition();
        var centre = OverheadView.terrainCentre(camera);
        int px = Math.floorDiv((int) Math.floor(centre.x), PAGE), pz = Math.floorDiv((int) Math.floor(centre.z), PAGE);
        if (!waterPass) {
            if (px != plannedX || pz != plannedZ) plan(px, pz);
            uploadReady();
        }
        if (tiles.isEmpty()) return;
        ShaderInstance previousShader = RenderSystem.getShader();
        int previousTexture = RenderSystem.getShaderTexture(0);
        int previousCoverageTexture = RenderSystem.getShaderTexture(1);
        int previousLightTexture = RenderSystem.getShaderTexture(2);
        if (!waterPass) updateCoverage(game, camera.x, camera.z);
        game.gameRenderer.lightTexture().turnOnLightLayer();
        RenderSystem.setShader(() -> shader); RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        RenderSystem.setShaderTexture(1, coverage.getId());
        RenderSystem.enableDepthTest(); RenderSystem.depthMask(true); RenderSystem.disableBlend(); RenderSystem.disableCull();
        if (waterPass) { RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc(); }
        shader.safeGetUniform("ShadeFactors").set(game.level.getShade(Direction.EAST, true), game.level.getShade(Direction.NORTH, true),
                game.level.getShade(Direction.UP, true), game.level.getShade(Direction.DOWN, true));
        int waterColour = BiomeColors.getAverageWaterColor(game.level, BlockPos.containing(centre));
        shader.safeGetUniform("WaterTint").set((waterColour >> 16 & 255) / 255F, (waterColour >> 8 & 255) / 255F, (waterColour & 255) / 255F);
        shader.safeGetUniform("SpriteU0").set(spriteBounds[0]); shader.safeGetUniform("SpriteV0").set(spriteBounds[1]);
        shader.safeGetUniform("SpriteU1").set(spriteBounds[2]); shader.safeGetUniform("SpriteV1").set(spriteBounds[3]);
        var visible = tiles.values().stream().filter(tile -> (waterPass ? tile.water : tile.vertices) != null && event.getFrustum().isVisible(tile.bounds))
                .sorted(waterPass ? Comparator.comparingDouble((Tile tile) -> distance(tile, camera.x, camera.z)).reversed()
                        : Comparator.comparingDouble((Tile tile) -> distance(tile, camera.x, camera.z))).toList();
        for (Tile tile : visible) {
            double dx = tile.x * PAGE + PAGE / 2.0 - centre.x, dz = tile.z * PAGE + PAGE / 2.0 - centre.z;
            if (dx * dx + dz * dz > (DISTANCE + PAGE) * (DISTANCE + PAGE) || !event.getFrustum().isVisible(tile.bounds)) continue;
            shader.safeGetUniform("TileOffset").set((float) (tile.x * PAGE - camera.x), (float) -camera.y, (float) (tile.z * PAGE - camera.z));
            Matrix4f modelView = new Matrix4f(event.getPoseStack().last().pose()).translate((float) (tile.x * PAGE - camera.x), (float) -camera.y, (float) (tile.z * PAGE - camera.z));
            var buffer = waterPass ? tile.water : tile.vertices;
            buffer.bind(); buffer.drawWithShader(modelView, event.getProjectionMatrix(), shader);
        }
        VertexBuffer.unbind(); RenderSystem.enableCull();
        RenderSystem.setShaderTexture(0, previousTexture); RenderSystem.setShaderTexture(1, previousCoverageTexture);
        RenderSystem.setShaderTexture(2, previousLightTexture); RenderSystem.disableBlend();
        RenderSystem.setShader(() -> previousShader);
    }
    private static double distance(Tile tile, double x, double z) {
        double dx = tile.x * PAGE + PAGE / 2.0 - x, dz = tile.z * PAGE + PAGE / 2.0 - z; return dx * dx + dz * dz;
    }
    private static void updateCoverage(Minecraft game, double cameraX, double cameraZ) {
        int radius = game.options.getEffectiveRenderDistance(), size = radius * 2 + 1;
        int minX = (OverheadView.controlsCamera() && game.player != null ? game.player.chunkPosition().x : (int) Math.floor(cameraX / 16)) - radius;
        int minZ = (OverheadView.controlsCamera() && game.player != null ? game.player.chunkPosition().z : (int) Math.floor(cameraZ / 16)) - radius;
        if (coverage == null || coverageSize != size) {
            if (coverage != null) coverage.close();
            coverage = new DynamicTexture(new NativeImage(size, size, false)); coverageSize = size;
        }
        var image = coverage.getPixels();
        for (int z = 0; z < size; z++) for (int x = 0; x < size; x++) {
            int cx = minX + x, cz = minZ + z, flags = 0;
            if (game.level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false) != null) {
                // Each channel describes one section; air/hidden sections cannot hold up the others.
                for (int section = 0; section < 4; section++) {
                    if (game.levelRenderer.isChunkCompiled(new BlockPos(cx * 16 + 8, -32 + section * 16, cz * 16 + 8))) flags |= 255 << (section * 8);
                }
            }
            image.setPixelRGBA(x, z, flags);
        }
        coverage.upload();
        shader.safeGetUniform("CoverageOrigin").set((float) (minX * 16 - cameraX), (float) (minZ * 16 - cameraZ));
        shader.safeGetUniform("CoverageSize").set((float) size);
    }
    private static void plan(int px, int pz) {
        plannedX = px; plannedZ = pz;
        session.centreX = px * PAGE; session.centreZ = pz * PAGE;
        int radius = DISTANCE / PAGE + 1;
        var stale = tiles.entrySet().iterator();
        while (stale.hasNext()) {
            var entry = stale.next(); var tile = entry.getValue();
            if ((long) (tile.x - px) * (tile.x - px) + (long) (tile.z - pz) * (tile.z - pz) > (radius + 1) * (radius + 1)) { tile.close(); stale.remove(); }
        }
        var queued = new ArrayList<Request>(); session.requests.drainTo(queued);
        for (var request : queued) session.scheduled.remove(request.key());
        for (int z = -radius; z <= radius; z++) for (int x = -radius; x <= radius; x++) {
            int distance = x * x + z * z;
            if (distance > radius * radius) continue;
            var request = new Request(px + x, pz + z, distance);
            if (!tiles.containsKey(request.key()) && session.scheduled.add(request.key())) session.requests.add(request);
        }
    }
    private static void uploadReady() {
        long deadline = System.nanoTime() + 2_000_000;
        for (int count = 0; count < 2; count++) {
            var data = session.complete.poll(); if (data == null) break;
            long key = ChunkPos.asLong(data.x(), data.z()); session.scheduled.remove(key);
            int dx = data.x() - plannedX, dz = data.z() - plannedZ, radius = DISTANCE / PAGE + 2;
            if (dx * dx + dz * dz > radius * radius || data.faces().isEmpty() || tiles.containsKey(key)) continue;
            if (sprites == null) {
                sprites = new TextureAtlasSprite[SPRITES.length];
                var atlas = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS);
                for (int index = 0; index < SPRITES.length; index++) sprites[index] = atlas.apply(ResourceLocation.withDefaultNamespace("block/" + SPRITES[index]));
                spriteBounds = new float[4][SPRITES.length];
                for (int index = 0; index < SPRITES.length; index++) {
                    spriteBounds[0][index] = sprites[index].getU0(); spriteBounds[1][index] = sprites[index].getV0();
                    spriteBounds[2][index] = sprites[index].getU1(); spriteBounds[3][index] = sprites[index].getV1();
                }
            }
            var solid = upload(data.faces().stream().filter(face -> face.texture() != DistantTerrainMesh.WATER).toList());
            var water = upload(data.faces().stream().filter(face -> face.texture() == DistantTerrainMesh.WATER).toList());
            tiles.put(key, new Tile(data.x(), data.z(), solid, water));
            if (tiles.size() == 1) LogUtils.getLogger().info("Distant terrain: first GPU tile uploaded ({} faces)", data.faces().size());
            if (System.nanoTime() >= deadline) break;
        }
    }
    private static VertexBuffer upload(List<DistantTerrainMesh.Quad> faces) {
        if (faces.isEmpty()) return null;
        var builder = uploadBuilder; builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (var face : faces) {
            float[] positions = face.positions(); int colour = face.colour();
            float ax = positions[3] - positions[0], ay = positions[4] - positions[1], az = positions[5] - positions[2];
            float bx = positions[6] - positions[0], by = positions[7] - positions[1], bz = positions[8] - positions[2];
            float nx = ay * bz - az * by, ny = az * bx - ax * bz;
            int uAxis = Math.abs(nx) > .001F ? 2 : 0, vAxis = Math.abs(ny) > .001F ? 2 : 1;
            for (int vertex = 0; vertex < 4; vertex++) {
                float u = positions[vertex * 3 + uAxis], v = positions[vertex * 3 + vAxis] * (vAxis == 1 ? -1 : 1);
                builder.vertex(positions[vertex * 3], positions[vertex * 3 + 1], positions[vertex * 3 + 2]).uv(u, v)
                        // Alpha packs the sprite and sky-light indices into two nibbles.
                        .color(colour >> 16 & 255, colour >> 8 & 255, colour & 255, face.texture() | face.skyLight() << 4).endVertex();
            }
        }
        var buffer = new VertexBuffer(VertexBuffer.Usage.STATIC); buffer.bind(); buffer.upload(builder.end()); VertexBuffer.unbind();
        return buffer;
    }
    private DistantTerrainRenderer() {}
}
