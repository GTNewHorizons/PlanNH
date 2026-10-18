package com.sbancuz.plannh.data.flowchart;

import java.util.UUID;

import org.jetbrains.annotations.ApiStatus;

import com.sbancuz.plannh.gui.node.PortWidget;

import it.unimi.dsi.fastutil.ints.IntIntPair;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Edge implements IValidated {

    private final UUID id;
    private final UUID sourceNodeId;
    private final UUID targetNodeId;
    private final IntIntPair sourceOutputIndex;
    private final IntIntPair targetInputIndex;
    @Setter
    private boolean inactive;

    public Edge(PortWidget source, PortWidget target) {
        this.id = UUID.randomUUID();
        this.sourceNodeId = source.getNode()
            .getId();
        this.targetNodeId = target.getNode()
            .getId();
        this.sourceOutputIndex = source.getIndex();
        this.targetInputIndex = target.getIndex();
    }

    @ApiStatus.Internal
    public Edge(UUID id, UUID sourceNodeId, UUID targetNodeId, IntIntPair sourceOutputIndex,
        IntIntPair targetInputIndex) {
        this.id = id;
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
        this.sourceOutputIndex = sourceOutputIndex;
        this.targetInputIndex = targetInputIndex;
    }

    public int getSourceOutputItemIndex() {
        return sourceOutputIndex.firstInt();
    }

    public int getTargetInputItemIndex() {
        return targetInputIndex.firstInt();
    }

    @Override
    public boolean invalid() {
        return id == null || sourceNodeId == null
            || targetNodeId == null
            || sourceOutputIndex == null
            || targetInputIndex == null
            || sourceOutputIndex.firstInt() < 0
            || sourceOutputIndex.secondInt() < 0
            || targetInputIndex.firstInt() < 0
            || targetInputIndex.secondInt() < 0;
    }

    public boolean sameConnection(Edge other) {
        return this.sourceNodeId.equals(other.sourceNodeId) && this.targetNodeId.equals(other.targetNodeId)
            && this.targetInputIndex.equals(other.targetInputIndex)
            && this.sourceOutputIndex.equals(other.sourceOutputIndex);
    }

    /** Saved connections remain data even when their current endpoints cannot participate. */
    public boolean isActive(final Node source, final Node target) {
        if (inactive || source == null || target == null || !source.isAvailable() || !target.isAvailable())
            return false;
        final int out = getSourceOutputItemIndex();
        final int in = getTargetInputItemIndex();
        if (out < 0 || out >= source.getOutputs()
            .size()
            || in < 0
            || in >= target.getInputs()
                .size())
            return false;
        final Port<?> output = source.getOutputs()
            .get(out);
        final Port<?> input = target.getInputs()
            .get(in);
        return sourceOutputIndex.secondInt() >= 0 && sourceOutputIndex.secondInt() < output.getAllStacks()
            .size()
            && targetInputIndex.secondInt() >= 0
            && targetInputIndex.secondInt() < input.getAllStacks()
                .size()
            && output.canConnect(input);
    }
}
