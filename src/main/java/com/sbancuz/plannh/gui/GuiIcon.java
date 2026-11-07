package com.sbancuz.plannh.gui;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.ColorType;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.sbancuz.plannh.PlanNH;

/** UI sprites; source attribution and licenses are in assets/plannh/licenses/. */
public enum GuiIcon implements IDrawable {

    UNDO("undo"),
    REDO("redo"),
    AUTO_LAYOUT("compact/align-center", 9),
    GRID("grid-3x3"),
    GROUP("folder"),
    NOTE("note"),
    SHARE("share"),
    COPY("copy"),
    IMPORT("clipboard"),
    MACHINE("cpu"),
    COVER_CHILDREN("fit-children", 10),
    DELETE("delete", 8),
    FLOWCHART("flowchart", 16);

    private final UITexture texture;
    private final int size;

    GuiIcon(final String asset) {
        this(asset, 12);
    }

    GuiIcon(final String asset, final int size) {
        texture = UITexture.fullImage(PlanNH.MODID, "gui/" + asset, ColorType.ICON);
        this.size = size;
    }

    public int size() {
        return size;
    }

    public UITexture texture() {
        return texture;
    }

    @Override
    public void draw(final GuiContext context, final int x, final int y, final int width, final int height,
        final WidgetTheme widgetTheme) {
        texture.draw(context, x, y, width, height, widgetTheme);
    }
}
