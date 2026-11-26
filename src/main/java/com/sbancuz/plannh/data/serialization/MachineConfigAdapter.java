package com.sbancuz.plannh.data.serialization;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.ProfileConfig;
import com.sbancuz.plannh.data.flowchart.balancer.Pin;

/**
 * Adds the save-format rules around Gson's reflective {@link MachineConfig} adapter. Ordinary config
 * fields stay reflective, while settings and target cleanup retain the compact current shape.
 */
public final class MachineConfigAdapter implements TypeAdapterFactory {

    private static final String PROFILE_CONFIGS = "profileConfigs";
    // saves before per-profile configs have the fields of `ProfileConfig` on the machine config
    private static final List<String> LEGACY_FIELDS = List.of("settings", "inputChance", "outputChance");

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
                final JsonObject profileConfigs = object.getAsJsonObject(PROFILE_CONFIGS);
                profileConfigs.entrySet()
                    .removeIf(entry -> isEmptyConfig(entry.getValue()));
                if (profileConfigs.entrySet()
                    .isEmpty()) object.remove(PROFILE_CONFIGS);
                final boolean serializeNulls = out.getSerializeNulls();
                out.setSerializeNulls(true);
                try {
                    jsonElementAdapter.write(out, object);
                } finally {
                    out.setSerializeNulls(serializeNulls);
                }
            }

            @Override
            public T read(final JsonReader in) throws IOException {
                final JsonObject object = jsonElementAdapter.read(in)
                    .getAsJsonObject();
                // Malformed optional targets leave the constructor defaults intact.
                for (final String field : new String[] { "copies", "rates", PROFILE_CONFIGS }) {
                    if (object.has(field) && !object.get(field)
                        .isJsonObject()) object.remove(field);
                }
                moveLegacyEdits(object);
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
                if (config.getTargetKind() == null) config.setTargetKind(Pin.NONE);
                config.getCopies()
                    .setCopies(
                        config.getCopies()
                            .copies());
                return value;
            }
        }.nullSafe();
    }

    /** Return whether each field of a saved {@link ProfileConfig} is an empty map. */
    private static boolean isEmptyConfig(final JsonElement config) {
        if (!config.isJsonObject()) return true;
        for (final Map.Entry<String, JsonElement> field : config.getAsJsonObject()
            .entrySet()) {
            final JsonElement value = field.getValue();
            if (!(value.isJsonObject() && value.getAsJsonObject()
                .entrySet()
                .isEmpty())) return false;
        }
        return true;
    }

    /**
     * Move the settings and port chances of a save without {@link ProfileConfig}s into the config of the saved profile
     * id.
     */
    private static void moveLegacyEdits(final JsonObject object) {
        final JsonObject legacy = new JsonObject();
        for (final String field : LEGACY_FIELDS) {
            final JsonElement value = object.remove(field);
            if (value != null) legacy.add(field, value);
        }
        if (legacy.entrySet()
            .isEmpty()) return;

        final JsonElement rawProfileId = object.get("profileId");
        final boolean hasProfileId = rawProfileId != null && rawProfileId.isJsonPrimitive();
        final String profileId = hasProfileId ? rawProfileId.getAsString() : MachineConfig.NO_PROFILE_ID;
        if (!object.has(PROFILE_CONFIGS)) object.add(PROFILE_CONFIGS, new JsonObject());
        object.getAsJsonObject(PROFILE_CONFIGS)
            .add(profileId, legacy);
    }
}
