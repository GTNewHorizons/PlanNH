package com.sbancuz.plannh.hologram;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.sbancuz.plannh.data.flowchart.Graph;
import com.sbancuz.plannh.data.flowchart.GraphData;

/** Queries and edits of the block anchors on plan elements, across every graph of a plan. */
public final class Anchors {

    private Anchors() {}

    /** Every element of every graph that is attached in {@code dimension}. */
    public static List<GraphData> attachedIn(final Iterable<Graph> graphs, final int dimension) {
        final List<GraphData> attached = new ArrayList<>();
        for (final Graph graph : graphs) {
            if (graph == null) continue;
            graph.allData()
                .filter(
                    data -> data.getAnchor() != null && data.getAnchor()
                        .dimension() == dimension)
                .forEach(attached::add);
        }
        return attached;
    }

    @Nullable
    public static GraphData attachedAt(final Iterable<Graph> graphs, final BlockAnchor anchor) {
        for (final Graph graph : graphs) {
            if (graph == null) continue;
            final GraphData found = graph.allData()
                .filter(data -> anchor.equals(data.getAnchor()))
                .findFirst()
                .orElse(null);
            if (found != null) return found;
        }
        return null;
    }

    /** Attaches {@code element} to {@code anchor}, first detaching whatever else sits on that block. */
    public static void attach(final Iterable<Graph> graphs, final GraphData element, final BlockAnchor anchor) {
        for (final Graph graph : graphs) {
            if (graph == null) continue;
            graph.allData()
                .filter(data -> data != element && anchor.equals(data.getAnchor()))
                .forEach(data -> data.setAnchor(null));
        }
        element.setAnchor(anchor);
    }

    public static void clearAnchors(final Graph graph) {
        graph.allData()
            .forEach(data -> data.setAnchor(null));
    }
}
