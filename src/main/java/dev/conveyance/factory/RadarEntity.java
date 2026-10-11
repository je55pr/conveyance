package dev.conveyance.factory;

import dev.conveyance.map.WorldMapData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class RadarEntity extends PoweredMachineEntity {
    // Factorio explores 29x29 sectors of 32 tiles. Each sector here spans 2x2
    // Minecraft chunks, giving exactly 928x928 blocks rather than an odd 16-block grid.
    public static final int SECTOR_RANGE = 14, SECTOR_BLOCKS = 32, DEMAND_KW = 300;
    public static final int AREA_CHUNKS = (SECTOR_RANGE * 2 + 1) * 2;
    public static final int AREA_BLOCKS = AREA_CHUNKS * 16;
    private record Offset(int x, int z) {}
    private static final List<Offset> SCAN = scanOrder();
    private int cursor;
    private boolean recovered;
    public RadarEntity(BlockPos pos, BlockState state) { super(FactoryContent.RADAR_ENTITY.get(), pos, state, DEMAND_KW, 40); }
    private static List<Offset> scanOrder() {
        var result = new ArrayList<Offset>();
        for (int x = -SECTOR_RANGE * 2; x < (SECTOR_RANGE + 1) * 2; x++)
            for (int z = -SECTOR_RANGE * 2; z < (SECTOR_RANGE + 1) * 2; z++) result.add(new Offset(x, z));
        // The central sector has four chunks, centred between offsets 0 and 1.
        result.sort(Comparator.<Offset>comparingInt(offset -> Math.max(Math.abs(offset.x * 2 - 1), Math.abs(offset.z * 2 - 1)))
                .thenComparingInt(offset -> (offset.x * 2 - 1) * (offset.x * 2 - 1) + (offset.z * 2 - 1) * (offset.z * 2 - 1))
                .thenComparingInt(Offset::z).thenComparingInt(Offset::x));
        return List.copyOf(result);
    }
    public static int minChunk(int blockCoordinate) { return Math.floorDiv(blockCoordinate, SECTOR_BLOCKS) * 2 - SECTOR_RANGE * 2; }
    public static int maxChunkExclusive(int blockCoordinate) { return minChunk(blockCoordinate) + AREA_CHUNKS; }
    @Override public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) WorldMapData.get(server).radar(worldPosition, true);
    }
    @Override public double powerDistanceSquared(BlockPos pole) {
        int width = getBlockState().getValue(RadarBlock.MULTIBLOCK) ? RadarBlock.WIDTH : 1;
        BlockPos far = RadarBlock.partPosition(worldPosition, getBlockState().getValue(RadarBlock.FACING), (width - 1) * RadarBlock.WIDTH + width - 1);
        int x = Math.max(Math.min(worldPosition.getX(), far.getX()), Math.min(Math.max(worldPosition.getX(), far.getX()), pole.getX()));
        int z = Math.max(Math.min(worldPosition.getZ(), far.getZ()), Math.min(Math.max(worldPosition.getZ(), far.getZ()), pole.getZ()));
        return pole.distSqr(new BlockPos(x, worldPosition.getY(), z));
    }
    public static void tick(Level level, BlockPos pos, BlockState state, RadarEntity radar) {
        if (!(level instanceof ServerLevel server)) return;
        if (!state.getValue(RadarBlock.MULTIBLOCK) && level.getGameTime() % 20 == 0 && RadarBlock.expandLegacy(level, pos, state)) state = level.getBlockState(pos);
        boolean powered = PowerNetwork.fraction(level, pos) > 0;
        if (state.getValue(RadarBlock.LIT) != powered) RadarBlock.setPowered(level, pos, powered);
        if (!radar.advanceWork()) return;
        var map = WorldMapData.get(server);
        int x = Math.floorDiv(pos.getX(), SECTOR_BLOCKS) * 2, z = Math.floorDiv(pos.getZ(), SECTOR_BLOCKS) * 2;
        // Overlapping radars skip shared discoveries and focus on unknown tiles.
        for (int attempt = 0; attempt < SCAN.size(); attempt++) {
            Offset offset = SCAN.get(radar.cursor);
            radar.cursor = (radar.cursor + 1) % SCAN.size();
            if (map.known(x + offset.x, z + offset.z)) continue;
            map.reveal(server, x + offset.x, z + offset.z);
            radar.setChanged();
            return;
        }
        // Once explored, revisit one already-loaded chunk per cycle to show
        // changes to forests and factories without loading distant machinery.
        Offset offset = SCAN.get(radar.cursor);
        radar.cursor = (radar.cursor + 1) % SCAN.size();
        if (server.getChunkSource().getChunkNow(x + offset.x, z + offset.z) != null) map.reveal(server, x + offset.x, z + offset.z);
        radar.setChanged();
    }
    public int covered() { return level instanceof ServerLevel server ? WorldMapData.get(server).coverage(
            minChunk(worldPosition.getX()), minChunk(worldPosition.getZ()),
            maxChunkExclusive(worldPosition.getX()), maxChunkExclusive(worldPosition.getZ())) : 0; }
    public static int total() { return SCAN.size(); }
    public void recover() {
        if (!(level instanceof ServerLevel server) || recovered) return;
        recovered = true;
        WorldMapData.get(server).radar(worldPosition, false);
        ItemStack packed = new ItemStack(FactoryContent.RADAR_ITEM.get());
        BlockItem.setBlockEntityData(packed, FactoryContent.RADAR_ENTITY.get(), saveWithoutMetadata());
        Block.popResource(level, worldPosition, packed);
    }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putInt("ScanCursor", cursor); }
    @Override public void load(CompoundTag tag) { super.load(tag); cursor = Math.floorMod(tag.getInt("ScanCursor"), SCAN.size()); }
}
