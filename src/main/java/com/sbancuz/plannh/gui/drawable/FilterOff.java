package com.sbancuz.plannh.gui.drawable;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.sbancuz.plannh.gui.PlannhColors;

/** The filter icon with a small, high-contrast strike through it. */
public final class FilterOff implements IDrawable {

    private static final IDrawable FILTER = GuiTextures.FILTER;

    @Override
    public void draw(final GuiContext context, final int x, final int y, final int width, final int height,
        final WidgetTheme widgetTheme) {
        FILTER.draw(context, x, y, width, height, widgetTheme);
        if (width < 4 || height < 4) return;

        final int steps = Math.min(width, height) - 2;
        final int outline = 0xFF101010;
        final int slash = PlannhColors.ACCENT_RED.getColor();
        for (int i = 0; i < steps; i++) {
            final int px = x + width - 3 - i;
            final int py = y + i;
            GuiDraw.drawRect(px, py, 3, 3, outline);
        }
        for (int i = 0; i < steps; i++) {
            final int px = x + width - 3 - i;
            final int py = y + i;
            GuiDraw.drawRect(px + 1, py + 1, 1, 1, slash);
        }
    }
}
