package com.sbancuz.plannh.nei.config;

import codechicken.nei.NEIClientConfig;
import codechicken.nei.api.API;
import codechicken.nei.config.OptionToggleButton;

public final class DebugConfig {

    public static final String ROUTE_DUMP_KEY = "plannh.debug.route_dump";

    private DebugConfig() {}

    public static void register() {
        NEIClientConfig.global.config.getTag(ROUTE_DUMP_KEY)
            .setDefaultValue("false");
        API.addOption(new OptionToggleButton(ROUTE_DUMP_KEY, true));
    }

    public static boolean routeDump() {
        return NEISettings.setting(ROUTE_DUMP_KEY)
            .getBooleanValue(false);
    }
}
