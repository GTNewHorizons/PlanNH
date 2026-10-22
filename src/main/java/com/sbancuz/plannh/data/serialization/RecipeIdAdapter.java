package com.sbancuz.plannh.data.serialization;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import codechicken.nei.recipe.Recipe;

public class RecipeIdAdapter implements JsonSerializer<Recipe.RecipeId>, JsonDeserializer<Recipe.RecipeId> {

    @Override
    public Recipe.RecipeId deserialize(final JsonElement json, final Type typeOfT,
        final JsonDeserializationContext context) throws JsonParseException {
        return Recipe.RecipeId.of(json.getAsJsonObject());
    }

    @Override
    public JsonElement serialize(final Recipe.RecipeId src, final Type typeOfSrc,
        final JsonSerializationContext context) {
        return src.toJsonObject();
    }
}
