package com.sbancuz.plannh.data.serialization.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Find the objects of one kind in the JSON tree of a saved graph. */
public final class GraphTree {

    private GraphTree() {}

    /**
     * Return the node objects in {@code nodes} and in the {@code children} of each group, also of a nested group. A
     * node in the {@code children} of a group is a separate object in the tree.
     */
    public static List<JsonObject> collectNodes(final JsonObject graph) {
        final List<JsonElement> pending = new ArrayList<>();
        for (final String field : List.of("nodes", "groups")) {
            final JsonElement entries = graph.get(field);
            if (entries != null && entries.isJsonArray()) entries.getAsJsonArray()
                .forEach(pending::add);
        }

        // a node has the type "node", and a group has a "children" object of child id to child
        final List<JsonObject> nodes = new ArrayList<>();
        while (!pending.isEmpty()) {
            final JsonElement element = pending.remove(pending.size() - 1);
            if (!element.isJsonObject()) continue;
            final JsonObject data = element.getAsJsonObject();
            final JsonElement type = data.get("type");
            if (type != null && type.isJsonPrimitive() && "node".equals(type.getAsString())) nodes.add(data);

            final JsonElement children = data.get("children");
            if (children == null || !children.isJsonObject()) continue;
            for (final Map.Entry<String, JsonElement> child : children.getAsJsonObject()
                .entrySet()) {
                pending.add(child.getValue());
            }
        }
        return nodes;
    }
}
