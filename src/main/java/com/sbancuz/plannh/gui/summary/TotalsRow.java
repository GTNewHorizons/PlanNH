package com.sbancuz.plannh.gui.summary;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.sbancuz.plannh.data.flowchart.Summary;
import com.sbancuz.plannh.gui.GuiHelper;
import com.sbancuz.plannh.gui.LangScope;
import com.sbancuz.plannh.gui.theme.PlannhThemes;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;
import com.sbancuz.plannh.gui.tooltips.TooltipTheme;

/**
 * {@code TotalsRow} writes the combined load of the selected recipes and the time of one cycle. The sentence has the
 * shape of the summary mode.
 *
 * <pre>
 *     CYCLES:      Ops: 3 Time: 240t (12s/cycle)
 *     THROUGHPUT:  Ops: 3 Cycle: 12s
 * </pre>
 */
final class TotalsRow extends SummaryFlow {

    private static final LangScope TOTALS = LangScope.ROOT.child("summary.tooltip.totals");

    TotalsRow(final Summary.Line.Totals totals, final Summary.Mode mode) {
        super(GuiAxis.X);
        final String ops = GuiHelper.formatCount(totals.operations());
        final String perCycle = GuiHelper.formatDuration(totals.durationTicks(), GuiHelper.RateUnit.SECONDS);
        final String ticks = GuiHelper.formatDuration(totals.durationTicks(), GuiHelper.RateUnit.TICKS);
        final IKey text = mode == Summary.Mode.THROUGHPUT ? IKey.lang("plannh.summary.totals.throughput", ops, perCycle)
            : IKey.lang("plannh.summary.totals.cycles", ops, ticks, perCycle);
        fullWidth().coverChildrenHeight(SummaryBody.LINE_H)
            .paddingLeft(SummaryBody.TEXT_X)
            .widgetTheme(PlannhThemes.ROW)
            .child(
                new TextWidget<>(text).name("summary.totals.text")
                    .widgetTheme(PlannhThemes.TEXT_INFO)
                    .textAlign(Alignment.CenterLeft)
                    .fullWidth())
            .tooltipPos(RichTooltip.Pos.NEXT_TO_MOUSE)
            .tooltipBuilder(
                TooltipBuilder.build(
                    TOTALS,
                    out -> out.title("title")
                        .separator()
                        .entry("operations", TooltipTheme.VALUE, ops)
                        .duration(TOTALS.lang("duration"), totals.durationTicks(), GuiHelper.RateUnit.SECONDS, false)))
            .tooltipAutoUpdate(true);
    }
}
