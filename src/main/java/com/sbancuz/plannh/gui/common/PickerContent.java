package com.sbancuz.plannh.gui.common;

import com.cleanroommc.modularui.api.widget.IWidget;

/** The content of a {@link SelectionPicker} popup, sized before the popup opens. */
public interface PickerContent extends IWidget {

    int pickerWidth();

    int pickerHeight();
}
