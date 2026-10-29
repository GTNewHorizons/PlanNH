package com.sbancuz.plannh.data.effect;

import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.properties.RecipeProperty;
import com.sbancuz.plannh.data.setting.Settings;

@FunctionalInterface
public interface EffectComputer {

    /** Returns fresh evaluation state; steps should read and write that result, not re-evaluate the profile. */
    EffectResult compute(MachineConfig cfg, RecipeContext ctx);

    default EffectComputer andThen(final EffectStep step) {
        final EffectComputer first = this;
        return (cfg, ctx) -> step.apply(first.compute(cfg, ctx), cfg, ctx);
    }

    default EffectComputer withTotalCost(final RecipeProperty<? extends Number> costProperty,
        final EffectFunction<? extends Number> costFn) {
        return (cfg, ctx) -> {
            final EffectResult res = this.compute(cfg, ctx);
            final Number cost = costFn.apply(res, cfg, ctx);
            if (cost == null) throw new IllegalArgumentException("Missing total energy");
            final double total = cost.doubleValue();
            if (!Double.isFinite(total) || total < 0 || res.durationTicks() <= 0) {
                throw new IllegalArgumentException("Invalid total energy or duration");
            }
            res.energyPerT(total / res.durationTicks());
            res.properties()
                .put(costProperty, cost);
            return res;
        };
    }

    default EffectComputer withAmortizedCost(final RecipeProperty<? extends Number> costProperty,
        final EffectFunction<? extends Number> costFn) {
        return (cfg, ctx) -> {
            final EffectResult res = this.compute(cfg, ctx);
            final Number cost = costFn.apply(res, cfg, ctx);
            if (cost == null) throw new IllegalArgumentException("Missing total energy");
            final double total = cost.doubleValue();
            if (!Double.isFinite(total) || total < 0) {
                throw new IllegalArgumentException("Invalid total energy: " + total);
            }
            if (res.durationTicks() <= 0) {
                throw new IllegalArgumentException("Cannot amortize energy over a nonpositive duration");
            }
            final double rate = total / res.durationTicks();
            res.energyPerT(rate);
            res.energyRateProperty(costProperty);
            res.properties()
                .put(costProperty, rate);
            return res;
        };
    }

    default EffectComputer withCostPerT(final RecipeProperty<? extends Number> costProperty,
        final EffectFunction<? extends Number> costFn) {
        return (cfg, ctx) -> {
            final EffectResult res = this.compute(cfg, ctx);
            final Number cost = costFn.apply(res, cfg, ctx);
            if (cost == null) throw new IllegalArgumentException("Missing energy rate");
            final double value = cost.doubleValue();
            if (!Double.isFinite(value) || value < 0) {
                throw new IllegalArgumentException("Invalid energy rate: " + value);
            }
            res.energyPerT(value);
            res.energyRateProperty(costProperty);
            res.properties()
                .put(costProperty, cost);
            return res;
        };
    }

    default EffectComputer computeTotal(final RecipeProperty<? extends Number> costProperty) {
        return (cfg, ctx) -> {
            final EffectResult res = this.compute(cfg, ctx);
            final double total = res.energyPerT() * res.durationTicks() * res.throughputFactor();
            if (!Double.isFinite(total) || total < 0) {
                throw new IllegalArgumentException("Unrepresentable total energy: " + total);
            }
            res.properties()
                .put(costProperty, total);
            return res;
        };
    }

    default EffectComputer amortizeCost(final RecipeProperty<? extends Number> prop) {
        return (cfg, ctx) -> {
            final EffectResult res = this.compute(cfg, ctx);
            if (res.energyPerT() == 0 && res.durationTicks() > 0) {
                final Number total = ctx.getOrDefault(prop, null);
                if (total != null) {
                    final double value = total.doubleValue();
                    if (!Double.isFinite(value) || value < 0) {
                        throw new IllegalArgumentException("Invalid total energy: " + value);
                    }
                    if (value > 0) res.energyPerT(value / res.durationTicks());
                }
            }
            return res;
        };
    }

    default EffectComputer applyParallelism() {
        return (cfg, ctx) -> {
            final EffectResult res = this.compute(cfg, ctx);
            final int machines = cfg.value(Settings.MACHINES, ctx);
            final int parallels = cfg.value(Settings.PARALLELS, ctx);
            res.throughputFactor(Math.multiplyExact(Math.multiplyExact(res.throughputFactor(), machines), parallels));
            return res;
        };
    }

}
