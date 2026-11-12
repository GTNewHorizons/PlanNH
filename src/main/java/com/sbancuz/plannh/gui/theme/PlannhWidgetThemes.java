package com.sbancuz.plannh.gui.theme;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntSupplier;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.IThemeApi;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.DrawableStack;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.theme.WidgetThemeKey;
import com.cleanroommc.modularui.utils.Color;
import com.gtnewhorizon.gtnhlib.color.ColorResource;
import com.sbancuz.plannh.data.flowchart.Summary;
import com.sbancuz.plannh.data.flowchart.balancer.Severity;
import com.sbancuz.plannh.gui.PlannhColors;

public final class PlannhWidgetThemes {

    public static final WidgetThemeKey<PlannhWidgetTheme> PANEL = register(
        "plannh_panel",
        theme(stack(fill(PlannhColors.PANEL_BG), frame(PlannhColors.PANEL_BORDER))));
    public static final WidgetThemeKey<PlannhWidgetTheme> PANEL_HEADER = register(
        "plannh_panel_header",
        theme(fill(PlannhColors.PANEL_BG_RAISED)));
    public static final WidgetThemeKey<PlannhWidgetTheme> PANEL_RULE = register(
        "plannh_panel_rule",
        theme(fill(PlannhColors.PANEL_BORDER)));
    public static final WidgetThemeKey<PlannhWidgetTheme> SEPARATOR = register(
        "plannh_separator",
        theme(fill(PlannhColors.SEPARATOR)));
    public static final WidgetThemeKey<PlannhWidgetTheme> ROW = register(
        "plannh_row",
        theme(null),
        theme(fill(PlannhColors.HOVER)));
    public static final WidgetThemeKey<PlannhWidgetTheme> MENU_ITEM = register(
        "plannh_menu_item",
        theme(stack(fill(PlannhColors.PANEL_BG), frame(PlannhColors.PANEL_BORDER))),
        theme(stack(fill(PlannhColors.PANEL_BG), fill(PlannhColors.HOVER), frame(PlannhColors.PANEL_BORDER))));
    public static final WidgetThemeKey<PlannhWidgetTheme> TEXT_FIELD = register(
        "plannh_text_field",
        theme(null),
        theme(stack(fill(PlannhColors.PANEL_BG), frame(PlannhColors.PANEL_BORDER))));

    public static final WidgetThemeKey<PlannhWidgetTheme> CARD_TEXT = register(
        "plannh_card_text",
        theme(null, PlannhColors.CARD_TEXT));
    public static final WidgetThemeKey<PlannhWidgetTheme> CARD_HEADER = register("plannh_card_header", theme(null));
    public static final WidgetThemeKey<PlannhWidgetTheme> NODE_HEADER = register(
        "plannh_node_header",
        theme(null, PlannhColors.CARD_TEXT));
    public static final WidgetThemeKey<PlannhWidgetTheme> NOTE = register(
        "plannh_note",
        theme(
            stack(
                frame(() -> Color.withAlpha(PlannhColors.NOTE_ACCENT.getColor(), 0xFF), 2),
                frame(PlannhColors.CARD_FRAME))));
    public static final WidgetThemeKey<PlannhWidgetTheme> NOTE_HEADER = register(
        "plannh_note_header",
        theme(fill(PlannhColors.NOTE_ACCENT)));
    public static final WidgetThemeKey<PlannhWidgetTheme> NOTE_BODY = register(
        "plannh_note_body",
        theme(fill(PlannhColors.NOTE_BG), PlannhColors.CARD_TEXT));
    public static final WidgetThemeKey<PlannhWidgetTheme> CLOSE_BUTTON = register(
        "plannh_close_button",
        theme(GuiTextures.MC_BUTTON, PlannhColors.CARD_TEXT).withIconColor(PlannhColors.CARD_TEXT::getColor),
        theme(GuiTextures.MC_BUTTON_HOVERED, PlannhColors.CARD_TEXT).withIconColor(PlannhColors.NEGATIVE::getColor));

    private static final UITexture GRIP = UITexture.builder()
        .location("plannh", "textures/gui/summary_grip.png")
        .fullImage()
        .iconColorType()
        .build();

    private static final Map<Summary.Section, WidgetThemeKey<PlannhWidgetTheme>> SECTIONS = new EnumMap<>(
        Summary.Section.class);
    private static final Map<Severity, WidgetThemeKey<PlannhWidgetTheme>> MESSAGE_ROWS = new EnumMap<>(Severity.class);

    static {
        for (final Summary.Section section : Summary.Section.VALUES) {
            final ColorResource color = sectionColor(section);
            SECTIONS.put(
                section,
                register(
                    "plannh_section_" + id(section),
                    theme(stack(fill(() -> PlannhColors.sectionBar(color.getColor())), GRIP), color)
                        .withIconColor(color::getColor)));
        }
        for (final Severity severity : Severity.values()) {
            final ColorResource color = severityColor(severity);
            MESSAGE_ROWS.put(
                severity,
                register(
                    "plannh_message_row_" + id(severity),
                    theme(null, color),
                    theme(fill(PlannhColors.HOVER), color)));
        }
    }

    private PlannhWidgetThemes() {}

    /** Registers the keys; call from {@code ClientProxy.preInit}, before MUI2 loads themes in its postInit. */
    public static void init() {}

    public static WidgetThemeKey<PlannhWidgetTheme> section(final Summary.Section section) {
        return SECTIONS.get(section);
    }

    public static WidgetThemeKey<PlannhWidgetTheme> messageRow(final Severity severity) {
        return MESSAGE_ROWS.get(severity);
    }

    private static ColorResource sectionColor(final Summary.Section section) {
        return switch (section) {
            case OUTPUTS -> PlannhColors.WARNING;
            case INPUTS -> PlannhColors.POSITIVE;
            case PROPERTIES, MACHINE_COUNTS -> PlannhColors.INFO;
            case CHOICES -> PlannhColors.SELECTED;
            case MESSAGES -> PlannhColors.POWER;
            case HELP -> PlannhColors.TEXT_SECONDARY;
            case ALL -> PlannhColors.TEXT;
        };
    }

    private static ColorResource severityColor(final Severity severity) {
        return switch (severity) {
            case ERROR -> PlannhColors.NEGATIVE;
            case WARN -> PlannhColors.WARNING;
            case INFO -> PlannhColors.INFO;
        };
    }

    private static String id(final Enum<?> constant) {
        return constant.name()
            .toLowerCase(Locale.ROOT);
    }

    private static WidgetThemeKey<PlannhWidgetTheme> register(final String id, final PlannhWidgetTheme theme) {
        return register(id, theme, theme.withNoHoverBackground());
    }

    /**
     * The parser ignores theme JSON so that the root colors of whatever theme the screen runs on
     * (vanilla, vanilla_dark, a pack's) can't override the palette.
     */
    private static WidgetThemeKey<PlannhWidgetTheme> register(final String id, final PlannhWidgetTheme theme,
        final PlannhWidgetTheme hover) {
        return IThemeApi.get()
            .widgetThemeKeyBuilder(id, PlannhWidgetTheme.class)
            .defaultTheme(theme)
            .defaultHoverTheme(hover)
            .parser((parent, json, fallback) -> parent)
            .register();
    }

    private static PlannhWidgetTheme theme(final @Nullable IDrawable background) {
        return theme(background, PlannhColors.TEXT);
    }

    private static PlannhWidgetTheme theme(final @Nullable IDrawable background, final ColorResource text) {
        return new PlannhWidgetTheme(background, text::getColor, () -> Color.WHITE.main);
    }

    private static IDrawable stack(final IDrawable... layers) {
        return new DrawableStack(layers);
    }

    private static IDrawable fill(final ColorResource color) {
        return fill(color::getColor);
    }

    private static IDrawable fill(final IntSupplier color) {
        return new DynamicDrawable(() -> new Rectangle().color(color.getAsInt()));
    }

    private static IDrawable frame(final ColorResource color) {
        return frame(color::getColor, 1);
    }

    private static IDrawable frame(final IntSupplier color, final int thickness) {
        return new DynamicDrawable(
            () -> new Rectangle().hollow(thickness)
                .color(color.getAsInt()));
    }
}
