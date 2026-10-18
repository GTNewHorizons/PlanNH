package com.sbancuz.plannh.data.serialization;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import it.unimi.dsi.fastutil.ints.IntIntPair;

public class IntIntPairDeserializer implements JsonDeserializer<IntIntPair>, JsonSerializer<IntIntPair> {

    @Override
    public IntIntPair deserialize(final JsonElement json, final Type type, final JsonDeserializationContext context) {
        if (!json.isJsonObject()) return null;
        final var object = json.getAsJsonObject();
        if (!object.has("left") || !object.has("right")) return null;
        return IntIntPair.of(
            object.get("left")
                .getAsInt(),
            object.get("right")
                .getAsInt());
    }

    @Override
    public JsonElement serialize(final IntIntPair pair, final Type type, final JsonSerializationContext context) {
        final JsonObject object = new JsonObject();
        object.addProperty("left", pair.leftInt());
        object.addProperty("right", pair.rightInt());
        return object;
    }
}
