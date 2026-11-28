package com.sbancuz.plannh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import java.util.stream.DoubleStream;

import org.junit.jupiter.api.Test;

import com.sbancuz.plannh.gui.GuiHelper;

/**
 * The two display rules that mean something rather than merely describe the formatter: a running
 * machine must not read as idle, and a big rate must not read as a bucket count.
 */
class RateDisplayTest {

    @Test
    void aRunningMachineNeverDisplaysAsZero() {
        // AUTO returns exact fractional counts, and a fast machine feeding a long assembly line
        // lands well under a hundredth - where two decimals would round it to "0" and claim the
        // machine is idle.
        for (final double count : new double[] { 4e-3, 1e-3, 1e-4, 1e-5 }) {
            assertNotEquals("0", GuiHelper.formatCount(count), "count " + count + " rendered as idle");
        }
    }

    @Test
    void bigRatesFollowTheSuffixTableTheRestOfThePackUses() {
        // NEI and AE2 both abbreviate with "kMGTPE" over 1000, so G is a billion here too - and B
        // stays what fluid amounts use for buckets rather than becoming a second spelling of giga.
        assertEquals("1.2k", GuiHelper.formatRate(1200f));
        assertEquals("1.5G", GuiHelper.formatRate(1.5e9f));
        assertEquals("2.0T", GuiHelper.formatRate(2e12f));
    }

    @Test
    void slotRateFitsFourCharactersAndReadsBackClose() {
        final double[] boundaries = { 0.0049, 0.005, 0.0051, 0.995, 0.996, 1, 9.95, 99.94, 99.96, 999.4, 999.5, 9_949,
            9_950, 999_499, 999_500, 1e18 };
        final Random random = new Random(7);
        final double[] generated = DoubleStream.generate(() -> Math.pow(10, random.nextDouble() * 22 - 4))
            .limit(2_000)
            .toArray();
        final double[] rates = DoubleStream.concat(DoubleStream.of(boundaries), DoubleStream.of(generated))
            .toArray();

        for (final double rate : rates) {
            final String text = GuiHelper.formatSlotRate(rate);
            assertTrue(text.length() <= 4, rate + " rendered as " + text);
            if (text.startsWith("<")) {
                assertTrue(rate < 0.01, rate + " rendered as " + text);
                continue;
            }
            final double shown = readSlotRate(text);
            if (rate < 1) assertTrue(Math.abs(shown - rate) <= 0.005 + 1e-12, rate + " rendered as " + text);
            else assertTrue(Math.abs(shown - rate) <= rate * 0.05, rate + " rendered as " + text);
        }
    }

    private static double readSlotRate(final String text) {
        final String suffixes = "kMGTPE";
        final int power = suffixes.indexOf(text.charAt(text.length() - 1)) + 1;
        final String digits = power == 0 ? text : text.substring(0, text.length() - 1);
        return Double.parseDouble(digits) * Math.pow(1000, power);
    }
}
