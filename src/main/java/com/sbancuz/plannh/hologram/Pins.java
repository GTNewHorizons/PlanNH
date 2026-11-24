package com.sbancuz.plannh.hologram;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.sbancuz.plannh.data.flowchart.Graph;
import com.sbancuz.plannh.data.flowchart.GraphData;

/** Queries and edits of the block pins on plan elements, across every graph of a plan. */
public final class Pins {

    private Pins() {}

    /** Every element of every graph that is pinned in {@code dimension}. */
    public static List<GraphData> pinnedIn(final Iterable<Graph> graphs, final int dimension) {
        final List<GraphData> pinned = new ArrayList<>();
        for (final Graph graph : graphs) {
            if (graph == null) continue;
            graph.allData()
                .filter(
                    data -> data.getPin() != null && data.getPin()
                        .dimension() == dimension)
                .forEach(pinned::add);
        }
        return pinned;
    }

    @Nullable
    public static GraphData pinnedAt(final Iterable<Graph> graphs, final BlockPin pin) {
        for (final Graph graph : graphs) {
            if (graph == null) continue;
            final GraphData found = graph.allData()
                .filter(data -> pin.equals(data.getPin()))
                .findFirst()
                .orElse(null);
            if (found != null) return found;
        }
        return null;
    }

    /** Pins {@code element} to {@code pin}, first unpinning whatever else sits on that block. */
    public static void pin(final Iterable<Graph> graphs, final GraphData element, final BlockPin pin) {
        for (final Graph graph : graphs) {
            if (graph == null) continue;
            graph.allData()
                .filter(data -> data != element && pin.equals(data.getPin()))
                .forEach(data -> data.setPin(null));
        }
        element.setPin(pin);
    }

    public static void clearPins(final Graph graph) {
        graph.allData()
            .forEach(data -> data.setPin(null));
    }
}
