package com.sbancuz.plannh.gui.common;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;

/** A flow whose collapsed children also invalidate its measurement and dependent ancestors. */
public abstract class ContentFlow extends Flow {

    protected ContentFlow(final GuiAxis axis) {
        super(axis);
    }

    @Override
    public void onChildChangeEnabled(final IWidget child, final boolean enabled) {
        if (isCollapseDisabledChild()) scheduleResize();
    }
}
