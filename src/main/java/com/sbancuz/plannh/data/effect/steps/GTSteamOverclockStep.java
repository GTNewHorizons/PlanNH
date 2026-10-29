package com.sbancuz.plannh.data.effect.steps;

import static com.sbancuz.plannh.data.provider.GTProvider.EU_PER_TICK;
import static com.sbancuz.plannh.data.provider.GTProvider.TOTAL_EU;
import static com.sbancuz.plannh.data.provider.GTSteamProvider.STEAM_EU_PERT;
import static com.sbancuz.plannh.data.provider.GTSteamProvider.TOTAL_STEAM_EU;

import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.EffectComputer;
import com.sbancuz.plannh.data.effect.EffectResult;
import com.sbancuz.plannh.data.effect.EffectStep;
import com.sbancuz.plannh.data.setting.Settings;

public class GTSteamOverclockStep implements EffectStep, EffectComputer {

    private GTSteamOverclockStep() {}

    public static GTSteamOverclockStep create() {
        return new GTSteamOverclockStep();
    }

    @Override
    public EffectResult compute(final MachineConfig cfg, final RecipeContext ctx) {
        final Object dur = ctx.properties().get(RecipePropertyAPI.DURATION_TICKS);
        final int d = dur instanceof final Number n ? n.intValue() : 0;
        return apply(new EffectResult(d, 0, 1), cfg, ctx);
    }

    @Override
    public EffectResult apply(final EffectResult current, final MachineConfig cfg, final RecipeContext ctx) {
        final long recipeEUt = recipeEUt(ctx);
        final int recipeDuration = current.durationTicks();

        if (recipeEUt <= 0 || recipeDuration <= 0) {
            current.durationTicks(recipeDuration);
            current.energyPerT(recipeEUt);
            return current;
        }

        final int eutDiscount = cfg.getProfile()
            .value(Settings.STEAM_EUT_DISCOUNT, cfg, ctx);
        final int durationModifier = cfg.getProfile()
            .value(Settings.STEAM_DURATION_MODIFIER, cfg, ctx);
        final int parallels = cfg.getProfile()
            .value(Settings.PARALLELS, cfg, ctx);
        final int machines = cfg.getProfile()
            .value(Settings.MACHINES, cfg, ctx);

        final long steamPerTick = Math.multiplyExact(recipeEUt, eutDiscount) / 100;
        final long rawDuration = (long) recipeDuration * durationModifier / 100;
        final int duration = (int) Math.clamp(rawDuration, 1, Integer.MAX_VALUE);

        current.energyPerT(steamPerTick);
        current.durationTicks(duration);
        current.throughputFactor(Math.multiplyExact(parallels, machines));

        current.properties()
            .put(STEAM_EU_PERT, steamPerTick);
        current.properties()
            .put(TOTAL_STEAM_EU, Math.multiplyExact(steamPerTick, duration));

        // Clear raw EU values so they don't appear in summary
        current.properties()
            .put(EU_PER_TICK, 0L);
        current.properties()
            .put(TOTAL_EU, 0L);

        return current;
    }

    static long recipeEUt(final RecipeContext ctx) {
        final Long euPerTick = ctx.getOrDefault(EU_PER_TICK, null);
        if (euPerTick != null && euPerTick > 0) return euPerTick;
        final Long totalEu = ctx.getOrDefault(TOTAL_EU, null);
        if (totalEu != null && totalEu > 0) {
            final Object dur = ctx.properties().get(RecipePropertyAPI.DURATION_TICKS);
            final int d = dur instanceof final Number n ? n.intValue() : 0;
            if (d > 0) return totalEu / d;
        }
        return 0;
    }
}
