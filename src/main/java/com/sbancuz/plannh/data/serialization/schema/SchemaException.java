package com.sbancuz.plannh.data.serialization.schema;

import java.io.Serial;

import lombok.Getter;

/** A save from a newer PlanNH. This build doesn't have the steps after {@link Schema#CURRENT_VERSION}. */
@Getter
public final class SchemaException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int savedVersion;

    public SchemaException(final int savedVersion) {
        super("schemaVersion " + savedVersion + " is newer than " + Schema.CURRENT_VERSION);
        this.savedVersion = savedVersion;
    }
}
