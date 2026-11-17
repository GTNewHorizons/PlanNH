package com.sbancuz.plannh.gui.common;

import java.util.function.IntSupplier;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.sbancuz.plannh.gui.theme.PlannhWidgetThemes;

public class HeaderTextWidget extends FlowchartTextFieldWidget {

    /**
     * The height a one-line title occupies.
     *
     * <p>
     * A fallback, not the answer. A group's header row is {@code coverChildrenHeight()} over this text and
     * the button row beside it, so the real height is the taller of the two — see
     * {@code GroupWidget.contentAreaOffset()}, which measures it.
     */
    public static final int HEIGHT = 14;

    public HeaderTextWidget(FlowchartWidget<?, ?> parent) {
        super(parent);
        name("header");
        widgetTheme(PlannhWidgetThemes.TEXT_FIELD);

        height(HEIGHT);
        margin(1);
        setScale(1f);
        expanded();

        handler.getText()
            .add(
                parent.getData()
                    .getHeader());
    }

    /** A title that is highlighted in {@code editingColor} while it is being edited. */
    public HeaderTextWidget(FlowchartWidget<?, ?> parent, IntSupplier editingColor) {
        this(parent);
        background(
            new DynamicDrawable(() -> isEditing ? new Rectangle().color(editingColor.getAsInt()) : IDrawable.EMPTY));
    }

    @Override
    public @NotNull Result onMousePressed(final int mouseButton) {
        if (mouseButton == 0 && Interactable.hasControlDown() && !isFocused()) return Result.IGNORE;
        return super.onMousePressed(mouseButton);
    }

    @Override
    public boolean canScrollHorizontally() {
        return false;
    }

    @Override
    protected void onTextChanged() {
        parent.getData()
            .setHeader(
                handler.getText()
                    .get(0));
    }
}
