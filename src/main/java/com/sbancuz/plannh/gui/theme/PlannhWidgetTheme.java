package com.sbancuz.plannh.gui.theme;

import java.util.function.IntSupplier;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.theme.TextFieldTheme;
import com.cleanroommc.modularui.utils.Color;
import com.sbancuz.plannh.gui.PlannhColors;

/**
 * Reads its colors from {@link PlannhColors} when drawn: key defaults are built in preInit, before
 * resource packs are loaded, so captured ints would miss their overrides.
 */
public final class PlannhWidgetTheme extends TextFieldTheme {

    private final @Nullable IDrawable background;
    private final IntSupplier textColor;
    private final IntSupplier iconColor;

    PlannhWidgetTheme(final @Nullable IDrawable background, final IntSupplier textColor, final IntSupplier iconColor) {
        super(0, 0, background, Color.WHITE.main, Color.WHITE.main, false, Color.WHITE.main, 0, 0);
        this.background = background;
        this.textColor = textColor;
        this.iconColor = iconColor;
    }

    PlannhWidgetTheme withIconColor(final IntSupplier iconColor) {
        return new PlannhWidgetTheme(background, textColor, iconColor);
    }

    @Override
    public @Nullable IDrawable getBackground() {
        return background;
    }

    @Override
    public int getTextColor() {
        return textColor.getAsInt();
    }

    @Override
    public int getIconColor() {
        return iconColor.getAsInt();
    }

    @Override
    public int getMarkedColor() {
        return PlannhColors.TEXT_SELECTION.getColor();
    }

    @Override
    public int getHintColor() {
        return PlannhColors.TEXT_MUTED.getColor();
    }

    @Override
    public PlannhWidgetTheme withNoHoverBackground() {
        return new PlannhWidgetTheme(IDrawable.NONE, textColor, iconColor);
    }
}
