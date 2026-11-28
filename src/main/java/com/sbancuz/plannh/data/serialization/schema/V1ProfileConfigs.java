package com.sbancuz.plannh.data.serialization.schema;

import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Move the settings and port chances of each machine config into the profile config of the saved profile id.
 *
 * <pre>
 *     version 0: "machineConfig": {"profileId": "p", "settings": {...}, "inputChance": {...}, "outputChance": {...}}
 *     version 1: "machineConfig": {"profileId": "p", "profileConfigs": {"p": {"settings": {...}, ...}}}
 * </pre>
 *
 * The edits of a machine config without a profile id go under the key {@code ""}.
 */
public final class V1ProfileConfigs implements SchemaStep {

    private static final List<String> PROFILE_FIELDS = List.of("settings", "inputChance", "outputChance");

    @Override
    public void upgradeGraph(final JsonObject graph) {
        for (final JsonObject node : GraphTree.collectNodes(graph)) {
            final JsonElement rawConfig = node.get("machineConfig");
            if (rawConfig == null || !rawConfig.isJsonObject()) continue;
            final JsonObject machineConfig = rawConfig.getAsJsonObject();

            final JsonObject profileConfig = new JsonObject();
            for (final String field : PROFILE_FIELDS) {
                final JsonElement value = machineConfig.remove(field);
                if (value != null) profileConfig.add(field, value);
            }
            if (profileConfig.entrySet()
                .isEmpty()) continue;

            final JsonElement rawProfileId = machineConfig.get("profileId");
            final boolean hasProfileId = rawProfileId != null && rawProfileId.isJsonPrimitive();
            final JsonObject profileConfigs = new JsonObject();
            profileConfigs.add(hasProfileId ? rawProfileId.getAsString() : "", profileConfig);
            machineConfig.add("profileConfigs", profileConfigs);
        }
    }
}
