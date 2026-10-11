package dev.conveyance.factory;

/** Factorio 2.0 base movement power and pickup/drop spikes, converted from 60 Hz to 20 Hz. */
public final class InserterPower {
    public static final double IDLE_KW = 0.4, BURNER_PEAK_KW = 144;
    public static double rotationKW(int reach, boolean burner) { return burner ? 39 : reach == 2 ? 6 : 4.2; }
    public static double extensionKW(int reach, boolean burner) { return burner ? 105 : reach == 2 ? 15 : 10.5; }
    public static double peakKW(int reach, boolean burner) { return rotationKW(reach, burner) + extensionKW(reach, burner) + (burner ? 0 : IDLE_KW); }
    public static double movementKW(int reach, boolean burner, double work, double cycle, double speed) {
        double half = cycle / 2.0, step = Math.min(speed, Math.max(0, half - work % half));
        if (step <= 0) return 0;
        double spike = (reach == 2 && !burner ? 4 : 5) / 60.0 * 20;
        double extensionFraction = Math.max(0, Math.min(1, (spike - work % half) / step));
        return (rotationKW(reach, burner) + extensionKW(reach, burner) * extensionFraction) * step;
    }
    public static double fastMovementKW(double work, double cycle) {
        double phase = work % (cycle / 2);
        return 16.8 + 42 * Math.max(0, Math.min(1, 2.0 / 3 - phase));
    }
    private InserterPower() {}
}
