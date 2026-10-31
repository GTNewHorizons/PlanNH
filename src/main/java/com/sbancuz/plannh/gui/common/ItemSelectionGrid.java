package com.sbancuz.plannh.gui.common;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;

/** Item choices edit the plan, never the source inventory or NEI's stacks. */
public final class ItemSelectionGrid extends SelectionGrid {

    public ItemSelectionGrid(final List<ItemStack> choices, final Consumer<ItemStack> select) {
        this(choices, COLUMNS, 6, ignored -> true, select);
    }

    public ItemSelectionGrid(final List<ItemStack> choices, final int maxColumns, final int maxRows,
        final Consumer<ItemStack> select) {
        this(choices, maxColumns, maxRows, ignored -> true, select);
    }

    public ItemSelectionGrid(final List<ItemStack> choices, final Predicate<ItemStack> accepts,
        final Consumer<ItemStack> select) {
        this(choices, COLUMNS, 6, accepts, select);
    }

    public ItemSelectionGrid(final List<ItemStack> choices, final Predicate<ItemStack> accepts,
        final BiConsumer<RichTooltip, ItemStack> contextTooltip, final Consumer<ItemStack> select) {
        this(choices, COLUMNS, 6, accepts, contextTooltip, select);
    }

    public ItemSelectionGrid(final List<ItemStack> choices, final int maxColumns, final int maxRows,
        final Predicate<ItemStack> accepts, final Consumer<ItemStack> select) {
        this(choices, maxColumns, maxRows, accepts, null, select);
    }

    public ItemSelectionGrid(final List<ItemStack> choices, final int maxColumns, final int maxRows,
        final Predicate<ItemStack> accepts, final BiConsumer<RichTooltip, ItemStack> contextTooltip,
        final Consumer<ItemStack> select) {
        name("item.selection.grid");
        final List<ButtonWidget<?>> buttons = new ArrayList<>();
        for (final ItemStack stack : choices) {
            if (stack == null || stack.getItem() == null) continue;
            final ItemStack choice = stack.copy();
            choice.stackSize = 1;
            final ItemStack display = choice.copy();
            buttons.add(
                new ButtonWidget<>().name("item.selection.item")
                    .size(SLOT)
                    .padding(1)
                    .background(GuiTextures.SLOT_ITEM)
                    .overlay(
                        new ItemDrawable(display).asIcon()
                            .size(SLOT - 2))
                    .tooltipBuilder(richTooltip -> {
                        richTooltip.addFromItem(display);
                        if (contextTooltip != null) contextTooltip.accept(richTooltip, choice);
                        if (!accepts.test(choice)) TooltipBuilder.create(richTooltip)
                            .langRow("plannh.settings.item_choice_invalid")
                            .flush();
                    })
                    .tooltipAutoUpdate(true)
                    .onMousePressed(mouse -> {
                        if (mouse != 0 && mouse != 1 && mouse != 2) return false;
                        select.accept(mouse == 2 ? null : choice.copy());
                        return true;
                    }));
        }
        choices(buttons, maxColumns, maxRows);
    }
}
