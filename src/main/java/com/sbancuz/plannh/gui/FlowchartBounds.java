package com.sbancuz.plannh.gui;

/** Shared geometry for the planner and NEI's symmetric container margins, in scaled GUI pixels. */
public record FlowchartBounds(int x, int y, int width, int height, int itemColumns, int containerWidth,
    int containerLeft, int itemPanelWidth, int itemPanelLeft) {

    public static final int LEFT_MARGIN = 5;
    public static final int TOP_MARGIN = 2;
    public static final int BOTTOM_MARGIN = 30;
    public static final int NEI_GAP = 6;
    public static final int BUTTON_SIZE = 16;
    public static final int TOOLBAR_GAP = 2;
    private static final int PANEL_PADDING = 2;
    private static final int SLOT_SIZE = 18;

    /** Aligns against NEI's actual bounds, including its configurable panel padding. */
    public FlowchartBounds alignToItemPanel(final int left, final int top) {
        if (itemColumns == 0) return this;
        final int panelTop = Math.max(0, top);
        return new FlowchartBounds(
            x,
            panelTop,
            Math.max(0, left - NEI_GAP - x),
            Math.max(0, height + y - panelTop),
            itemColumns,
            containerWidth,
            containerLeft,
            itemPanelWidth,
            left);
    }

    public static FlowchartBounds calculate(final int screenWidth, final int screenHeight, final int configuredColumns,
        final boolean showItems) {
        final int width = Math.max(0, screenWidth);
        final int height = (int) Math.max(0L, (long) screenHeight - TOP_MARGIN - BOTTOM_MARGIN);
        if (!showItems) {
            return new FlowchartBounds(
                LEFT_MARGIN,
                TOP_MARGIN,
                Math.max(0, width - LEFT_MARGIN),
                height,
                0,
                width,
                0,
                0,
                width);
        }

        // ItemPanel.calculateBounds caps the panel against a 176px container, then snaps to slots.
        // Use long arithmetic: the columns option accepts Integer.MAX_VALUE.
        final long maxPanelWidth = ((long) width - 176) / 2 - PANEL_PADDING * 2;
        final long requestedWidth = Math.max(1L, configuredColumns) * SLOT_SIZE;
        final int columns = (int) Math.max(1, Math.min(requestedWidth, maxPanelWidth) / SLOT_SIZE);
        final int panelWidth = columns * SLOT_SIZE;
        final int panelLeft = width - panelWidth - PANEL_PADDING;
        final int containerLeft = (int) Math.min(width / 2L, (long) panelWidth + PANEL_PADDING * 2 + 2);
        return new FlowchartBounds(
            LEFT_MARGIN,
            TOP_MARGIN,
            Math.max(0, panelLeft - NEI_GAP - LEFT_MARGIN),
            height,
            columns,
            width - containerLeft * 2,
            containerLeft,
            panelWidth,
            panelLeft);
    }
}
