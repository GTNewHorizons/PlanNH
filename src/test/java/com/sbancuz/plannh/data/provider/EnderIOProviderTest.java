package com.sbancuz.plannh.data.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.steps.CoFHCompat;
import com.sbancuz.plannh.data.setting.Settings;

import crazypants.enderio.nei.VatRecipeHandler;

class EnderIOProviderTest {

    private static MachineProfile enderProfile;

    @BeforeAll
    static void initializeProfile() {
        ProviderTestFixture.bootstrap();
        enderProfile = new EnderIOProvider().getProfiles(ProviderTestFixture.allocate(VatRecipeHandler.class), 0)
            .getFirst();
    }

    @Test
    void childOverridesSurviveParentChangesAndResetRestoresDerivation() {
        final RecipeContext ctx = RecipeContext.builder()
            .property(CoFHCompat.RF_COST, 100L)
            .build();
        final MachineConfig cfg = new MachineConfig(enderProfile);
        cfg.set(EnderIOProvider.CAPACITOR, 0);
        cfg.set(Settings.RF_PER_TICK, 7);

        cfg.set(EnderIOProvider.CAPACITOR, 2);
        assertEquals(7, enderProfile.value(Settings.RF_PER_TICK, cfg, ctx));

        cfg.reset(Settings.RF_PER_TICK);
        final int derivedRate = enderProfile.value(Settings.RF_PER_TICK, cfg, ctx);
        assertNotEquals(7, derivedRate);
    }

}
