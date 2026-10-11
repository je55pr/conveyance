package dev.conveyance.world;

import net.minecraft.core.BlockPos;

/** Deterministic natural wells. No login-time placement or guaranteed starter oil. */
public final class OilFields {
    public static boolean at(long seed, int x, int z) {
        var site = siteAt(seed, x, z);
        return site != null && site.getX() == x && site.getZ() == z;
    }
    public static BlockPos siteAt(long seed, int x, int z) {
        return siteAt(seed, x, z, 2);
    }
    public static BlockPos siteAt(long seed, int x, int z, int generation) {
        int rx = Math.floorDiv(x, 160), rz = Math.floorDiv(z, 160);
        long hash = seed ^ rx * 0x9E3779B97F4A7C15L ^ rz * 0xC2B2AE3D27D4EB4FL;
        hash = (hash ^ hash >>> 30) * 0xBF58476D1CE4E5B9L; hash = (hash ^ hash >>> 27) * 0x94D049BB133111EBL; hash ^= hash >>> 31;
        if (generation >= 3 && Math.floorMod(hash >>> 8, generation >= 4 ? 15 : 3) != 0) return null;
        int cx = rx * 160 + 30 + (int) Math.floorMod(hash, 80), cz = rz * 160 + 30 + (int) Math.floorMod(hash >>> 16, 80);
        if (Math.abs(x - cx) <= 1 && Math.abs(z - cz) <= 1) return new BlockPos(cx, 0, cz);
        if (Math.abs(x - cx - 6) <= 1 && Math.abs(z - cz) <= 1) return new BlockPos(cx + 6, 0, cz);
        if (Math.abs(x - cx) <= 1 && Math.abs(z - cz - 6) <= 1) return new BlockPos(cx, 0, cz + 6);
        return null;
    }
    private OilFields() {}
}
