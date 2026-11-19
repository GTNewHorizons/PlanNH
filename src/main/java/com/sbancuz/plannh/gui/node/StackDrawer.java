package com.sbancuz.plannh.gui.node;

import codechicken.nei.PositionedStack;

/** Draws a stack of a planner recipe in place of NEI's {@code RecipeWidget}. */
@FunctionalInterface
public interface StackDrawer {

    /** Return false to let NEI draw the stack. */
    boolean drawStack(PositionedStack stack);
}
