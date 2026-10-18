package com.sbancuz.plannh.data.provider.gregtech;

import static com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil.formatNumber;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.util.StatCollector;

import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.EffectResult;
import com.sbancuz.plannh.data.machine.MachineVariant;
import com.sbancuz.plannh.data.provider.GTProvider;
import com.sbancuz.plannh.data.setting.Settings;

import gregtech.api.logic.ProcessingRun;
import gregtech.api.logic.ResolvedRecipe;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.OverclockCalculator;

/**
 * GregTech's implementation of {@link MachineVariant#run}: turns "Maceration Stack with HSS-G coils at IV" into a
 * duration, an EU/t draw and a parallel count, from the machine's ProcessingSpec or else GT's describer.
 *
 * <p>
 * A spec comes first, because GregTech checks at load that each machine runs as its spec computes, and a spec models
 * the structure around a multiblock. A describer matches every singleblock at no extra cost.
 */
public final class GTPresetApplier {

    private GTPresetApplier() {}

    /**
     * Machine's result for this recipe, called by {@code Effects.machineDriven}. A recipe GregTech would not start at
     * this structure returns the recipe's numbers unchanged, with GregTech's rejection reason. Null for a machine with
     * neither a spec nor a describer, or a recipe with no energy or duration for a describer to overclock.
     */
    @Nullable
    public static EffectResult run(@Nonnull final GTMachineIndex.MachineEntry entry, final RecipeContext ctx,
        final MachineConfig config, final EffectResult recipe) {
        final GTNodeState found = config.nodeState(GTNodeState.class);
        final GTNodeState state = found != null ? found : new GTNodeState();
        final int machines = config.get(Settings.MACHINES);
        final ResolvedRecipe resolved = GTSettings.resolved(ctx, state);
        if (resolved != null) {
            final ResolvedRecipe capped = state.getOverride(GTOverride.PARALLEL) instanceof final Integer cap
                ? resolved.capParallel(cap)
                : resolved;
            final ProcessingRun run = capped.calculate(applyOverrides(capped.toCalculator(), state));
            if (!run.result()
                .wasSuccessful())
                return recipe.rejectedBecause(
                    run.result()
                        .getDisplayString());
            return effect(run, machines);
        }
        if (GTSettings.inUncalculatedMode(ctx, state)) {
            return recipe.rejectedBecause(StatCollector.translateToLocal("plannh.gt.mode_not_calculated"));
        }

        // singleblock: GregTech's describer, the same calculator the machine and NEI use
        final GTRecipe gtRecipe = ctx.getOrDefault(GTProvider.GT_RECIPE, null);
        if (entry.describer() == null || gtRecipe == null || gtRecipe.mEUt <= 0 || gtRecipe.mDuration <= 0) return null;
        final OverclockCalculator calculator = applyOverrides(
            entry.describer()
                .createCalculator(gtRecipe),
            state).calculate();
        return new EffectResult(calculator.getDuration(), calculator.getConsumption(), machines);
    }

    /** Node's numbers for a run GregTech computed, at this many machines. */
    @Nonnull
    public static EffectResult effect(final ProcessingRun run, final int machines) {
        return new EffectResult(run.ticks(), run.euPerTick(), run.parallel() * machines)
            .withOutputs(expectedOutput(run.output()), details(run));
    }

    /** Average output per parallel: success chance times yield per success. */
    private static double expectedOutput(final ProcessingRun.Output output) {
        return output.successChance() * output.yield();
    }

    /** Run's costs and gains besides EU/t, and its odds, listed beside the node's numbers. */
    @Nonnull
    private static List<String> details(final ProcessingRun run) {
        final List<String> details = new ArrayList<>();
        if (!run.output()
            .equals(ProcessingRun.Output.CERTAIN)) {
            details.add(
                StatCollector.translateToLocalFormatted(
                    "plannh.gt.output_odds",
                    GTSettings.percent(
                        run.output()
                            .successChance()),
                    GTSettings.percent(
                        run.output()
                            .yield())));
        }
        final ProcessingRun.RunEu eu = run.eu();
        if (eu.startup()
            .signum() != 0) {
            details.add(StatCollector.translateToLocalFormatted("plannh.gt.startup_eu", formatNumber(eu.startup())));
        }
        if (eu.perRun()
            .signum() != 0) {
            details.add(StatCollector.translateToLocalFormatted("plannh.gt.eu_per_run", formatNumber(eu.perRun())));
        }
        if (eu.generated()
            .signum() != 0) {
            details
                .add(StatCollector.translateToLocalFormatted("plannh.gt.eu_generated", formatNumber(eu.generated())));
        }
        return details;
    }

    /**
     * Lays the node's numbers over the machine's. Only the overrides set on the node are applied, so the rest keep the
     * machine's value at full precision, not the rounded percentage a row shows. Ticking Advanced changes no number
     * until a row is edited.
     */
    @Nonnull
    public static OverclockCalculator applyOverrides(final OverclockCalculator calculator, final GTNodeState state) {
        if (state.getAmps() != null) calculator.setAmperage(state.getAmps());
        // in declaration order, so unlimited tier skips are applied after a typed cap
        for (final GTOverride override : GTOverride.values()) {
            final Object value = state.getOverride(override);
            if (value != null) override.apply(calculator, value);
        }
        return calculator;
    }
}
