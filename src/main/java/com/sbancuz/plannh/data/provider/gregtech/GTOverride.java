package com.sbancuz.plannh.data.provider.gregtech;

import java.util.function.BiConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import javax.annotation.Nullable;

import gregtech.api.logic.ProcessingSpec.Quantity;
import gregtech.api.util.OverclockCalculator;

/**
 * A number a node can set over what its machine's spec gives, grouped by the {@link Quantity} it changes. Each reads
 * the machine's value from GregTech's calculator and writes the node's value into it, so the row and the maths share
 * one calculator.
 *
 * <p>
 * Percentages are what a row shows. A value left unset keeps the machine's exact double.
 */
public enum GTOverride {

    /** A cap below the machine's maximum. Applied to the resolved recipe, not the calculator. */
    PARALLEL(Quantity.PARALLEL, "parallels", 1, Integer.MAX_VALUE, null, null),
    SPEED(Quantity.DURATION, "speed", 10, 10000, c -> GTSettings.percent(1 / c.getDurationModifier()),
        (c, v) -> c.setDurationModifier(100.0 / Math.max(1, v))),
    EU_DISCOUNT(Quantity.EU_MODIFIER, "eut_discount", 0, 100, c -> GTSettings.percent(c.getEUtDiscount()),
        (c, v) -> c.setEUtDiscount(v / 100.0)),
    EU_PER_OC(Quantity.OVERCLOCK, "eut_increase_per_oc", 100, 1000, c -> GTSettings.percent(c.getEUtIncreasePerOC()),
        (c, v) -> c.setEUtIncreasePerOC(v / 100.0)),
    DURATION_PER_OC(Quantity.OVERCLOCK, "duration_decrease_per_oc", 100, 1000,
        c -> GTSettings.percent(c.getDurationDecreasePerOC()), (c, v) -> c.setDurationDecreasePerOC(v / 100.0)),
    LASER_OC(Quantity.OVERCLOCK, "laser_oc", OverclockCalculator::isLaserOC, OverclockCalculator::setLaserOC),
    NO_OVERCLOCK(Quantity.OVERCLOCK, "no_overclock", OverclockCalculator::isNoOverclock,
        OverclockCalculator::setNoOverclock),
    /** Applied after {@link #MAX_TIER_SKIPS}, so ticking it wins over a typed cap. */
    MAX_TIER_SKIPS(Quantity.TIER_SKIPS, "max_tier_skips", 0, 10, c -> Math.min(10, c.getMaxTierSkips()),
        OverclockCalculator::setMaxTierSkips),
    UNLIMITED_SKIPS(Quantity.TIER_SKIPS, "unlimited_skips", c -> c.getMaxTierSkips() == Integer.MAX_VALUE, (c, on) -> {
        if (on) c.setUnlimitedTierSkips();
        else if (c.getMaxTierSkips() == Integer.MAX_VALUE)
            c.setMaxTierSkips(OverclockCalculator.DEFAULT_MAX_TIER_SKIPS);
    }),
    MACHINE_HEAT(Quantity.HEAT, "machine_heat", 0, 100000, OverclockCalculator::getMachineHeat,
        OverclockCalculator::setMachineHeat),
    /** Zero for a machine without heat, since such a recipe's special value stores something else. */
    RECIPE_HEAT(Quantity.HEAT, "recipe_heat", 0, 100000, OverclockCalculator::getRecipeHeat,
        OverclockCalculator::setRecipeHeat),
    HEAT_OC(Quantity.HEAT, "heat_oc", OverclockCalculator::isHeatOC, OverclockCalculator::setHeatOC),
    HEAT_DISCOUNT(Quantity.HEAT, "heat_discount", OverclockCalculator::isHeatDiscount,
        OverclockCalculator::setHeatDiscount),
    /** GregTech's heat discount base, per 900K of headroom. */
    HEAT_DISCOUNT_MULT(Quantity.HEAT, "heat_discount_mult", 0, 200,
        c -> GTSettings.percent(c.getHeatDiscountMultiplier()), (c, v) -> c.setHeatDiscountMultiplier(v / 100.0));

    public final Quantity quantity;
    /** Its key within the quantity's group in a save, and its label's lang key. */
    public final String key;
    public final int min;
    public final int max;
    @Nullable
    private final ToIntFunction<OverclockCalculator> readInt;
    @Nullable
    private final ObjIntConsumer<OverclockCalculator> applyInt;
    @Nullable
    private final Predicate<OverclockCalculator> readFlag;
    @Nullable
    private final BiConsumer<OverclockCalculator, Boolean> applyFlag;

    GTOverride(final Quantity quantity, final String key, final int min, final int max,
        @Nullable final ToIntFunction<OverclockCalculator> read,
        @Nullable final ObjIntConsumer<OverclockCalculator> apply) {
        this.quantity = quantity;
        this.key = key;
        this.min = min;
        this.max = max;
        this.readInt = read;
        this.applyInt = apply;
        this.readFlag = null;
        this.applyFlag = null;
    }

    GTOverride(final Quantity quantity, final String key, final Predicate<OverclockCalculator> read,
        final BiConsumer<OverclockCalculator, Boolean> apply) {
        this.quantity = quantity;
        this.key = key;
        this.min = 0;
        this.max = 1;
        this.readInt = null;
        this.applyInt = null;
        this.readFlag = read;
        this.applyFlag = apply;
    }

    public boolean isFlag() {
        return readFlag != null;
    }

    /** The machine's number, as the row shows it. */
    public int machineInt(final OverclockCalculator calculator) {
        return readInt == null ? 0 : readInt.applyAsInt(calculator);
    }

    public boolean machineFlag(final OverclockCalculator calculator) {
        return readFlag != null && readFlag.test(calculator);
    }

    /** Writes the node's value. {@link #PARALLEL} is not a calculator value and is left alone. */
    public void apply(final OverclockCalculator calculator, final Object value) {
        if (applyFlag != null && value instanceof final Boolean on) applyFlag.accept(calculator, on);
        else if (applyInt != null && value instanceof final Integer number) applyInt.accept(calculator, number);
    }
}
