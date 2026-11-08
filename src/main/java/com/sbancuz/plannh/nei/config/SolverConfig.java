package com.sbancuz.plannh.nei.config;

import java.util.Arrays;

import codechicken.nei.NEIClientConfig;
import codechicken.nei.api.API;

public final class SolverConfig {

    public static final String FREE_INGREDIENTS_KEY = "plannh.solver.free_ingredients";
    public static final String FREE_INGREDIENTS_DEFAULT = "Water";
    public static final String EFFORT_KEY = "plannh.solver.effort_percent";
    public static final int EFFORT_DEFAULT = 100;
    public static final int EFFORT_MIN = 25;
    public static final int EFFORT_MAX = 200;

    private SolverConfig() {}

    public static void register() {
        NEIClientConfig.global.config.getTag(FREE_INGREDIENTS_KEY)
            .setDefaultValue(FREE_INGREDIENTS_DEFAULT);
        NEIClientConfig.global.config.getTag(EFFORT_KEY)
            .setDefaultValue(Integer.toString(EFFORT_DEFAULT));
        API.addOption(new ConfigOptions.TextField(FREE_INGREDIENTS_KEY));
        API.addOption(new ConfigOptions.IntegerField(EFFORT_KEY, EFFORT_MIN, EFFORT_MAX));
    }

    /** Display names separated by semicolons, matched without case or surrounding whitespace. */
    public static String freeIngredients() {
        return NEISettings.setting(FREE_INGREDIENTS_KEY)
            .getValue(FREE_INGREDIENTS_DEFAULT);
    }

    public static boolean isFreeIngredient(final String displayName) {
        return Arrays.stream(freeIngredients().split(";"))
            .map(String::trim)
            .filter(name -> !name.isEmpty())
            .anyMatch(name -> name.equalsIgnoreCase(displayName.trim()));
    }

    public static int effort() {
        return Math.clamp(
            NEISettings.setting(EFFORT_KEY)
                .getIntValue(EFFORT_DEFAULT),
            EFFORT_MIN,
            EFFORT_MAX);
    }
}
