package com.sbancuz.plannh.data.serialization.schema;

import com.google.gson.JsonObject;

/** Rewrite a saved tree in place from one schema version to the next. */
public interface SchemaStep {

    void upgradeGraph(JsonObject graph);

    default void upgradePlan(final JsonObject plan) {}
}
