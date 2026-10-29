package com.sbancuz.plannh.data;

import java.util.Map;

import javax.annotation.Nullable;

import org.jetbrains.annotations.Contract;

import com.sbancuz.plannh.data.properties.RecipeProperty;

public record RecipeContext(Map<RecipeProperty<?>, Object> properties) {

    @SuppressWarnings("unchecked")
    @Contract("_, !null -> !null")
    @Nullable
    public <T> T getOrDefault(final RecipeProperty<T> prop, @Nullable final T defaultValue) {
        final T value = (T) properties.get(prop);
        return value != null ? value : defaultValue;
    }
}
