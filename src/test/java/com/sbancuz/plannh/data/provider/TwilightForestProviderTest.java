package com.sbancuz.plannh.data.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.EffectResult;

import codechicken.nei.recipe.FurnaceRecipeHandler;
import sun.misc.Unsafe;

class TwilightForestProviderTest {

    private static final Unsafe UNSAFE = unsafe();

    @BeforeAll
    static void initializeMinecraftFixture() {
        ProviderTestFixture.bootstrap();
    }

    @Test
    void cinderBonusIsRestrictedToOreAndWoodLogInputs() {
        assertTrue(TwilightForestProvider.isBonusOreName("orePlanNHTest"));
        assertTrue(TwilightForestProvider.isBonusOreName("logWood"));
        assertFalse(TwilightForestProvider.isBonusOreName("dustPlanNHTest"));
        assertFalse(TwilightForestProvider.isBonusOreName("logRubber"));
    }

    @Test
    void cinderEffectUsesConfiguredExpectedDuration() {
        final MachineProfile profile = new TwilightForestProvider().getProfiles(allocate(FurnaceRecipeHandler.class), 0)
            .getFirst();
        final MachineConfig config = new MachineConfig(profile);
        config.set(TwilightForestProvider.CINDER_LOG_COUNT, 10);

        final EffectResult effect = config.computeEffect(
            RecipeContext.builder()
                .property(RecipePropertyAPI.DURATION_TICKS, 999)
                .build());

        assertEquals(40, effect.durationTicks());
    }

    @SuppressWarnings("unchecked")
    private static <T> T allocate(final Class<T> type) {
        try {
            return (T) UNSAFE.allocateInstance(type);
        } catch (final InstantiationException failure) {
            throw new AssertionError("Could not allocate native handler fixture", failure);
        }
    }

    private static Unsafe unsafe() {
        try {
            final Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (final ReflectiveOperationException failure) {
            throw new AssertionError("Could not access the test fixture runtime", failure);
        }
    }
}
