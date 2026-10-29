package com.sbancuz.plannh.data;

import java.util.Objects;
import java.util.function.BiConsumer;

/** Shared configuration specialized once into an independent, identified builder. */
public final class ProfileFactory<M> {

    private final MachineProfile.Template template;
    private final BiConsumer<MachineProfile.Builder, M> specialize;

    ProfileFactory(final MachineProfile.Definition<?> source, final BiConsumer<MachineProfile.Builder, M> specialize) {
        template = new MachineProfile.Template(source);
        this.specialize = Objects.requireNonNull(specialize);
    }

    public MachineProfile.Builder builder(final String id, final M modifiers) {
        final MachineProfile.Builder builder = new MachineProfile.Builder(id, template);
        specialize.accept(builder, modifiers);
        return builder;
    }
}
