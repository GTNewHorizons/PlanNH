package com.sbancuz.plannh.data.flowchart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class UndoHistoryTransactionTest {

    @Test
    void failedAdoptionLeavesBothStacksUntouched() {
        final Graph graph = new Graph("before");
        final UndoHistory history = graph.getUndoHistory();
        final String before = history.beginEdit(graph);
        graph.setName("after");
        history.commitEdit(before, graph);

        assertThrows(
            IllegalStateException.class,
            () -> history.undo(graph, ignored -> { throw new IllegalStateException("rebuild failed"); }));

        assertTrue(history.canUndo());
        assertFalse(history.canRedo());
        assertEquals("after", graph.getName());
    }

    @Test
    void successfulAdoptionMovesHistoryAfterConsumerReturns() {
        final Graph graph = new Graph("before");
        final UndoHistory history = graph.getUndoHistory();
        final String before = history.beginEdit(graph);
        graph.setName("after");
        history.commitEdit(before, graph);

        final AtomicReference<Graph> adopted = new AtomicReference<>();
        final Graph restored = history.undo(graph, adopted::set);

        assertSame(restored, adopted.get());
        assertFalse(history.canUndo());
        assertTrue(history.canRedo());
        assertEquals("before", restored.getName());
    }

    @Test
    void invalidSnapshotReturnsWithoutChangingHistory() throws Exception {
        final Graph graph = new Graph("current");
        final UndoHistory history = graph.getUndoHistory();
        final Field field = UndoHistory.class.getDeclaredField("undoStack");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        final Deque<String> stack = (Deque<String>) field.get(history);
        stack.push("not-a-graph");
        final AtomicReference<Graph> adopted = new AtomicReference<>();

        assertSame(graph, history.undo(graph, adopted::set));

        assertTrue(history.canUndo());
        assertFalse(history.canRedo());
        assertNull(adopted.get());
    }
}
