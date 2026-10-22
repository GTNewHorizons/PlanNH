package com.sbancuz.plannh.data.serialization;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.sbancuz.plannh.data.flowchart.balancer.ChoiceKey;
import com.sbancuz.plannh.data.flowchart.balancer.PortRef;

public final class ChoiceKeyAdapter implements JsonDeserializer<ChoiceKey> {

    @Override
    public ChoiceKey deserialize(final JsonElement json, final Type type, final JsonDeserializationContext context) {
        if (!json.isJsonObject()) return null;
        final var object = json.getAsJsonObject();
        if (!object.has("gateAnchors") || !object.get("gateAnchors")
            .isJsonArray()) return null;

        final List<PortRef> anchors = new ArrayList<>();
        for (final JsonElement element : object.getAsJsonArray("gateAnchors")) {
            anchors.add(context.deserialize(element, PortRef.class));
        }
        return ChoiceKey.of(anchors);
    }
}
