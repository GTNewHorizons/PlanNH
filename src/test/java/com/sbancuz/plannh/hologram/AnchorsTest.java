package com.sbancuz.plannh.hologram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sbancuz.plannh.data.flowchart.Graph;
import com.sbancuz.plannh.data.flowchart.Group;
import com.sbancuz.plannh.data.flowchart.Note;
import com.sbancuz.plannh.data.serialization.Serializer;

// Nodes need a recipe handler to construct, so these tests use notes and groups.
class AnchorsTest {

    private static Note noteIn(final Graph graph) {
        final Note note = new Note();
        graph.getNotes()
            .put(note.getId(), note);
        return note;
    }

    private static Group groupIn(final Graph graph) {
        final Group group = new Group();
        graph.getGroups()
            .put(group.getId(), group);
        return group;
    }

    @Test
    void anchorSurvivesEncodeDecode() {
        final Graph graph = new Graph("anchors");
        final Note note = noteIn(graph);
        final Group group = groupIn(graph);
        note.setAnchor(new BlockAnchor(-1, 5, 64, -7));
        group.setAnchor(new BlockAnchor(0, 1, 2, 3));

        final Graph loaded = Serializer.decodeGraph(Serializer.encodeGraph(graph));

        assertEquals(
            new BlockAnchor(-1, 5, 64, -7),
            loaded.getNotes()
                .get(note.getId())
                .getAnchor());
        assertEquals(
            new BlockAnchor(0, 1, 2, 3),
            loaded.getGroups()
                .get(group.getId())
                .getAnchor());
    }

    @Test
    void unanchoredElementDecodesWithNullAnchor() {
        final Graph graph = new Graph("none");
        final Note note = noteIn(graph);

        final Graph loaded = Serializer.decodeGraph(Serializer.encodeGraph(graph));

        assertNull(
            loaded.getNotes()
                .get(note.getId())
                .getAnchor());
    }

    @Test
    void attachMovesBlockToNewElement() {
        final Graph graph = new Graph("move");
        final Note first = noteIn(graph);
        final Note second = noteIn(graph);
        final BlockAnchor block = new BlockAnchor(0, 1, 2, 3);

        Anchors.attach(List.of(graph), first, block);
        Anchors.attach(List.of(graph), second, block);

        assertNull(first.getAnchor());
        assertEquals(block, second.getAnchor());
        assertSame(second, Anchors.attachedAt(List.of(graph), block));
    }

    @Test
    void attachedInFiltersDimensionAcrossGraphs() {
        final Graph one = new Graph("one");
        final Graph two = new Graph("two");
        final Note overworld = noteIn(one);
        final Group nether = groupIn(one);
        final Note otherOverworld = noteIn(two);
        noteIn(two);
        Anchors.attach(List.of(one, two), overworld, new BlockAnchor(0, 0, 0, 0));
        Anchors.attach(List.of(one, two), nether, new BlockAnchor(-1, 0, 0, 0));
        Anchors.attach(List.of(one, two), otherOverworld, new BlockAnchor(0, 1, 0, 0));

        assertEquals(
            2,
            Anchors.attachedIn(List.of(one, two), 0)
                .size());
        assertEquals(
            1,
            Anchors.attachedIn(List.of(one, two), -1)
                .size());
    }

    @Test
    void clearAnchorsClearsEveryElementKind() {
        final Graph graph = new Graph("clear");
        final Note note = noteIn(graph);
        final Group group = groupIn(graph);
        final Note nested = new Note();
        group.getChildren()
            .put(nested.getId(), nested);
        note.setAnchor(new BlockAnchor(0, 0, 0, 0));
        group.setAnchor(new BlockAnchor(0, 1, 0, 0));
        nested.setAnchor(new BlockAnchor(0, 2, 0, 0));

        Anchors.clearAnchors(graph);

        assertNull(note.getAnchor());
        assertNull(group.getAnchor());
        assertNull(nested.getAnchor());
    }
}
