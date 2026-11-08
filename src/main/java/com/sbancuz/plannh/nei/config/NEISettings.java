package com.sbancuz.plannh.nei.config;

import codechicken.lib.config.ConfigTag;
import codechicken.lib.config.ConfigTagParent;
import codechicken.nei.NEIClientConfig;

public final class NEISettings {

    private static final ConfigTagParent defaults = new ConfigTagParent() {

        @Override
        public void saveConfig() {}

        @Override
        public String getNameQualifier() {
            return "";
        }
    };
    private static volatile boolean registered;

    private NEISettings() {}

    public static void register() {
        AppearanceConfig.register();
        SolverConfig.register();
        LayoutConfig.register();
        DebugConfig.register();
        KeybindsConfig.register();
        registered = true;
    }

    public static ConfigTag setting(final String key) {
        // Solver/layout code can run before NEI boots, including in the headless harness.
        return registered ? NEIClientConfig.getSetting(key) : defaults.getTag(key);
    }
}
