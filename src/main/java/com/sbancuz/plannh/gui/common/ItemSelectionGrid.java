package com.sbancuz.plannh.gui.common;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.sbancuz.plannh.Compat;

import gregtech.api.util.GTUtility;

/** Item choices edit the plan, never the source inventory or NEI's stacks. */
public final class ItemSelectionGrid extends SelectionGrid {

    public ItemSelectionGrid(final List<ItemStack> choices, final Consumer<ItemStack> select) {
        this(choices, COLUMNS, 6, select);
    }

    public ItemSelectionGrid(final List<ItemStack> choices, final int maxColumns, final int maxRows,
        final Consumer<ItemStack> select) {
        name("item.selection.grid");
        final List<ButtonWidget<?>> buttons = new ArrayList<>();
        for (final ItemStack stack : choices) {
            if (stack == null || stack.getItem() == null) continue;
            final ItemStack choice = stack.copy();
            choice.stackSize = 1;
            final ItemStack display = choice.copy();
            if (Compat.GREGTECH.isLoaded) GTUtility.updateItemStack(display);
            buttons.add(
                new ButtonWidget<>().name("item.selection.item")
                    .size(SLOT)
                    .padding(1)
                    .background(GuiTextures.SLOT_ITEM)
                    .overlay(
                        new ItemDrawable(display).asIcon()
                            .size(SLOT - 2))
                    .tooltipBuilder(tooltip -> tooltip.addFromItem(display))
                    .onMousePressed(mouse -> {
                        if (mouse != 0 && mouse != 1 && mouse != 2) return false;
                        select.accept(mouse == 2 ? null : choice.copy());
                        return true;
                    }));
        }
        choices(buttons, maxColumns, maxRows);
    }
}
