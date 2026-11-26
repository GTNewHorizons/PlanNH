package com.sbancuz.plannh.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sbancuz.plannh.data.effect.EffectResult;
import com.sbancuz.plannh.data.serialization.Serializer;
import com.sbancuz.plannh.data.setting.Settings;

/**
 * {@link MachineConfig#selectProfile} stores the settings and port chances of the profile before the switch, and
 * restores the stored edits of the new profile, also after a save.
 */
public final class MachineConfigProfileTest {

    private static final MachineProfile FIRST = profile("profile-test-first");
    private static final MachineProfile SECOND = profile("profile-test-second");

    @Test
    public void switchingBackRestoresTheEditsOfAProfile() {
        for (final Edits edits : cases()) {
            final MachineConfig config = new MachineConfig(FIRST);
            edits.apply(config);

            config.selectProfile(SECOND);
            assertFalse(config.isExplicit(Settings.MACHINES), "a profile without edits starts at the defaults");
            assertNull(config.chanceOverride(true, edits.port()));
            config.set(Settings.MACHINES, edits.machines() + 1);

            config.selectProfile(FIRST);
            edits.assertOn(config);
            config.selectProfile(SECOND);
            assertEquals(edits.machines() + 1, config.getOrDefault(Settings.MACHINES));
        }
    }

    @Test
    public void aSaveKeepsTheEditsOfAProfileThatTheNodeDoesNotUse() {
        for (final Edits edits : cases()) {
            final MachineConfig config = new MachineConfig(FIRST);
            edits.apply(config);
            config.selectProfile(SECOND);

            final MachineConfig loaded = Serializer.GSON.fromJson(Serializer.GSON.toJson(config), MachineConfig.class);
            loaded.setProfile(SECOND);
            loaded.selectProfile(FIRST);
            edits.assertOn(loaded);
        }
    }

    @Test
    public void aSaveWithoutProfileConfigsLoadsTheEditsIntoTheSavedProfile() {
        for (final Edits edits : cases()) {
            final MachineConfig config = new MachineConfig(FIRST);
            edits.apply(config);

            // a save before per-profile configs has the fields of `ProfileConfig` on the machine config
            final JsonObject saved = Serializer.GSON.toJsonTree(config)
                .getAsJsonObject();
            final JsonObject profileConfig = saved.remove("profileConfigs")
                .getAsJsonObject()
                .getAsJsonObject(FIRST.id());
            for (final Map.Entry<String, JsonElement> field : profileConfig.entrySet()) {
                saved.add(field.getKey(), field.getValue());
            }

            final MachineConfig loaded = Serializer.GSON.fromJson(saved, MachineConfig.class);
            loaded.setProfile(FIRST);
            edits.assertOn(loaded);
        }
    }

    /** Generated edits from a fixed seed, plus the bounds of the machine count and of a chance. */
    private static List<Edits> cases() {
        final List<Edits> cases = new ArrayList<>();
        cases.add(new Edits(1, 0, 0d, 0d));
        cases.add(new Edits(4096, 8, 1d, 1d));

        final Random random = new Random(42);
        for (int i = 0; i < 50; i++) {
            cases.add(new Edits(1 + random.nextInt(4096), random.nextInt(9), random.nextDouble(), random.nextDouble()));
        }
        return cases;
    }

    private static MachineProfile profile(final String id) {
        return MachineProfile.builder(id)
            .setting(Settings.MACHINES)
            .effect((cfg, ctx) -> new EffectResult(20, 0, 1))
            .build();
    }

    /** A machine count, and an input and an output chance on one port. */
    private record Edits(int machines, int port, double inputChance, double outputChance) {

        private void apply(final MachineConfig config) {
            config.set(Settings.MACHINES, machines);
            config.overrideChance(true, port, inputChance);
            config.overrideChance(false, port, outputChance);
        }

        private void assertOn(final MachineConfig config) {
            assertEquals(machines, config.getOrDefault(Settings.MACHINES));
            assertEquals(inputChance, config.chanceOverride(true, port));
            assertEquals(outputChance, config.chanceOverride(false, port));
        }
    }
}
