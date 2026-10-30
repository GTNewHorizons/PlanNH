package com.sbancuz.plannh.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.sbancuz.plannh.data.serialization.Serializer;

class MachineConfigProfilePersistenceTest {

    @Test
    void profileIdSurvivesTheSerializerGsonRoundTrip() {
        final MachineProfile profile = profile("persisted");
        final MachineConfig original = new MachineConfig(profile);

        final JsonObject saved = Serializer.GSON.toJsonTree(original)
            .getAsJsonObject();
        final MachineConfig restored = Serializer.GSON.fromJson(saved, MachineConfig.class);

        assertEquals(
            "persisted",
            saved.get("profileId")
                .getAsString());
        assertEquals("persisted", restored.getProfileId());
        assertNull(restored.getProfile());
    }

    @Test
    void legacyConfigWithoutAProfileIdStillLoads() {
        final JsonObject saved = Serializer.GSON.toJsonTree(new MachineConfig(profile("legacy")))
            .getAsJsonObject();
        saved.remove("profileId");

        final MachineConfig restored = Serializer.GSON.fromJson(saved, MachineConfig.class);

        assertNull(restored.getProfileId());
        assertNull(restored.getProfile());
    }

    private static MachineProfile profile(final String id) {
        return MachineProfile.builder(id)
            .build();
    }
}
