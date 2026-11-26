package com.sbancuz.plannh.gui.theme;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import net.minecraftforge.common.MinecraftForge;

import com.cleanroommc.modularui.api.IThemeApi;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.DrawableSerialization;
import com.cleanroommc.modularui.drawable.DrawableStack;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.theme.ReloadThemeEvent;
import com.cleanroommc.modularui.theme.TextFieldTheme;
import com.cleanroommc.modularui.theme.ThemeBuilder;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.theme.WidgetThemeKey;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.utils.JsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.gtnewhorizon.gtnhlib.color.ColorResource;
import com.sbancuz.plannh.data.flowchart.Summary;
import com.sbancuz.plannh.data.flowchart.balancer.Severity;
import com.sbancuz.plannh.gui.PlannhColors;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * The flowchart's ModularUI2 theme: the widget theme keys, as a tree of sub-keys, and the screen
 * theme that styles them from {@link PlannhColors}.
 * <p>
 * A sub-key inherits from its parent key and ignores the root properties of the screen theme, so
 * only this theme styles them. Text fields and buttons hang off MUI2's own keys, so they keep
 * vanilla's look and widget theme type. The theme is defined again on every theme reload, after
 * GTNHLib has re-read the {@code ColorResource} lang overrides, the way GT's {@code GTGuiTheme} does.
 */
public final class PlannhThemes {

    public static final String FLOWCHART = "plannh:flowchart";

    // spotless:off
    public static final WidgetThemeKey<WidgetTheme> ROOT = IThemeApi.get()
        .widgetThemeKeyBuilder("plannh", WidgetTheme.class)
        .defaultTheme(new WidgetTheme(18, 18, null,Color.WHITE.main, Color.WHITE.main, false, Color.WHITE.main))
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
        PANEL_OPAQUE = PANEL.createSubKey("panelOpaque"),
        MENU_ITEM    = PANEL.createSubKey("menuItem"),
        PANEL_HEADER = ROOT.createSubKey("panelHeader"),
        PANEL_RULE   = ROOT.createSubKey("panelRule"),
        SEPARATOR    = ROOT.createSubKey("separator"),
        ROW          = ROOT.createSubKey("row");
    public static final WidgetThemeKey<WidgetTheme> CHECKLIST_ROW = ROW.createSubKey("checklistRow");

    // ── Cards: node, note, group ──
    public static final WidgetThemeKey<WidgetTheme>
        CARD         = ROOT.createSubKey("card"),
        CARD_MUTED   = CARD.createSubKey("cardMuted"),
        CARD_INPUT   = CARD.createSubKey("cardInput"),
        CARD_OUTPUT  = CARD.createSubKey("cardOutput"),
        CARD_INFO    = CARD.createSubKey("cardInfo"),
        CARD_POWER   = CARD.createSubKey("cardPower"),
        CARD_BADGE   = CARD.createSubKey("cardBadge"),
        CARD_CHANGED = CARD.createSubKey("cardChanged"),
        NOTE         = CARD.createSubKey("note"),
        NOTE_HEADER  = NOTE.createSubKey("noteHeader");

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

    private static final UITexture GRIP = UITexture.builder()
        .location("plannh", "textures/gui/summary_grip.png")
        .fullImage()
        .iconColorType()
        .name("plannh_summary_grip")
        .build();

    // One builder for the whole session: MUI2 keeps every registered builder across reloads.
    private static final Builder THEME = new Builder(FLOWCHART);

    private PlannhThemes() {}

    /** Call from {@code ClientProxy.preInit}: the keys must exist before MUI2 loads themes in its postInit. */
    public static void init() {
        define();
        IThemeApi.get()
            .registerTheme(THEME);
        MinecraftForge.EVENT_BUS.register(new PlannhThemes());
    }

    @SubscribeEvent
    public void onReloadThemes(final ReloadThemeEvent.Pre event) {
        define();
    }

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

    private static void define() {
        THEME.parent("vanilla");

        // Root: white panel text, no shadow, no default size
        THEME.textColor(ROOT, PlannhColors.TEXT.getColor())
            .textShadow(ROOT, false)
            .defaultWidth(ROOT, 0)
            .defaultHeight(ROOT, 0);

        // Text roles on panels
        THEME.textColor(TEXT_SECONDARY, PlannhColors.TEXT_SECONDARY.getColor())
            .textColor(TEXT_MUTED, PlannhColors.TEXT_MUTED.getColor())
            .iconColor(TEXT_MUTED, PlannhColors.TEXT_MUTED.getColor())
            .textColor(TEXT_INFO, PlannhColors.INFO.getColor())
            .textColor(TEXT_WARNING, PlannhColors.WARNING.getColor())
            .textColor(TEXT_SELECTED, PlannhColors.SELECTED.getColor())
            .iconColor(TEXT_SELECTED, PlannhColors.SELECTED.getColor())
            .textColor(TEXT_NEGATIVE, PlannhColors.NEGATIVE.getColor());

        // Panels: one fill and frame; menu items and rows add the hover tint
        final IDrawable panelFill = fill(PlannhColors.PANEL_BG.getColor());
        final IDrawable panelFrame = frame(PlannhColors.PANEL_BORDER.getColor(), 1);
        final IDrawable hoverFill = fill(PlannhColors.HOVER.getColor());
        final IDrawable panelBox = new DrawableStack(panelFill, panelFrame);
        final IDrawable opaqueFill = fill(Color.withAlpha(PlannhColors.PANEL_BG.getColor(), 0xFF));
        THEME.background(PANEL, panelBox)
            .background(PANEL_OPAQUE, new DrawableStack(opaqueFill, panelFrame))
            .background(PANEL_HEADER, fill(PlannhColors.PANEL_BG_RAISED.getColor()))
            .background(PANEL_RULE, fill(PlannhColors.PANEL_BORDER.getColor()))
            .background(SEPARATOR, fill(PlannhColors.SEPARATOR.getColor()));

        // A hover block needs its normal block beside it, or MUI2 takes the normal one from the parent theme
        THEME.background(MENU_ITEM, panelBox)
            .hoverBackground(MENU_ITEM, new DrawableStack(panelFill, hoverFill, panelFrame))
            .background(ROW, IDrawable.EMPTY)
            .hoverBackground(ROW, hoverFill);

        // Checklist rows: secondary text and positive ticks
        THEME.background(CHECKLIST_ROW, IDrawable.EMPTY)
            .hoverBackground(CHECKLIST_ROW, hoverFill)
            .textColor(CHECKLIST_ROW, PlannhColors.TEXT_SECONDARY.getColor())
            .iconColor(CHECKLIST_ROW, PlannhColors.POSITIVE.getColor());

        // Summary sections: a darkened bar in the role color, with the grip and title in it
        for (final Summary.Section section : Summary.Section.VALUES) {
            final int role = sectionColor(section).getColor();
            final IDrawable bar = fill(PlannhColors.sectionBar(role));
            THEME.background(section(section), new DrawableStack(bar, GRIP))
                .textColor(section(section), role)
                .iconColor(section(section), role);
        }

        // Cards
        final int noteAccent = PlannhColors.NOTE_ACCENT.getColor();
        final IDrawable noteAccentFrame = frame(Color.withAlpha(noteAccent, 0xFF), 2);
        final IDrawable cardFrame = frame(PlannhColors.CARD_FRAME.getColor(), 1);
        THEME.textColor(CARD, PlannhColors.CARD_TEXT.getColor())
            .textColor(CARD_MUTED, PlannhColors.CARD_TEXT_MUTED.getColor())
            .textColor(CARD_INPUT, PlannhColors.CARD_INPUT.getColor())
            .textColor(CARD_OUTPUT, PlannhColors.CARD_OUTPUT.getColor())
            .textColor(CARD_INFO, PlannhColors.CARD_INFO.getColor())
            .textColor(CARD_POWER, PlannhColors.CARD_POWER.getColor())
            .textColor(CARD_CHANGED, PlannhColors.CARD_CHANGED.getColor())
            .textColor(CARD_BADGE, PlannhColors.TEXT.getColor())
            .textShadow(CARD_BADGE, true)
            .background(NOTE, new DrawableStack(noteAccentFrame, cardFrame))
            .background(NOTE_HEADER, fill(noteAccent));

        // Canvas grid lines
        THEME.color(CANVAS, PlannhColors.GRID.getColor());

        // Text fields: no box by default, the toolbar's shows one on hover and while editing
        THEME.textColor(TEXT_FIELD, PlannhColors.TEXT.getColor())
            .markedColor(TEXT_FIELD, PlannhColors.TEXT_SELECTION.getColor())
            .hintColor(TEXT_FIELD, PlannhColors.TEXT_MUTED.getColor())
            .background(TEXT_FIELD, IDrawable.EMPTY)
            .defaultWidth(TEXT_FIELD, 0)
            .defaultHeight(TEXT_FIELD, 0)
            .background(TOOLBAR_FIELD, IDrawable.EMPTY)
            .hoverBackground(TOOLBAR_FIELD, panelBox)
            .textColor(CARD_FIELD, PlannhColors.CARD_TEXT.getColor())
            .background(NOTE_FIELD, fill(PlannhColors.NOTE_BG.getColor()));

        // Buttons: the close button keeps vanilla's textures and turns its icon red on hover
        final int cardText = PlannhColors.CARD_TEXT.getColor();
        THEME.background(CLOSE_BUTTON, GuiTextures.MC_BUTTON)
            .textColor(CLOSE_BUTTON, cardText)
            .iconColor(CLOSE_BUTTON, cardText)
            .hoverBackground(CLOSE_BUTTON, GuiTextures.MC_BUTTON_HOVERED)
            .iconHoverColor(CLOSE_BUTTON, PlannhColors.NEGATIVE.getColor())
            .textColor(DANGER_BUTTON, PlannhColors.NEGATIVE.getColor());
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

    private static IDrawable fill(final int color) {
        return new Rectangle().color(color);
    }

    private static IDrawable frame(final int color, final int thickness) {
        return new Rectangle().hollow(thickness)
            .color(color);
    }

    /** MUI2's {@link ThemeBuilder}, with backgrounds that survive serialization and text field colors on any key. */
    private static final class Builder extends ThemeBuilder<Builder> {

        private Builder(final String id) {
            super(id);
        }

        @Override
        public Builder background(final WidgetThemeKey<?> key, final IDrawable background) {
            mergeAdd(key.getFullName(), new JsonBuilder().add(IThemeApi.BACKGROUND, serialize(background)));
            return this;
        }

        @Override
        public Builder hoverBackground(final WidgetThemeKey<?> key, final IDrawable background) {
            final String hoverKey = key.getFullName() + IThemeApi.HOVER_SUFFIX;
            mergeAdd(hoverKey, new JsonBuilder().add(IThemeApi.BACKGROUND, serialize(background)));
            return this;
        }

        public Builder markedColor(final WidgetThemeKey<?> key, final int color) {
            mergeAdd(key.getFullName(), new JsonBuilder().add(IThemeApi.MARKED_COLOR, color));
            return this;
        }

        public Builder hintColor(final WidgetThemeKey<?> key, final int color) {
            mergeAdd(key.getFullName(), new JsonBuilder().add(IThemeApi.HINT_COLOR, color));
            return this;
        }

        /**
         * MUI2 serializes a {@link DrawableStack}'s layers without their type, so they load back
         * empty, and {@link IDrawable#EMPTY} as JSON null, which reads back as "inherit".
         */
        private static JsonElement serialize(final IDrawable drawable) {
            if (drawable == IDrawable.EMPTY) return new JsonPrimitive("empty");
            if (!(drawable instanceof DrawableStack stack)) return DrawableSerialization.serialize(drawable);

            final JsonArray layers = new JsonArray();
            for (final IDrawable layer : stack.getDrawables()) {
                layers.add(serialize(layer));
            }
            return layers;
        }
    }
}
