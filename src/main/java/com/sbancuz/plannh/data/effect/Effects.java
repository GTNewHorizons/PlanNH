package com.sbancuz.plannh.data.effect;

import java.util.function.BiPredicate;
import java.util.function.ToIntBiFunction;

import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.properties.RecipeProperty;
import com.sbancuz.plannh.data.setting.IntegerSettingDef;

public final class Effects {

    private Effects() {}

    public static EffectComputer constant(final int durationTicks, final long energyPerTick) {
        return (cfg, ctx) -> new EffectResult(durationTicks, energyPerTick, 1);
    }

    public static EffectComputer durationFromHandler() {
        return (cfg, ctx) -> {
            final int duration = (int) (Integer) ctx.getOrDefault(RecipePropertyAPI.DURATION_TICKS, 0);
            return new EffectResult(duration, 0, 1);
        };
    }

    public static EffectComputer durationFromTotal(final RecipeProperty<? extends Number> prop,
                                                   final IntegerSettingDef rateKey) {
        return (cfg, ctx) -> {
            final Object dur = ctx.properties().get(RecipePropertyAPI.DURATION_TICKS);
            int duration = dur instanceof final Number n ? n.intValue() : 0;
            if (duration <= 0) {
                final int rate = cfg.getProfile().value(rateKey, cfg, ctx);
                final Number total = ctx.getOrDefault(prop, null);
                if (rate > 0 && total != null && total.longValue() > 0) {
                    duration = Math.max(1, (int) (total.longValue() / rate));
                }
            }
            return new EffectResult(duration, 0, 1);
        };
    }

    public static EffectComputer durationFromFormula(final ToIntBiFunction<MachineConfig, RecipeContext> formula) {
        return (cfg, ctx) -> {
            final Object dur = ctx.properties().get(RecipePropertyAPI.DURATION_TICKS);
            int duration = dur instanceof final Number n ? n.intValue() : 0;
            if (duration <= 0) {
                duration = Math.max(1, formula.applyAsInt(cfg, ctx));
            }
            return new EffectResult(duration, 0, 1);
        };
    }

    public static EffectComputer onlyIf(final BiPredicate<MachineConfig, RecipeContext> condition,
        final EffectComputer delegate) {
        return (cfg, ctx) -> {
            if (condition.test(cfg, ctx)) return delegate.compute(cfg, ctx);
            final Object dur = ctx.properties().get(RecipePropertyAPI.DURATION_TICKS);
            return new EffectResult(dur instanceof final Number n ? n.intValue() : 0, 0, 1);
        };
    }

    public static EffectComputer clearCost() {
        return (cfg, ctx) -> {
            final Object dur = ctx.properties().get(RecipePropertyAPI.DURATION_TICKS);
            final int d = dur instanceof final Number n ? n.intValue() : 0;
            return new EffectResult(d, 0, 1);
        };
    }

    public static EffectComputer firstOf(final EffectComputer... alternatives) {
        return (cfg, ctx) -> {
            for (final var alt : alternatives) {
                final var result = alt.compute(cfg, ctx);
                if (result.durationTicks() > 0) return result;
            }
            return alternatives[0].compute(cfg, ctx);
        };
    }
}
