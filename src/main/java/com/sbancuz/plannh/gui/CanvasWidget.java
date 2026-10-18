package com.sbancuz.plannh.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.layout.IViewport;
import com.cleanroommc.modularui.api.layout.IViewportStack;
import com.cleanroommc.modularui.api.widget.IDraggable;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.drawable.Stencil;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widgets.menu.Menu;
import com.sbancuz.plannh.Config;
import com.sbancuz.plannh.PlanNH;
import com.sbancuz.plannh.api.PlanAPI;
import com.sbancuz.plannh.client.ScreenEffect;
import com.sbancuz.plannh.client.UIBlurEffect;
import com.sbancuz.plannh.data.flowchart.Edge;
import com.sbancuz.plannh.data.flowchart.Graph;
import com.sbancuz.plannh.data.flowchart.GraphData;
import com.sbancuz.plannh.data.flowchart.Group;
import com.sbancuz.plannh.data.flowchart.MachineGroup;
import com.sbancuz.plannh.data.flowchart.Node;
import com.sbancuz.plannh.data.flowchart.Note;
import com.sbancuz.plannh.data.flowchart.Plan;
import com.sbancuz.plannh.data.flowchart.UndoHistory;
import com.sbancuz.plannh.gui.common.FlowchartWidget;
import com.sbancuz.plannh.gui.edge.ArrowWidget;
import com.sbancuz.plannh.gui.group.GroupWidget;
import com.sbancuz.plannh.gui.layout.ChartLayouter;
import com.sbancuz.plannh.gui.layout.ElkLayoutStrategy;
import com.sbancuz.plannh.gui.layout.LayoutStrategy;
import com.sbancuz.plannh.gui.node.NodeWidget;
import com.sbancuz.plannh.gui.node.PortWidget;
import com.sbancuz.plannh.gui.note.NoteWidget;
import com.sbancuz.plannh.nei.NEIPlanConfig;

import codechicken.lib.config.ConfigTag;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.recipe.IRecipeHandler;
import lombok.Getter;
import lombok.Setter;

public class CanvasWidget extends ParentWidget<CanvasWidget> implements Interactable, IViewport, IDraggable {

    public static final int GRID_SIZE = 20;
    private static final int GRID_MAJOR = 5;

    private static final int NODE_W_ESTIMATE = 120;
    private static final int NODE_H_ESTIMATE = 80;
    private static final int AUTO_PLACE_GAP_X = 80;
    private static final int PORT_SPACING = 18;
    private static final int PORT_ORIGIN = 10;
    private static final int MIN_GRID_SPACING = 4;

    private static final float ZOOM_STEP = 0.15f;
    private static final float ZOOM_MIN = 0.1f;
    private static final float ZOOM_MAX = 5.0f;

    // Orthogonal arrow routing (world-space units).
    /** Arrow-router granularity and node clearance, in world units. Both are the router's to define. */
    public static final int ROUTE_CELL = 6;
    public static final int ROUTE_MARGIN = 12;
    private static final ArrowRouter ARROW_ROUTER = new ArrowRouter(ROUTE_CELL, ROUTE_MARGIN);

    /**
     * The narrowest gap between two columns of machines the arrow router can still turn in.
     *
     * <p>
     * Asked of the router rather than restated, because these are its numbers. Auto-layout needs the
     * figure to size its inter-column spacing, and an earlier attempt transcribed the answer into the layout
     * settings as 24 - commented as "twice the margin plus one cell", which is 30 - and then never called
     * the check from anywhere in production.
     */
    public static int requiredRouteCorridor() {
        return ARROW_ROUTER.requiredCorridor();
    }

    /**
     * Arranges the chart. Constructed with the engine so nothing here names a concrete implementation.
     */
    @Getter
    private final ChartLayouter layouter;

    @NotNull
    @Getter
    private Graph graph;

    @Getter
    private final Map<UUID, NodeWidget> nodeWidgets = new HashMap<>();
    @Getter
    private final Map<UUID, FlowchartWidget<?, ?>> flowchartWidgets = new HashMap<>();
    @Getter
    private final Map<UUID, ArrowWidget> arrowWidgets = new HashMap<>();

    private boolean panning = false;
    private int panStartMouseX, panStartMouseY;
    private float panStartX, panStartY;

    @Getter
    private boolean menuOpen;
    private final Menu<?> contextMenu;

    @Nullable
    @Getter
    @Setter
    private PortWidget neiTransferSource;

    private final ModularPanel panel;

    private final ScreenEffect effect = new UIBlurEffect();
    private final ConfigTag showGrid;
    private final ConfigTag backgroundColor;

    private boolean needsReroute = true;

    public CanvasWidget(Menu<?> menu, ModularPanel panel) {
        this(menu, panel, new ElkLayoutStrategy());
    }

    /**
     * @param strategy the layout engine. Injected here rather than hard-wired inside {@link
     *                 ChartLayouter} because that is the one thing which varies: the layouter needs this
     *                 canvas and the canvas would otherwise need the layouter, and passing the engine in
     *                 is what breaks that cycle without giving up the seam. Nothing in this class names a
     *                 concrete implementation
     */
    public CanvasWidget(Menu<?> menu, ModularPanel panel, final LayoutStrategy strategy) {
        this.layouter = new ChartLayouter(this, strategy);
        this.graph = Plan.getActiveGraph();
        this.panel = panel;

        showGrid = NEIClientConfig.getSetting(NEIPlanConfig.ConfigShowGrid.KEY);
        backgroundColor = NEIClientConfig.getSetting(NEIPlanConfig.ConfigBackgroundColor.KEY);

        full();
        marginBottom(18);
        name("canvas");

        contextMenu = menu;
        rebuildWidgets();

        background(new DynamicDrawable(() -> new Rectangle().color(getBackgroundColor())));
    }

    /**
     * Positions an auto-wired node beside its lookup origin: producers left, consumers right.
     * Searches outward in a grid pattern from the ideal position to find the closest non-overlapping spot.
     */
    public void placeBesideOrigin(final Node added, final Node origin, final boolean addedFeedsOrigin) {
        final NodeWidget originWidget = nodeWidgets.get(origin.getId());
        if (originWidget == null) return;

        final int originW = originWidget.getArea().width;
        final int baseX = Math.round(
            addedFeedsOrigin ? origin.getX() - originW - AUTO_PLACE_GAP_X : origin.getX() + originW + AUTO_PLACE_GAP_X);
        final int baseY = Math.round(origin.getY());

        final int[] spot = findFreeSpot(baseX, baseY, NODE_W_ESTIMATE, NODE_H_ESTIMATE);
        added.setX(spot[0]);
        added.setY(spot[1]);
    }

    private boolean overlapsAnyNode(final int x, final int y, final int w, final int h) {
        for (final FlowchartWidget<?, ?> widget : flowchartWidgets.values()) {
            final GraphData n = widget.getData();
            final int nw = widget.getArea().width;
            final int nh = widget.getArea().height;

            if (x < n.getX() + nw && n.getX() < x + w && y < n.getY() + nh && n.getY() < y + h) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds a non-overlapping position near the given point, searching outward in a grid pattern.
     * Checks positions at increasing distances, preferring spots closer to the ideal location.
     */
    private int[] findFreeSpot(final int x, final int y, final int w, final int h) {
        if (!overlapsAnyNode(x, y, w, h)) return new int[] { x, y };
        final int step = GRID_SIZE;
        for (int ring = 1; ring <= 15; ring++) {
            for (int ox = -ring; ox <= ring; ox++) {
                for (int oy = -ring; oy <= ring; oy++) {
                    if (Math.max(Math.abs(ox), Math.abs(oy)) != ring) continue;
                    final int cx = x + ox * step;
                    final int cy = y + oy * step;
                    if (!overlapsAnyNode(cx, cy, w, h)) return new int[] { cx, cy };
                }
            }
        }
        return new int[] { x, y };
    }

    /**
     * Called by a FlowchartWidget after its size changes (e.g. settings opened).
     * Checks if the resized widget now overlaps any neighbor, and if so, pushes the neighbor away.
     */
    public void resolveOverlapsFrom(final FlowchartWidget<?, ?> source) {
        final GraphData srcData = source.getData();
        final int srcW = source.getArea().width;
        final int srcH = source.getArea().height;

        for (final FlowchartWidget<?, ?> other : flowchartWidgets.values()) {
            if (other == source) continue;
            final GraphData dstData = other.getData();
            final int dstW = other.getArea().width;
            final int dstH = other.getArea().height;

            if (srcData.getX() < dstData.getX() + dstW && dstData.getX() < srcData.getX() + srcW
                && srcData.getY() < dstData.getY() + dstH
                && dstData.getY() < srcData.getY() + srcH) {
                final int[] spot = findFreeSpot(dstData.getX(), dstData.getY(), dstW, dstH);

                PlanAPI.recordEdit(graph, () -> {
                    dstData.setX(spot[0]);
                    dstData.setY(spot[1]);
                    other.reposition();
                });
            }
        }
    }

    public void setGraph(final Graph newGraph) {
        final Graph oldGraph = graph;
        neiTransferSource = null;
        try {
            graph = newGraph;
            rebuildWidgets();
        } catch (final Exception | LinkageError failure) {
            graph = oldGraph;
            try {
                rebuildWidgets();
            } catch (final Exception | LinkageError rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            throw failure;
        }
    }

    /**
     * One entry point, not three: removeAll() drops every child, so a partial rebuild loses the rest.
     */
    private void rebuildWidgets() {
        removeAll();
        flowchartWidgets.clear();
        nodeWidgets.clear(); // needs to be here since groups can also add nodes
        rebuildNoteWidgets();
        rebuildGroupWidgets();
        rebuildNodeWidgets();
        rebuildArrowWidgets();
    }

    public void undoGraph() {
        // Ends any text-edit session first: its bracket must commit before the graph is swapped,
        // and committing after the redo snapshot is taken would clear the redo stack again.
        getContext().removeFocus();
        final UndoHistory history = PlanAPI.undoHistory();
        if (!history.canUndo()) return;
        try {
            history.undo(graph, this::adoptRestoredGraph);
            PlanAPI.save();
        } catch (final Exception | LinkageError failure) {
            PlanNH.LOG.error("Failed to undo graph edit", failure);
            PlanAPI.showMessage("plannh.error.history");
        }
    }

    public void redoGraph() {
        getContext().removeFocus();
        final UndoHistory history = PlanAPI.undoHistory();
        if (!history.canRedo()) return;
        try {
            history.redo(graph, this::adoptRestoredGraph);
            PlanAPI.save();
        } catch (final Exception | LinkageError failure) {
            PlanNH.LOG.error("Failed to redo graph edit", failure);
            PlanAPI.showMessage("plannh.error.history");
        }
    }

    // View and mode settings are not part of an edit, so they carry over from the live graph.
    // The active slot must adopt the restored graph, or the next save writes the pre-undo state back.
    private void adoptRestoredGraph(final Graph restored) {
        final Graph oldGraph = graph;
        final Plan plan = Plan.getInstance();
        final int activeIndex = plan.getActiveIndex();
        final Graph oldSlot = plan.getGraphs()
            .get(activeIndex);

        restored.setZoom(graph.getZoom());
        restored.setPanX(graph.getPanX());
        restored.setPanY(graph.getPanY());
        restored.setSnapToGrid(graph.isSnapToGrid());
        restored.setBalanceMode(graph.getBalanceMode());
        restored.setUndoHistory(graph.getUndoHistory());
        try {
            setGraph(restored);
            plan.getGraphs()
                .set(activeIndex, restored);
        } catch (final Exception | LinkageError failure) {
            try {
                plan.getGraphs()
                    .set(activeIndex, oldSlot);
            } catch (final Exception | LinkageError rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            try {
                setGraph(oldGraph);
            } catch (final Exception | LinkageError rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            throw failure;
        }
    }

    // todo redo
    public void recheckMembershipAndFit() {
        /*
         * for (final Node node : graph.getNodes()) {
         * updateNodeGroupMembership(node);
         * }
         * autoFitGroups();
         */
    }

    public void autoLayout() {
        layouter.autoLayout();
    }

    public void rebuildNoteWidgets() {
        for (final Note note : graph.getNotes()
            .values()) child(new NoteWidget(this, note));
    }

    public void rebuildGroupWidgets() {
        for (final Group group : graph.getGroups()
            .values()) child(GroupWidget.of(this, group));
    }

    public void rebuildNodeWidgets() {
        for (final Node node : graph.getNodes()
            .values()) {
            child(new NodeWidget(this, node));
        }
    }

    public void rebuildArrowWidgets() {
        arrowWidgets.clear();
        for (final Edge edge : graph.getEdges()
            .values()) {
            if (hasVisibleEndpoints(edge)) child(new ArrowWidget(this, edge));
        }
    }

    private boolean hasVisibleEndpoints(final Edge edge) {
        final NodeWidget source = nodeWidgets.get(edge.getSourceNodeId());
        final NodeWidget target = nodeWidgets.get(edge.getTargetNodeId());
        return source != null && target != null
            && edge.isActive(source.getData(), target.getData())
            && source.getPortWidget(edge.getSourceOutputIndex(), false) != null
            && target.getPortWidget(edge.getTargetInputIndex(), true) != null;
    }

    public int getBackgroundColor() {
        return backgroundColor.getHexValue(PlannhColors.BACKGROUND.getColor());
    }

    @Override
    public void draw(final ModularGuiContext context, final WidgetThemeEntry<?> widgetTheme) {
        final int width = getArea().width;
        final int height = getArea().height;
        final int x = getArea().x;
        final int y = getArea().y;
        if (width <= 0 || height <= 0) return;

        super.draw(context, widgetTheme);
        effect.execute(x, y, width, height);
        if (showGrid.getIntValue(NEIPlanConfig.ConfigShowGrid.ON) == NEIPlanConfig.ConfigShowGrid.ON) {
            drawGrid(width, height);
        }
    }

    private void drawGrid(final int w, final int h) {
        final float spacing = GRID_SIZE * graph.getZoom();
        if (spacing < MIN_GRID_SPACING) return;

        final int gridColor = PlannhColors.GRID_LINE.getColor();
        final int majorColor = PlannhColors.GRID_MAJOR.getColor();

        final int firstKX = (int) Math.ceil(-graph.getPanX() / spacing);
        final float startX = graph.getPanX() + firstKX * spacing;
        for (float x = startX; x < w; x += spacing) {
            final int k = firstKX + Math.round((x - startX) / spacing);
            GuiDraw.drawRect(Math.round(x), 0, 1, h, k % GRID_MAJOR == 0 ? majorColor : gridColor);
        }

        final int firstKY = (int) Math.ceil(-graph.getPanY() / spacing);
        final float startY = graph.getPanY() + firstKY * spacing;
        for (float y = startY; y < h; y += spacing) {
            final int k = firstKY + Math.round((y - startY) / spacing);
            GuiDraw.drawRect(0, Math.round(y), w, 1, k % GRID_MAJOR == 0 ? majorColor : gridColor);
        }
    }

    /**
     * World-space (un-zoomed) Y of a port relative to the node's top-left corner.
     */
    private static int portWorldY(final int index) {
        return (index + 1) * PORT_SPACING + PORT_ORIGIN;
    }

    public int getCanvasMouseX() {
        return Math.round((getCanvasPosX(getContext().getAbsMouseX()) - graph.getPanX()) / graph.getZoom());
    }

    public int getCanvasMouseY() {
        return Math.round((getCanvasPosY(getContext().getAbsMouseY()) - graph.getPanY()) / graph.getZoom());
    }

    public int getCanvasScreenCenterX() {
        return Math.round((getCanvasPosX(getArea().width / 2) - graph.getPanX()) / graph.getZoom());
    }

    public int getCanvasScreenCenterY() {
        return Math.round((getCanvasPosY(getArea().height / 2) - graph.getPanY()) / graph.getZoom());
    }

    public int getCanvasPosX(int absPosX) {
        return absPosX - getArea().x;
    }

    public int getCanvasPosY(int absPosY) {
        return absPosY - getArea().y;
    }

    /**
     * Recomputes orthogonal arrow routes when the graph layout changes.
     * Routes are stored in world space so panning does not invalidate them.
     */
    private void ensureRoutes() {
        if (!needsReroute) return;

        final List<ArrowRouter.Rect> obstacles = flowchartWidgets.values()
            .stream()
            .filter(FlowchartWidget::isObstacle)
            .map(flowchartWidget -> {
                Area widgetArea = flowchartWidget.getArea();
                return new ArrowRouter.Rect(
                    getCanvasPosX(widgetArea.x),
                    getCanvasPosY(widgetArea.y),
                    widgetArea.width,
                    widgetArea.height,
                    flowchartWidget.getData()
                        .getId());
            })
            .toList();

        final List<ArrowRouter.Request> requests = new ArrayList<>();
        for (final Edge edge : graph.getEdges()
            .values()) {
            if (!hasVisibleEndpoints(edge) || !arrowWidgets.containsKey(edge.getId())) continue;
            final NodeWidget src = nodeWidgets.get(edge.getSourceNodeId());
            final NodeWidget dst = nodeWidgets.get(edge.getTargetNodeId());
            if (src == null || dst == null) continue;

            Area srcArea = src.getPortWidget(edge.getSourceOutputIndex(), false)
                .getArea();
            Area dstArea = dst.getPortWidget(edge.getTargetInputIndex(), true)
                .getArea();

            requests.add(
                new ArrowRouter.Request(
                    edge.getId(),
                    getCanvasPosX(srcArea.x + srcArea.width / 2),
                    getCanvasPosY(srcArea.y + srcArea.height / 2),
                    getCanvasPosX(dstArea.x + dstArea.width / 2),
                    getCanvasPosY(dstArea.y + dstArea.height / 2),
                    src.getData()
                        .getId(),
                    dst.getData()
                        .getId()));
        }

        // todo refresh this comment
        // Chips are no-turn zones rather than obstacles: a chip sits directly on the approach to
        // its own port, so blocking it would seal the only way in and drop the edge to a
        // straight-line fallback that ignores everything. Passing behind a label is fine; turning
        // under one is what reads as the arrow terminating there.
        final Set<UUID> fellBack = new HashSet<>();
        Map<UUID, List<int[]>> edgeRoutes = ARROW_ROUTER.route(obstacles, new ArrayList<>(), requests, fellBack);
        edgeRoutes.forEach(
            (uuid, coords) -> arrowWidgets.get(uuid)
                .setCoords(coords));

        // A fallback ignores every obstacle, so it is the one route that can end up crossing a
        // node or cornering under a label however the zones are set up. Worth saying out loud
        // rather than leaving someone to infer it from a screenshot.
        if (!fellBack.isEmpty()) {
            PlanNH.LOG
                .info("Arrow routing fell back for {} of {} edges: {}", fellBack.size(), requests.size(), fellBack);
        }

        needsReroute = false;

        if (!Config.debugRouteDump) return;

        // The routing input replays headlessly (ArrowRouter is Minecraft-free); dump it on
        // every recompute so any bad-looking route can be rebuilt from the dev log.
        PlanNH.LOG.info(reproDump(obstacles, requests));

        // A route several times longer than its direct distance means the router wrapped
        // around the chart; call it out so the dump above gets looked at.
        for (final ArrowRouter.Request q : requests) {
            final List<int[]> path = edgeRoutes.get(q.key());
            if (path == null) continue;
            int len = 0;
            for (int i = 1; i < path.size(); i++) {
                len += Math.abs(path.get(i)[0] - path.get(i - 1)[0]) + Math.abs(path.get(i)[1] - path.get(i - 1)[1]);
            }
            final int direct = Math.abs(q.dx() - q.sx()) + Math.abs(q.dy() - q.sy());
            if (len <= direct * 3 + 200) continue;
            PlanNH.LOG.info(
                "Arrow route wrapped: edge {} ({},{})->({},{}) len={} direct={}",
                q.key(),
                q.sx(),
                q.sy(),
                q.dx(),
                q.dy(),
                len,
                direct);
        }
    }

    private static String reproDump(final List<ArrowRouter.Rect> obstacles, final List<ArrowRouter.Request> requests) {
        final StringBuilder sb = new StringBuilder("Route repro: obstacles=");
        for (final ArrowRouter.Rect r : obstacles) {
            sb.append(r.x())
                .append(',')
                .append(r.y())
                .append(',')
                .append(r.w())
                .append(',')
                .append(r.h())
                .append(';');
        }
        sb.append(" requests=");
        for (final ArrowRouter.Request r : requests) {
            sb.append(r.sx())
                .append(',')
                .append(r.sy())
                .append(',')
                .append(r.dx())
                .append(',')
                .append(r.dy())
                .append(';');
        }
        return sb.toString();
    }

    @Override
    public @NotNull Result onMousePressed(final int mouseButton) {
        // Close context menu on any click
        menuOpen = false;

        if (mouseButton == 1) {
            /*
             * // Check if over a group header (pass click through for its own right-click menu)
             * for (final GroupWidget gw : groupWidgets.values()) {
             * final Area a = gw.getArea();
             * if (containsPoint(a, absMx, absMy)) {
             * showGroupContextMenu(gw, absMx, absMy);
             * return Result.SUCCESS;
             * }
             * }
             * // Check if over a node
             * for (final RecipeNodeWidget widget : nodeWidgets.values()) {
             * final Area a = widget.getArea();
             * if (containsPoint(a, absMx, absMy)) {
             * showNodeContextMenu(widget, absMx, absMy);
             * return Result.SUCCESS;
             * }
             * }
             */

            // Empty canvas -> open context menu
            openContextMenu();
            return Result.SUCCESS;
        }
        return Result.IGNORE;
    }

    private void openContextMenu() {
        contextMenu.pos(getContext().getAbsMouseX(), getContext().getAbsMouseY());
        menuOpen = true;
    }

    @Override
    public boolean onMouseScroll(final UpOrDown direction, final int amount) {
        // Close context menu on any scroll ?
        // menuOpen = false;

        float delta = direction == UpOrDown.UP ? ZOOM_STEP : -ZOOM_STEP;
        delta *= Math.max(1, Math.abs(amount));
        final float oldZoom = graph.getZoom();
        graph.setZoom(Math.clamp(graph.getZoom() + delta, ZOOM_MIN, ZOOM_MAX));
        final float ratio = graph.getZoom() / oldZoom;

        final float mxRel = getCanvasPosX(getContext().getAbsMouseX());
        final float myRel = getCanvasPosY(getContext().getAbsMouseY());

        graph.setPanX(mxRel - (mxRel - graph.getPanX()) * ratio);
        graph.setPanY(myRel - (myRel - graph.getPanY()) * ratio);

        // updatePositions();
        return true;
    }

    public void addNote(int x, int y) {
        PlanAPI.recordEdit(graph, () -> {
            Note note = new Note();
            note.setX(x);
            note.setY(y);

            graph.getNotes()
                .put(note.getId(), note);
            child(new NoteWidget(this, note));
        });

        menuOpen = false;
    }

    public void addGroup(int x, int y) {
        addGroup(x, y, new Group());
    }

    /** A group whose recipes share one machine; see {@link MachineGroup}. */
    public void addMachineGroup(int x, int y) {
        addGroup(x, y, new MachineGroup());
    }

    private void addGroup(final int x, final int y, final Group group) {
        PlanAPI.recordEdit(graph, () -> {
            group.setX(x);
            group.setY(y);

            graph.getGroups()
                .put(group.getId(), group);
            child(GroupWidget.of(this, group));
        });

        menuOpen = false;
    }

    public void addNode(int x, int y, IRecipeHandler handler, int recipeIndex) {
        Node node = null;
        final Set<UUID> previousEdges = new HashSet<>(
            graph.getEdges()
                .keySet());
        try {
            node = new Node(handler, recipeIndex);
            if (!node.isAvailable()) {
                PlanAPI.showMessage("plannh.error.recipe_add");
                return;
            }
            final String before = graph.getUndoHistory()
                .beginEdit(graph);
            if (neiTransferSource != null) {
                final PortWidget portWidget = neiTransferSource;
                final Node origin = portWidget.getNode();
                final boolean addedFeedsOrigin = !portWidget.getPortType()
                    .isEdgeSource();
                placeBesideOrigin(node, origin, addedFeedsOrigin);
            } else {
                final int[] spot = findFreeSpot(x, y, NODE_W_ESTIMATE, NODE_H_ESTIMATE);
                node.setX(spot[0]);
                node.setY(spot[1]);
            }

            NodeWidget nodeWidget = new NodeWidget(this, node);
            graph.addNode(node);
            child(nodeWidget);

            if (neiTransferSource != null) {
                PortWidget portWidget = neiTransferSource;
                PortWidget.PortType portType = neiTransferSource.getPortType();

                nodeWidget.getPortWidgets(portType.isEdgeSource())
                    .values()
                    .stream()
                    .filter(other -> other.canConnect(portWidget))
                    .findFirst()
                    .ifPresent(other -> {
                        PortWidget source;
                        PortWidget target;

                        if (portType.isEdgeSource()) {
                            source = portWidget;
                            target = other;
                        } else {
                            source = other;
                            target = portWidget;
                        }

                        PortWidget.addArrow(this, source, target);
                    });

            }
            graph.getUndoHistory()
                .commitEdit(before, graph);
            neiTransferSource = null;
            menuOpen = false;
        } catch (final Exception | LinkageError failure) {
            rollbackFailedNode(node, previousEdges, failure);
            PlanNH.LOG.error("Failed to add recipe node", failure);
            PlanAPI.showMessage(
                "plannh.error.recipe_add",
                handler == null ? "null"
                    : handler.getClass()
                        .getName(),
                recipeIndex);
        }
    }

    private void rollbackFailedNode(final Node node, final Set<UUID> previousEdges, final Throwable failure) {
        if (node == null) return;

        try {
            graph.getNodes()
                .remove(node.getId());
        } catch (final Exception | LinkageError cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        }

        for (final UUID edgeId : new HashSet<>(
            graph.getEdges()
                .keySet())) {
            if (previousEdges.contains(edgeId)) continue;
            try {
                graph.getEdges()
                    .remove(edgeId);
            } catch (final Exception | LinkageError cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            final ArrowWidget arrow = arrowWidgets.remove(edgeId);
            if (arrow == null) continue;
            try {
                nodeWidgets.values()
                    .forEach(
                        widget -> widget.getArrowWidgets()
                            .remove(arrow));
            } catch (final Exception | LinkageError cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            try {
                if (arrow.getParent() == this) remove(arrow);
            } catch (final Exception | LinkageError cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
        }

        final FlowchartWidget<?, ?> widget = flowchartWidgets.remove(node.getId());
        nodeWidgets.remove(node.getId());
        if (widget != null) {
            try {
                if (widget.getParent() == this) remove(widget);
                else widget.dispose();
            } catch (final Exception | LinkageError cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
        }
        try {
            graph.bumpVersion();
        } catch (final Exception | LinkageError cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        }
        try {
            needsReroute();
        } catch (final Exception | LinkageError cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        }
    }

    @Override
    public void transformChildren(IViewportStack stack) {
        stack.translate(graph.getPanX(), graph.getPanY());
        stack.scale(graph.getZoom(), graph.getZoom());
    }

    @Override
    public void preDraw(ModularGuiContext context, boolean transformed) {
        if (!transformed) {
            Stencil.applyAtZero(getArea(), context);
        }
    }

    @Override
    public void postDraw(ModularGuiContext context, boolean transformed) {
        if (!transformed) {
            Stencil.remove();
        }
    }

    @Override
    public void drawMovingState(ModularGuiContext context, float partialTicks) {}

    @Override
    public boolean onDragStart(int button) {
        if (button == 0 || button == 2) {
            panStartX = graph.getPanX();
            panStartY = graph.getPanY();
            panStartMouseX = getContext().getAbsMouseX();
            panStartMouseY = getContext().getAbsMouseY();
            return true;
        }
        return false;
    }

    @Override
    public void onDragEnd(boolean successful) {
        if (!successful) {
            graph.setPanX(panStartX);
            graph.setPanY(panStartY);
        }
    }

    @Override
    public void onDrag(int mouseButton, long timeSinceLastClick) {
        final int dx = getContext().getAbsMouseX() - panStartMouseX;
        final int dy = getContext().getAbsMouseY() - panStartMouseY;
        graph.setPanX(panStartX + dx);
        graph.setPanY(panStartY + dy);
    }

    @Override
    public @Nullable Area getMovingArea() {
        return null;
    }

    @Override
    public boolean isMoving() {
        return panning;
    }

    @Override
    public void setMoving(boolean moving) {
        panning = moving;
    }

    public boolean isMouseInsideCanvas() {
        ModularGuiContext context = getContext();
        return isInside(context, getCanvasPosX(context.getAbsMouseX()), getCanvasPosY(context.getAbsMouseY()), false);
    }

    @Override
    public @NotNull ModularPanel getPanel() {
        return panel;
    }

    @Override
    public void postResize() {
        ensureRoutes();
    }

    public void needsReroute() {
        needsReroute = true;
    }
}
