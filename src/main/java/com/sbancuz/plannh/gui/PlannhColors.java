package com.sbancuz.plannh.gui;

import com.cleanroommc.modularui.utils.Color;
import com.gtnewhorizon.gtnhlib.color.ColorResource;

public final class PlannhColors {

    private static final ColorResource.Factory C = new ColorResource.Factory("plannh");

    // spotless:off
    // ── Surfaces (dark translucent panels: summary, context menu, messages) ──
    public static final ColorResource
        PANEL_BG        = C.argb("panel_bg",        "0xC8282C34"),
        PANEL_BG_RAISED = C.argb("panel_bg_raised", "0xE0484F5C"),
        PANEL_BORDER    = C.argb("panel_border",    "0x5A9CB8D0"),
        SEPARATOR       = C.argb("separator",       "0x3A98B4CC"),
        HOVER           = C.argb("hover",           "0x1EFFFFFF"),
        HIGHLIGHT       = C.argb("highlight",       "0x80FFFFFF");

    // ── Text on panels ──
    public static final ColorResource
        TEXT           = C.rgb("text",           "0xFFFFFF"),
        TEXT_SECONDARY = C.rgb("text_secondary", "0xB8BDC8"),
        TEXT_MUTED     = C.rgb("text_muted",     "0x99A0AC"),
        TEXT_SELECTION = C.rgb("text_selection", "0x2F72A8");

    // ── Value roles ──
    public static final ColorResource
        INFO     = C.rgb("info",     "0x88AAFF"),
        POWER    = C.rgb("power",    "0xFFFFAA"),
        WARNING  = C.rgb("warning",  "0xFFCC66"),
        SELECTED = C.rgb("selected", "0x77DDDD"),
        POSITIVE = C.rgb("positive", "0x88FF88"),
        NEGATIVE = C.rgb("negative", "0xFF8888");

    // ── Cards (node, note, group: light backgrounds) ──
    public static final ColorResource
        CARD_FRAME      = C.rgb("card_frame",      "0x373737"),
        CARD_TEXT       = C.rgb("card_text",       "0x444444"),
        CARD_TEXT_MUTED = C.rgb("card_text_muted", "0x888888"),
        CARD_INPUT      = C.rgb("card_input",      "0xA00000"),
        CARD_OUTPUT     = C.rgb("card_output",     "0x006E00");

    // ── Notes ──
    public static final ColorResource
        NOTE_BG     = C.argb("note_bg",     "0xC8FFF0A0"),
        NOTE_ACCENT = C.argb("note_accent", "0xA0B4A050");

    // ── Canvas ──
    public static final ColorResource
        GRID = C.argb("grid", "0x18FFFFFF");

    // ── Ports & edges ──
    public static final ColorResource
        PORT_UNKNOWN  = C.argb("port_unknown",  "0xDC888888"),
        PORT_INACTIVE = C.argb("port_inactive", "0xAAAAAAAA"),
        OUTLINE_DARK  = C.argb("outline_dark",  "0xC00A0A0A"),
        OUTLINE_LIGHT = C.argb("outline_light", "0xC0F2F2F2");
    // spotless:on

    private static final int SECTION_BAR_ALPHA = 0x92;
    private static final float SECTION_BAR_SHADE = 0.7f;
    private static final int NOTE_EDITING_ALPHA = 0xC8;
    private static final int PORT_INACTIVE_OVERLAY_ALPHA = 0x80;

    private PlannhColors() {}

    public static int sectionBar(final int roleColor) {
        return Color.argb(
            (int) (Color.getRed(roleColor) * SECTION_BAR_SHADE),
            (int) (Color.getGreen(roleColor) * SECTION_BAR_SHADE),
            (int) (Color.getBlue(roleColor) * SECTION_BAR_SHADE),
            SECTION_BAR_ALPHA);
    }

    public static int gridMajor() {
        final int grid = GRID.getColor();
        return Color.withAlpha(grid, Math.min(255, Color.getAlpha(grid) * 2));
    }

    public static int noteEditing() {
        return Color.withAlpha(NOTE_ACCENT.getColor(), NOTE_EDITING_ALPHA);
    }

    public static int portInactiveOverlay() {
        return Color.withAlpha(PORT_INACTIVE.getColor(), PORT_INACTIVE_OVERLAY_ALPHA);
    }

    /**
     * Contrast outline for a color: light outline under dark colors, dark outline under light
     * ones, so derived colors read on any world background.
     */
    public static int outlineFor(final int color) {
        return Color.getLuminance(color) < 0.5f ? OUTLINE_LIGHT.getColor() : OUTLINE_DARK.getColor();
    }

    /**
     * Generates a deterministic title bar color from a recipe name hash.
     * Uses golden-ratio hue distribution for visual variety.
     */
    public static int titleColor(final String name) {
        final int hash = name.hashCode() % 1000;
        final float hue = hash * 0.618033988749895f;
        final int rgb = java.awt.Color.HSBtoRGB(hue, 0.45f, 0.55f);
        final int r = rgb >> 16 & 0xFF;
        final int g = rgb >> 8 & 0xFF;
        final int b = rgb & 0xFF;
        return Color.argb(r, g, b, 100);
    }
}
