package com.sbancuz.plannh.gui.common;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.sbancuz.plannh.api.PlanAPI;
import com.sbancuz.plannh.gui.CanvasWidget;
import com.sbancuz.plannh.gui.GuiIcon;
import com.sbancuz.plannh.gui.PlannhColors;

public class CloseButtonWidget extends ButtonWidget<CloseButtonWidget> {

    private final FlowchartWidget<?, ?> parent;

    public CloseButtonWidget(FlowchartWidget<?, ?> parent) {
        this.parent = parent;
        name("close");
        background(GuiTextures.MC_BUTTON);
        hoverBackground(GuiTextures.MC_BUTTON_HOVERED);
        overlay(
            GuiIcon.DELETE.asIcon()
                .size(GuiIcon.DELETE.size()));
        hoverOverlay(
            GuiIcon.DELETE.texture()
                .withColorOverride(PlannhColors.ACCENT_RED.getColor())
                .asIcon()
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
