package com.sbancuz.plannh.gui.summary;

import java.util.List;

import javax.annotation.Nonnull;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.theme.WidgetThemeKey;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.sbancuz.plannh.api.PlanAPI;
import com.sbancuz.plannh.data.flowchart.Plan;
import com.sbancuz.plannh.data.flowchart.Summary;
import com.sbancuz.plannh.data.flowchart.balancer.Note;
import com.sbancuz.plannh.gui.GuiIcon;
import com.sbancuz.plannh.gui.theme.PlannhThemes;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;
import com.sbancuz.plannh.gui.tooltips.TooltipTheme;

/**
 * A clickable choice row: a checkbox ticked on the active answer, and the reason on hover.
 */
final class ChoiceRow extends SummaryFlow implements Interactable {

    private final Summary.Line.Choice choice;

    /** The row for one answer, with the answer's status, reason and solver notes in the tooltip. */
    ChoiceRow(final Summary.Line.Choice choice, final List<Note> notes) {
        super(GuiAxis.X);
        this.choice = choice;

        // The active answer is ticked and accented, the alternatives are empty boxes in the muted colour.
        final WidgetThemeKey<WidgetTheme> role = choice.active() ? PlannhThemes.TEXT_SELECTED : PlannhThemes.TEXT_MUTED;
        final GuiIcon box = choice.active() ? GuiIcon.CHECKBOX_ON : GuiIcon.CHECKBOX;
        final IDrawable check = box.asIcon()
            .size(SummaryHeader.ICON_SIZE);

        fullWidth().coverChildrenHeight(SummaryBody.LINE_H)
            .widgetTheme(PlannhThemes.ROW)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(
                // The box fills the text gutter, so the answer lines up with the text of every other row.
                new Widget<>().name("summary.choice.check")
                    .size(SummaryBody.TEXT_X, SummaryHeader.ICON_SIZE)
                    .widgetTheme(role)
                    .overlay(check))
            .child(
                new TextWidget<>(IKey.str(choice.displayName())).name("summary.choice.text")
                    .widgetTheme(role)
                    .textAlign(Alignment.CenterLeft)
                    .expanded());

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
