package com.sbancuz.plannh.gui;

import com.cleanroommc.modularui.animation.Animator;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.sizer.Unit;
import com.cleanroommc.modularui.widgets.TextWidget;

/** One message, with its own spawn position and MUI2 animation. */
public final class CursorMessageWidget extends ParentWidget<CursorMessageWidget> {

    private static final int DURATION_MS = 3_000;
    private static final int FADE_MS = 600;
    private static final int MARGIN = 4;
    private static final int PADDING = 5;
    private static final int CURSOR_OFFSET = 8;

    private final Animator lifetime = new Animator().duration(DURATION_MS)
        .onFinish(() -> {
            setEnabled(false);
            if (isValid()) ((ParentWidget<?>) getParent()).remove(this);
        });

    public CursorMessageWidget(final IKey message, final int cursorX, final int cursorY, final int maxWidth) {
        name("screen.message");
        coverChildren();
        padding(PADDING);
        final MessageText text = new MessageText(message).maxWidth(maxWidth)
            .textAlign(Alignment.TopLeft)
            .shadow(false)
            // Native font rendering treats alpha values below 4 as fully opaque.
            .color(
                () -> Color
                    .withAlpha(PlannhColors.TEXT_WHITE.getColor(), Math.max(4 / 255f, opacity(lifetime.getValue()))));
        child(text);
        background(
            new DynamicDrawable(() -> new Rectangle().color(fadedColor(PlannhColors.CONTEXT_BG.getColor()))),
            new DynamicDrawable(
                () -> new Rectangle().hollow()
                    .color(fadedColor(PlannhColors.CONTEXT_BORDER.getColor()))));
        left(
            () -> clampPosition(
                cursorX + CURSOR_OFFSET,
                text.getDefaultWidth() + 2 * PADDING,
                getScreen().getScreenArea().width),
            Unit.Measure.PIXEL);
        top(
            () -> clampPosition(
                cursorY + CURSOR_OFFSET,
                text.getDefaultHeight() + 2 * PADDING,
                getScreen().getScreenArea().height),
            Unit.Measure.PIXEL);
    }

    @Override
    public void onInit() {
        lifetime.animate();
    }

    private int fadedColor(final int color) {
        return Color.withAlpha(color, Color.getAlphaF(color) * opacity(lifetime.getValue()));
    }

    static float opacity(final float progress) {
        return Math.max(0, Math.min(1, (1 - progress) * DURATION_MS / FADE_MS));
    }

    static int clampPosition(final int position, final int size, final int screenSize) {
        return Math.max(MARGIN, Math.min(position, screenSize - size - MARGIN));
    }

    @Override
    public boolean canHover() {
        return false;
    }

    @Override
    public boolean canHoverThrough() {
        return true;
    }

    @Override
    public boolean canClickThrough() {
        return true;
    }

    @Override
    public void dispose() {
        lifetime.stop(true);
        super.dispose();
    }

    private static final class MessageText extends TextWidget<MessageText> {

        private MessageText(final IKey message) {
            super(message);
        }

        @Override
        public boolean canHover() {
            return false;
        }

        @Override
        public boolean canClickThrough() {
            return true;
        }
    }
}
