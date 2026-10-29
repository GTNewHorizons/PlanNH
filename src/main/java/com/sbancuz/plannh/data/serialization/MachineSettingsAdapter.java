package com.sbancuz.plannh.data.serialization;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.sbancuz.plannh.data.setting.SettingDef;
import com.sbancuz.plannh.data.setting.Settings;

/** Serializes the exact typed settings map used by {@code MachineConfig}. */
public final class MachineSettingsAdapter
    implements JsonSerializer<Map<SettingDef<?>, Object>>, JsonDeserializer<Map<SettingDef<?>, Object>> {

    public static final Type SETTINGS_TYPE = new TypeToken<Map<SettingDef<?>, Object>>() {}.getType();

    @Override
    public JsonElement serialize(final Map<SettingDef<?>, Object> source, final Type type,
        final JsonSerializationContext context) {
        final Map<String, JsonElement> saved = new TreeMap<>();
        for (final Map.Entry<SettingDef<?>, Object> entry : source.entrySet()) {
            final SettingDef<?> def = entry.getKey();
            final Object value = entry.getValue();
            if (def == null) continue;
            final JsonElement encoded = def.serialize(value, Object.class, context);
            saved.put(def.getKey(), encoded == null ? com.google.gson.JsonNull.INSTANCE : encoded);
        }
        return context.serialize(saved);
    }

    @Override
    public Map<SettingDef<?>, Object> deserialize(final JsonElement json, final Type type,
        final JsonDeserializationContext context) {
        final Map<SettingDef<?>, Object> settings = new HashMap<>();
        if (!json.isJsonObject()) return settings;

        for (final Map.Entry<String, JsonElement> entry : json.getAsJsonObject()
            .entrySet()) {
            final SettingDef<?> def = Settings.get(entry.getKey());
            if (def == null) continue;
            final Object value = entry.getValue()
                .isJsonNull() ? null : def.deserialize(entry.getValue(), Object.class, context);
            settings.put(def, value);
        }
        return settings;
    }
}
