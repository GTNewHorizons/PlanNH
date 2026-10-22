package com.sbancuz.plannh.data.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.sbancuz.plannh.PlanNH;
import com.sbancuz.plannh.api.PlanAPI;
import com.sbancuz.plannh.data.flowchart.Graph;
import com.sbancuz.plannh.data.flowchart.GraphData;
import com.sbancuz.plannh.data.flowchart.GraphIndex;
import com.sbancuz.plannh.data.flowchart.Node;
import com.sbancuz.plannh.data.flowchart.Note;
import com.sbancuz.plannh.data.flowchart.Plan;
import com.sbancuz.plannh.data.flowchart.Summary;
import com.sbancuz.plannh.data.flowchart.balancer.ChoiceKey;
import com.sbancuz.plannh.data.flowchart.balancer.PortRef;

import codechicken.nei.recipe.Recipe;
import it.unimi.dsi.fastutil.ints.IntIntPair;

public final class Serializer {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .enableComplexMapKeySerialization()
        .registerTypeAdapterFactory(new GraphAdapter())
        .registerTypeAdapter(GraphIndex.class, new GraphIndexAdapter<>())
        .registerTypeAdapter(GraphData.class, new GraphDataDeserializer())
        .registerTypeAdapterFactory(new MachineConfigAdapter())
        .registerTypeAdapter(MachineSettingsAdapter.SETTINGS_TYPE, new MachineSettingsAdapter())
        .registerTypeAdapter(PortRef.class, new PortRefAdapter())
        .registerTypeAdapter(ChoiceKey.class, new ChoiceKeyAdapter())
        .registerTypeAdapter(Recipe.RecipeId.class, new RecipeIdAdapter())
        .registerTypeAdapter(IntIntPair.class, new IntIntPairDeserializer())
        .registerTypeAdapter(ItemStack.class, new ItemStackAdapter())
        .create();

    private Serializer() {}

    @Nonnull
    public static String encodeGraph(final Graph graph) {
        try {
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (final GZIPOutputStream gzip = new GZIPOutputStream(bytes);
                final OutputStreamWriter writer = new OutputStreamWriter(gzip, StandardCharsets.UTF_8)) {
                GSON.toJson(graph, Graph.class, writer);
            }
            return Base64.getEncoder()
                .encodeToString(bytes.toByteArray());
        } catch (final Exception | LinkageError failure) {
            throw new RuntimeException("Failed to encode flowchart", failure);
        }
    }

    @Nullable
    public static Graph decodeGraph(final String data) {
        try {
            final byte[] bytes = Base64.getDecoder()
                .decode(data);
            try (final GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(bytes));
                final InputStreamReader reader = new InputStreamReader(gzip, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, Graph.class);
            }
        } catch (final Exception | LinkageError failure) {
            PlanNH.LOG.warn("Failed to decode flowchart: {}", failure.getMessage());
            return null;
        }
    }

    @Nonnull
    public static String encodePlan(final Plan plan) {
        return encodePlan(plan, false);
    }

    @Nonnull
    public static String encodePlanDebug(final Plan plan) {
        return encodePlan(plan, true);
    }

    private static String encodePlan(final Plan plan, final boolean debug) {
        final JsonObject root = GSON.toJsonTree(plan, Plan.class)
            .getAsJsonObject();
        final JsonArray graphs = new JsonArray();
        for (final Graph graph : plan.getGraphs()) {
            if (graph != null)
                graphs.add(debug ? GSON.toJsonTree(graph, Graph.class) : new JsonPrimitive(encodeGraph(graph)));
        }
        root.add("graphs", graphs);
        return GSON.toJson(root);
    }

    @Nullable
    public static Plan decodePlan(final String json) {
        int malformed = 0;
        try {
            final JsonObject root = GSON.fromJson(json, JsonObject.class);
            if (root == null || !root.has("graphs")
                || !root.get("graphs")
                    .isJsonArray())
                return null;
            final Plan plan = GSON.fromJson(root, Plan.class);
            if (plan.getSummary() == null) plan.setSummary(new Summary());
            for (final JsonElement entry : root.getAsJsonArray("graphs")) {
                Graph graph = null;
                try {
                    if (entry.isJsonObject()) graph = GSON.fromJson(entry, Graph.class);
                    else if (entry.isJsonPrimitive() && entry.getAsJsonPrimitive()
                        .isString()) {
                            graph = decodeGraph(entry.getAsString());
                        }
                } catch (final Exception | LinkageError ignored) {}
                if (graph == null) malformed++;
                else plan.getGraphs()
                    .add(graph);
            }
            return plan;
        } catch (final Exception | LinkageError failure) {
            PlanNH.LOG.warn("Failed to decode plan: {}", failure.getMessage());
            return null;
        } finally {
            if (malformed > 0) PlanAPI.showMessage("plannh.warning.malformed_entries", malformed);
        }
    }

    @Nonnull
    public static String toMermaid(final Graph graph) {
        final StringBuilder sb = new StringBuilder("flowchart LR\n");
        for (final Node node : graph.getNodes()
            .values()) {
            final String id = mermaidId(node.getId());
            final String label = node.getMachineName()
                .isEmpty() ? "?" : node.getMachineName();
            sb.append("    ")
                .append(id)
                .append("[\"")
                .append(escapeMermaid(label))
                .append("\"]\n");
        }
        for (final Note note : graph.getNotes()
            .values()) {
            sb.append("    %% Note: ");
            for (final String line : note.getText()) sb.append(escapeMermaid(line))
                .append("\n");
        }
        return sb.toString();
    }

    @Nonnull
    private static String mermaidId(final UUID uuid) {
        return "n" + uuid.toString()
            .replace("-", "")
            .substring(0, 8);
    }

    @Nonnull
    private static String escapeMermaid(final String value) {
        return value.replace("\"", "#quot;")
            .replace("\n", "<br/>");
    }

}
