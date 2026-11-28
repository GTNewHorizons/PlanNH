package com.sbancuz.plannh.gui.tooltips;

import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.api.drawable.IKey;

/**
 * The colour of each kind of tooltip text. To change a tooltip colour of PlanNH, change the constant here.
 *
 * <p>
 * Text is gray, and a value has the colour of the kind of value. The colours of a value follow GregTech's
 * {@code TooltipHelper}. A tooltip line can have only the 16 legacy §-codes. {@code RichText} of MUI2 flattens a line
 * to one formatted string, and {@code FormattingState} doesn't have an ARGB field, so MUI2 drops the colour of
 * {@code IKey.color(int)} on a tooltip line.
 */
public enum TooltipTheme {

    /** Text and the label of a value. */
    BODY(EnumChatFormatting.GRAY),
    /** The title of a tooltip, and the name of a group of rows. */
    NAME(EnumChatFormatting.WHITE),
    /** A count or a name that the reader can't change. */
    VALUE(EnumChatFormatting.WHITE),
    /** An amount per unit of time. */
    RATE(EnumChatFormatting.GREEN),
    TIME(EnumChatFormatting.LIGHT_PURPLE),
    POWER(EnumChatFormatting.YELLOW),
    /** A number the reader can change. */
    TUNABLE(EnumChatFormatting.GOLD),
    /** A setting left off, or a choice that isn't picked. */
    MUTED(EnumChatFormatting.DARK_GRAY),
    /** The choice in use in a list of choices. Aqua is the closest §-code to the selected colour of the GUI. */
    SELECTED(EnumChatFormatting.AQUA),
    /** A state the reader wants, such as a rate on target. */
    GOOD(EnumChatFormatting.GREEN),
    /** A state the reader may want to check, such as a rate over the rate target. */
    CAUTION(EnumChatFormatting.GOLD),
    /** A warning before an action that deletes work. */
    WARNING(EnumChatFormatting.RED),
    /** What a click, a key or a drop does. */
    HINT(EnumChatFormatting.GRAY, EnumChatFormatting.ITALIC),
    DIVIDER(EnumChatFormatting.GRAY);

    private final EnumChatFormatting[] codes;
    private final String prefix;

    TooltipTheme(final EnumChatFormatting... codes) {
        this.codes = codes;
        final StringBuilder joined = new StringBuilder();
        for (final EnumChatFormatting code : codes) joined.append(code);
        this.prefix = joined.toString();
    }

    /**
     * Wrap {@code text} in the colour of this role. {@code style()} doesn't change {@code text}, so a widget can share
     * the key.
     */
    public IKey style(final IKey text) {
        return IKey.comp(text)
            .style(codes);
    }

    public IKey style(final String text) {
        return IKey.str(text)
            .style(codes);
    }

    public String prefix() {
        return prefix;
    }
}
