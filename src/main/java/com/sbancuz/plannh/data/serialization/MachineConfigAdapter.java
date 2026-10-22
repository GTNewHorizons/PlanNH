package com.sbancuz.plannh.data.serialization;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.flowchart.balancer.Pin;
import com.sbancuz.plannh.data.setting.SettingDef;
import com.sbancuz.plannh.data.setting.Settings;

/**
 * Adds the save-format rules around Gson's reflective {@link MachineConfig} adapter. Ordinary config
 * fields stay reflective, while settings and target cleanup retain the compact current shape.
 */
public final class MachineConfigAdapter implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
        if (type.getRawType() != MachineConfig.class) return null;

        final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
        final TypeAdapter<JsonElement> jsonElementAdapter = gson.getAdapter(JsonElement.class);
        return new TypeAdapter<T>() {

            @Override
            public void write(final JsonWriter out, final T value) throws IOException {
                final JsonElement tree = delegate.toJsonTree(value);
                final JsonObject object = tree.getAsJsonObject();
                final MachineConfig config = (MachineConfig) value;
                final Set<String> profileSettings = new HashSet<>();
                for (final SettingDef<?> def : config.getProfile()
                    .defs()) profileSettings.add(def.getKey());

                object.getAsJsonObject("settings")
                    .entrySet()
                    .removeIf(entry -> !profileSettings.contains(entry.getKey()));

                final JsonObject rates = object.getAsJsonObject("rates")
                    .getAsJsonObject("rates");
                rates.entrySet()
                    .removeIf(
                        entry -> entry.getValue()
                            .isJsonNull()
                            || !(entry.getValue()
                                .getAsDouble() > 0));
                object.remove("rates");
                if (!rates.entrySet()
                    .isEmpty()) object.add("rates", rates);
                jsonElementAdapter.write(out, object);
            }

            @Override
            public T read(final JsonReader in) throws IOException {
                final JsonObject object = jsonElementAdapter.read(in)
                    .getAsJsonObject();
                // Malformed optional targets leave the constructor defaults intact.
                for (final String field : new String[] { "copies", "rates" }) {
                    if (object.has(field) && !object.get(field)
                        .isJsonObject()) object.remove(field);
                }
                if (object.has("targetKind") && !object.get("targetKind")
                    .isJsonPrimitive()) object.remove("targetKind");
                if (object.has("rates")) {
                    final JsonObject rates = object.getAsJsonObject("rates");
                    rates.entrySet()
                        .removeIf(entry -> {
                            try {
                                Integer.parseInt(entry.getKey());
                                return !(entry.getValue()
                                    .getAsDouble() > 0);
                            } catch (final RuntimeException ignored) {
                                return true;
                            }
                        });
                    final JsonObject wrapper = new JsonObject();
                    wrapper.add("rates", rates);
                    object.add("rates", wrapper);
                }
                final T value = delegate.fromJsonTree(object);
                if (value == null) return null;

                final MachineConfig config = (MachineConfig) value;
                final MachineProfile profile = config.getProfile();
                if (!profile.id()
                    .equals(config.getProfileId())) config.setProfileId(profile.id());
                for (final SettingDef<?> def : profile.defs()) config.get(def);
                config.get(Settings.MACHINES);
                if (config.getTargetKind() == null) config.setTargetKind(Pin.NONE);
                config.getCopies()
                    .setCopies(
                        config.getCopies()
                            .copies());
                return value;
            }
        }.nullSafe();
    }
}
