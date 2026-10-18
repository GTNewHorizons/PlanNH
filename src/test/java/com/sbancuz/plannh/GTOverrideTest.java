package com.sbancuz.plannh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.sbancuz.plannh.data.provider.gregtech.GTNodeState;
import com.sbancuz.plannh.data.provider.gregtech.GTOverride;
import com.sbancuz.plannh.data.provider.gregtech.GTPresetApplier;
import com.sbancuz.plannh.data.provider.gregtech.StructureState;

import gregtech.api.enums.GTValues;
import gregtech.api.logic.ModifierKind;
import gregtech.api.logic.ProcessingSpec;
import gregtech.api.util.OverclockCalculator;

/**
 * Advanced mode is an override: a stored value replaces only its setting, and every other number
 * stays as the machine computed it. A separate calculator whose unset rows fall back to global
 * defaults would change untouched numbers when Advanced is ticked.
 */
class GTOverrideTest {

    /** A perfect-overclocking machine, where a stray override changes the numbers most. */
    private static ProcessingSpec spec() {
        return ProcessingSpec.builder()
            .overclock(4, 4)
            .build();
    }

    private static StructureState state() {
        return new StructureState(5, 1, 0, Map.of(ModifierKind.COIL, 5L));
    }

    /** A node with one override set, or none. */
    private static GTNodeState node(@Nullable final GTOverride override, final Object value) {
        final GTNodeState node = GTNodeState.load(new JsonObject());
        if (override != null) node.setOverride(override, value);
        return node;
    }

    private static OverclockCalculator build(final GTNodeState node) {
        final OverclockCalculator calc = GTSpecs.calculator(spec(), state(), GTValues.VP[1], 1024, 0);
        GTPresetApplier.applyOverrides(calc, node);
        return calc.setParallel(1)
            .setAmperageOC(true)
            .calculate();
    }

    /** The LCR perfect-overclocks. A node with no overrides must not change that. */
    @Test
    void noStoredKeysMeansTheMachinesOwnNumbers() {
        final OverclockCalculator machine = build(node(null, 0));
        final OverclockCalculator plain = new OverclockCalculator().setRecipeEUt(GTValues.VP[1])
            .setEUt(GTValues.V[5])
            .setDuration(1024)
            .setParallel(1)
            .setAmperageOC(true)
            .calculate();

        assertTrue(machine.getDuration() < plain.getDuration(), "the machine's perfect OC should still apply");
    }

    @Test
    void aStoredOverclockFactorReplacesTheMachines() {
        final OverclockCalculator machine = build(node(null, 0));
        final OverclockCalculator overridden = build(node(GTOverride.DURATION_PER_OC, 200));

        assertNotEquals(
            machine.getDuration(),
            overridden.getDuration(),
            "overriding the OC factor must change the result");
    }

    /** A set value is a value, so 0 is a valid tier-skip limit and not "unset". */
    @Test
    void zeroTierSkipsIsExpressible() {
        final OverclockCalculator noSkips = GTSpecs.calculator(spec(), state(), GTValues.V[6], 1024, 0);
        GTPresetApplier.applyOverrides(noSkips, node(GTOverride.MAX_TIER_SKIPS, 0));

        assertTrue(GTValues.V[6] > noSkips.getMaxAllowedRecipeEUt(), "a stored 0 must mean no skipping, not 'unset'");
    }

    /** A stored discount equal to the nominal default is a user value and must be applied. */
    @Test
    void aStoredValueEqualToTheDefaultIsStillApplied() {
        final OverclockCalculator overridden = build(node(GTOverride.EU_DISCOUNT, 50));
        final OverclockCalculator machine = build(node(null, 0));

        assertTrue(overridden.getConsumption() < machine.getConsumption(), "a 50% discount must halve the draw");
        assertEquals(machine.getDuration(), overridden.getDuration(), "an EU discount is not a speed change");
    }
}
