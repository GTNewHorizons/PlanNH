package com.sbancuz.plannh.gui;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widgets.ListWidget;

public abstract class ClampedListWidget<I extends IWidget, W extends ClampedListWidget<I, W>> extends ListWidget<I, W> {

    @Override
    public void onChildChangeEnabled(final IWidget child, final boolean enabled) {
        // Use the resize pipeline for content height, wrapping and scrollbar geometry together.
        if (isCollapseDisabledChild()) scheduleResize();
    }

    @Override
    public boolean postLayoutWidgets() {
        final boolean done = super.postLayoutWidgets();
        getScrollData().clamp(getScrollArea());
        return done;
    }
}
