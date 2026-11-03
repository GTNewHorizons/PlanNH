package com.sbancuz.plannh.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sbancuz.plannh.Compat;

import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.ModMetadata;

class UnsupportedPackTest {

    @Test
    void rejectsUnsupportedPackAndAcceptsSupportedPack() {
        assertEquals(
            4,
            UnsupportedPackException
                .findOutdatedMods(
                    mods(
                        Map.of(
                            Compat.IDs.GREGTECH,
                            "5.09.51.482",
                            Compat.IDs.NEI,
                            "2.8.44-GTNH",
                            Compat.IDs.MODULARUI2,
                            "2.2.18-1.7.10",
                            Compat.IDs.GTNHLIB,
                            "0.7.10")))
                .size());
        assertTrue(
            UnsupportedPackException
                .findOutdatedMods(
                    mods(
                        Map.of(
                            Compat.IDs.GREGTECH,
                            "5.09.54.205",
                            Compat.IDs.NEI,
                            "2.8.155-GTNH",
                            Compat.IDs.MODULARUI2,
                            "2.3.92-1.7.10",
                            Compat.IDs.GTNHLIB,
                            "0.11.52")))
                .isEmpty());
    }

    private static Map<String, ModContainer> mods(final Map<String, String> versions) {
        final Map<String, ModContainer> mods = new HashMap<>();
        versions.forEach((id, version) -> {
            final ModMetadata metadata = new ModMetadata();
            metadata.modId = id;
            metadata.name = id;
            metadata.version = version;
            mods.put(id, new DummyModContainer(metadata));
        });
        return mods;
    }
}
