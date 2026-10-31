package com.sbancuz.plannh.data.effect;

import java.util.function.BiPredicate;
import java.util.function.ToIntBiFunction;

import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.properties.RecipeProperty;
import com.sbancuz.plannh.data.setting.IntegerSettingDef;
import com.sbancuz.plannh.data.setting.Settings;

public final class Effects {

    private Effects() {}

    public static EffectComputer constant(final int durationTicks, final long energyPerTick) {
        return (cfg, ctx) -> new EffectResult(durationTicks, energyPerTick, 1);
    }

    public static EffectComputer durationFromHandler() {
        return duration((cfg, ctx) -> ctx.get(RecipePropertyAPI.DURATION_TICKS));
    }

    /** Sets duration directly, without preferring an unrelated handler duration. */
    public static EffectComputer duration(final ToIntBiFunction<MachineConfig, RecipeContext> formula) {
        return (cfg, ctx) -> new EffectResult(formula.applyAsInt(cfg, ctx), 0, 1);
    }

    public static EffectComputer durationFromSetting(final IntegerSettingDef setting) {
        return duration((cfg, ctx) -> cfg.value(setting, ctx));
    }

    public static EffectComputer durationFromConfigOrProperty() {
        return firstOf(durationFromSetting(Settings.DURATION_TICKS), durationFromHandler());
    }

    public static EffectComputer durationFromTotal(final RecipeProperty<? extends Number> prop,
        final IntegerSettingDef rateKey) {
        return firstOf(durationFromHandler(), duration((cfg, ctx) -> {
            final int rate = cfg.value(rateKey, ctx);
            final Number total = ctx.getOrDefault(prop, null);
            return rate > 0 && total != null && total.longValue() > 0 ? Math.max(1, (int) (total.longValue() / rate))
                : 0;
        }));
    }

    public static EffectComputer durationFromFormula(final ToIntBiFunction<MachineConfig, RecipeContext> formula) {
        return firstOf(durationFromHandler(), duration((cfg, ctx) -> Math.max(1, formula.applyAsInt(cfg, ctx))));
    }

    public static EffectComputer onlyIf(final BiPredicate<MachineConfig, RecipeContext> condition,
        final EffectComputer delegate) {
        final EffectComputer fallback = durationFromHandler();
        return (cfg, ctx) -> condition.test(cfg, ctx) ? delegate.compute(cfg, ctx) : fallback.compute(cfg, ctx);
    }

    public static EffectComputer clearCost() {
        return durationFromHandler();
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
