package dev.conveyance.factory;

public interface PowerProducer {
    int outputPower();
    /** Capacity available this tick, independently of whether the network needs it. */
    default double availablePower() { return outputPower(); }
}
