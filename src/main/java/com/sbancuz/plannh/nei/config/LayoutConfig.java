package com.sbancuz.plannh.nei.config;

import com.sbancuz.plannh.PlanNH;
import com.sbancuz.plannh.gui.layout.LayoutSettings;

import codechicken.nei.NEIClientConfig;
import codechicken.nei.api.API;

public final class LayoutConfig {

    public static final String NODE_SPACING_KEY = "plannh.layout.node_spacing";
    public static final int NODE_SPACING_DEFAULT = 20;
    public static final int NODE_SPACING_MIN = 10;
    public static final int NODE_SPACING_MAX = 80;
    public static final String LAYER_SPACING_KEY = "plannh.layout.layer_spacing";
    public static final int LAYER_SPACING_DEFAULT = 70;
    public static final int LAYER_SPACING_MIN = 40;
    public static final int LAYER_SPACING_MAX = 200;
    public static final String THOROUGHNESS_KEY = "plannh.layout.thoroughness";
    public static final int THOROUGHNESS_DEFAULT = 30;
    public static final int THOROUGHNESS_MIN = 1;
    public static final int THOROUGHNESS_MAX = 30;

    private LayoutConfig() {}

    public static void register() {
        NEIClientConfig.global.config.getTag(NODE_SPACING_KEY)
            .setDefaultValue(Integer.toString(NODE_SPACING_DEFAULT));
        NEIClientConfig.global.config.getTag(LAYER_SPACING_KEY)
            .setDefaultValue(Integer.toString(LAYER_SPACING_DEFAULT));
        NEIClientConfig.global.config.getTag(THOROUGHNESS_KEY)
            .setDefaultValue(Integer.toString(THOROUGHNESS_DEFAULT));
        API.addOption(new ConfigOptions.IntegerField(NODE_SPACING_KEY, NODE_SPACING_MIN, NODE_SPACING_MAX));
        API.addOption(new ConfigOptions.IntegerField(LAYER_SPACING_KEY, LAYER_SPACING_MIN, LAYER_SPACING_MAX));
        API.addOption(new ConfigOptions.IntegerField(THOROUGHNESS_KEY, THOROUGHNESS_MIN, THOROUGHNESS_MAX));
    }

    /** The router owns the corridor floor; a smaller configured gap must not prevent arrows from turning. */
    public static LayoutSettings settings(final int corridorFloor, final int groupMinWidth, final int groupMinHeight) {
        final int layerSpacing = Math.clamp(
            NEISettings.setting(LAYER_SPACING_KEY)
                .getIntValue(LAYER_SPACING_DEFAULT),
            LAYER_SPACING_MIN,
            LAYER_SPACING_MAX);
        if (layerSpacing < corridorFloor) {
            PlanNH.LOG.info(
                "Auto-layout layer spacing {} is below the {} the arrow router needs to turn in; using {}",
                layerSpacing,
                corridorFloor,
                corridorFloor);
        }
        return new LayoutSettings(
            Math.clamp(
                NEISettings.setting(NODE_SPACING_KEY)
                    .getIntValue(NODE_SPACING_DEFAULT),
                NODE_SPACING_MIN,
                NODE_SPACING_MAX),
            Math.max(layerSpacing, corridorFloor),
            Math.clamp(
                NEISettings.setting(THOROUGHNESS_KEY)
                    .getIntValue(THOROUGHNESS_DEFAULT),
                THOROUGHNESS_MIN,
                THOROUGHNESS_MAX),
            groupMinWidth,
            groupMinHeight);
    }
}
