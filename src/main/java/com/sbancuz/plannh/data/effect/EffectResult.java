package com.sbancuz.plannh.data.effect;

import java.util.HashMap;
import java.util.Map;

import com.sbancuz.plannh.data.properties.RecipeProperty;

public class EffectResult {

    private int durationTicks;
    private double costPerT;
    private int throughputFactor;
    private RecipeProperty<? extends Number> energyRateProperty;
    private final Map<RecipeProperty<?>, Object> properties = new HashMap<>();

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
}
