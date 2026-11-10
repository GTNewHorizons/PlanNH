package com.sbancuz.plannh.gui.common;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.sbancuz.plannh.api.PlanAPI;
import com.sbancuz.plannh.gui.CanvasWidget;
import com.sbancuz.plannh.gui.GuiIcon;
import com.sbancuz.plannh.gui.theme.PlannhWidgetThemes;

public class CloseButtonWidget extends ButtonWidget<CloseButtonWidget> {

    private final FlowchartWidget<?, ?> parent;

    public CloseButtonWidget(FlowchartWidget<?, ?> parent) {
        this.parent = parent;
        name("close");
        widgetTheme(PlannhWidgetThemes.CLOSE_BUTTON);
        overlay(
            GuiIcon.DELETE.asIcon()
                .size(GuiIcon.DELETE.size()));
        size(12);
        addTooltipLine("Delete this widget");
        marginRight(2);
    }

    @Override
    public @NotNull Result onMousePressed(int mouseButton) {
        CanvasWidget canvas = parent.getCanvas();
        if (canvas.isMouseInsideCanvas()) {
            PlanAPI.recordEdit(canvas.getGraph(), () -> {
                ((ParentWidget<?>) parent.getParent()).remove(parent);
                parent.removeFromGraph();
            });
        }
        return Result.SUCCESS;
    }
}
