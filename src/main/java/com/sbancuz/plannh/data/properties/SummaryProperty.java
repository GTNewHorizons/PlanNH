package com.sbancuz.plannh.data.properties;

import java.util.function.Function;

import com.sbancuz.plannh.gui.GuiHelper;

import lombok.experimental.SuperBuilder;

@SuperBuilder(builderMethodName = "emptyBuilder")
public class SummaryProperty<T> extends RecipeProperty<T> {

    @lombok.Builder.Default
    private final Function<Double, String> amountFormatter = GuiHelper::formatRate;

    public String formatAmount(final double value) {
        return amountFormatter.apply(value);
    }

    public static <T> SummaryPropertyBuilder<T, ?, ?> builder(final String key, final T defaultValue) {
        return SummaryProperty.<T>emptyBuilder()
            .key(key)
            .defaultValue(defaultValue);
    }
}
