package com.sbancuz.plannh.data.provider.enderio;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.sbancuz.plannh.data.ChartMinimums;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.EffectComputer;
import com.sbancuz.plannh.data.effect.Effects;
import com.sbancuz.plannh.data.effect.steps.CoFHCompat;
import com.sbancuz.plannh.data.setting.Settings;

/**
 * The EnderIO node model, separate from the provider that reads the recipes.
 *
 * <p>
 * The provider references NEI's handler classes, and loading those loads LWJGL. This class has only
 * the model, so its arithmetic can be tested against EnderIO's numbers without a display.
 */
public final class EnderIOProfile {

    private EnderIOProfile() {}

    public static final String ID = "enderio";

    /** The chart floor key for the capacitor. */
    public static final String CAPACITOR_FLOOR = "eio_capacitor";

    /**
     * Duration and power draw, both from the capacitor. A field separate from the profile so tests can
     * run it headless: building a profile loads NEI's config for the burnable-override row, and NEI
     * loads LWJGL.
     */
    public static final EffectComputer EFFECT = Effects
        .durationFromFormula((ctx, config) -> durationTicks(ctx, config.nodeState(EnderIONodeState.class)))
        .withCostPerT(
            CoFHCompat.RF_PER_T,
            (current, config, ctx) -> energy(ctx) == 0 ? 0L
                : (long) rfPerTick(config.nodeState(EnderIONodeState.class)))
        .applyParallelism();

    /**
     * Built per call, not stored in a field: a profile reads NEI's config while it is built, and this
     * class loads long before a screen exists.
     */
    @Nonnull
    public static MachineProfile profile() {
        return MachineProfile.builder(ID, "EnderIO")
            .setting(Settings.MACHINES)
            .setting(Settings.TICK_MODIFIER)
            .nodeState(EnderIONodeState::load)
            // A recipe stores its energy, not its duration. Duration depends on the capacitor. Reading
            // a duration off the recipe list would fix it at the capacitor in use when the node was made.
            // The total energy is taken from the recipe unchanged, since a capacitor does not change it.
            .effect(EFFECT)
            .build();
    }

    /** Registers the capacitor as a chart floor, so it is chosen once per chart. */
    public static void registerChartMinimum() {
        ChartMinimums.register(
            ChartMinimums.Minimum
                .strongest(CAPACITOR_FLOOR, "Cap", 0, EnderIOCapacitors.highestTier(), EnderIOCapacitors::label));
    }

    /**
     * The recipe's energy divided by the capacitor's RF/t, which is all of EnderIO's timing model. A
     * named method, not inline in the effect chain, so tests can call it: running the chain loads
     * RecipePropertyAPI, whose static initializer builds a FluidStack and so requires a game with
     * registered fluids.
     */
    public static int durationTicks(@Nonnull final RecipeContext ctx, @Nullable final EnderIONodeState state) {
        return (int) (energy(ctx) / rfPerTick(state));
    }

    /** The RF/t the node's capacitor supplies to a recipe. */
    public static int rfPerTick(@Nullable final EnderIONodeState state) {
        return EnderIOCapacitors.rfPerTick(
            state != null ? state.capacitorTier()
                : ChartMinimums.floor(CAPACITOR_FLOOR, EnderIOCapacitors.highestTier()));
    }

    /**
     * The recipe's total energy. Zero for the Enchanter, which spends experience levels, so its RF/t is
     * set to 0 as well.
     */
    private static long energy(@Nonnull final RecipeContext ctx) {
        final Object total = ctx.properties()
            .get(CoFHCompat.RF_COST);
        return total instanceof final Number n ? n.longValue() : 0L;
    }
}
