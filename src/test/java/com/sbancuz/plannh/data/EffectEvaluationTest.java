package com.sbancuz.plannh.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sbancuz.plannh.data.effect.EffectResult;
import com.sbancuz.plannh.data.provider.ProviderTestFixture;
import com.sbancuz.plannh.data.setting.Settings;
import com.sbancuz.plannh.harness.TestIngredients;

class EffectEvaluationTest {

    @BeforeAll
    static void initializeMinecraftFixture() {
        ProviderTestFixture.bootstrap();
    }

    @Test
    void chanceOverridesReplaceTheMultiplierIncludingZero() {
        final MachineProfile profile = MachineProfile.builder("explicit-multipliers")
            .effect((cfg, ctx) -> {
                final EffectResult result = new EffectResult(10, 0, 1);
                result.setMultiplier(true, 0, 2d);
                result.setMultiplier(false, 0, 3d);
                return result;
            })
            .inputMultiplier((_, cfg, ctx, index) -> 10d + index)
            .outputMultiplier((_, cfg, ctx, index) -> 20d + index)
            .build();
        final MachineConfig config = new MachineConfig(profile);
        config.overrideChance(true, 0, 0d);
        config.overrideChance(false, 0, 0d);

        final EffectResult result = config.computeEffect(recipe(2, 2));

        assertEquals(0d, result.multiplier(true, 0));
        assertEquals(11d, result.multiplier(true, 1));
        assertEquals(0d, result.multiplier(false, 0));
        assertEquals(21d, result.multiplier(false, 1));
    }

    @Test
    void settingChangesProduceNewSnapshotsWithoutChangingOldResults() {
        final MachineProfile profile = MachineProfile.builder("live-settings")
            .derive(Settings.PARALLELS, (cfg, ctx) -> cfg.value(Settings.MACHINES, ctx) * 2)
            .effect((cfg, ctx) -> {
                final int machines = cfg.value(Settings.MACHINES, ctx);
                final int parallels = cfg.value(Settings.PARALLELS, ctx);
                return new EffectResult(machines + parallels, 0, 1);
            })
            .inputMultiplier((_, cfg, ctx, index) -> cfg.value(Settings.MACHINES, ctx))
            .build();
        final MachineConfig config = new MachineConfig(profile);
        config.set(Settings.MACHINES, 2);

        final EffectResult oldResult = config.computeEffect(recipe(1, 0));
        config.set(Settings.MACHINES, 4);
        final EffectResult newResult = config.computeEffect(recipe(1, 0));

        assertEquals(6, oldResult.durationTicks());
        assertEquals(2d, oldResult.multiplier(true, 0));
        assertEquals(12, newResult.durationTicks());
        assertEquals(4d, newResult.multiplier(true, 0));
    }

    private static RecipeContext recipe(final int inputs, final int outputs) {
        final RecipeContext.Builder builder = RecipeContext.builder();
        for (int i = 0; i < inputs; i++) builder.input(TestIngredients.port("input-" + i, 1));
        for (int i = 0; i < outputs; i++) builder.output(TestIngredients.port("output-" + i, 1));
        return builder.build();
    }

}
