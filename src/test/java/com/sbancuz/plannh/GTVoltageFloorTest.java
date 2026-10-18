package com.sbancuz.plannh;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sbancuz.plannh.data.provider.gregtech.GTSettings;

import gregtech.api.enums.GTValues;

/**
 * A machine below the recipe's EU/t cannot run it, so those tiers are not options and a stored one is
 * raised to the minimum. There is no "off": a GT node always draws power, and an unset tier means the
 * recipe's minimum.
 */
class GTVoltageFloorTest {

    @Test
    void theMinimumTierIsTheOneThatCoversTheRecipe() {
        // 480 EU/t requires HV (512), not MV (128)
        assertEquals(3, GTSettings.minimumVoltageTier(480L));
        assertEquals(1, GTSettings.minimumVoltageTier(GTValues.V[1]));
        assertEquals(5, GTSettings.minimumVoltageTier(GTValues.V[5]));
    }

    @Test
    void anUnsetTierMeansTheRecipeMinimum() {
        assertEquals(3, GTSettings.voltageTier(480L, null));
    }

    /** A stored tier below the recipe's, or a name GregTech does not have, is raised to the recipe's tier. */
    @Test
    void aStoredTierBelowTheMinimumIsFloored() {
        assertEquals(3, GTSettings.voltageTier(480L, GTValues.VN[1]));
        assertEquals(3, GTSettings.voltageTier(480L, "OFF"));
    }

    @Test
    void aStoredTierAboveTheMinimumIsHonoured() {
        assertEquals(7, GTSettings.voltageTier(480L, GTValues.VN[7]));
    }
}
