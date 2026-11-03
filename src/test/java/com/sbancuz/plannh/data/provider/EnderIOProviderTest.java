package com.sbancuz.plannh.data.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.steps.CoFHCompat;
import com.sbancuz.plannh.data.setting.Settings;

import crazypants.enderio.EnderIO;
import crazypants.enderio.material.ItemCapacitor;
import crazypants.enderio.nei.VatRecipeHandler;
import crazypants.enderio.power.Capacitors;

class EnderIOProviderTest {

    private static MachineProfile enderProfile;
    private static ItemCapacitor originalCapacitorItem;

    @BeforeAll
    static void initializeProfile() {
        ProviderTestFixture.bootstrap();
        originalCapacitorItem = EnderIO.itemBasicCapacitor;
        if (EnderIO.itemBasicCapacitor == null) EnderIO.itemBasicCapacitor = new ItemCapacitor() {};
        enderProfile = new EnderIOProvider().getProfiles(ProviderTestFixture.allocate(VatRecipeHandler.class), 0)
            .getFirst();
    }

    @AfterAll
    static void restoreCapacitorItem() {
        EnderIO.itemBasicCapacitor = originalCapacitorItem;
    }

    @Test
    void childOverridesSurviveParentChangesAndResetRestoresDerivation() {
        final RecipeContext ctx = RecipeContext.builder()
            .property(CoFHCompat.RF_COST, 100L)
            .build();
        final MachineConfig cfg = new MachineConfig(enderProfile);
        cfg.set(EnderIOProvider.CAPACITOR, Capacitors.BASIC_CAPACITOR.getItemStack());
        cfg.set(Settings.RF_PER_TICK, 7);

        cfg.set(EnderIOProvider.CAPACITOR, Capacitors.ENDER_CAPACITOR.getItemStack());
        assertEquals(7, enderProfile.value(Settings.RF_PER_TICK, cfg, ctx));

        cfg.reset(Settings.RF_PER_TICK);
        final int derivedRate = enderProfile.value(Settings.RF_PER_TICK, cfg, ctx);
        assertNotEquals(7, derivedRate);
    }

}
