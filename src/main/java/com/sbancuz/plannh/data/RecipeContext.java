package com.sbancuz.plannh.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import org.jetbrains.annotations.Contract;

import com.sbancuz.plannh.data.flowchart.Port;
import com.sbancuz.plannh.data.properties.RecipeProperty;

/** Finalized recipe facts and port ordering. Individual ports remain configurable by their owning node. */
public final class RecipeContext {

    private final Map<RecipeProperty<?>, Object> properties;
    private final List<Port<?>> inputs;
    private final List<Port<?>> outputs;

    private RecipeContext(final Builder builder) {
        properties = Collections.unmodifiableMap(new HashMap<>(builder.properties));
        inputs = List.copyOf(builder.inputs);
        outputs = List.copyOf(builder.outputs);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<RecipeProperty<?>, Object> properties() {
        return properties;
    }

    public List<Port<?>> inputs() {
        return inputs;
    }

    public List<Port<?>> outputs() {
        return outputs;
    }

    public <T> T get(final RecipeProperty<T> property) {
        return getOrDefault(property, property.getDefaultValue());
    }

    public boolean contains(final RecipeProperty<?> property) {
        return properties.containsKey(property);
    }

    @SuppressWarnings("unchecked")
    @Contract("_, !null -> !null")
    @Nullable
    public <T> T getOrDefault(final RecipeProperty<T> property, @Nullable final T fallback) {
        final T value = (T) properties.get(property);
        return value != null ? value : fallback;
    }

    /** Mutable extraction state. Building transfers ownership of its ports to a single context. */
    public static final class Builder {

        private final Map<RecipeProperty<?>, Object> properties = new HashMap<>();
        private final List<Port<?>> inputs = new ArrayList<>();
        private final List<Port<?>> outputs = new ArrayList<>();
        private boolean built;

        private Builder() {}

        public <T> Builder property(final RecipeProperty<T> property, @Nullable final T value) {
            properties.put(Objects.requireNonNull(property), value);
            return this;
        }

        public <T> T get(final RecipeProperty<T> property) {
            return getOrDefault(property, property.getDefaultValue());
        }

        @SuppressWarnings("unchecked")
        public <T> T getOrDefault(final RecipeProperty<T> property, final T fallback) {
            final T value = (T) properties.get(property);
            return value != null ? value : fallback;
        }

        public boolean contains(final RecipeProperty<?> property) {
            return properties.containsKey(property);
        }

        public Builder input(final Port<?> port) {
            inputs.add(Objects.requireNonNull(port));
            return this;
        }

        public Builder output(final Port<?> port) {
            outputs.add(Objects.requireNonNull(port));
            return this;
        }

        public List<Port<?>> inputs() {
            return inputs;
        }

        public List<Port<?>> outputs() {
            return outputs;
        }

        public RecipeContext build() {
            if (built) throw new IllegalStateException("Recipe context has already been built");
            built = true;
            deduplicate(inputs);
            deduplicate(outputs);
            return new RecipeContext(this);
        }

        private static void deduplicate(final List<Port<?>> ports) {
            for (int i = 0; i < ports.size(); i++) {
                final Port<?> port = ports.get(i);
                for (int j = i + 1; j < ports.size();) {
                    if (!port.canConnect(ports.get(j))) {
                        j++;
                        continue;
                    }
                    port.merge(ports.remove(j));
                }
            }
        }
    }
}
