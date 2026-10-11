package dev.conveyance.factory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

/** Damage belongs to a whole building, never to each multiblock casing. */
public final class BuildingDamage extends SavedData {
    public record Part(BlockPos pos, BlockState state) {}
    private record Health(String block, float remaining) {}
    private final Map<Long, Health> health = new HashMap<>();
    private static final ThreadLocal<Boolean> REPLACING = ThreadLocal.withInitial(() -> false);
    public static boolean replacing() { return REPLACING.get(); }
    public static BuildingDamage get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(BuildingDamage::load, BuildingDamage::new, "conveyance_building_health");
    }
    private static BuildingDamage load(CompoundTag tag) {
        var data = new BuildingDamage(); var list = tag.getList("Buildings", 10);
        for (int i = 0; i < list.size(); i++) {
            var entry = list.getCompound(i); float value = entry.getFloat("Health");
            if (Float.isFinite(value) && value > 0) data.health.put(entry.getLong("Pos"), new Health(entry.getString("Block"), value));
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        var list = new ListTag();
        health.forEach((pos, value) -> { var entry = new CompoundTag(); entry.putLong("Pos", pos); entry.putString("Block", value.block); entry.putFloat("Health", value.remaining); list.add(entry); });
        tag.put("Buildings", list); return tag;
    }
    public static boolean building(BlockState state) {
        return !(state.getBlock() instanceof WreckBlock) && (state.getBlock() instanceof FactoryBlock
                || state.getBlock() instanceof MachinePartBlock || state.getBlock() instanceof MinerPartBlock
                || state.getBlock() instanceof RadarPartBlock || state.getBlock() instanceof PowerPolePartBlock
                || state.getBlock() instanceof StoneWallBlock);
    }
    public static int maximum(BlockState state) {
        if (state.getBlock() instanceof StoneWallBlock) return 350;
        if (state.getBlock() instanceof MachineBlock machine) return machine.kind == MachineKind.LASER_TURRET ? 1000 : machine.kind == MachineKind.GUN_TURRET ? 400 : 300;
        if (state.getBlock() instanceof MinerBlock) return 300;
        return state.getBlock() instanceof ConveyorBlock || state.getBlock() instanceof PipeBlock ? 150 : 200;
    }
    private static String key(BlockState state) { return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(); }
    public static float current(ServerLevel level, BlockPos pos) {
        BlockPos root = FactoryActions.root(level, pos); var state = level.getBlockState(root);
        var entry = get(level).health.get(root.asLong());
        return entry != null && entry.block.equals(key(state)) ? Math.min(maximum(state), entry.remaining) : maximum(state);
    }
    public static void clear(ServerLevel level, BlockPos root) {
        if (get(level).health.remove(root.asLong()) != null) get(level).setDirty();
        level.destroyBlockProgress(crackId(root), root, -1);
    }
    private static int crackId(BlockPos pos) { return -1 - (pos.hashCode() & 0x3fffffff); }
    public static boolean hurt(ServerLevel level, BlockPos pos, float amount) {
        if (!Float.isFinite(amount) || amount <= 0 || !level.hasChunkAt(pos) || !building(level.getBlockState(pos))) return false;
        BlockPos root = FactoryActions.root(level, pos); var state = level.getBlockState(root);
        if (state.getBlock() instanceof FactoryPartBlock) return false;
        var data = get(level); float remaining = current(level, root) - amount;
        if (remaining <= 0) { wreck(level, root); return true; }
        data.health.put(root.asLong(), new Health(key(state), remaining)); data.setDirty();
        level.destroyBlockProgress(crackId(root), root, Math.min(9, (int) (10 * (1 - remaining / maximum(state)))));
        level.sendParticles(ParticleTypes.CRIT, pos.getX() + .5, pos.getY() + .6, pos.getZ() + .5, 3, .2, .15, .2, .02);
        return true;
    }
    public static List<Part> parts(Level level, BlockPos root) {
        var result = new ArrayList<Part>(); var state = level.getBlockState(root);
        if (state.getBlock() instanceof MachineBlock machine) {
            for (int part = 0; part < machine.kind.parts(); part++) add(level, result, MachineBlock.partPosition(root, state.getValue(MachineBlock.FACING), machine.kind, part), root);
        } else if (state.getBlock() instanceof MinerBlock) {
            for (int part = 0; part < MinerBlock.PART_COUNT; part++) add(level, result, MinerBlock.partPosition(root, state.getValue(MinerBlock.FACING), part), root);
        } else if (state.getBlock() instanceof RadarBlock) {
            for (int part = 0; part < RadarBlock.PART_COUNT; part++) add(level, result, RadarBlock.partPosition(root, state.getValue(RadarBlock.FACING), part), root);
        } else if (state.getBlock() instanceof PowerPoleBlock) {
            for (int part = 0; part < PowerPoleBlock.HEIGHT; part++) add(level, result, root.above(part), root);
        } else if (state.getBlock() instanceof StoneWallBlock) {
            add(level, result, root, root); add(level, result, root.above(), root);
        } else result.add(new Part(root, state));
        return result;
    }
    private static void add(Level level, List<Part> parts, BlockPos pos, BlockPos root) {
        if (level.hasChunkAt(pos) && FactoryActions.root(level, pos).equals(root)) parts.add(new Part(pos, level.getBlockState(pos)));
    }
    private static void wreck(ServerLevel level, BlockPos root) {
        var entity = level.getBlockEntity(root);
        // Settle pooled fluid before storing a machine's local share.
        if (entity instanceof FluidEndpoint) FluidNetwork.settle(level);
        var pieces = parts(level, root);
        CompoundTag stored = entity == null ? new CompoundTag() : entity.saveWithoutMetadata();
        var loot = entity == null ? List.<net.minecraft.world.item.ItemStack>of() : FactoryPickups.block(entity).stream().skip(1).map(net.minecraft.world.item.ItemStack::copy).toList();
        BlockState original = level.getBlockState(root);
        REPLACING.set(true);
        try {
            // Root removal invokes the existing whole-building teardown, with drops suppressed.
            level.removeBlock(root, false);
            for (var piece : pieces) if (!level.getBlockState(piece.pos).isAir()) level.removeBlock(piece.pos, false);
            for (var piece : pieces) if (piece.pos.getY() == root.getY()) {
                level.setBlockAndUpdate(piece.pos, FactoryContent.WRECK.get().defaultBlockState());
                if (level.getBlockEntity(piece.pos) instanceof WreckEntity wreck) wreck.setRoot(root);
            }
            if (level.getBlockEntity(root) instanceof WreckEntity wreck) wreck.store(original, pieces, stored, loot);
        } finally { REPLACING.remove(); }
        clear(level, root);
        level.sendParticles(ParticleTypes.SMOKE, root.getX() + .5, root.getY() + .5, root.getZ() + .5, 18, .8, .35, .8, .025);
    }
    private BuildingDamage() {}
}
