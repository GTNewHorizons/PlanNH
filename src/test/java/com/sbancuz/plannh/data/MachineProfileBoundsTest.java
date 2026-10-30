package com.sbancuz.plannh.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sbancuz.plannh.data.setting.Settings;

class MachineProfileBoundsTest {

    @Test
    void profileValueClampsExplicitAndDerivedValues() {
        final MachineProfile profile = MachineProfile.builder("bounded")
            .setting(Settings.CATALYST_ACCEL_CARD, 0, 3)
            .derive(Settings.CATALYST_ACCEL_CARD, (_, _) -> 10)
            .build();
        final RecipeContext context = RecipeContext.builder()
            .build();

        final MachineConfig explicit = new MachineConfig(profile);
        explicit.set(Settings.CATALYST_ACCEL_CARD, -10);
        assertEquals(0, profile.value(Settings.CATALYST_ACCEL_CARD, explicit, context));

        final MachineConfig derived = new MachineConfig(profile);
        assertEquals(3, profile.value(Settings.CATALYST_ACCEL_CARD, derived, context));
    }

    @Test
    void factoryProfilesHaveIndependentSpecializedRanges() {
        final MachineProfile.Template template = MachineProfile.template()
            .setting(Settings.CATALYST_ACCEL_CARD, 0, 3);
        final ProfileFactory<Integer> factory = template
            .asFactory((builder, maximum) -> builder.setting(Settings.CATALYST_ACCEL_CARD, 0, maximum));

        final MachineProfile limited = factory.builder("limited", 3)
            .build();
        final MachineProfile full = factory.builder("full", 5)
            .build();

        assertEquals(3, limited.maximum(Settings.CATALYST_ACCEL_CARD));
        assertEquals(5, full.maximum(Settings.CATALYST_ACCEL_CARD));

        final MachineConfig limitedConfig = new MachineConfig(limited);
        limitedConfig.set(Settings.CATALYST_ACCEL_CARD, 5);
        assertEquals(
            3,
            limited.value(
                Settings.CATALYST_ACCEL_CARD,
                limitedConfig,
                RecipeContext.builder()
                    .build()));

        final MachineConfig fullConfig = new MachineConfig(full);
        fullConfig.set(Settings.CATALYST_ACCEL_CARD, 5);
        assertEquals(
            5,
            full.value(
                Settings.CATALYST_ACCEL_CARD,
                fullConfig,
                RecipeContext.builder()
                    .build()));
    }
}
