package com.sbancuz.plannh.data.serialization;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.sbancuz.plannh.data.flowchart.GraphData;
import com.sbancuz.plannh.data.flowchart.GraphIndex;
import com.sbancuz.plannh.data.flowchart.IValidated;

/** Converts index values, without knowing which graph field contains the index. */
final class GraphIndexAdapter<T extends IValidated>
    implements JsonSerializer<GraphIndex<T>>, JsonDeserializer<GraphIndex<T>> {

    @Override
    public JsonElement serialize(final GraphIndex<T> index, final Type type, final JsonSerializationContext context) {
        final JsonArray array = new JsonArray();
        for (final T value : index.values()) {
            if (index.isValid(value)) {
                array.add(context.serialize(value, value instanceof GraphData ? GraphData.class : value.getClass()));
            }
        }
        return array;
    }

    @Override
    @SuppressWarnings("unchecked")
    public GraphIndex<T> deserialize(final JsonElement json, final Type type,
        final JsonDeserializationContext context) {
        final GraphIndex<T> index = new GraphIndex<>();
        if (!json.isJsonArray()) {
            index.rejectLoaded();
            return index;
        }
        final Class<?> expected = (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
        final Type entryType = GraphData.class.isAssignableFrom(expected) ? GraphData.class : expected;
        for (final JsonElement element : json.getAsJsonArray()) {
            try {
                final IValidated value = context.deserialize(element, entryType);
                if (expected.isInstance(value) && !value.invalid()) index.addLoaded((T) value);
                else index.rejectLoaded();
            } catch (final Exception | LinkageError ignored) {
                index.rejectLoaded();
            }
        }
        return index;
    }
}
