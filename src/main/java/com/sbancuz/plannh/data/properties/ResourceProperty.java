package com.sbancuz.plannh.data.properties;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.ObjLongConsumer;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import com.cleanroommc.modularui.screen.RichTooltip;
import com.sbancuz.plannh.data.flowchart.Port;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder(builderMethodName = "emptyBuilder")
public class ResourceProperty<T> extends SummaryProperty<T> {

    @lombok.Builder.Default
    private final ToLongFunction<T> amountExtractor = _ -> 1;
    @lombok.Builder.Default
    private final ObjLongConsumer<T> amountUpdater = (_, _) -> {};

    @lombok.Builder.Default
    private final BiPredicate<Port<T>, Port<T>> connectionChecker = (_, _) -> true;
    @lombok.Builder.Default
    private final ToIntFunction<T> hashCodeExtractor = Objects::hashCode;

    @lombok.Builder.Default
    private final ToIntFunction<T> colorProvider = _ -> -1;
    @lombok.Builder.Default
    private final BiConsumer<RichTooltip, T> tooltip = (_, _) -> {};

    // Pin/arrow fallback colors; opaque white so a type that declares none stays visible.
    @Getter
    @lombok.Builder.Default
    private final int pinInputColor = 0xFFFFFFFF;
    @Getter
    @lombok.Builder.Default
    private final int pinOutputColor = 0xFFFFFFFF;
    @Getter
    @lombok.Builder.Default
    private final int arrowColor = 0xFFFFFFFF;

    @Override
    public String displayName() {
        return getKey();
    }

    public long extractAmount(final T value) {
        return amountExtractor.applyAsLong(value);
    }

    public void setAmount(final T value, final long newAmount) {
        amountUpdater.accept(value, newAmount);
    }

    public boolean canConnect(final Port<T> a, final Port<T> b) {
        return connectionChecker.test(a, b);
    }

    public int hashValue(final T value) {
        return hashCodeExtractor.applyAsInt(value);
    }

    /** Representative bare-RGB color for this value, or -1 when none is derivable. */
    public int color(final T value) {
        return colorProvider.applyAsInt(value);
    }

    @SuppressWarnings("unchecked")
    public void addTooltip(final RichTooltip tooltip, final Object value) {
        this.tooltip.accept(tooltip, (T) value);
    }

    public static <T> ResourcePropertyBuilder<T, ?, ?> builder(final String key, final T defaultValue) {
        return ResourceProperty.<T>emptyBuilder()
            .key(key)
            .defaultValue(defaultValue);
    }
}
