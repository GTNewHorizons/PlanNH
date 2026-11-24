package com.sbancuz.plannh.gui.summary;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.theme.WidgetThemeKey;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.sbancuz.plannh.data.flowchart.balancer.Severity;
import com.sbancuz.plannh.gui.theme.PlannhThemes;

final class TextRow extends SummaryFlow {

    TextRow(final IKey text, final Severity severity) {
        this(text, PlannhThemes.severityText(severity), "summary.message");
    }

    TextRow(final IKey text, final WidgetThemeKey<?> textTheme, final String name) {
        super(GuiAxis.X);
        name(name);
        fullWidth().coverChildrenHeight(SummaryBody.LINE_H)
            .widgetTheme(PlannhThemes.ROW)
            .child(
                new TextWidget<>(text).name(name + ".text")
                    .widgetTheme(textTheme)
                    .paddingLeft(SummaryBody.TEXT_X)
                    .textAlign(Alignment.CenterLeft)
                    .fullWidth());
    }
}
