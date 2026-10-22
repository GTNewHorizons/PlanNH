package com.sbancuz.plannh.data.serialization;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sbancuz.plannh.api.PlanAPI;
import com.sbancuz.plannh.data.flowchart.Graph;

/** Runs model validation around Gson's ordinary reflective graph adapter. */
final class GraphAdapter implements TypeAdapterFactory {

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
        if (type.getRawType() != Graph.class) return null;
        final TypeAdapter<Graph> delegate = gson.getDelegateAdapter(this, TypeToken.get(Graph.class));
        return (TypeAdapter<T>) new TypeAdapter<Graph>() {

            @Override
            public void write(final JsonWriter out, final Graph graph) throws IOException {
                graph.prepareForSave();
                delegate.write(out, graph);
            }

            @Override
            public Graph read(final JsonReader in) throws IOException {
                final Graph graph = delegate.read(in);
                if (graph == null) return null;
                final int malformed = graph.validate();
                final long unavailable = graph.allNodes()
                    .filter(node -> !node.isAvailable())
                    .count();
                if (unavailable > 0) PlanAPI.showMessage("plannh.warning.unavailable_nodes", unavailable);
                if (malformed > 0) PlanAPI.showMessage("plannh.warning.malformed_entries", malformed);
                return graph;
            }
        }.nullSafe();
    }
}
