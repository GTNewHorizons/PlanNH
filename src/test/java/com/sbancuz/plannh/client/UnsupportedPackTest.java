package com.sbancuz.plannh.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
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
 * Checks {@link UnsupportedPackException#findOutdatedMods} on version strings in each mod's mcmod.info format, with
 * the {@code -GTNH} and {@code -1.7.10} suffixes.
 */
class UnsupportedPackTest {

    /**
     * Per mod id, a {@link String#format} pattern that takes the minor and patch numbers, and the first supported
     * release.
     */
    private static final SortedMap<String, VersionFormat> VERSION_FORMATS = ImmutableSortedMap.of(
        Compat.IDs.GREGTECH,
        new VersionFormat("5.09.%d.%d", new Release(54, 192)),
        Compat.IDs.NEI,
        new VersionFormat("2.%d.%d-GTNH", new Release(8, 148)),
        Compat.IDs.MODULARUI2,
        new VersionFormat("2.%d.%d-1.7.10", new Release(3, 55)),
        Compat.IDs.GTNHLIB,
        new VersionFormat("0.%d.%d", new Release(11, 5)));

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
    void rejectsEveryVersionBelowTheMinimumAndNoOther() {
        final int CASES_PER_MOD = 500;
        final Random random = new Random(20261019);
        for (final Map.Entry<String, VersionFormat> entry : VERSION_FORMATS.entrySet()) {
            final Release minimum = entry.getValue()
                .minimum();
            // The boundary cases are one patch either side of the minimum, a lower minor with a higher patch, and a
            // higher minor with patch 0.
            final List<Release> releases = new ArrayList<>(
                List.of(
                    new Release(minimum.minor(), minimum.patch() - 1),
                    minimum,
                    new Release(minimum.minor() - 1, 2 * minimum.patch()),
                    new Release(minimum.minor() + 1, 0)));
            for (int i = 0; i < CASES_PER_MOD; i++) {
                releases
                    .add(new Release(minimum.minor() - 1 + random.nextInt(3), random.nextInt(2 * minimum.patch() + 1)));
            }

            for (final Release release : releases) {
                final String version = entry.getValue()
                    .format(release);
                final List<OutdatedMod> outdated = UnsupportedPackException
                    .findOutdatedMods(createModContainers(Map.of(entry.getKey(), version)));
                assertEquals(release.isOlderThan(minimum), !outdated.isEmpty(), version);
            }
        }
    }

    @Test
    void absentModIsNeverReported() {
        for (final String absentModId : VERSION_FORMATS.keySet()) {
            final Map<String, String> versionsByModId = new HashMap<>();
            for (final Map.Entry<String, VersionFormat> entry : VERSION_FORMATS.entrySet()) {
                if (entry.getKey()
                    .equals(absentModId)) continue;
                versionsByModId.put(
                    entry.getKey(),
                    entry.getValue()
                        .format(new Release(0, 0)));
            }

            final List<OutdatedMod> outdated = UnsupportedPackException
                .findOutdatedMods(createModContainers(versionsByModId));
            assertEquals(VERSION_FORMATS.size() - 1, outdated.size(), outdated.toString());
            assertTrue(
                outdated.stream()
                    .noneMatch(
                        mod -> mod.name()
                            .equals(absentModId)),
                outdated.toString());
        }
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

    private record Release(int minor, int patch) {

        boolean isOlderThan(final Release other) {
            return minor < other.minor || (minor == other.minor && patch < other.patch);
        }
    }

    private record VersionFormat(String pattern, Release minimum) {

        String format(final Release release) {
            return String.format(pattern, release.minor(), release.patch());
        }
    }
}
