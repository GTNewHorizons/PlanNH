package com.sbancuz.plannh.gui.common;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.api.widget.Interactable;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;

/** A row that a middle-click resets. {@link TooltipBuilder#resetHint} writes the tooltip rows of the reset. */
public interface MiddleClickReset extends Interactable {

    @Override
    default @NotNull Result onMousePressed(final int mouseButton) {
        return mouseButton == 2 && resetSelection() ? Result.SUCCESS : Result.IGNORE;
    }

    /** Reset the row when the user set the row's value. */
    default boolean resetSelection() {
        if (!(isValid() && isExplicit())) return false;
        reset();
        return true;
    }

    boolean isValid();

    /** Whether the user set the value of this row. */
    boolean isExplicit();

    void reset();
}
