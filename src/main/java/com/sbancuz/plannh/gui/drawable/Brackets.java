package com.sbancuz.plannh.gui.drawable;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.utils.Color;
import com.sbancuz.plannh.gui.PlannhColors;

/** Outlined brackets along the left and right edges of a header. */
public final class Brackets implements IDrawable {

    private final int color;
    private final int outline;

    public Brackets(final int color) {
        this.color = Color.withAlpha(color, 0xFF);
        this.outline = PlannhColors.outlineFor(this.color);
    }

    @Override
    public void draw(final GuiContext context, final int x, final int y, final int width, final int height,
        final WidgetTheme widgetTheme) {
        GuiDraw.drawRect(x, y, 4, height, outline);
        GuiDraw.drawRect(x + width - 4, y, 4, height, outline);
        GuiDraw.drawRect(x, y, 7, 4, outline);
        GuiDraw.drawRect(x + width - 7, y, 7, 4, outline);
        GuiDraw.drawRect(x, y + height - 4, 7, 4, outline);
        GuiDraw.drawRect(x + width - 7, y + height - 4, 7, 4, outline);
        GuiDraw.drawRect(x + 1, y + 1, 2, height - 2, color);
        GuiDraw.drawRect(x + width - 3, y + 1, 2, height - 2, color);
        GuiDraw.drawRect(x + 1, y + 1, 5, 2, color);
        GuiDraw.drawRect(x + width - 6, y + 1, 5, 2, color);
        GuiDraw.drawRect(x + 1, y + height - 3, 5, 2, color);
        GuiDraw.drawRect(x + width - 6, y + height - 3, 5, 2, color);
    }
}
