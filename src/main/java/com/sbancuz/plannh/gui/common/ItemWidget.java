package com.sbancuz.plannh.gui.common;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.sbancuz.plannh.gui.PlannhColors;

import lombok.Getter;

/**
 * An item in the padded area of the widget, with the stack size when the size is not 1. The tooltip starts with the
 * item's tooltip, and a later {@code tooltipBuilder} adds rows below: MUI2 chains the builders. The tooltip is rebuilt
 * when the item changes, and when shift goes down or up, because an item's tooltip can have more rows under shift.
 * <p>
 * A button: a click handler makes the widget clickable. With no handler, a click goes on to the widget below. The
 * button background of the theme is off, and the hover background of the theme is on.
 */
public class ItemWidget<W extends ItemWidget<W>> extends ButtonWidget<W> {

    private final Supplier<ItemStack> source;
    private final ItemDrawable drawable = new ItemDrawable();
    /** A copy of the item of {@code source} at the last update, or {@code null} for no item. */
    @Getter
    private @Nullable ItemStack shownItem;
    private IDrawable count = IDrawable.EMPTY;
    private boolean shiftHeld;

    public ItemWidget(@Nullable final ItemStack item) {
        this(() -> item);
    }

    public ItemWidget(final Supplier<ItemStack> source) {
        this.source = source;
        disableThemeBackground(true);
        refreshItem();
        tooltipBuilder(tooltip -> { if (shownItem != null) tooltip.addFromItem(shownItem); });
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        refreshItem();

        final boolean shift = Interactable.hasShiftDown();
        if (shift == shiftHeld) return;
        shiftHeld = shift;
        markTooltipDirty();
    }

    private void refreshItem() {
        final ItemStack item = source.get();
        if (ItemStack.areItemStacksEqual(shownItem, item)) return;
        shownItem = item == null || item.getItem() == null ? null : item.copy();
        drawable.setItem(shownItem);

        final boolean counted = shownItem != null && shownItem.stackSize != 1;
        count = counted ? IKey.str(Integer.toString(shownItem.stackSize))
            .alignment(Alignment.BottomRight)
            .color(PlannhColors.TEXT::getColor)
            .shadow(true) : IDrawable.EMPTY;
        markTooltipDirty();
    }

    @Override
    public void draw(final ModularGuiContext context, final WidgetThemeEntry<?> widgetTheme) {
        if (shownItem == null) return;
        drawable.drawAtZeroPadded(context, getArea(), widgetTheme.getTheme());
        count.drawAtZeroPadded(context, getArea(), widgetTheme.getTheme());
    }
}
