package com.sbancuz.plannh.gui.common;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.sbancuz.plannh.gui.CanvasWidget;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;

/** Header action for scoping the summary to a flowchart widget. */
public class SummaryFilterButton extends ButtonWidget<SummaryFilterButton> {

    private final CanvasWidget canvas;
    private final FlowchartWidget<?, ?> owner;

    public SummaryFilterButton(final FlowchartWidget<?, ?> owner) {
        this.owner = owner;
        this.canvas = owner.getCanvas();

        name("summary.filter")
            .background(new DynamicDrawable(() -> isActive() ? GuiTextures.MC_BUTTON_PRESSED : GuiTextures.MC_BUTTON))
            .hoverBackground(
                new DynamicDrawable(
                    () -> isActive() ? GuiTextures.MC_BUTTON_HOVERED_PRESSED : GuiTextures.MC_BUTTON_HOVERED))
            .overlay(
                new DynamicDrawable(
                    () -> isActive() ? GuiTextures.FILTER.withColorOverride(owner.getSummaryFilterColor())
                        .asIcon()
                        .size(10)
                        : GuiTextures.FILTER.asIcon()
                            .size(10)))
            .tooltipBuilder(
                tooltip -> TooltipBuilder.create(tooltip)
                    .langRow(isActive() ? "plannh.summary.scope.filter.remove" : "plannh.summary.scope.filter.action")
                    .langRow("plannh.summary.scope.filter.ctrl_hint")
                    .flush())
            .tooltipAutoUpdate(true);
    }

    private boolean isActive() {
        return owner.getData()
            .isSummaryFiltered();
    }

    @Override
    public @NotNull Result onMousePressed(final int mouseButton) {
        if (mouseButton == 0 && canvas.isMouseInsideCanvas()) owner.getData()
            .setSummaryFiltered(!isActive());
        return Result.SUCCESS;
    }
}
