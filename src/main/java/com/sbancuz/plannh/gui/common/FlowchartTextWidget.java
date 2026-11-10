package com.sbancuz.plannh.gui.common;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.sbancuz.plannh.gui.theme.PlannhWidgetThemes;

public class FlowchartTextWidget extends TextWidget<FlowchartTextWidget> implements IFlowchartDraggable {

    private final FlowchartWidget<?, ?> parent;

    public FlowchartTextWidget(IKey key, FlowchartWidget<?, ?> parent) {
        super(key);
        this.parent = parent;
        widgetTheme(PlannhWidgetThemes.CARD_TEXT);
    }

    public FlowchartTextWidget(String key, FlowchartWidget<?, ?> parent) {
        super(key);
        this.parent = parent;
        widgetTheme(PlannhWidgetThemes.CARD_TEXT);
    }

    @Override
    public FlowchartWidget<?, ?> getFlowchartParent() {
        return parent;
    }
}
