package com.sbancuz.plannh.gui.summary;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.sbancuz.plannh.data.flowchart.Summary;
import com.sbancuz.plannh.data.flowchart.Summary.Line;
import com.sbancuz.plannh.gui.FlowchartList;
import com.sbancuz.plannh.gui.theme.PlannhWidgetThemes;

/**
 * The title row of one summary section: the reorder grip, a section-colored highlight bar, the
 * section's name (with a row count for the content sections), and a fold toggle wired straight
 * into the {@link Summary}'s fold bitmask.
 */
class SummaryHeader extends ParentWidget<SummaryHeader> {

    public static final int HEADER_H = 18;
    private static final int PAD = 4;

    protected SummaryHeader(final SummaryWidget panel, final Summary data, final Summary.Section section,
        FlowchartList sectionsList) {
        name("summary.section.header");
        fullWidth().height(HEADER_H)
            .widgetTheme(PlannhWidgetThemes.PANEL_HEADER);

        child(
            SummaryFlow.row()
                .name("summary.section.header.row")
                .full()
                .childPadding(PAD)
                .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
                .child(
                    SummaryFlow.row()
                        .name("summary.section.header.title")
                        .coverChildren()
                        .childPadding(PAD)
                        .child(
                            new FlowchartList.Grip(sectionsList).name("summary.grip")
                                .widgetTheme(PlannhWidgetThemes.section(section)))
                        .child(
                            new TextWidget<>(headerTitle(section, data)).name("summary.section.header.text")
                                .widgetTheme(PlannhWidgetThemes.section(section))
                                .disableThemeBackground(true)
                                .disableHoverThemeBackground(true)))
                .child(foldToggle(data, section)));

        child(
            new Widget<>().name("summary.section.header.rule")
                .fullWidth()
                .height(1)
                .widgetTheme(PlannhWidgetThemes.SEPARATOR));

    }

    static CycleButtonWidget foldToggle(final Summary data, final Summary.Section section) {
        return new CycleButtonWidget().name("summary.fold")
            .stateCount(2)
            .size(HEADER_H, HEADER_H)
            .stateOverlay(true, IKey.str("V"))
            .stateOverlay(false, IKey.str("^"))
            .value(new BoolValue.Dynamic(() -> data.isSummaryFold(section), val -> data.setSummaryFold(section, val)));
    }

    private static IKey headerTitle(final Summary.Section section, final Summary data) {
        final IKey title = IKey.lang(section.titleKey());
        if (section == Summary.Section.HELP) return title;
        return IKey.dynamicKey(() -> IKey.comp(title, IKey.str(" (" + count(section, data) + ")")));
    }

    private static int count(final Summary.Section section, final Summary data) {
        if (section == Summary.Section.CHOICES || section == Summary.Section.MACHINE_COUNTS) {
            return (int) data.lines(section)
                .stream()
                .filter(
                    line -> section == Summary.Section.CHOICES ? line instanceof Line.Choice
                        : line instanceof Line.Measure<?>)
                .count();
        }
        return data.lineCount(section);
    }

}
