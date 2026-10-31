package com.sbancuz.plannh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.flowchart.Edge;
import com.sbancuz.plannh.data.flowchart.Graph;
import com.sbancuz.plannh.data.flowchart.MachineGroup;
import com.sbancuz.plannh.data.flowchart.Node;
import com.sbancuz.plannh.data.flowchart.Summary;
import com.sbancuz.plannh.data.flowchart.balancer.BalanceMode;
import com.sbancuz.plannh.data.flowchart.balancer.BalanceResult;
import com.sbancuz.plannh.data.flowchart.balancer.BalanceView;
import com.sbancuz.plannh.data.flowchart.balancer.Balancer;
import com.sbancuz.plannh.data.flowchart.balancer.ModelData;
import com.sbancuz.plannh.data.flowchart.balancer.Pin;
import com.sbancuz.plannh.data.serialization.Serializer;
import com.sbancuz.plannh.data.setting.Settings;
import com.sbancuz.plannh.harness.GtnhFlowLoader;
import com.sbancuz.plannh.harness.TestIngredients;

import codechicken.nei.recipe.Recipe;
import it.unimi.dsi.fastutil.ints.IntIntMutablePair;

class UnavailableNodeCalculationTest {

    @Test
    void unavailableNodeContributesNothingToBalanceOrTopologyDespiteStaleState() {
        final Graph graph = new Graph();
        final Node available = node("available", List.of(), List.of("wire"), 2);
        final Node unavailable = node("unavailable", List.of("wire"), List.of(), 99);
        unavailable.getMachineConfig()
            .getRates()
            .rates()
            .put(0, 500d);
        unavailable.getMachineConfig()
            .setTargetKind(Pin.TARGET_RATE);
        markUnavailable(unavailable);
        graph.addNode(available);
        graph.addNode(unavailable);
        graph.addEdge(edge(available, unavailable));

        final Balancer.RunContext run = Balancer.run(BalanceMode.AUTO, graph);
        final ModelData model = run.ctx().model;

        assertEquals(1, model.machines.size());
        assertEquals(
            available.getId(),
            model.machines.get(0)
                .node()
                .getId());
        assertTrue(model.edges.isEmpty(), "an edge touching an unavailable node is not runtime topology");
        assertTrue(model.connectedPorts.isEmpty());
        assertTrue(model.gates.isEmpty());
        assertFalse(run.ctx().anyPin, "a stale target on an unavailable node is not a solver pin");

        final BalanceResult result = Balancer.balance(graph, BalanceMode.NONE);
        assertEquals(2, result.totalOperations(), 1e-9);
        assertEquals(
            List.of(available.getId()),
            result.nodeBalances()
                .keySet()
                .stream()
                .toList());
        assertEquals(
            2,
            graph.getNodes()
                .size(),
            "the graph still owns both nodes");
        assertEquals(
            1,
            graph.getEdges()
                .size(),
            "the graph still owns the edge");
        assertEquals(
            1,
            unavailable.getInputs()
                .size(),
            "unavailable ports are preserved");
        assertEquals(
            500,
            unavailable.getMachineConfig()
                .getRates()
                .rate(0),
            1e-9,
            "unavailable settings are preserved");
    }

    @Test
    void unavailableNodeDoesNotConsumeMachineGroupPoolMembership() {
        final Graph graph = new Graph();
        final Node available = node("available", List.of(), List.of("product"), 1);
        final Node unavailable = node("unavailable", List.of(), List.of(), 7);
        markUnavailable(unavailable);
        graph.addNode(available);
        graph.addNode(unavailable);

        final MachineGroup group = new MachineGroup();
        group.setMachineCapacity(1);
        group.getChildren()
            .put(available.getId(), available);
        group.getChildren()
            .put(unavailable.getId(), unavailable);
        graph.getGroups()
            .put(group.getId(), group);

        final ModelData model = Balancer.run(BalanceMode.NONE, graph)
            .ctx().model;

        assertEquals(1, model.pools.size());
        assertEquals(
            List.of(0),
            model.pools.get(0)
                .machines());
        assertEquals(
            1,
            model.pools.get(0)
                .capacity());
    }

    @Test
    void unavailableNodeIsAbsentFromSummaryAndBoundaryEvenWithAnEdge() {
        final Graph graph = new Graph();
        graph.setBalanceMode(BalanceMode.NONE);
        final Node available = node("available", List.of(), List.of("product"), 2);
        final Node unavailable = node("unavailable", List.of("product"), List.of(), 99);
        markUnavailable(unavailable);
        graph.addNode(available);
        graph.addNode(unavailable);
        graph.addEdge(edge(available, unavailable));

        final Summary summary = new Summary().recompute(graph);

        assertEquals(
            List.of("product"),
            summary.lines(Summary.Section.OUTPUTS)
                .stream()
                .map(Summary.Line::displayName)
                .toList());
        assertTrue(
            summary.lines(Summary.Section.MACHINE_COUNTS)
                .stream()
                .filter(Summary.Line.Measure.class::isInstance)
                .map(Summary.Line.Measure.class::cast)
                .anyMatch(
                    line -> line.displayName()
                        .equals("available")));
        assertTrue(
            summary.lines(Summary.Section.MACHINE_COUNTS)
                .stream()
                .filter(Summary.Line.Measure.class::isInstance)
                .map(Summary.Line.Measure.class::cast)
                .noneMatch(
                    line -> line.displayName()
                        .equals("unavailable")));
        assertEquals(
            2,
            summary.lines(Summary.Section.MACHINE_COUNTS)
                .stream()
                .filter(Summary.Line.Totals.class::isInstance)
                .map(Summary.Line.Totals.class::cast)
                .findFirst()
                .orElseThrow()
                .operations(),
            1e-9);

        final List<BalanceView.Boundary> boundary = BalanceView.boundary(graph);
        assertEquals(1, boundary.size());
        assertEquals(
            "product",
            boundary.get(0)
                .ingredient());
        assertEquals(
            available.getId(),
            boundary.get(0)
                .port()
                .nodeId());
    }

    @Test
    void removingTheLastRecipeClearsTheSummary() {
        final Graph graph = new Graph();
        graph.setBalanceMode(BalanceMode.NONE);
        final Node recipe = node("recipe", List.of(), List.of("product"), 2);
        graph.addNode(recipe);
        final Summary summary = new Summary().recompute(graph);
        assertFalse(
            summary.lines(Summary.Section.OUTPUTS)
                .isEmpty());

        graph.removeNode(recipe.getId());
        summary.recompute(graph);

        assertEquals(graph.getVersion(), summary.calculatedAt());
        assertTrue(
            summary.lines(Summary.Section.OUTPUTS)
                .isEmpty());
        assertTrue(
            summary.lines(Summary.Section.INPUTS)
                .isEmpty());
        assertTrue(
            summary.lines(Summary.Section.MACHINE_COUNTS)
                .isEmpty());
        assertTrue(
            summary.balance()
                .nodeBalances()
                .isEmpty());
    }

    @Test
    void resourceMismatchAtSamePortIndicesIsExcludedFromSolverAndConfiguredBoundary() {
        final Graph graph = connectedGraph("source", "target");
        final Edge edge = graph.getEdges()
            .values()
            .iterator()
            .next();
        final Node source = graph.getNodes()
            .get(edge.getSourceNodeId());
        final Node target = graph.getNodes()
            .get(edge.getTargetNodeId());
        final ModelData model = Balancer.run(BalanceMode.NONE, graph)
            .ctx().model;

        assertTrue(model.edges.isEmpty(), "same indices do not make different resources connect");
        assertEquals(
            List.of("source", "target"),
            configuredBoundary(graph).stream()
                .map(BalanceView.Boundary::ingredient)
                .sorted()
                .toList());
    }

    @Test
    void persistedInactiveEdgeIsExcludedEvenWhenBothNodesAreAvailable() {
        final Graph graph = connectedGraph("wire", "wire");
        final Edge edge = graph.getEdges()
            .values()
            .iterator()
            .next();
        final Node target = graph.getNodes()
            .get(edge.getTargetNodeId());
        edge.setInactive(true);

        final ModelData model = Balancer.run(BalanceMode.NONE, graph)
            .ctx().model;

        assertTrue(edge.isInactive());
        assertFalse(
            edge.isActive(
                graph.getNodes()
                    .get(edge.getSourceNodeId()),
                graph.getNodes()
                    .get(edge.getTargetNodeId())));
        assertTrue(model.edges.isEmpty());
        assertEquals(2, configuredBoundary(graph).size(), "inactive wiring leaves both ports at the boundary");
    }

    @Test
    void inactiveEdgeFlagSurvivesGraphSerialization() throws Exception {
        final Node source = serializableNode("source");
        final Node target = serializableNode("target");
        final Graph graph = new Graph();
        graph.addNode(source);
        graph.addNode(target);
        final Edge edge = edge(source, target);
        edge.setInactive(true);
        graph.addEdge(edge);

        final Graph restored = Serializer.decodeGraph(Serializer.encodeGraph(graph));

        assertTrue(
            restored.getEdges()
                .get(edge.getId())
                .isInactive());
    }

    private static Node node(final String name, final List<String> inputs, final List<String> outputs,
        final int copies) {
        final RecipeContext.Builder recipe = RecipeContext.builder();
        inputs.forEach(input -> recipe.input(TestIngredients.port(input, 1)));
        outputs.forEach(output -> recipe.output(TestIngredients.port(output, 1)));
        final Node node = new Node(UUID.randomUUID(), name, 0, 0, GtnhFlowLoader.defaultProfile(), recipe.build());
        node.getMachineConfig()
            .set(Settings.MACHINES, copies);
        return node;
    }

    private static Edge edge(final Node source, final Node target) {
        return new Edge(
            UUID.randomUUID(),
            source.getId(),
            target.getId(),
            new IntIntMutablePair(0, 0),
            new IntIntMutablePair(0, 0));
    }

    private static Graph connectedGraph(final String sourceName, final String targetName) {
        final Graph graph = new Graph();
        final Node source = node(sourceName, List.of(), List.of(sourceName), 1);
        final Node target = node(targetName, List.of(targetName), List.of(), 1);
        graph.addNode(source);
        graph.addNode(target);
        graph.addEdge(edge(source, target));
        return graph;
    }

    @SuppressWarnings("unchecked")
    private static List<BalanceView.Boundary> configuredBoundary(final Graph graph) {
        try {
            final Method method = BalanceView.class.getDeclaredMethod("configuredBoundary", Graph.class);
            method.setAccessible(true);
            return (List<BalanceView.Boundary>) method.invoke(null, graph);
        } catch (final ReflectiveOperationException failure) {
            throw new AssertionError("configured boundary calculation changed", failure);
        }
    }

    private static Node serializableNode(final String name) throws Exception {
        final Node node = node(name, List.of(name), List.of(name), 1);
        set(node, "recipeId", Recipe.RecipeId.of(null, name, List.of()));
        set(node, "selectedProfile", null);
        set(node, "initialized", true);
        return node;
    }

    private static void set(final Node node, final String field, final Object value) throws Exception {
        final Field declared = Node.class.getDeclaredField(field);
        declared.setAccessible(true);
        declared.set(node, value);
    }

    private static void markUnavailable(final Node node) {
        try {
            final Field selectedProfile = Node.class.getDeclaredField("selectedProfile");
            selectedProfile.setAccessible(true);
            selectedProfile.set(node, null);
        } catch (final ReflectiveOperationException failure) {
            throw new AssertionError("Node selected profile field changed", failure);
        }
    }
}
