package com.sbancuz.plannh.data.flowchart;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

import com.sbancuz.plannh.api.PlanAPI;
import com.sbancuz.plannh.data.serialization.Serializer;

/**
 * Stack-based undo/redo over whole-graph snapshots.
 *
 * <p>
 * Every user edit is bracketed by {@link #beginEdit} / {@link #commitEdit}: begin captures the
 * pre-edit state as a token the caller holds, commit records it only if the edit actually changed
 * something. Snapshots reuse the save-file encoding, so compound edits like cascading deletes need
 * no special handling.
 *
 * <p>
 * Camera state is normalized out of snapshots (and re-applied by the caller on restore), so
 * panning between edits neither pollutes the stack nor teleports the view on undo.
 */
public final class UndoHistory {

    private static final int MAX_DEPTH = 64;

    private final Deque<String> undoStack = new ArrayDeque<>();
    private final Deque<String> redoStack = new ArrayDeque<>();

    /** Captures the state about to be edited; pass the token to {@link #commitEdit}. */
    public String beginEdit(final Graph graph) {
        return encodeNormalized(graph);
    }

    /** Records the pre-edit state; no-op edits leave no trace. */
    public void commitEdit(final String before, final Graph graph) {
        if (before == null || before.equals(encodeNormalized(graph))) return;
        pushLimited(undoStack, before);
        redoStack.clear();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /** Camera fields come back at defaults. */
    public Graph undo(final Graph current) {
        return undo(current, ignored -> {});
    }

    /**
     * Restores the next undo graph through {@code adopter}. History is changed only after the
     * adopter returns, so a failed widget rebuild leaves both stacks untouched.
     */
    public Graph undo(final Graph current, final Consumer<Graph> adopter) {
        return restore(current, adopter, undoStack, redoStack);
    }

    /** Camera fields come back at defaults. */
    public Graph redo(final Graph current) {
        return redo(current, ignored -> {});
    }

    /** See {@link #undo(Graph, Consumer)}. */
    public Graph redo(final Graph current, final Consumer<Graph> adopter) {
        return restore(current, adopter, redoStack, undoStack);
    }

    private Graph restore(final Graph current, final Consumer<Graph> adopter, final Deque<String> source,
        final Deque<String> destination) {
        if (source.isEmpty()) return current;

        final String target = source.peek();
        final String currentSnapshot = encodeNormalized(current);
        final Graph restored = Serializer.decodeGraph(target);
        if (restored == null) {
            PlanAPI.showMessage("plannh.error.history");
            return current;
        }

        // Adoption may rebuild a widget tree and is allowed to fail. Do not touch either stack first.
        adopter.accept(restored);

        pushLimited(destination, currentSnapshot);
        source.pop();
        return restored;
    }

    private static void pushLimited(final Deque<String> stack, final String snapshot) {
        stack.push(snapshot);
        while (stack.size() > MAX_DEPTH) stack.removeLast();
    }

    /**
     * Encodes with the camera zeroed so snapshots compare by content. The setters are plain
     * field writes, so the flip-encode-restore is invisible to everything else.
     */
    private static String encodeNormalized(final Graph graph) {
        final float zoom = graph.getZoom();
        final float panX = graph.getPanX();
        final float panY = graph.getPanY();
        graph.setZoom(1);
        graph.setPanX(0);
        graph.setPanY(0);
        try {
            return Serializer.encodeGraph(graph);
        } finally {
            graph.setZoom(zoom);
            graph.setPanX(panX);
            graph.setPanY(panY);
        }
    }
}
