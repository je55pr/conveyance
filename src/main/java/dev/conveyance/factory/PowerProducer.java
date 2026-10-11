package dev.conveyance.factory;

public interface PowerProducer {
    double outputPower();
    /** Capacity available this tick, independently of whether the network needs it. */
    default double availablePower() { return outputPower(); }
    /** Fuel and steam generators throttle to the demand remaining after solar. */
    default boolean consumesFuelPower() { return false; }
}
