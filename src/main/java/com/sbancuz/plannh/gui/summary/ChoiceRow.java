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
import com.sbancuz.plannh.gui.LangScope;
import com.sbancuz.plannh.gui.theme.PlannhThemes;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;
import com.sbancuz.plannh.gui.tooltips.TooltipTheme;

/**
 * A clickable choice row: a checkbox ticked on the active answer, and the reason on hover.
 */
final class ChoiceRow extends SummaryFlow implements Interactable {

    private static final LangScope CHOICE = LangScope.ROOT.child("summary.tooltip.choice");

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

        tooltipPos(RichTooltip.Pos.NEXT_TO_MOUSE).tooltipBuilder(TooltipBuilder.build(CHOICE, out -> {
            final boolean active = choice.active();
            out.title(IKey.str(choice.displayName()))
                .separator()
                .entry(
                    "status",
                    active ? TooltipTheme.GOOD : TooltipTheme.MUTED,
                    CHOICE.lang(active ? "active" : "alternative"));
            if (choice.reason() != null) out.entry(
                "reason",
                TooltipTheme.VALUE,
                choice.reason()
                    .render());
            else out.text("default");

            out.separator()
                .hint(active ? "click.active" : "click.alternative");
            if (notes.isEmpty()) return;
            out.separator();
            for (final Note note : notes) out.line(IKey.str(note.render()));
        }))
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
