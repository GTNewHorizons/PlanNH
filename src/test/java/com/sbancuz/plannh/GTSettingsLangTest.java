package com.sbancuz.plannh;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sbancuz.plannh.data.provider.gregtech.GTOverride;
import com.sbancuz.plannh.data.provider.gregtech.GTSettings;
import com.sbancuz.plannh.data.setting.SettingDef;
import com.sbancuz.plannh.data.setting.Settings;

/**
 * SettingDef resolves its label in the constructor, so a setting with no lang entry draws the raw key,
 * such as "plannh.settings.gt_advanced", in the node panel. The raw key appears only in game, never in
 * a headless run.
 */
class GTSettingsLangTest {

    private static Set<String> langKeys() throws IOException {
        final Set<String> keys = new HashSet<>();
        try (InputStream in = GTSettingsLangTest.class.getResourceAsStream("/assets/plannh/lang/en_US.lang")) {
            if (in == null) fail("en_US.lang is not on the test classpath");
            for (final String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                final int eq = line.indexOf('=');
                if (eq > 0 && !line.startsWith("#")) keys.add(
                    line.substring(0, eq)
                        .trim());
            }
        }
        return keys;
    }

    /** The fixed rows, found by reflection so a new one is covered without editing this test, then the overrides. */
    private static List<String> gtRowKeys() throws IllegalAccessException {
        final List<String> keys = new ArrayList<>();
        for (final Field field : GTSettings.class.getDeclaredFields()) {
            if (field.getType() == String.class && Modifier.isStatic(field.getModifiers())
                && field.getName()
                    .endsWith("_ROW")) {
                field.setAccessible(true);
                keys.add((String) field.get(null));
            }
        }
        for (final GTOverride override : GTOverride.values()) keys.add(override.key);
        return keys;
    }

    private static List<String> sharedSettingKeys() throws IllegalAccessException {
        final List<String> keys = new ArrayList<>();
        for (final Field field : Settings.class.getDeclaredFields()) {
            if (SettingDef.class.isAssignableFrom(field.getType()) && Modifier.isStatic(field.getModifiers())) {
                keys.add(((SettingDef<?>) field.get(null)).getKey());
            }
        }
        return keys;
    }

    @Test
    void everyGtRowHasALabel() throws Exception {
        assertLabelled(gtRowKeys());
    }

    @Test
    void everySharedSettingHasALabel() throws Exception {
        assertLabelled(sharedSettingKeys());
    }

    private static void assertLabelled(final List<String> keys) throws IOException {
        final Set<String> lang = langKeys();
        final List<String> missing = new ArrayList<>();
        for (final String key : keys) {
            if (!lang.contains("plannh.settings." + key)) missing.add(key);
        }
        assertTrue(missing.isEmpty(), "rows with no lang entry, they will render as raw keys: " + missing);
    }

    @Test
    void theKeysAreActuallyBeingFound() throws Exception {
        assertTrue(gtRowKeys().size() >= 5, "reflection found no row keys, so the test proves nothing");
        assertTrue(sharedSettingKeys().size() >= 5, "reflection found no shared settings, so the test proves nothing");
        assertTrue(langKeys().contains("plannh.settings.machines"), "lang file parsed but looks wrong");
    }
}
