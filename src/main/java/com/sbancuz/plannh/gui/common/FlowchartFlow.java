package com.sbancuz.plannh.gui.common;

import com.cleanroommc.modularui.api.GuiAxis;

public class FlowchartFlow extends ContentFlow implements IFlowchartDraggable {

    private final FlowchartWidget<?, ?> parent;

    public FlowchartFlow(GuiAxis axis, FlowchartWidget<?, ?> parent) {
        super(axis);
        this.parent = parent;
    }

    public static FlowchartFlow row(FlowchartWidget<?, ?> parent) {
        return new FlowchartFlow(GuiAxis.X, parent);
    }

    public static FlowchartFlow col(FlowchartWidget<?, ?> parent) {
        return new FlowchartFlow(GuiAxis.Y, parent);
    }

    public static FlowchartFlow column(FlowchartWidget<?, ?> parent) {
        return new FlowchartFlow(GuiAxis.Y, parent);
    }

    @Override
    public FlowchartWidget<?, ?> getFlowchartParent() {
        return parent;
    }
}
