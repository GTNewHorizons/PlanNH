package com.sbancuz.plannh.data.flowchart;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sbancuz.plannh.data.flowchart.balancer.BalanceMode;
import com.sbancuz.plannh.data.flowchart.balancer.BalanceResult;
import com.sbancuz.plannh.data.flowchart.balancer.BalanceView;
import com.sbancuz.plannh.data.flowchart.balancer.ChoiceKey;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Graph {

    /**
     * Sorted, so the graph hands its contents back in id order and every consumer that needs a
     * reproducible answer gets one without sorting first - the solver, the serializer, the router
     * and the layout all read these directly.
     */
    private final GraphIndex<Node> nodes = new GraphIndex<>();
    private final GraphIndex<Edge> edges = new GraphIndex<>();
    private final GraphIndex<Note> notes = new GraphIndex<>();
    private final GraphIndex<Group> groups = new GraphIndex<>();

    @Setter
    private String name;

    @Setter
    private float zoom = 1f;
    @Setter
    private float panX;
    @Setter
    private float panY;
    @Setter
    private boolean snapToGrid;

    private BalanceMode balanceMode = BalanceMode.AUTO;

    /**
     * Per-graph undo/redo stack, transient because snapshots are content-encoded and never stored.
     */
    @Setter
    private transient UndoHistory undoHistory = new UndoHistory();

    /**
     * The display view, built on first ask after a solve rather than with it: the canvas wants the
     * boundary every frame and never the choices, which the summary reads straight from the solve.
     */
    private transient List<BalanceView.Boundary> boundaryView = null;

    /**
     * Monotonic counter bumped on every mutation; derived caches (the solve, the summary, the
     * boundary view) each compare against it to know when they are stale. Transient because
     * a loaded plan starts cold and re-derives everything on first ask.
     */
    private transient long version = 0;

    /**
     * The graph version the solve caches above were built from.
     */
    private transient long solvedAt = -1;

    public Graph() {
        this.name = "";
    }

    public Graph(final String name) {
        this.name = name;
    }

    /** Configures a filtered save view without removing entries from the live indexes. */
    public void prepareForSave() {
        final Set<UUID> ids = allNodes().map(Node::getId)
            .collect(Collectors.toSet());
        edges.setValidator(edge -> ids.contains(edge.getSourceNodeId()) && ids.contains(edge.getTargetNodeId()));
    }

    /** Repairs loaded model state and returns the number of discarded entries. */
    public int validate() {
        final int malformed = nodes.validate() + notes.validate() + groups.validate();
        prepareForSave();
        if (name == null) name = "";
        if (balanceMode == null) balanceMode = BalanceMode.AUTO;
        return malformed + edges.validate();
    }

    /** Grouped nodes belong to their group's children, not the top-level node index. */
    public Stream<Node> allNodes() {
        return Stream.concat(
            nodes.values()
                .stream(),
            groups.values()
                .stream())
            .flatMap(Graph::nodesIn);
    }

    private static Stream<Node> nodesIn(final GraphData data) {
        if (data.invalid()) return Stream.empty();
        if (data instanceof Node node) return Stream.of(node);
        if (data instanceof Group group) {
            return group.getChildren()
                .entrySet()
                .stream()
                .filter(
                    entry -> entry.getValue() != null && entry.getKey()
                        .equals(
                            entry.getValue()
                                .getId()))
                .flatMap(entry -> nodesIn(entry.getValue()));
        }
        return Stream.empty();
    }

    /**
     * Mutations bump the graph version internally; callers that change solve-relevant
     * state should route through the graph's own methods instead. This is closely related to the maps at
     * the beginning which should have proper accessors
     */
    public void bumpVersion() {
        version++;
        boundaryView = null;
    }

    public ChoiceKey getExcessChoice() {
        return Plan.getInstance()
            .getSummary()
            .getExcessChoice();
    }

    public void setExcessChoice(final ChoiceKey choice) {
        Plan.getInstance()
            .getSummary()
            .setExcessChoice(choice);
        bumpVersion();
    }

    public void setBalanceMode(final BalanceMode mode) {
        balanceMode = mode;
        bumpVersion();
    }

    public void removeNode(final UUID id) {
        nodes.remove(id);

        edges.values()
            .removeIf(
                e -> e.getSourceNodeId()
                    .equals(id)
                    || e.getTargetNodeId()
                        .equals(id));
        bumpVersion();
    }

    public BalanceResult balance() {
        if (solvedAt != version) {
            Plan.getInstance()
                .getSummary()
                .recompute(this);
            solvedAt = version;
        }
        return Plan.getInstance()
            .getSummary()
            .balance();
    }

    /**
     * Everything crossing the chart's boundary. Held from solve to solve because the canvas asks
     * once per frame and the answer only moves when the chart does.
     */
    public List<BalanceView.Boundary> boundary() {
        balance(); // drops a view built before the last edit
        if (boundaryView == null) boundaryView = BalanceView.boundary(this);
        return boundaryView;
    }

    public void addNode(final Node node) {
        nodes.put(node.id, node);
        bumpVersion();
    }

    public void addEdge(final Edge edge) {
        edges.values()
            .removeIf(e -> e.sameConnection(edge));
        edges.put(edge.getId(), edge);
        bumpVersion();
    }

    public void removeEdge(final UUID id) {
        edges.remove(id);
        bumpVersion();
    }
}
