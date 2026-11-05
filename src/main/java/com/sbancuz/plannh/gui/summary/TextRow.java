package com.sbancuz.plannh.gui.summary;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.sbancuz.plannh.data.flowchart.balancer.Severity;
import com.sbancuz.plannh.gui.PlannhColors;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;
import com.sbancuz.plannh.gui.tooltips.TooltipTheme;

final class TextRow extends SummaryFlow {

    TextRow(final IKey text, final int color, final String name, @Nullable final Severity severity) {
        super(GuiAxis.X);
        name(name);
        fullWidth().coverChildrenHeight(SummaryBody.LINE_H)
            .hoverBackground(new Rectangle().color(PlannhColors.SUMMARY_ROW_HOVER.getColor()))
            .child(
                new TextWidget<>(text).name(name + ".text")
                    .color(color)
                    .paddingLeft(SummaryBody.TEXT_X)
                    .textAlign(Alignment.CenterLeft)
                    .fullWidth())
            .tooltipPos(RichTooltip.Pos.NEXT_TO_MOUSE)
            .tooltipBuilder(tooltip -> {
                final TooltipBuilder out = TooltipBuilder.create(tooltip)
                    .row(TooltipTheme.key(TooltipTheme.Role.LABEL, text.get()));
                if (severity != null) {
                    out.separator();
                    out.entry(
                        IKey.lang("plannh.summary.tooltip.text.severity"),
                        TooltipTheme.Role.IDENTITY,
                        IKey.lang(switch (severity) {
                        case ERROR -> "plannh.summary.tooltip.text.error";
                        case WARN -> "plannh.summary.tooltip.text.warn";
                        case INFO -> "plannh.summary.tooltip.text.info";
                    }));
                }
                out.flush();
            })
            .tooltipAutoUpdate(true);
    }
}
