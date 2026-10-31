package com.sbancuz.plannh.data.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
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
        final Graph graph = new Graph("unavailable recipe");
        graph.addNode(node);

        final Graph loaded = Serializer.decodeGraph(Serializer.encodeGraph(graph));
        loaded.getNodes()
            .get(node.getId())
            .setX(101);
        final Node restored = Serializer.decodeGraph(Serializer.encodeGraph(loaded))
            .getNodes()
            .get(node.getId());

        assertFalse(restored.isAvailable());
        assertEquals(101, restored.getX());
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

}
