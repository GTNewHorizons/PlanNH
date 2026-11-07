package com.sbancuz.plannh.gui.node;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.sbancuz.plannh.gui.ClampedListWidget;

class ConfigurationAreaWidget extends ClampedListWidget<IWidget, ConfigurationAreaWidget> {

    /** How many rows the region shows before the screen bound takes over as the limit. */
    // TODO: Config pass
    private static final int VISIBLE_ROWS = 10;
    private static final int SCROLLBAR_GAP = 4;

    private int avgRow;

    ConfigurationAreaWidget() {
        name("node.config");
        showScrollShadows(false);

        fullWidth().crossAxisAlignment(Alignment.CrossAxis.START)
            .paddingRight(SCROLLBAR_GAP)
            .scrollDirection(new VerticalScrollData())
            .maxSize(() -> avgRow == 0 ? Integer.MAX_VALUE : VISIBLE_ROWS * avgRow);
    }

    @Override
    public void postResize() {
        super.postResize();
        final int avg = averageRowHeight();
        if (avg != avgRow) {
            avgRow = avg;
            // postLayoutWidgets runs before MUI clears dirty flags; postResize runs after.
            scheduleResize();
        }
    }

    @Override
    public boolean canClickThrough() {
        return true;
    }

    /**
     * The average row height of the open folds, measured after their layout has finished.
     */
    private int averageRowHeight() {
        int height = 0, count = 0;
        for (final IWidget section : getChildren()) {
            if (!section.isEnabled() || !(section instanceof final ParentWidget<?> fold)) continue;
            for (final IWidget row : fold.getChildren()) {
                final Area a = row.getArea();
                height+= a.height + a.getMargin().getTotal(GuiAxis.Y);
                count += 1;
            }
        }
        return count != 0 ? height / count : 0;
    }
}
