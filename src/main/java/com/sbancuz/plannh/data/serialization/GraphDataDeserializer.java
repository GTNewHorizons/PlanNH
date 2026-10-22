package com.sbancuz.plannh.data.serialization;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.sbancuz.plannh.data.flowchart.GraphData;
import com.sbancuz.plannh.data.flowchart.Group;
import com.sbancuz.plannh.data.flowchart.MachineGroup;
import com.sbancuz.plannh.data.flowchart.Node;
import com.sbancuz.plannh.data.flowchart.Note;

public class GraphDataDeserializer implements JsonDeserializer<GraphData>, JsonSerializer<GraphData> {

    @Override
    public JsonElement serialize(final GraphData data, final Type type, final JsonSerializationContext context) {
        final JsonElement serialized = context.serialize(data, data.getClass());
        if (!(data instanceof Group group) || group.getChildren() == null || !serialized.isJsonObject())
            return serialized;
        final JsonObject children = serialized.getAsJsonObject()
            .getAsJsonObject("children");
        if (children == null) return serialized;
        for (final Map.Entry<UUID, GraphData> entry : group.getChildren()
            .entrySet()) {
            if (!validChild(entry)) children.remove(
                entry.getKey()
                    .toString());
        }
        return serialized;
    }

    @Override
    public GraphData deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
        throws JsonParseException {
        if (!json.isJsonObject()) return null;
        final JsonElement discriminator = json.getAsJsonObject()
            .get("type");
        if (discriminator == null || !discriminator.isJsonPrimitive()) return null;
        try {
            final GraphData data = switch (discriminator.getAsString()) {
                case Note.TYPE -> context.deserialize(json, Note.class);
                case Group.TYPE -> context.deserialize(json, Group.class);
                case MachineGroup.TYPE -> context.deserialize(json, MachineGroup.class);
                case Node.TYPE -> context.deserialize(json, Node.class);
                default -> null;
            };
            if (data == null || data.invalid()) return null;
            if (data instanceof Node node) node.init();
            if (data instanceof Group group && group.getChildren() != null) {
                group.getChildren()
                    .entrySet()
                    .removeIf(entry -> !validChild(entry));
            }
            return data;
        } catch (final Exception | LinkageError ignored) {
            return null;
        }
    }

    private static boolean validChild(final Map.Entry<UUID, GraphData> entry) {
        final GraphData child = entry.getValue();
        return child != null && !child.invalid()
            && child.getId()
                .equals(entry.getKey());
    }
}
