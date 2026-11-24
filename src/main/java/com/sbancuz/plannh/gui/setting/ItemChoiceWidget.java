package com.sbancuz.plannh.gui.setting;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.integration.recipeviewer.RecipeViewerGhostIngredientSlot;
import com.cleanroommc.modularui.integration.recipeviewer.RecipeViewerIngredientProvider;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.sbancuz.plannh.data.setting.ChoiceSettingDef;
import com.sbancuz.plannh.gui.common.ItemSelectionGrid;
import com.sbancuz.plannh.gui.common.ItemWidget;
import com.sbancuz.plannh.gui.common.SelectionPicker;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;

/** Client-only item selection: NEI drops edit the plan, never an inventory or server-side slot. */
public final class ItemChoiceWidget extends ItemWidget<ItemChoiceWidget>
    implements RecipeViewerGhostIngredientSlot<ItemStack>, RecipeViewerIngredientProvider {

    private final Predicate<ItemStack> accepts;
    private final Consumer<ItemStack> select;
    private final Consumer<Runnable> edit;
    private Supplier<List<ItemStack>> choices;
    private Runnable reset;
    private SelectionPicker picker;
    private String hint = "plannh.settings.item_drop_hint";
    private BiConsumer<RichTooltip, ItemStack> pickerTooltip;
    private IntConsumer scrollHandler;
    private final IntUnaryOperator selectionCount;

    /**
     * A slot with the item of {@code selection}. A selected item gets a count in the range of {@code anchor}, or 1 when
     * {@code anchor} doesn't have a count. Each change of the selection runs inside {@code edit}.
     */
    public ItemChoiceWidget(final ChoiceSettingDef<?> anchor, final Supplier<ItemStack> selection,
        final Predicate<ItemStack> accepts, final Consumer<ItemStack> select, final Consumer<Runnable> edit) {
        super(selection);
        this.accepts = accepts;
        this.select = select;
        this.edit = edit;
        selectionCount = count -> anchor.isCounted()
            ? Math.clamp(count, Math.max(1, anchor.getMinCount()), anchor.getMaxCount())
            : 1;
        name("settings.item_choice");
        size(18).padding(1)
            .background(GuiTextures.SLOT_ITEM)
            .tooltipBuilder(
                tooltip -> TooltipBuilder.create(tooltip)
                    .langRow(hint)
                    .flush())
            .tooltipAutoUpdate(true);
    }

    public ItemChoiceWidget hint(final String hint) {
        this.hint = hint;
        markTooltipDirty();
        return this;
    }

    public ItemChoiceWidget picker(final Supplier<List<ItemStack>> choices, final Runnable reset) {
        this.choices = choices;
        this.reset = reset;
        return this;
    }

    public ItemChoiceWidget pickerTooltip(final BiConsumer<RichTooltip, ItemStack> tooltip) {
        this.pickerTooltip = tooltip;
        return this;
    }

    /** Handles wheel input locally instead of passing it through to the canvas. */
    public ItemChoiceWidget scrollHandler(final IntConsumer handler) {
        this.scrollHandler = handler;
        return this;
    }

    @Override
    public boolean onMouseScroll(final UpOrDown direction, final int amount) {
        if (scrollHandler == null) return false;
        scrollHandler.accept(direction == UpOrDown.UP ? 1 : -1);
        return true;
    }

    public boolean resetSelection() {
        if (reset == null) return false;
        closePicker();
        edit.accept(reset);
        return true;
    }

    public ItemSelectionGrid createPicker() {
        final List<ItemStack> offered = choices.get()
            .stream()
            .filter(stack -> stack != null && stack.getItem() != null)
            .toList();
        return new ItemSelectionGrid(offered, accepts, pickerTooltip, stack -> {
            if (stack == null) {
                resetSelection();
            } else if (accepts.test(stack)) {
                closePicker();
                edit.accept(() -> select.accept(selectedStack(stack)));
            }
        });
    }

    @Override
    public Result onMousePressed(final int mouseButton) {
        if (choices == null) return super.onMousePressed(mouseButton);
        if (mouseButton == 2) return resetSelection() ? Result.SUCCESS : Result.IGNORE;
        if (mouseButton != 0 && mouseButton != 1) return Result.IGNORE;
        if (!isValid()) return Result.IGNORE;
        if (picker == null) picker = new SelectionPicker(this, this::createPicker);
        picker.toggle();
        return Result.SUCCESS;
    }

    private void closePicker() {
        if (picker != null) picker.close();
    }

    @Override
    public boolean handleDragAndDrop(final ItemStack draggedStack, final int button) {
        // ModularUI routes every mouse button here while an NEI ghost drag is active.
        if (button == 2) {
            final boolean reset = onMousePressed(button) == Result.SUCCESS;
            if (reset && draggedStack != null) draggedStack.stackSize = 0;
            return reset;
        }
        if (draggedStack == null || draggedStack.getItem() == null
            || draggedStack.stackSize <= 0
            || button != 0 && button != 1
            || !accepts.test(draggedStack)) return false;
        final ItemStack selected = selectedStack(draggedStack);
        closePicker();
        edit.accept(() -> select.accept(selected));
        // This is NEI's disposable ghost stack, not the source inventory/recipe stack.
        draggedStack.stackSize = 0;
        return true;
    }

    private ItemStack selectedStack(final ItemStack source) {
        final ItemStack selected = source.copy();
        selected.stackSize = selectionCount.applyAsInt(source.stackSize);
        return selected;
    }

    @Override
    public ItemStack getStackForRecipeViewer() {
        return getShownItem();
    }
}
