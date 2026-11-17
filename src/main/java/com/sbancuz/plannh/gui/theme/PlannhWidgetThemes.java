package com.sbancuz.plannh.gui.theme;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import com.cleanroommc.modularui.api.IThemeApi;
import com.cleanroommc.modularui.theme.TextFieldTheme;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.theme.WidgetThemeKey;
import com.cleanroommc.modularui.utils.Color;
import com.sbancuz.plannh.data.flowchart.Summary;
import com.sbancuz.plannh.data.flowchart.balancer.Severity;

/**
 * The widget theme keys of the flowchart, as a tree of sub-keys. A sub-key inherits from its parent
 * key and ignores the root properties of the screen theme, so only {@link PlannhTheme} styles them.
 * Text fields and the close button hang off MUI2's own keys, so they keep vanilla's look and type.
 */
public final class PlannhWidgetThemes {

    // spotless:off
    public static final WidgetThemeKey<WidgetTheme> ROOT = IThemeApi.get()
        .widgetThemeKeyBuilder("plannh", WidgetTheme.class)
        .defaultTheme(new WidgetTheme(0, 0, null, Color.WHITE.main, Color.WHITE.main, false, Color.WHITE.main))
        .register();

    // ── Text on panels ──
    public static final WidgetThemeKey<WidgetTheme>
        TEXT           = ROOT.createSubKey("text"),
        TEXT_SECONDARY = TEXT.createSubKey("textSecondary"),
        TEXT_MUTED     = TEXT.createSubKey("textMuted"),
        TEXT_INFO      = TEXT.createSubKey("textInfo"),
        TEXT_WARNING   = TEXT.createSubKey("textWarning"),
        TEXT_SELECTED  = TEXT.createSubKey("textSelected"),
        TEXT_NEGATIVE  = TEXT.createSubKey("textNegative");

    // ── Panels: summary, context menu ──
    public static final WidgetThemeKey<WidgetTheme>
        PANEL        = ROOT.createSubKey("panel"),
        MENU_ITEM    = PANEL.createSubKey("menuItem"),
        PANEL_HEADER = ROOT.createSubKey("panelHeader"),
        PANEL_RULE   = ROOT.createSubKey("panelRule"),
        SEPARATOR    = ROOT.createSubKey("separator"),
        ROW          = ROOT.createSubKey("row");

    // ── Cards: node, note, group ──
    public static final WidgetThemeKey<WidgetTheme>
        CARD        = ROOT.createSubKey("card"),
        CARD_MUTED  = CARD.createSubKey("cardMuted"),
        CARD_INPUT  = CARD.createSubKey("cardInput"),
        CARD_OUTPUT = CARD.createSubKey("cardOutput"),
        CARD_INFO   = CARD.createSubKey("cardInfo"),
        CARD_POWER  = CARD.createSubKey("cardPower"),
        NOTE        = CARD.createSubKey("note"),
        NOTE_HEADER = NOTE.createSubKey("noteHeader");

    // ── Canvas ──
    public static final WidgetThemeKey<WidgetTheme> CANVAS = ROOT.createSubKey("canvas");

    // ── Text fields ──
    public static final WidgetThemeKey<TextFieldTheme>
        TEXT_FIELD    = IThemeApi.TEXT_FIELD.createSubKey("plannh"),
        TOOLBAR_FIELD = TEXT_FIELD.createSubKey("plannhToolbar"),
        CARD_FIELD    = TEXT_FIELD.createSubKey("plannhCard"),
        NOTE_FIELD    = CARD_FIELD.createSubKey("plannhNote");

    // ── Buttons ──
    public static final WidgetThemeKey<WidgetTheme>
        CLOSE_BUTTON  = IThemeApi.CLOSE_BUTTON.createSubKey("plannh"),
        DANGER_BUTTON = IThemeApi.BUTTON.createSubKey("plannhDanger");
    // spotless:on

    private static final Map<Summary.Section, WidgetThemeKey<WidgetTheme>> SECTIONS = new EnumMap<>(
        Summary.Section.class);

    static {
        for (final Summary.Section section : Summary.Section.VALUES) {
            final String id = section.name()
                .toLowerCase(Locale.ROOT);
            SECTIONS.put(section, ROOT.createSubKey("section_" + id));
        }
    }

    private PlannhWidgetThemes() {}

    /** Registers the keys; call from {@code ClientProxy.preInit}, before MUI2 loads themes in its postInit. */
    public static void init() {}

    public static WidgetThemeKey<WidgetTheme> section(final Summary.Section section) {
        return SECTIONS.get(section);
    }

    public static WidgetThemeKey<WidgetTheme> severityText(final Severity severity) {
        return switch (severity) {
            case ERROR -> TEXT_NEGATIVE;
            case WARN -> TEXT_WARNING;
            case INFO -> TEXT_INFO;
        };
    }
}
