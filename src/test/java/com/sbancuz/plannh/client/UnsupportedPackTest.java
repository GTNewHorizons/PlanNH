package com.sbancuz.plannh.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSortedMap;
import com.sbancuz.plannh.Compat;
import com.sbancuz.plannh.client.UnsupportedPackException.OutdatedMod;

import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.ModMetadata;

/**
 * Checks {@link UnsupportedPackException#findOutdatedMods} on the mcmod.info versions of real packs, with the
 * {@code -GTNH} and {@code -1.7.10} suffixes.
 */
class UnsupportedPackTest {

    /**
     * Per mod id, a {@link String#format} pattern that takes the minor and patch numbers, and the minimum's two
     * numbers.
     */
    private static final SortedMap<String, VersionFormat> VERSION_FORMATS = ImmutableSortedMap.of(
        Compat.IDs.GREGTECH,
        new VersionFormat("5.09.%d.%d", 54, 192),
        Compat.IDs.NEI,
        new VersionFormat("2.%d.%d-GTNH", 8, 148),
        Compat.IDs.MODULARUI2,
        new VersionFormat("2.%d.%d-1.7.10", 3, 55),
        Compat.IDs.GTNHLIB,
        new VersionFormat("0.%d.%d", 11, 5));

    @Test
    void gtnh284IsRejectedOnEveryLibrary() {
        final List<OutdatedMod> outdated = UnsupportedPackException.findOutdatedMods(
            createModContainers(
                Map.of(
                    Compat.IDs.GREGTECH,
                    "5.09.51.482",
                    Compat.IDs.NEI,
                    "2.8.44-GTNH",
                    Compat.IDs.MODULARUI2,
                    "2.2.18-1.7.10",
                    Compat.IDs.GTNHLIB,
                    "0.7.10")));
        assertEquals(4, outdated.size(), outdated.toString());
    }

    @Test
    void gtnh290Rc2IsAccepted() {
        final List<OutdatedMod> outdated = UnsupportedPackException.findOutdatedMods(
            createModContainers(
                Map.of(
                    Compat.IDs.GREGTECH,
                    "5.09.54.205",
                    Compat.IDs.NEI,
                    "2.8.155-GTNH",
                    Compat.IDs.MODULARUI2,
                    "2.3.92-1.7.10",
                    Compat.IDs.GTNHLIB,
                    "0.11.52")));
        assertTrue(outdated.isEmpty(), outdated.toString());
    }

    @Test
    void gtnh290Rc1IsRejectedOnGregTechAndNei() {
        // RC-1 predates both APIs the required mixins inject into
        final List<OutdatedMod> outdated = UnsupportedPackException.findOutdatedMods(
            createModContainers(
                Map.of(
                    Compat.IDs.GREGTECH,
                    "5.09.54.183",
                    Compat.IDs.NEI,
                    "2.8.145-GTNH",
                    Compat.IDs.MODULARUI2,
                    "2.3.91-1.7.10",
                    Compat.IDs.GTNHLIB,
                    "0.11.51")));
        assertEquals(2, outdated.size(), outdated.toString());
    }

    @Test
    void eachMinimumIsTheFirstSupportedRelease() {
        final List<OutdatedMod> atMinimum = UnsupportedPackException.findOutdatedMods(
            createModContainers(
                Map.of(
                    Compat.IDs.GREGTECH,
                    "5.09.54.192",
                    Compat.IDs.NEI,
                    "2.8.148-GTNH",
                    Compat.IDs.MODULARUI2,
                    "2.3.55-1.7.10",
                    Compat.IDs.GTNHLIB,
                    "0.11.5")));
        assertTrue(atMinimum.isEmpty(), atMinimum.toString());

        final List<OutdatedMod> belowMinimum = UnsupportedPackException.findOutdatedMods(
            createModContainers(
                Map.of(
                    Compat.IDs.GREGTECH,
                    "5.09.54.191",
                    Compat.IDs.NEI,
                    "2.8.147-GTNH",
                    Compat.IDs.MODULARUI2,
                    "2.3.54-1.7.10",
                    Compat.IDs.GTNHLIB,
                    "0.11.4")));
        assertEquals(4, belowMinimum.size(), belowMinimum.toString());
    }

    @Test
    void rejectsEveryVersionBelowTheMinimumAndNoOther() {
        final int CASES_PER_MOD = 500;
        final Random random = new Random(20261019);
        for (final Map.Entry<String, VersionFormat> entry : VERSION_FORMATS.entrySet()) {
            final VersionFormat format = entry.getValue();
            for (int i = 0; i < CASES_PER_MOD; i++) {
                final int minor = format.minimumMinor() - 1 + random.nextInt(3);
                final int patch = random.nextInt(2 * format.minimumPatch() + 1);
                final String version = String.format(format.pattern(), minor, patch);

                final boolean isOlder = minor < format.minimumMinor()
                    || (minor == format.minimumMinor() && patch < format.minimumPatch());
                final List<OutdatedMod> outdated = UnsupportedPackException
                    .findOutdatedMods(createModContainers(Map.of(entry.getKey(), version)));
                assertEquals(isOlder, !outdated.isEmpty(), version);
            }
        }
    }

    @Test
    void absentModsAreNotReported() {
        // GregTech is optional, so a pack without GregTech is judged on the other libraries alone
        final List<OutdatedMod> outdated = UnsupportedPackException.findOutdatedMods(
            createModContainers(
                Map.of(
                    Compat.IDs.NEI,
                    "2.8.155-GTNH",
                    Compat.IDs.MODULARUI2,
                    "2.3.92-1.7.10",
                    Compat.IDs.GTNHLIB,
                    "0.11.52")));
        assertTrue(outdated.isEmpty(), outdated.toString());
    }

    private static Map<String, ModContainer> createModContainers(final Map<String, String> versionsByModId) {
        final Map<String, ModContainer> modsById = new HashMap<>();
        for (final Map.Entry<String, String> entry : versionsByModId.entrySet()) {
            final ModMetadata metadata = new ModMetadata();
            metadata.modId = entry.getKey();
            metadata.name = entry.getKey();
            metadata.version = entry.getValue();
            modsById.put(entry.getKey(), new DummyModContainer(metadata));
        }
        return modsById;
    }

    private record VersionFormat(String pattern, int minimumMinor, int minimumPatch) {}
}
