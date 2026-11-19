package com.sbancuz.plannh.data.effect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleSupplier;

import com.sbancuz.plannh.data.flowchart.Port;
import com.sbancuz.plannh.data.properties.RecipeProperty;

/**
 * Per-evaluation quantities. Port multipliers are recipe-relative, independent of throughput and tick scaling. After
 * {@code MachineProfile.computeEffect()}, a port's multiplier is the port's chance per craft.
 */
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

    public void setMultiplier(final boolean input, final int index, final double value) {
        (input ? inputMultipliers : outputMultipliers).put(index, value);
    }

    public double[] quantities(final boolean input, final List<Port<?>> ports) {
        final double[] quantities = new double[ports.size()];
        for (int i = 0; i < quantities.length; i++) {
            quantities[i] = quantity(input, i, ports.get(i));
        }
        return quantities;
    }

    /** What one operation consumes or produces of {@code port}, throughput factor included. */
    public double quantity(final boolean input, final int index, final Port<?> port) {
        return Math.max(0d, port.getAmount()) * multiplier(input, index) * throughputFactor;
    }

    public double multiplier(final boolean input, final int index) {
        return (input ? inputMultipliers : outputMultipliers).getOrDefault(index, 1d);
    }

    public double multiplier(final boolean input, final int index, final DoubleSupplier fallback) {
        final Double value = (input ? inputMultipliers : outputMultipliers).get(index);
        return value == null ? fallback.getAsDouble() : value;
    }
}
