package com.sbancuz.plannh.data.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.steps.CoFHCompat;
import com.sbancuz.plannh.data.setting.Settings;

import crazypants.enderio.nei.VatRecipeHandler;
import crazypants.enderio.power.Capacitors;
import sun.misc.Unsafe;

class EnderIOProviderTest {

    private static final Unsafe UNSAFE = unsafe();
    private static MachineProfile enderProfile;

    @BeforeAll
    static void initializeProfile() {
        enderProfile = new EnderIOProvider().getProfiles(allocate(VatRecipeHandler.class), 0)
            .getFirst();
    }

    @Test
    void capacitorChangesUpdateDerivedRateAndDuration() {
        final RecipeContext ctx = contextWithEnergy(100L);
        final MachineConfig cfg = new MachineConfig(enderProfile);
        cfg.set(Settings.EIO_CAPACITOR, 0);

        final int firstRate = enderProfile.value(Settings.RF_PER_TICK, cfg, ctx);
        cfg.set(Settings.EIO_CAPACITOR, 1);
        final int secondRate = enderProfile.value(Settings.RF_PER_TICK, cfg, ctx);

        assertEquals(Capacitors.VALUES[0].capacitor.getMaxEnergyExtracted(), firstRate);
        assertEquals(Capacitors.VALUES[1].capacitor.getMaxEnergyExtracted(), secondRate);
        assertEquals(ceil(100, secondRate), enderProfile.value(Settings.DURATION_TICKS, cfg, ctx));
    }

    @Test
    void childOverridesSurviveParentChangesAndResetRestoresDerivation() {
        final RecipeContext ctx = contextWithEnergy(100L);
        final MachineConfig cfg = new MachineConfig(enderProfile);
        cfg.set(Settings.EIO_CAPACITOR, 0);
        cfg.set(Settings.RF_PER_TICK, 7);

        cfg.set(Settings.EIO_CAPACITOR, 2);
        assertEquals(7, enderProfile.value(Settings.RF_PER_TICK, cfg, ctx));
        assertEquals(ceil(100, 7), enderProfile.value(Settings.DURATION_TICKS, cfg, ctx));
        assertEquals(
            ceil(100, 7),
            cfg.computeEffect(ctx)
                .durationTicks());

        cfg.reset(Settings.RF_PER_TICK);
        final int derivedRate = enderProfile.value(Settings.RF_PER_TICK, cfg, ctx);
        assertEquals(ceil(100, derivedRate), enderProfile.value(Settings.DURATION_TICKS, cfg, ctx));
    }

    private static RecipeContext contextWithEnergy(final long energy) {
        return RecipeContext.builder()
            .property(CoFHCompat.RF_COST, energy)
            .build();
    }

    private static int ceil(final long energy, final int rate) {
        return (int) ((energy - 1) / rate + 1);
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
