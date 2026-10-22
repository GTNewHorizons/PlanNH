package com.sbancuz.plannh.data.serialization;

import java.lang.reflect.Type;
import java.util.UUID;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.sbancuz.plannh.data.flowchart.balancer.PortRef;

/** Gson 2.2.4 cannot populate native record components reflectively. */
public final class PortRefAdapter implements JsonDeserializer<PortRef> {

    @Override
    public PortRef deserialize(final JsonElement json, final Type type, final JsonDeserializationContext context) {
        if (!json.isJsonObject()) return null;
        final var object = json.getAsJsonObject();
        if (!object.has("nodeId") || !object.has("portIndex") || !object.has("input")) return null;
        return new PortRef(
            UUID.fromString(
                object.get("nodeId")
                    .getAsString()),
            object.get("portIndex")
                .getAsInt(),
            object.get("input")
                .getAsBoolean());
    }
}
