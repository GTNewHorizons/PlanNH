package com.sbancuz.plannh.data.effect;

import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleSupplier;

import com.sbancuz.plannh.data.properties.RecipeProperty;

/** Per-evaluation quantities. Port multipliers are recipe-relative, independent of throughput and tick scaling. */
public class EffectResult {

    private int durationTicks;
    private double costPerT;
    private int throughputFactor;
    private RecipeProperty<? extends Number> energyRateProperty;
    private final Map<RecipeProperty<?>, Object> properties = new HashMap<>();
    private final Map<Integer, Double> inputMultipliers = new HashMap<>();
    private final Map<Integer, Double> outputMultipliers = new HashMap<>();

    public EffectResult(int durationTicks, double costPerT, int throughputFactor) {
        this.durationTicks = durationTicks;
        energyPerT(costPerT);
        this.throughputFactor = throughputFactor;
    }

    public int durationTicks() {
        return durationTicks;
    }

    public double energyPerT() {
        return costPerT;
    }

    public int throughputFactor() {
        return throughputFactor;
    }

    public void durationTicks(int value) {
        this.durationTicks = value;
    }

    public void energyPerT(double value) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException("Invalid energy per tick: " + value);
        }
        this.costPerT = value;
    }

    public RecipeProperty<? extends Number> energyRateProperty() {
        return energyRateProperty;
    }

    public void energyRateProperty(final RecipeProperty<? extends Number> value) {
        energyRateProperty = value;
    }

    public Map<RecipeProperty<?>, Object> properties() {
        return properties;
    }

    public void throughputFactor(int value) {
        this.throughputFactor = value;
    }

    public double inputMultiplier(final int index) {
        return inputMultipliers.getOrDefault(index, 1d);
    }

    public double inputMultiplier(final int index, final DoubleSupplier fallback) {
        final Double value = inputMultipliers.get(index);
        return value == null ? fallback.getAsDouble() : value;
    }

    public void inputMultiplier(final int index, final double value) {
        inputMultipliers.put(index, value);
    }

    public double outputMultiplier(final int index) {
        return outputMultipliers.getOrDefault(index, 1d);
    }

    public double outputMultiplier(final int index, final DoubleSupplier fallback) {
        final Double value = outputMultipliers.get(index);
        return value == null ? fallback.getAsDouble() : value;
    }

    public void outputMultiplier(final int index, final double value) {
        outputMultipliers.put(index, value);
    }
}
