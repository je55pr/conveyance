package dev.conveyance.world;

/** Continuous cosmetic climate; changing colours never changes blocks or world layout. */
public final class SurfacePalette {
    public static int base(long seed, double x, double z) {
        double dry = dryness(seed, x, z);
        return dry < .65 ? mix(0x79AA62, 0xB7AA64, smooth(dry / .65)) : mix(0xB7AA64, 0xAA835A, smooth((dry - .65) / .35));
    }
    public static double dryness(long seed, double x, double z) { return clamp(.5 + .65 * noise(seed, x / 768, z / 768, 0xA4093822L) + .18 * noise(seed, x / 192, z / 192, 0x299F31D0L)); }
    public static double treeDensity(long seed, double x, double z) { return 1 - .9 * smooth(clamp((dryness(seed, x, z) - .15) / .65)); }
    public static double pollutionStrength(double amount) { return smooth(clamp(Math.log1p(Math.max(0, amount)) / Math.log1p(120))); }
    public static int overlay(int colour, double pollution, double territory) {
        colour = mix(colour, 0x7D7A73, pollutionStrength(pollution) * .8);
        double strength = smooth(clamp(territory));
        return mix(colour, mix(0x915B9A, 0xAF534D, strength), strength * .92);
    }
    public static int mix(int a, int b, double amount) {
        double t = clamp(amount);
        int r = (int) ((a >> 16 & 255) * (1 - t) + (b >> 16 & 255) * t);
        int g = (int) ((a >> 8 & 255) * (1 - t) + (b >> 8 & 255) * t);
        int blue = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return r << 16 | g << 8 | blue;
    }
    private static double noise(long seed, double x, double z, long salt) {
        int ix = (int) Math.floor(x), iz = (int) Math.floor(z); double tx = smooth(x - ix), tz = smooth(z - iz);
        return (1 - tz) * ((1 - tx) * value(seed, ix, iz, salt) + tx * value(seed, ix + 1, iz, salt))
                + tz * ((1 - tx) * value(seed, ix, iz + 1, salt) + tx * value(seed, ix + 1, iz + 1, salt));
    }
    private static double value(long seed, int x, int z, long salt) {
        long bits = seed ^ salt ^ x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL;
        bits = (bits ^ bits >>> 30) * 0xBF58476D1CE4E5B9L; bits = (bits ^ bits >>> 27) * 0x94D049BB133111EBL; bits ^= bits >>> 31;
        return (bits >>> 11) * 0x1.0p-53 * 2 - 1;
    }
    private static double clamp(double value) { return Math.max(0, Math.min(1, value)); }
    private static double smooth(double value) { return value * value * (3 - 2 * value); }
    private SurfacePalette() {}
}
