package com.sbancuz.plannh.data.serialization.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.flowchart.Graph;
import com.sbancuz.plannh.data.flowchart.Group;
import com.sbancuz.plannh.data.flowchart.Node;
import com.sbancuz.plannh.data.flowchart.Plan;
import com.sbancuz.plannh.data.serialization.Serializer;
import com.sbancuz.plannh.data.setting.Settings;

/**
 * {@link Serializer} reads a save of any version up to {@link Schema#CURRENT_VERSION} in the current schema, and
 * rejects a save of a newer version.
 */
public final class SchemaTest {

    private static final List<String> PROFILE_FIELDS = List.of("settings", "inputChance", "outputChance");
    private static final JsonPrimitive CURRENT = new JsonPrimitive(Schema.CURRENT_VERSION);

    @Test
    public void aVersion0GraphLoadsTheEditsIntoTheSavedProfile() throws IOException {
        final Graph graph = Serializer.decodeGraph(pack(readFixture("schema/graph-v0.json")));

        final Node top = graph.getNodes()
            .get(new UUID(0, 1));
        final Group group = graph.getGroups()
            .get(new UUID(0, 3));
        final Node child = (Node) group.getChildren()
            .get(new UUID(0, 2));
        assertEdits(top.getMachineConfig(), 3, 0.25, 0.75);
        assertEdits(child.getMachineConfig(), 5, 0.5, 0.125);
    }

    @Test
    public void anUpgradeMovesTheProfileFieldsOfEachNodeIntoTheSavedProfile() throws IOException, SchemaException {
        for (final JsonObject saved : version0Graphs()) {
            final JsonObject upgraded = Schema.upgrade(copy(saved), SchemaStep::upgradeGraph);
            final List<JsonObject> before = GraphTree.collectNodes(saved);
            final List<JsonObject> after = GraphTree.collectNodes(upgraded);
            assertEquals(CURRENT, upgraded.get(Schema.VERSION_KEY));
            assertEquals(before.size(), after.size());

            for (int i = 0; i < before.size(); i++) {
                final JsonObject savedConfig = before.get(i)
                    .getAsJsonObject("machineConfig");
                final JsonObject config = after.get(i)
                    .getAsJsonObject("machineConfig");
                assertProfileFieldsMoved(savedConfig, config);
            }
        }
    }

    @Test
    public void anUpgradeOfACurrentTreeLeavesTheTreeUnchanged() throws IOException, SchemaException {
        for (final JsonObject saved : version0Graphs()) {
            final JsonObject current = Schema.upgrade(saved, SchemaStep::upgradeGraph);
            final JsonObject again = Schema.upgrade(copy(current), SchemaStep::upgradeGraph);
            assertEquals(current, again);
        }
    }

    @Test
    public void aNewerVersionDoesNotLoad() throws IOException {
        final JsonObject newer = readFixture("schema/graph-v0.json");
        newer.addProperty(Schema.VERSION_KEY, Schema.CURRENT_VERSION + 1);

        final SchemaException rejected = assertThrows(
            SchemaException.class,
            () -> Schema.upgrade(newer, SchemaStep::upgradeGraph));
        assertEquals(Schema.CURRENT_VERSION + 1, rejected.getSavedVersion());
        assertNull(Serializer.decodeGraph(pack(newer)));
    }

    @Test
    public void aMalformedVersionDoesNotLoad() throws IOException {
        final List<JsonElement> malformed = List.of(
            new JsonPrimitive("one"),
            new JsonPrimitive(1.5),
            new JsonPrimitive(-1),
            new JsonArray(),
            new JsonObject());
        for (final JsonElement rawVersion : malformed) {
            final JsonObject graph = readFixture("schema/graph-v0.json");
            graph.add(Schema.VERSION_KEY, rawVersion);

            assertThrows(JsonParseException.class, () -> Schema.upgrade(graph, SchemaStep::upgradeGraph));
            assertNull(Serializer.decodeGraph(pack(graph)));
        }
    }

    @Test
    public void aSaveHasTheCurrentVersion() throws IOException {
        final Graph graph = new Graph("current");
        final Plan plan = Serializer.decodePlan("{\"graphs\":[]}");
        plan.getGraphs()
            .add(graph);

        final JsonObject savedPlan = Serializer.GSON.fromJson(Serializer.encodePlan(plan), JsonObject.class);
        final JsonObject debugPlan = Serializer.GSON.fromJson(Serializer.encodePlanDebug(plan), JsonObject.class);
        final JsonObject debugGraph = debugPlan.getAsJsonArray("graphs")
            .get(0)
            .getAsJsonObject();
        assertEquals(CURRENT, savedPlan.get(Schema.VERSION_KEY));
        assertEquals(CURRENT, debugPlan.get(Schema.VERSION_KEY));
        assertEquals(CURRENT, debugGraph.get(Schema.VERSION_KEY));
        assertEquals(CURRENT, unpack(Serializer.encodeGraph(graph)).get(Schema.VERSION_KEY));
    }

    private static void assertEdits(final MachineConfig config, final int machines, final double input,
        final double output) {
        assertEquals(machines, config.getOrDefault(Settings.MACHINES));
        assertEquals(input, config.chanceOverride(true, 0));
        assertEquals(output, config.chanceOverride(false, 1));
    }

    private static void assertProfileFieldsMoved(final JsonObject savedConfig, final JsonObject config) {
        final boolean hasProfileFields = PROFILE_FIELDS.stream()
            .anyMatch(savedConfig::has);
        assertFalse(config.has("settings") || config.has("inputChance") || config.has("outputChance"));
        if (!hasProfileFields) {
            assertEquals(savedConfig, config);
            return;
        }

        final JsonElement rawProfileId = savedConfig.get("profileId");
        final String key = rawProfileId == null ? MachineConfig.NO_PROFILE_ID : rawProfileId.getAsString();
        final JsonObject profileConfig = config.getAsJsonObject("profileConfigs")
            .getAsJsonObject(key);
        for (final String field : PROFILE_FIELDS) {
            assertEquals(savedConfig.get(field), profileConfig.get(field));
        }
    }

    /**
     * The fixture, plus graphs generated from a fixed seed. The generated graphs have nodes in {@code nodes} and in
     * nested groups. Some nodes have a profile id, and each node has a random subset of the profile fields.
     */
    private static List<JsonObject> version0Graphs() throws IOException {
        final List<JsonObject> graphs = new ArrayList<>();
        graphs.add(readFixture("schema/graph-v0.json"));

        final Random random = new Random(42);
        for (int i = 0; i < 50; i++) {
            final JsonArray nodes = new JsonArray();
            final JsonObject inner = group(random, null);
            final JsonArray groups = new JsonArray();
            groups.add(group(random, inner));
            for (int n = random.nextInt(4); n > 0; n--) nodes.add(node(random));

            final JsonObject graph = new JsonObject();
            graph.add("nodes", nodes);
            graph.add("groups", groups);
            graphs.add(graph);
        }
        return graphs;
    }

    private static JsonObject group(final Random random, final JsonObject nested) {
        final JsonObject children = new JsonObject();
        for (int n = random.nextInt(3); n > 0; n--) children.add(
            UUID.randomUUID()
                .toString(),
            node(random));
        if (nested != null) children.add(
            UUID.randomUUID()
                .toString(),
            nested);

        final JsonObject group = new JsonObject();
        group.addProperty("type", "group");
        group.add("children", children);
        return group;
    }

    private static JsonObject node(final Random random) {
        final JsonObject config = new JsonObject();
        if (random.nextBoolean()) config.addProperty("profileId", "profile-" + random.nextInt(3));
        for (final String field : PROFILE_FIELDS) {
            if (!random.nextBoolean()) continue;
            final JsonObject value = new JsonObject();
            value.addProperty(Integer.toString(random.nextInt(9)), random.nextDouble());
            config.add(field, value);
        }

        final JsonObject node = new JsonObject();
        node.addProperty("type", "node");
        node.add("machineConfig", config);
        return node;
    }

    private static JsonObject readFixture(final String path) throws IOException {
        try (InputStream stream = SchemaTest.class.getClassLoader()
            .getResourceAsStream(path);
            InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return Serializer.GSON.fromJson(reader, JsonObject.class);
        }
    }

    private static JsonObject copy(final JsonObject tree) {
        return Serializer.GSON.fromJson(Serializer.GSON.toJson(tree), JsonObject.class);
    }

    private static String pack(final JsonObject graph) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            gzip.write(
                Serializer.GSON.toJson(graph)
                    .getBytes(StandardCharsets.UTF_8));
        }
        return Base64.getEncoder()
            .encodeToString(bytes.toByteArray());
    }

    private static JsonObject unpack(final String encoded) throws IOException {
        final byte[] bytes = Base64.getDecoder()
            .decode(encoded);
        try (InputStreamReader reader = new InputStreamReader(
            new GZIPInputStream(new ByteArrayInputStream(bytes)),
            StandardCharsets.UTF_8)) {
            return Serializer.GSON.fromJson(reader, JsonObject.class);
        }
    }
}
