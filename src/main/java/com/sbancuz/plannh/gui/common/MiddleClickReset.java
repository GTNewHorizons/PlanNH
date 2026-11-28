package com.sbancuz.plannh.gui.common;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.api.widget.Interactable;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;

/** A row that a middle-click resets. {@link #addResetRows} adds the tooltip rows of the reset. */
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

    default TooltipBuilder addResetRows(final TooltipBuilder out) {
        return out.langRow(isExplicit() ? "plannh.settings.choice_manual" : "plannh.settings.choice_auto")
            .langRow("plannh.settings.middle_reset_hint");
    }
}
