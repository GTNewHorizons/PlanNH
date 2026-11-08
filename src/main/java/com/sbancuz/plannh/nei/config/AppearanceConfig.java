package com.sbancuz.plannh.nei.config;

import codechicken.nei.NEIClientConfig;
import codechicken.nei.api.API;
import codechicken.nei.config.OptionCycled;
import codechicken.nei.config.OptionIntegerField;

/** NEI-backed appearance settings for the flowchart. */
public final class AppearanceConfig {

    public static final String ITEM_COLUMNS_KEY = "plannh.appearance.item_columns";
    public static final int ITEM_COLUMNS_MIN = 6;
    public static final int ITEM_COLUMNS_MAX = Integer.MAX_VALUE;
    public static final int ITEM_COLUMNS_DEFAULT = 9;

    public static final String BLUR_STRENGTH_KEY = "plannh.appearance.blur_strength";
    public static final int BLUR_STRENGTH_MIN = 0;
    public static final int BLUR_STRENGTH_MAX = 255;
    public static final int BLUR_STRENGTH_DEFAULT = 16;

    public static final String SHOW_GRID_KEY = "plannh.appearance.show_grid";
    public static final int SHOW_GRID_DEFAULT = 1;

    public static final String BACKGROUND_COLOR_KEY = "plannh.appearance.background_color";
    public static final int BACKGROUND_COLOR_DEFAULT = 0x22000000;

    private AppearanceConfig() {}

    public static void register() {
        NEIClientConfig.global.config.getTag(ITEM_COLUMNS_KEY)
            .setDefaultValue(Integer.toString(ITEM_COLUMNS_DEFAULT));
        NEIClientConfig.global.config.getTag(BLUR_STRENGTH_KEY)
            .setDefaultValue(Integer.toString(BLUR_STRENGTH_DEFAULT));
        NEIClientConfig.global.config.getTag(SHOW_GRID_KEY)
            .setDefaultValue(Integer.toString(SHOW_GRID_DEFAULT));
        NEIClientConfig.global.config.getTag(BACKGROUND_COLOR_KEY)
            .setDefaultValue("0x" + Integer.toHexString(BACKGROUND_COLOR_DEFAULT));

        API.addOption(new ConfigOptions.IntegerField(ITEM_COLUMNS_KEY, ITEM_COLUMNS_MIN, ITEM_COLUMNS_MAX));
        API.addOption(new ConfigOptions.IntegerField(BLUR_STRENGTH_KEY, BLUR_STRENGTH_MIN, BLUR_STRENGTH_MAX));
        API.addOption(new OptionCycled(SHOW_GRID_KEY, 2));
        API.addOption(new ConfigOptions.IntegerField(BACKGROUND_COLOR_KEY, 0, OptionIntegerField.UNSIGNED_INT_MAX) {

            @Override
            public boolean isValidValue(final String value) {
                return value.startsWith("0x") && super.isValidValue(value);
            }
        });
    }

    public static int itemColumns() {
        return Math.max(
            ITEM_COLUMNS_MIN,
            NEISettings.setting(ITEM_COLUMNS_KEY)
                .getIntValue(ITEM_COLUMNS_DEFAULT));
    }

    public static int blurStrength() {
        return Math.clamp(
            NEISettings.setting(BLUR_STRENGTH_KEY)
                .getIntValue(BLUR_STRENGTH_DEFAULT),
            BLUR_STRENGTH_MIN,
            BLUR_STRENGTH_MAX);
    }

    public static boolean showGrid() {
        return NEISettings.setting(SHOW_GRID_KEY)
            .getIntValue(SHOW_GRID_DEFAULT) == SHOW_GRID_DEFAULT;
    }

    public static int backgroundColor() {
        return NEISettings.setting(BACKGROUND_COLOR_KEY)
            .getHexValue(BACKGROUND_COLOR_DEFAULT);
    }

}
