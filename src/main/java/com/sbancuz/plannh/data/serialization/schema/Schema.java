package com.sbancuz.plannh.data.serialization.schema;

import java.util.List;
import java.util.function.BiConsumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.sbancuz.plannh.PlanNH;

/**
 * The schema version of a saved graph or plan, and the {@link SchemaStep}s from version 0 to {@link #CURRENT_VERSION}.
 * A save without {@link #VERSION_KEY} is version 0.
 */
public final class Schema {

    public static final String VERSION_KEY = "schemaVersion";
    // the step at index i upgrades version i to version i + 1
    private static final List<SchemaStep> STEPS = List.of(new V1ProfileConfigs());
    public static final int CURRENT_VERSION = STEPS.size();

    private Schema() {}

    /**
     * Run the steps from the saved version of {@code tree} to {@link #CURRENT_VERSION}, and return {@code tree}.
     *
     * @param upgrade {@link SchemaStep#upgradeGraph} or {@link SchemaStep#upgradePlan}
     * @throws JsonParseException when {@link #VERSION_KEY} isn't a whole number from 0
     */
    public static JsonObject upgrade(final JsonObject tree, final BiConsumer<SchemaStep, JsonObject> upgrade)
        throws SchemaException {
        final int savedVersion = readVersion(tree);
        if (savedVersion > CURRENT_VERSION) throw rejectNewer(savedVersion);

        for (final SchemaStep step : STEPS.subList(savedVersion, CURRENT_VERSION)) {
            upgrade.accept(step, tree);
        }
        return writeVersion(tree);
    }

    /** Set the version of {@code tree} to {@link #CURRENT_VERSION}, and return {@code tree}. */
    public static JsonObject writeVersion(final JsonObject tree) {
        tree.addProperty(VERSION_KEY, CURRENT_VERSION);
        return tree;
    }

    private static int readVersion(final JsonObject tree) {
        final JsonElement rawVersion = tree.get(VERSION_KEY);
        if (rawVersion == null) return 0;

        final boolean isNumber = rawVersion.isJsonPrimitive() && rawVersion.getAsJsonPrimitive()
            .isNumber();
        final double number = isNumber ? rawVersion.getAsDouble() : -1;
        final int savedVersion = (int) number;
        if (savedVersion != number || savedVersion < 0) {
            throw new JsonParseException(
                VERSION_KEY + " " + rawVersion + " is malformed (expected a whole number such as 1)");
        }
        return savedVersion;
    }

    private static SchemaException rejectNewer(final int savedVersion) {
        PlanNH.LOG.warn("schema.newer saved={} current={}", savedVersion, CURRENT_VERSION);
        return new SchemaException(savedVersion);
    }
}
