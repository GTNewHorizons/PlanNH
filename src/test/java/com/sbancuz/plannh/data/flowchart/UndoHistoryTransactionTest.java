package com.sbancuz.plannh.data.flowchart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

}
