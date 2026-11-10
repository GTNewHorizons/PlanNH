package com.sbancuz.plannh.gui.summary;

import java.util.Locale;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.sbancuz.plannh.data.flowchart.Summary;
import com.sbancuz.plannh.gui.GuiHelper;
import com.sbancuz.plannh.gui.PlannhColors;
import com.sbancuz.plannh.gui.theme.PlannhWidgetThemes;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;
import com.sbancuz.plannh.gui.tooltips.TooltipTheme;

/**
 * The selected recipes' totals: combined workload plus the time one full cycle takes. The model only
 * supplies the numbers; the sentence is built and localized here, in the panel, in the shape
 * the current mode wants (cycles: "Time: 240t (12.0s/cycle)", throughput: "Cycle: 12.0s").
 */
final class TotalsRow extends SummaryFlow {

    TotalsRow(final Summary.Line.Totals totals, final Summary.Mode mode) {
        super(GuiAxis.X);
        final String ops = GuiHelper.formatCount(totals.operations());
        final String sec = String
            .format(Locale.ROOT, "%.2f", (double) totals.durationTicks() / GuiHelper.TICKS_PER_SECOND);
        final IKey text = mode == Summary.Mode.THROUGHPUT ? IKey.lang("plannh.summary.totals.throughput", ops, sec)
            : IKey.lang("plannh.summary.totals.cycles", ops, totals.durationTicks(), sec);
        fullWidth().coverChildrenHeight(SummaryBody.LINE_H)
            .paddingLeft(SummaryBody.TEXT_X)
            .widgetTheme(PlannhWidgetThemes.ROW)
            .child(
                new TextWidget<>(text).name("summary.totals.text")
                    .color(PlannhColors.INFO::getColor)
                    .textAlign(Alignment.CenterLeft)
                    .fullWidth())
            .tooltipPos(RichTooltip.Pos.NEXT_TO_MOUSE)
            .tooltipBuilder(
                tooltip -> TooltipBuilder.create(tooltip)
                    .row(
                        IKey.lang("plannh.summary.tooltip.totals.title")
                            .style(TooltipTheme.Role.LABEL.code()))
                    .separator()
                    .entry(IKey.lang("plannh.summary.tooltip.totals.operations"), TooltipTheme.Role.IDENTITY, ops)
                    .duration(
                        StatCollector.translateToLocal("plannh.summary.tooltip.totals.duration"),
                        totals.durationTicks(),
                        GuiHelper.RateUnit.SECONDS,
                        false)
                    .flush())
            .tooltipAutoUpdate(true);
    }
}
