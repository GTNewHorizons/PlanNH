package com.sbancuz.plannh.data.flowchart;

import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import com.sbancuz.plannh.hologram.BlockAnchor;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class GraphData implements IValidated {

    protected final UUID id;
    protected int x;
    protected int y;
    protected String header;
    protected final String type = getType();
    private boolean summaryFiltered;
    /** Block in the world this element is shown above as a hologram; null if not attached. */
    @Nullable
    private BlockAnchor anchor;

    protected GraphData(UUID id) {
        this.id = id;
        header = StringUtils.capitalize(getType());
    }

    public abstract String getType();

    @Override
    public boolean invalid() {
        return id == null || header == null || type == null;
    }
}
