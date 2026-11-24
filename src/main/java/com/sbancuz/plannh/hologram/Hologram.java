package com.sbancuz.plannh.hologram;

import java.util.Objects;

/**
 * A text label floating at a block position. Immutable; the text is sanitized to a single line
 * and the color is a 24-bit RGB value.
 */
public record Hologram(int dimension, int x, int y, int z, String text, int color) {

    public static final int MAX_TEXT_LENGTH = 256;
    public static final int DEFAULT_COLOR = 0xFFFFFF;

    public Hologram {
        Objects.requireNonNull(text, "text");
        String clean = text.replace('\r', ' ')
            .replace('\n', ' ')
            .trim();
        if (clean.length() > MAX_TEXT_LENGTH) clean = clean.substring(0, MAX_TEXT_LENGTH);
        text = clean;
        color &= 0xFFFFFF;
    }

    public HologramPos pos() {
        return new HologramPos(dimension, x, y, z);
    }

    public Hologram withText(final String newText) {
        return new Hologram(dimension, x, y, z, newText, color);
    }
}
