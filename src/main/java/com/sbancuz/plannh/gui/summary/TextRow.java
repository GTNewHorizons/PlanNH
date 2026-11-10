package com.sbancuz.plannh.gui.summary;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.theme.WidgetThemeKey;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.gtnewhorizon.gtnhlib.color.ColorResource;
import com.sbancuz.plannh.data.flowchart.balancer.Severity;
import com.sbancuz.plannh.gui.theme.PlannhWidgetThemes;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;
import com.sbancuz.plannh.gui.tooltips.TooltipTheme;

final class TextRow extends SummaryFlow {

    TextRow(final IKey text, final ColorResource color, final String name) {
        this(text, PlannhWidgetThemes.ROW, color, name, null);
    }

    TextRow(final IKey text, final Severity severity) {
        this(text, PlannhWidgetThemes.messageRow(severity), null, "summary.message", severity);
    }

    private TextRow(final IKey text, final WidgetThemeKey<?> rowTheme, @Nullable final ColorResource color,
        final String name, @Nullable final Severity severity) {
        super(GuiAxis.X);
        name(name);
        final TextWidget<?> label = new TextWidget<>(text).name(name + ".text")
            .widgetTheme(rowTheme)
            .disableThemeBackground(true)
            .disableHoverThemeBackground(true)
            .paddingLeft(SummaryBody.TEXT_X)
            .textAlign(Alignment.CenterLeft)
            .fullWidth();
        if (color != null) label.color(color::getColor);
        fullWidth().coverChildrenHeight(SummaryBody.LINE_H)
            .widgetTheme(rowTheme)
            .child(label)
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
