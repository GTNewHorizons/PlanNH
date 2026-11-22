package com.sbancuz.plannh.gui.summary;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.theme.WidgetThemeKey;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.sbancuz.plannh.data.flowchart.balancer.Severity;
import com.sbancuz.plannh.gui.theme.PlannhThemes;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;
import com.sbancuz.plannh.gui.tooltips.TooltipTheme;

final class TextRow extends SummaryFlow {

    TextRow(final IKey text, final WidgetThemeKey<?> textTheme, final String name) {
        this(text, textTheme, name, null);
    }

    TextRow(final IKey text, final Severity severity) {
        this(text, PlannhThemes.severityText(severity), "summary.message", severity);
    }

    private TextRow(final IKey text, final WidgetThemeKey<?> textTheme, final String name,
        @Nullable final Severity severity) {
        super(GuiAxis.X);
        name(name);
        fullWidth().coverChildrenHeight(SummaryBody.LINE_H)
            .widgetTheme(PlannhThemes.ROW)
            .child(
                new TextWidget<>(text).name(name + ".text")
                    .widgetTheme(textTheme)
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
