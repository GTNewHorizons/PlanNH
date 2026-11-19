package com.sbancuz.plannh.data.setting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.effect.EffectResult;
import com.sbancuz.plannh.data.serialization.Serializer;

/** Every output of a machine shares one extent, so the rate targets of a machine are "at least" targets. */
class RateTargetTest {

    @Test
    void theExtentMeetsEveryTargetAndOneTargetExactly() {
        final Random random = new Random(11);
        for (int round = 0; round < 500; round++) {
            final double[] outQty = quantities(random);
            final RateTarget target = new RateTarget();
            for (int output = 0; output < outQty.length; output++) {
                if (random.nextBoolean()) target.setRate(output, random.nextDouble() * 50);
            }

            final double extent = target.extent(outQty);
            final RateTarget.Live binding = target.binding(outQty);
            for (final RateTarget.Live live : target.liveTargets(outQty)) {
                assertTrue(extent * live.quantity() >= live.rate() * (1 - 1e-12), "a target below its rate");
            }
            if (binding == null) {
                assertEquals(0, extent);
                continue;
            }
            assertEquals(binding.rate(), extent * binding.quantity(), binding.rate() * 1e-12);
        }
    }

    @Test
    void aTargetOfZeroIsNoTarget() {
        final RateTarget target = new RateTarget();
        target.setRate(0, 5);
        target.setRate(0, 0);

        assertEquals(0, target.rate(0));
        assertTrue(
            target.liveTargets(new double[] { 1 })
                .isEmpty());
    }

    @Test
    void outputTargetsSurviveASave() {
        final MachineProfile profile = MachineProfile.builder("rate-targets")
            .effect((cfg, ctx) -> new EffectResult(10, 0, 1))
            .build();
        final MachineConfig config = new MachineConfig(profile);
        config.getRates()
            .setRate(1, 2.5);
        config.getRates()
            .setRate(0, 7);

        final MachineConfig loaded = Serializer.GSON.fromJson(Serializer.GSON.toJson(config), MachineConfig.class);

        assertNotNull(loaded);
        assertEquals(
            2.5,
            loaded.getRates()
                .rate(1));
        assertEquals(
            7,
            loaded.getRates()
                .rate(0));
    }

    private static double[] quantities(final Random random) {
        final double[] quantities = new double[random.nextInt(4)];
        for (int i = 0; i < quantities.length; i++) {
            // an output with a quantity of 0 has no live target
            quantities[i] = random.nextInt(4) == 0 ? 0 : random.nextDouble() * 100;
        }
        return quantities;
    }
}
