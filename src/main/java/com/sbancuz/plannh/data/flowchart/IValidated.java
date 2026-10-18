package com.sbancuz.plannh.data.flowchart;

import java.util.UUID;

public interface IValidated {

    UUID getId();

    /**
     * @return true if a field has been incorrectly loaded. This usally means checking for null values.
     */
    boolean invalid();
}
