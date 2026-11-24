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
class PinsTest {

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
    void pinSurvivesEncodeDecode() {
        final Graph graph = new Graph("pins");
        final Note note = noteIn(graph);
        final Group group = groupIn(graph);
        note.setPin(new BlockPin(-1, 5, 64, -7));
        group.setPin(new BlockPin(0, 1, 2, 3));

        final Graph loaded = Serializer.decodeGraph(Serializer.encodeGraph(graph));

        assertEquals(
            new BlockPin(-1, 5, 64, -7),
            loaded.getNotes()
                .get(note.getId())
                .getPin());
        assertEquals(
            new BlockPin(0, 1, 2, 3),
            loaded.getGroups()
                .get(group.getId())
                .getPin());
    }

    @Test
    void unpinnedElementDecodesWithNullPin() {
        final Graph graph = new Graph("none");
        final Note note = noteIn(graph);

        final Graph loaded = Serializer.decodeGraph(Serializer.encodeGraph(graph));

        assertNull(
            loaded.getNotes()
                .get(note.getId())
                .getPin());
    }

    @Test
    void pinMovesBlockToNewElement() {
        final Graph graph = new Graph("move");
        final Note first = noteIn(graph);
        final Note second = noteIn(graph);
        final BlockPin block = new BlockPin(0, 1, 2, 3);

        Pins.pin(List.of(graph), first, block);
        Pins.pin(List.of(graph), second, block);

        assertNull(first.getPin());
        assertEquals(block, second.getPin());
        assertSame(second, Pins.pinnedAt(List.of(graph), block));
    }

    @Test
    void pinnedInFiltersDimensionAcrossGraphs() {
        final Graph one = new Graph("one");
        final Graph two = new Graph("two");
        final Note overworld = noteIn(one);
        final Group nether = groupIn(one);
        final Note otherOverworld = noteIn(two);
        noteIn(two);
        Pins.pin(List.of(one, two), overworld, new BlockPin(0, 0, 0, 0));
        Pins.pin(List.of(one, two), nether, new BlockPin(-1, 0, 0, 0));
        Pins.pin(List.of(one, two), otherOverworld, new BlockPin(0, 1, 0, 0));

        assertEquals(
            2,
            Pins.pinnedIn(List.of(one, two), 0)
                .size());
        assertEquals(
            1,
            Pins.pinnedIn(List.of(one, two), -1)
                .size());
    }

    @Test
    void clearPinsClearsEveryElementKind() {
        final Graph graph = new Graph("clear");
        final Note note = noteIn(graph);
        final Group group = groupIn(graph);
        final Note nested = new Note();
        group.getChildren()
            .put(nested.getId(), nested);
        note.setPin(new BlockPin(0, 0, 0, 0));
        group.setPin(new BlockPin(0, 1, 0, 0));
        nested.setPin(new BlockPin(0, 2, 0, 0));

        Pins.clearPins(graph);

        assertNull(note.getPin());
        assertNull(group.getPin());
        assertNull(nested.getPin());
    }
}
