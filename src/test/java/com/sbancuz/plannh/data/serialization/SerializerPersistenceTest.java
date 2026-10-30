package com.sbancuz.plannh.data.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.flowchart.Edge;
import com.sbancuz.plannh.data.flowchart.Graph;
import com.sbancuz.plannh.data.flowchart.Group;
import com.sbancuz.plannh.data.flowchart.Node;
import com.sbancuz.plannh.data.flowchart.balancer.Pin;
import com.sbancuz.plannh.harness.GtnhFlowLoader;

import codechicken.nei.recipe.Recipe;
import it.unimi.dsi.fastutil.ints.IntIntMutablePair;

class SerializerPersistenceTest {

    @Test
    void unavailableNodeKeepsItsPersistedFields() throws Exception {
        final Node node = unavailableNode();
        final Graph graph = new Graph("missing");
        graph.addNode(node);

        final Graph loaded = Serializer.decodeGraph(Serializer.encodeGraph(graph));
        final Node restored = loaded.getNodes()
            .get(node.getId());
        assertNotNull(restored);
        assertFalse(restored.isAvailable());
        assertTrue(restored.isInitialized());
        assertNotNull(restored.getInputs());
        assertNotNull(restored.getOutputs());
        assertEquals(7, restored.getX());
        assertEquals(11, restored.getY());
        assertNull(
            restored.getMachineConfig()
                .getProfile());

        restored.setX(31);
        restored.setY(37);
        final Graph moved = Serializer.decodeGraph(Serializer.encodeGraph(loaded));
        assertEquals(
            31,
            moved.getNodes()
                .get(node.getId())
                .getX());
        assertEquals(
            37,
            moved.getNodes()
                .get(node.getId())
                .getY());
        assertNull(
            moved.getNodes()
                .get(node.getId())
                .getMachineConfig()
                .getProfile());

        moved.getNodes()
            .remove(node.getId());
        assertTrue(
            Serializer.decodeGraph(Serializer.encodeGraph(moved))
                .getNodes()
                .isEmpty());
    }

    @Test
    void unavailableNestedNodeKeepsGroupMapRepresentation() throws Exception {
        final Node node = unavailableNode();
        final Group group = new Group();
        group.getChildren()
            .put(node.getId(), node);
        final Graph graph = new Graph("nested");
        graph.getGroups()
            .put(group.getId(), group);

        final Graph loaded = Serializer.decodeGraph(Serializer.encodeGraph(graph));
        final Node restored = (Node) loaded.getGroups()
            .get(group.getId())
            .getChildren()
            .get(node.getId());
        assertFalse(restored.isAvailable());
        assertTrue(restored.isInitialized());
        assertNotNull(restored.getInputs());
        assertNotNull(restored.getOutputs());
        assertNull(
            restored.getMachineConfig()
                .getProfile());
    }

    @Test
    void unavailableNodeKeepsKnownSettingsWhenMoved() throws Exception {
        final Node node = unavailableNode();
        node.getMachineConfig()
            .getCopies()
            .setCopies(4);
        node.getMachineConfig()
            .setTargetKind(Pin.FIXED_COPIES);
        node.getMachineConfig()
            .getRates()
            .rates()
            .put(0, 12.5);
        final Graph graph = new Graph("config");
        graph.addNode(node);

        final Graph loaded = Serializer.decodeGraph(Serializer.encodeGraph(graph));
        loaded.getNodes()
            .get(node.getId())
            .setX(101);
        final Node restored = Serializer.decodeGraph(Serializer.encodeGraph(loaded))
            .getNodes()
            .get(node.getId());

        assertEquals(101, restored.getX());
        assertNull(
            restored.getMachineConfig()
                .getProfile());
        assertEquals(
            4,
            restored.getMachineConfig()
                .getCopies()
                .copies());
        assertEquals(
            Pin.FIXED_COPIES,
            restored.getMachineConfig()
                .getTargetKind());
        assertEquals(
            12.5,
            restored.getMachineConfig()
                .getRates()
                .rate(0));
    }

    @Test
    void gsonParsesNodeFieldsReflectively() throws Exception {
        final Node original = unavailableNode();
        final JsonObject encoded = Serializer.GSON.toJsonTree(original)
            .getAsJsonObject();
        final Node restored = Serializer.GSON.fromJson(encoded, Node.class);

        assertEquals(
            "test",
            encoded.getAsJsonObject("machineConfig")
                .get("profileId")
                .getAsString());
        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getMachineName(), restored.getMachineName());
        assertEquals(original.getX(), restored.getX());
        assertNotNull(original.getRecipeId());
        assertNotNull(restored.getRecipeId());
        assertNull(
            restored.getMachineConfig()
                .getProfile());
        assertEquals(
            "test",
            restored.getMachineConfig()
                .getProfileId());
        assertFalse(restored.isAvailable());
    }

    @Test
    void nestedNodeTargetsRoundTripInCurrentFormat() throws Exception {
        final Node node = unavailableNode();
        node.getMachineConfig()
            .getCopies()
            .setCopies(4);
        node.getMachineConfig()
            .setTargetKind(Pin.FIXED_COPIES);
        final Group group = new Group();
        group.getChildren()
            .put(node.getId(), node);
        final Graph graph = new Graph("nested targets");
        graph.getGroups()
            .put(group.getId(), group);
        final JsonObject source = unpack(Serializer.encodeGraph(graph));
        final Node restored = (Node) Serializer.decodeGraph(pack(source))
            .getGroups()
            .get(group.getId())
            .getChildren()
            .get(node.getId());

        assertEquals(
            4,
            restored.getMachineConfig()
                .getCopies()
                .copies());
        assertEquals(
            Pin.FIXED_COPIES,
            restored.getMachineConfig()
                .getTargetKind());
    }

    @Test
    void duplicateNodesAreDroppedAndEmptyGraphPayloadUsesDefaults() throws Exception {
        final Graph graph = new Graph("duplicates");
        graph.addNode(unavailableNode());
        final JsonObject root = unpack(Serializer.encodeGraph(graph));
        final JsonArray nodes = root.getAsJsonArray("nodes");
        nodes.add(nodes.get(0));
        nodes.add(new JsonPrimitive("malformed"));

        final Graph loaded = Serializer.decodeGraph(pack(root));
        assertEquals(
            1,
            loaded.getNodes()
                .size());
        JsonObject preserved = unpack(Serializer.encodeGraph(loaded));
        assertEquals(
            1,
            preserved.getAsJsonArray("nodes")
                .size());
        assertFalse(preserved.has("__plannh_quarantine.nodes"));

        loaded.getNodes()
            .clear();
        preserved = unpack(Serializer.encodeGraph(loaded));
        assertEquals(
            0,
            preserved.getAsJsonArray("nodes")
                .size());
        assertFalse(preserved.has("__plannh_quarantine.nodes"));
        final Graph repeated = Serializer.decodeGraph(Serializer.encodeGraph(loaded));
        assertNotNull(repeated);
        assertTrue(
            repeated.getNodes()
                .isEmpty());
        final Graph emptyGraph = Serializer.decodeGraph(pack(new JsonObject()));
        assertNotNull(emptyGraph);
        assertTrue(
            emptyGraph.getNodes()
                .isEmpty());
        assertTrue(
            emptyGraph.getNotes()
                .isEmpty());
        assertTrue(
            emptyGraph.getGroups()
                .isEmpty());
        assertTrue(
            emptyGraph.getEdges()
                .isEmpty());
    }

    @Test
    void mismatchedNestedMapKeyIsDropped() throws Exception {
        final Node node = unavailableNode();
        final Group group = new Group();
        group.getChildren()
            .put(node.getId(), node);
        final Graph graph = new Graph("bad key");
        graph.getGroups()
            .put(group.getId(), group);

        final JsonObject root = unpack(Serializer.encodeGraph(graph));
        final JsonObject groupJson = root.getAsJsonArray("groups")
            .get(0)
            .getAsJsonObject();
        final JsonElement children = groupJson.get("children");
        if (children.isJsonObject()) {
            final JsonObject wrong = new JsonObject();
            wrong.add(
                UUID.randomUUID()
                    .toString(),
                children.getAsJsonObject()
                    .entrySet()
                    .iterator()
                    .next()
                    .getValue());
            groupJson.add("children", wrong);
        } else {
            final JsonArray pair = children.getAsJsonArray()
                .get(0)
                .getAsJsonArray();
            final JsonArray wrongPair = new JsonArray();
            wrongPair.add(Serializer.GSON.toJsonTree(UUID.randomUUID()));
            wrongPair.add(pair.get(1));
            final JsonArray wrong = new JsonArray();
            wrong.add(wrongPair);
            groupJson.add("children", wrong);
        }

        final Graph loaded = Serializer.decodeGraph(pack(root));
        final Group restored = loaded.getGroups()
            .get(group.getId());
        assertTrue(
            restored.getChildren()
                .isEmpty());
        final JsonObject preserved = unpack(Serializer.encodeGraph(loaded));
        assertFalse(
            preserved.getAsJsonArray("groups")
                .get(0)
                .getAsJsonObject()
                .has("__plannh_quarantine.children"));
    }

    @Test
    void duplicateEdgesAndInvalidChildrenAreDropped() throws Exception {
        final Node first = unavailableNode();
        final Node second = unavailableNode();
        final Group group = new Group();
        group.getChildren()
            .put(first.getId(), first);
        final Graph nested = new Graph("duplicate children");
        nested.getGroups()
            .put(group.getId(), group);
        final JsonObject nestedRoot = unpack(Serializer.encodeGraph(nested));
        final JsonObject nestedGroup = nestedRoot.getAsJsonArray("groups")
            .get(0)
            .getAsJsonObject();
        final JsonElement childJson = nestedGroup.get("children");
        if (childJson.isJsonObject()) {
            final JsonObject children = childJson.getAsJsonObject();
            children.add(
                UUID.randomUUID()
                    .toString(),
                children.entrySet()
                    .iterator()
                    .next()
                    .getValue());
        } else {
            nestedGroup.getAsJsonArray("children")
                .add(
                    nestedGroup.getAsJsonArray("children")
                        .get(0));
        }
        final JsonObject nestedPreserved = unpack(Serializer.encodeGraph(Serializer.decodeGraph(pack(nestedRoot))));
        final JsonObject nestedOutputGroup = nestedPreserved.getAsJsonArray("groups")
            .get(0)
            .getAsJsonObject();
        assertEquals(
            1,
            nestedOutputGroup.getAsJsonObject("children")
                .entrySet()
                .size());
        assertFalse(nestedOutputGroup.has("__plannh_quarantine.children"));

        final Graph edged = new Graph("duplicate edges");
        edged.addNode(first);
        edged.addNode(second);
        edged.addEdge(
            new Edge(
                UUID.randomUUID(),
                first.getId(),
                second.getId(),
                new IntIntMutablePair(0, 0),
                new IntIntMutablePair(0, 0)));
        final JsonObject edgeRoot = unpack(Serializer.encodeGraph(edged));
        final JsonArray edges = edgeRoot.getAsJsonArray("edges");
        edges.add(edges.get(0));
        final Graph decoded = Serializer.decodeGraph(pack(edgeRoot));
        final int decodedEdges = decoded.getEdges()
            .size();
        assertEquals(1, decodedEdges, "decoded edges: " + decodedEdges + ", JSON: " + edges);
        final JsonObject edgePreserved = unpack(Serializer.encodeGraph(decoded));
        assertEquals(
            1,
            edgePreserved.getAsJsonArray("edges")
                .size());
        final JsonObject sourceIndex = edgePreserved.getAsJsonArray("edges")
            .get(0)
            .getAsJsonObject()
            .getAsJsonObject("sourceOutputIndex");
        assertEquals(
            0,
            sourceIndex.get("left")
                .getAsInt());
        assertEquals(
            0,
            sourceIndex.get("right")
                .getAsInt());
        assertFalse(edgePreserved.has("__plannh_quarantine.edges"));
    }

    @Test
    void invalidGraphDecodeReturnsNull() throws Exception {
        assertNull(Serializer.decodeGraph("not-a-graph"));
    }

    @Test
    void malformedStateIsNotWritten() throws Exception {
        final Node valid = unavailableNode();
        final Graph graph = new Graph("valid only");
        graph.addNode(valid);
        final Node invalid = new Node(
            UUID.randomUUID(),
            "invalid",
            0,
            0,
            GtnhFlowLoader.defaultProfile(),
            RecipeContext.builder()
                .build());
        set(invalid, "machineConfig", null);
        graph.addNode(invalid);
        graph.addEdge(
            new Edge(
                UUID.randomUUID(),
                valid.getId(),
                valid.getId(),
                new IntIntMutablePair(-1, 0),
                new IntIntMutablePair(0, 0)));
        graph.addEdge(
            new Edge(
                UUID.randomUUID(),
                valid.getId(),
                UUID.randomUUID(),
                new IntIntMutablePair(0, 0),
                new IntIntMutablePair(0, 0)));
        final Group group = new Group();
        group.getChildren()
            .put(UUID.randomUUID(), unavailableNode());
        graph.getGroups()
            .put(group.getId(), group);

        final JsonObject saved = unpack(Serializer.encodeGraph(graph));

        assertEquals(
            1,
            saved.getAsJsonArray("nodes")
                .size());
        assertEquals(
            0,
            saved.getAsJsonArray("edges")
                .size());
        assertTrue(
            saved.getAsJsonArray("groups")
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("children")
                .entrySet()
                .isEmpty());
    }

    @Test
    void unknownFieldsAreDroppedOnTheNextSave() throws Exception {
        final Graph graph = new Graph("unknown");
        graph.addNode(unavailableNode());
        final JsonObject root = unpack(Serializer.encodeGraph(graph));
        root.addProperty("futureGraphField", true);
        root.getAsJsonArray("nodes")
            .get(0)
            .getAsJsonObject()
            .addProperty("futureNodeField", "discard");

        final JsonObject saved = unpack(Serializer.encodeGraph(Serializer.decodeGraph(pack(root))));
        assertFalse(saved.has("futureGraphField"));
        assertFalse(
            saved.getAsJsonArray("nodes")
                .get(0)
                .getAsJsonObject()
                .has("futureNodeField"));
    }

    private static Node unavailableNode() throws Exception {
        final Node node = new Node(
            UUID.randomUUID(),
            "missing",
            7,
            11,
            GtnhFlowLoader.defaultProfile(),
            RecipeContext.builder()
                .build());
        set(node, "recipeId", Recipe.RecipeId.of(null, "test", List.of()));
        set(node, "selectedProfile", null);
        set(node, "initialized", true);
        return node;
    }

    private static void set(final Node node, final String field, final Object value) throws Exception {
        final Field declared = Node.class.getDeclaredField(field);
        declared.setAccessible(true);
        declared.set(node, value);
    }

    private static JsonObject unpack(final String encoded) throws Exception {
        final byte[] bytes = Base64.getDecoder()
            .decode(encoded);
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(bytes));
            InputStreamReader reader = new InputStreamReader(gzip, StandardCharsets.UTF_8)) {
            return Serializer.GSON.fromJson(reader, JsonObject.class);
        }
    }

    private static String pack(final JsonObject root) throws Exception {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes);
            OutputStreamWriter writer = new OutputStreamWriter(gzip, StandardCharsets.UTF_8)) {
            writer.write(Serializer.GSON.toJson(root));
        }
        return Base64.getEncoder()
            .encodeToString(bytes.toByteArray());
    }
}
