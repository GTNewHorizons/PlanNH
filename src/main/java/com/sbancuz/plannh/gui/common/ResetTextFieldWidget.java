package com.sbancuz.plannh.gui.common;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

/** A text field that passes a middle-click to the {@link MiddleClickReset} row around the field. */
public final class ResetTextFieldWidget extends TextFieldWidget {

    @Override
    public @NotNull Result onMousePressed(final int mouseButton) {
        if (mouseButton != 2) return super.onMousePressed(mouseButton);

        for (IWidget widget = getParent(); widget != null; widget = widget.getParent()) {
            if (!(widget instanceof MiddleClickReset row)) continue;
            // `onRemoveFocus()` writes the field's text to the value. Drop the focus before the reset
            getContext().removeFocus();
            row.resetSelection();
            return Result.STOP;
        }
        return super.onMousePressed(mouseButton);
    }
}
