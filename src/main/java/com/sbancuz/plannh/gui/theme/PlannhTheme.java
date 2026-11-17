package com.sbancuz.plannh.gui.theme;

import static com.sbancuz.plannh.gui.theme.PlannhWidgetThemes.*;

import net.minecraftforge.common.MinecraftForge;

import com.cleanroommc.modularui.api.IThemeApi;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.DrawableSerialization;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.theme.ReloadThemeEvent;
import com.cleanroommc.modularui.theme.ThemeBuilder;
import com.cleanroommc.modularui.theme.WidgetThemeKey;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.utils.JsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.gtnewhorizon.gtnhlib.color.ColorResource;
import com.sbancuz.plannh.data.flowchart.Summary;
import com.sbancuz.plannh.gui.PlannhColors;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * The flowchart's screen theme, filled from {@link PlannhColors}. It is rebuilt on every theme
 * reload, after GTNHLib has re-read the {@code ColorResource} lang overrides.
 */
public final class PlannhTheme {

    public static final String ID = "plannh:flowchart";

    private static final UITexture GRIP = UITexture.builder()
        .location("plannh", "textures/gui/summary_grip.png")
        .fullImage()
        .iconColorType()
        .name("plannh_summary_grip")
        .build();

    // One builder for the whole session: MUI2 keeps every registered builder across reloads.
    private static final ThemeBuilder<?> THEME = new ThemeBuilder<>(ID).parent("vanilla");

    private PlannhTheme() {}

    /** Call from {@code ClientProxy.preInit}, after {@link PlannhWidgetThemes#init()}. */
    public static void init() {
        IThemeApi.get()
            .registerTheme(THEME);
        MinecraftForge.EVENT_BUS.register(new PlannhTheme());
    }

    @SubscribeEvent
    public void onReloadThemes(final ReloadThemeEvent.Pre event) {
        build();
    }

    private static void build() {
        // Root: white panel text, no shadow, no default size
        THEME.defaultWidth(ROOT, 0)
            .defaultHeight(ROOT, 0)
            .textColor(ROOT, PlannhColors.TEXT.getColor())
            .textShadow(ROOT, false);

        // Text roles on panels
        text(TEXT_SECONDARY, PlannhColors.TEXT_SECONDARY);
        text(TEXT_MUTED, PlannhColors.TEXT_MUTED);
        text(TEXT_INFO, PlannhColors.INFO);
        text(TEXT_WARNING, PlannhColors.WARNING);
        text(TEXT_SELECTED, PlannhColors.SELECTED);
        text(TEXT_NEGATIVE, PlannhColors.NEGATIVE);

        // Panels: one fill and frame; menu items and rows add the hover tint
        final IDrawable panelFill = fill(PlannhColors.PANEL_BG.getColor());
        final IDrawable panelFrame = frame(PlannhColors.PANEL_BORDER.getColor(), 1);
        final IDrawable hoverFill = fill(PlannhColors.HOVER.getColor());
        final JsonArray panelBox = layers(panelFill, panelFrame);
        background(PANEL, panelBox);
        THEME.background(PANEL_HEADER, fill(PlannhColors.PANEL_BG_RAISED.getColor()))
            .background(PANEL_RULE, fill(PlannhColors.PANEL_BORDER.getColor()))
            .background(SEPARATOR, fill(PlannhColors.SEPARATOR.getColor()));

        // A hover block needs its normal block beside it, or MUI2 takes the normal one from vanilla
        background(MENU_ITEM, panelBox);
        hoverBackground(MENU_ITEM, layers(panelFill, hoverFill, panelFrame));
        empty(ROW);
        THEME.hoverBackground(ROW, hoverFill);

        // Summary sections: a darkened bar in the role color, with the grip and title in it
        for (final Summary.Section section : Summary.Section.VALUES) {
            final int role = sectionColor(section).getColor();
            final WidgetThemeKey<?> key = section(section);
            background(key, layers(fill(PlannhColors.sectionBar(role)), GRIP));
            THEME.textColor(key, role)
                .iconColor(key, role);
        }

        // Cards
        text(CARD, PlannhColors.CARD_TEXT);
        text(CARD_MUTED, PlannhColors.CARD_TEXT_MUTED);
        text(CARD_INPUT, PlannhColors.CARD_INPUT);
        text(CARD_OUTPUT, PlannhColors.CARD_OUTPUT);
        text(CARD_INFO, PlannhColors.CARD_INFO);
        text(CARD_POWER, PlannhColors.CARD_POWER);
        final int noteAccent = PlannhColors.NOTE_ACCENT.getColor();
        final IDrawable noteAccentFrame = frame(Color.withAlpha(noteAccent, 0xFF), 2);
        final IDrawable cardFrame = frame(PlannhColors.CARD_FRAME.getColor(), 1);
        background(NOTE, layers(noteAccentFrame, cardFrame));
        THEME.background(NOTE_HEADER, fill(noteAccent));

        // Canvas grid lines
        THEME.color(CANVAS, PlannhColors.GRID.getColor());

        // Text fields: no box by default, the toolbar's shows one on hover and while editing
        textField(TEXT_FIELD, PlannhColors.TEXT);
        empty(TEXT_FIELD);
        THEME.defaultWidth(TEXT_FIELD, 0)
            .defaultHeight(TEXT_FIELD, 0);
        empty(TOOLBAR_FIELD);
        hoverBackground(TOOLBAR_FIELD, panelBox);
        text(CARD_FIELD, PlannhColors.CARD_TEXT);
        THEME.background(NOTE_FIELD, fill(PlannhColors.NOTE_BG.getColor()));

        // Buttons: the close button keeps vanilla's textures and turns its icon red on hover
        final int cardText = PlannhColors.CARD_TEXT.getColor();
        THEME.background(CLOSE_BUTTON, GuiTextures.MC_BUTTON)
            .textColor(CLOSE_BUTTON, cardText)
            .iconColor(CLOSE_BUTTON, cardText)
            .hoverBackground(CLOSE_BUTTON, GuiTextures.MC_BUTTON_HOVERED)
            .iconHoverColor(CLOSE_BUTTON, PlannhColors.NEGATIVE.getColor());
        text(DANGER_BUTTON, PlannhColors.NEGATIVE);
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

    private static void text(final WidgetThemeKey<?> key, final ColorResource color) {
        THEME.textColor(key, color.getColor());
    }

    private static void textField(final WidgetThemeKey<?> key, final ColorResource text) {
        text(key, text);
        THEME.mergeAdd(
            key.getFullName(),
            new JsonBuilder().add(IThemeApi.MARKED_COLOR, PlannhColors.TEXT_SELECTION.getColor())
                .add(IThemeApi.HINT_COLOR, PlannhColors.TEXT_MUTED.getColor()));
    }

    /** {@code IDrawable.EMPTY} serializes to JSON null, which MUI2 reads as "inherit"; the string doesn't. */
    private static void empty(final WidgetThemeKey<?> key) {
        background(key, new JsonPrimitive("empty"));
    }

    private static void background(final WidgetThemeKey<?> key, final JsonElement background) {
        THEME.mergeAdd(key.getFullName(), new JsonBuilder().add(IThemeApi.BACKGROUND, background));
    }

    private static void hoverBackground(final WidgetThemeKey<?> key, final JsonElement background) {
        final String hoverKey = key.getFullName() + IThemeApi.HOVER_SUFFIX;
        THEME.mergeAdd(hoverKey, new JsonBuilder().add(IThemeApi.BACKGROUND, background));
    }

    /**
     * A layered background. MUI2 serializes a {@code DrawableStack}'s layers without their type,
     * so they would load back empty; each layer is serialized on its own instead.
     */
    private static JsonArray layers(final IDrawable... layers) {
        final JsonArray array = new JsonArray();
        for (final IDrawable layer : layers) {
            array.add(DrawableSerialization.serialize(layer));
        }
        return array;
    }

    private static IDrawable fill(final int color) {
        return new Rectangle().color(color);
    }

    private static IDrawable frame(final int color, final int thickness) {
        return new Rectangle().hollow(thickness)
            .color(color);
    }
}
