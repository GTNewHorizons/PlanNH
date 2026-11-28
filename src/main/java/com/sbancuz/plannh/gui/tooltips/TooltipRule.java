package com.sbancuz.plannh.gui.tooltips;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.api.drawable.ITextLine;
import com.cleanroommc.modularui.screen.viewport.GuiContext;

/**
 * The divider between two groups in a tooltip, as wide as the tooltip.
 */
public final class TooltipRule implements ITextLine {

    public static final TooltipRule INSTANCE = new TooltipRule();

    private static final String STYLE = TooltipTheme.DIVIDER.prefix() + EnumChatFormatting.STRIKETHROUGH;
    private static final String DASH = "-";

    // `draw()` rebuilds the dashes only when the width of the tooltip changes
    private int dashedWidth = -1;
    private String dashes = "";

    private TooltipRule() {}

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight(final FontRenderer fr) {
        return fr.FONT_HEIGHT;
    }

    @Override
    public void draw(final GuiContext context, final FontRenderer fr, final float x, final float y, final int color,
        final boolean shadow, final int availableWidth, final int availableHeight) {
        if (availableWidth != dashedWidth) {
            final int per = Math.max(1, fr.getStringWidth(STYLE + DASH));
            dashes = STYLE + DASH.repeat(Math.max(1, availableWidth / per));
            dashedWidth = availableWidth;
        }
        fr.drawString(dashes, (int) x, (int) y, color, shadow);
    }

    @Override
    public Object getHoveringElement(final FontRenderer fr, final int x, final int y) {
        return null;
    }
}
