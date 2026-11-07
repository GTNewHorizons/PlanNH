package com.sbancuz.plannh.gui.drawable;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;

/** The two-pixel bevel of a button texture, without its opaque center. */
public final class ButtonFrame implements IDrawable {

    private final UITexture texture;

    public ButtonFrame(final UITexture texture) {
        this.texture = texture;
    }

    @Override
    public void draw(final GuiContext context, final int x, final int y, final int width, final int height,
        final WidgetTheme widgetTheme) {
        if (width < 4 || height < 4) return;
        texture.drawSubArea(x, y, 2, 2, 0, 0, 0.125f, 0.125f);
        texture.drawSubArea(x + 2, y, width - 4, 2, 0.125f, 0, 0.875f, 0.125f);
        texture.drawSubArea(x + width - 2, y, 2, 2, 0.875f, 0, 1, 0.125f);
        texture.drawSubArea(x, y + 2, 2, height - 4, 0, 0.125f, 0.125f, 0.875f);
        texture.drawSubArea(x + width - 2, y + 2, 2, height - 4, 0.875f, 0.125f, 1, 0.875f);
        texture.drawSubArea(x, y + height - 2, 2, 2, 0, 0.875f, 0.125f, 1);
        texture.drawSubArea(x + 2, y + height - 2, width - 4, 2, 0.125f, 0.875f, 0.875f, 1);
        texture.drawSubArea(x + width - 2, y + height - 2, 2, 2, 0.875f, 0.875f, 1, 1);
    }
}
