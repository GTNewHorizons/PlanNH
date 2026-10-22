package com.sbancuz.plannh.data.flowchart;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Predicate;

import lombok.Setter;

/** A sorted UUID index whose saved representation contains only its values. */
public final class GraphIndex<T extends IValidated> extends TreeMap<UUID, T> {

    @Setter
    private Predicate<? super T> validator = value -> true;
    // Keep duplicate candidates until graph-wide validation can resolve their references.
    private final List<T> duplicates = new ArrayList<>();
    private int malformed;

    public boolean isValid(final T value) {
        return !value.invalid() && validator.test(value);
    }

    public void addLoaded(final T value) {
        if (putIfAbsent(value.getId(), value) != null) duplicates.add(value);
    }

    public void rejectLoaded() {
        malformed++;
    }

    public int validate() {
        final int before = size() + duplicates.size() + malformed;
        values().removeIf(value -> !isValid(value));
        for (final T value : duplicates) {
            if (isValid(value)) putIfAbsent(value.getId(), value);
        }
        duplicates.clear();
        malformed = 0;
        return before - size();
    }
}
