package com.sbancuz.plannh.gui.common;

import java.lang.ref.WeakReference;
import java.util.function.Supplier;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.widgets.Dialog;

/** An owner-local client panel, independent of the canvas viewport and its draw order. */
public final class SelectionPicker {

    private final IWidget owner;
    private final Supplier<? extends SelectionGrid> choices;
    private IPanelHandler handler;

    public SelectionPicker(final IWidget owner, final Supplier<? extends SelectionGrid> choices) {
        this.owner = owner;
        this.choices = choices;
    }

    public boolean isOpen() {
        return handler != null && handler.isPanelOpen();
    }

    public void toggle() {
        if (isOpen()) {
            close();
            return;
        }
        if (!isOwnerAvailable()) return;
        if (handler == null) {
            // The main panel retains handlers for the screen's lifetime, not the lifetime of a settings row.
            final WeakReference<SelectionPicker> picker = new WeakReference<>(this);
            handler = IPanelHandler.simple(owner.getPanel(), (_, _) -> {
                final SelectionPicker current = picker.get();
                if (current == null) throw new IllegalStateException("Picker owner was disposed");
                return current.new Popup();
            }, true);
        }
        handler.deleteCachedPanel();
        handler.openPanel();
    }

    public void close() {
        if (handler != null) handler.closePanel();
    }

    boolean isOwnerAvailable() {
        for (IWidget widget = owner; widget != null; widget = widget.getParent()) {
            if (!widget.isValid() || !widget.isEnabled()) return false;
        }
        return true;
    }

    public final class Popup extends Dialog<Void> {

        private Popup() {
            super("selection.picker." + System.identityHashCode(SelectionPicker.this));
            final SelectionGrid grid = choices.get();
            final int width = grid.pickerWidth() + 8;
            final int height = grid.pickerHeight() + 8;
            final var context = owner.getContext();
            final var screen = context.getScreenArea();
            size(width, height);
            padding(4);
            background(GuiTextures.MC_BACKGROUND);
            setCloseOnOutOfBoundsClick(true);
            pos(
                Math.clamp(context.getAbsMouseX() - width, 0, Math.max(0, screen.width - width)),
                Math.clamp(context.getAbsMouseY(), 0, Math.max(0, screen.height - height)));
            child(grid);
        }

        @Override
        public boolean onMousePressed(final int mouseButton) {
            try {
                if (isOwnerAvailable()) super.onMousePressed(mouseButton);
            } finally {
                closeIfOpen();
            }
            return true;
        }

        @Override
        public void onUpdate() {
            super.onUpdate();
            if (!isOwnerAvailable()) closeIfOpen();
        }

        @Override
        public void onClose() {
            handler.deleteCachedPanel();
            super.onClose();
        }
    }
}
