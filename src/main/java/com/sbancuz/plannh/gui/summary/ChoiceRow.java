package com.sbancuz.plannh.gui.summary;

import java.util.List;

import javax.annotation.Nonnull;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.sbancuz.plannh.api.PlanAPI;
import com.sbancuz.plannh.data.flowchart.Plan;
import com.sbancuz.plannh.data.flowchart.Summary;
import com.sbancuz.plannh.data.flowchart.balancer.Note;
import com.sbancuz.plannh.gui.PlannhColors;
import com.sbancuz.plannh.gui.theme.PlannhWidgetThemes;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;
import com.sbancuz.plannh.gui.tooltips.TooltipTheme;

/**
 * A clickable choice row: the active answer is lead-marked; the reason it gives up on hover.
 */
final class ChoiceRow extends SummaryFlow implements Interactable {

    private final Summary.Line.Choice choice;

    ChoiceRow(final Summary.Line.Choice choice, final List<Note> notes) {
        super(GuiAxis.X);
        this.choice = choice;

        fullWidth().coverChildrenHeight(SummaryBody.LINE_H)
            .widgetTheme(PlannhWidgetThemes.ROW)
            .child(
                new TextWidget<>(IKey.str((choice.active() ? "> " : "  ") + choice.displayName()))
                    .name("summary.choice.text")
                    .paddingLeft(SummaryBody.TEXT_X)
                    .color((choice.active() ? PlannhColors.SELECTED : PlannhColors.TEXT_MUTED)::getColor)
                    .textAlign(Alignment.CenterLeft)
                    .fullWidth());

        tooltipPos(RichTooltip.Pos.NEXT_TO_MOUSE).tooltipBuilder(tooltip -> {
            final TooltipBuilder out = TooltipBuilder.create(tooltip)
                .row(TooltipTheme.key(TooltipTheme.Role.LABEL, choice.displayName()))
                .separator()
                .entry(
                    IKey.lang("plannh.summary.tooltip.choice.status"),
                    choice.active() ? TooltipTheme.Role.ACCENT : TooltipTheme.Role.MUTED,
                    IKey.lang(
                        choice.active() ? "plannh.summary.tooltip.choice.active"
                            : "plannh.summary.tooltip.choice.alternative"));
            if (choice.reason() != null) {
                out.entry(
                    IKey.lang("plannh.summary.tooltip.choice.reason"),
                    TooltipTheme.Role.BODY,
                    choice.reason()
                        .render());
            } else {
                out.langRow("plannh.summary.tooltip.choice.default");
            }
            out.separator()
                .langRow(
                    choice.active() ? "plannh.summary.tooltip.choice.click.active"
                        : "plannh.summary.tooltip.choice.click.alternative");
            if (!notes.isEmpty()) {
                out.separator();
                for (final Note note : notes) out.row(IKey.str(note.render()));
            }
            out.flush();
        })
            .tooltipAutoUpdate(true);
    }

    @Override
    public @Nonnull Result onMousePressed(final int mouseButton) {
        if (mouseButton != 0) return Result.IGNORE;
        PlanAPI.recordEdit(Plan.getActiveGraph(), () -> {
            Plan.getInstance()
                .getSummary()
                .setExcessChoice(choice.key());
            Plan.getActiveGraph()
                .bumpVersion();
        });
        return Result.SUCCESS;
    }
}
