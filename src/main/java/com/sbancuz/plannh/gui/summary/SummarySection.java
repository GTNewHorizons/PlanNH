package com.sbancuz.plannh.gui.summary;

import java.util.Locale;

import com.cleanroommc.modularui.widget.ParentWidget;
import com.sbancuz.plannh.data.flowchart.Plan;
import com.sbancuz.plannh.data.flowchart.Summary;
import com.sbancuz.plannh.gui.FlowchartList;

/**
 * One section of the summary panel: the accent-bar title row with a fold toggle, plus the row body. The whole section
 * is hidden when the section has nothing to show, or when the user hid the section from the sections menu. The body
 * alone is hidden when the section is folded away, so the header stays clickable.
 */
class SummarySection extends ParentWidget<SummarySection> {

    private static final int INNER_GAP = 3;

    private final Summary data;
    private final Summary.Section section;

    SummarySection(final SummaryWidget panel, final Summary.Section section, FlowchartList sectionsList) {
        this.data = Plan.getInstance()
            .getSummary();
        this.section = section;

        name(
            "summary.section." + section.name()
                .toLowerCase(Locale.ROOT));
        fullWidth().coverChildrenHeight()
            .setEnabledIf(_ -> !data.isSectionHidden(section) && data.lineCount(section) > 0);

        child(
            SummaryFlow.col()
                .name("summary.section.stack")
                .fullWidth()
                .coverChildrenHeight()
                .childPadding(INNER_GAP)
                .collapseDisabledChild()
                .child(new SummaryHeader(panel, data, section, sectionsList))
                .child(new SummaryBody(panel, data, section)));
    }

    Summary.Section section() {
        return section;
    }
}
