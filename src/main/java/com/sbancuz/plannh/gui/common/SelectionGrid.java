package com.sbancuz.plannh.gui.common;

import java.util.List;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widgets.layout.Grid;

/** Compact, uniformly sized choices shared by item and extractor pickers. */
public class SelectionGrid extends Grid {

    public static final int SLOT = 18;
    public static final int COLUMNS = 9;

    private int pickerWidth;
    private int pickerHeight;

    public SelectionGrid choices(final List<? extends IWidget> buttons) {
        return choices(buttons, COLUMNS, 6);
    }

    protected SelectionGrid choices(final List<? extends IWidget> buttons, final int maxColumns, final int maxRows) {
        final int count = Math.max(1, buttons.size());
        final int columns = Math.clamp((int) Math.ceil(Math.sqrt(count)), 1, Math.max(1, maxColumns));
        final int totalRows = (count + columns - 1) / columns;
        final int rows = Math.min(totalRows, Math.max(1, maxRows));
        final boolean scrolling = totalRows > rows;
        pickerWidth = columns * SLOT + (scrolling ? 4 : 0);
        pickerHeight = rows * SLOT;
        size(pickerWidth, pickerHeight);
        alignment(Alignment.TopLeft);
        if (scrolling) scrollable(new VerticalScrollData(false, 4));
        final List<List<IWidget>> matrix = Grid
            .createGridOfWidthElements(columns, buttons, (_, _, _, button) -> button);
        // MUI adds an empty row after an exactly filled last row.
        if (matrix.getLast()
            .isEmpty() && matrix.size() > 1) matrix.removeLast();
        grid(matrix);
        return this;
    }

    public int pickerWidth() {
        return pickerWidth;
    }

    public int pickerHeight() {
        return pickerHeight;
    }
}
