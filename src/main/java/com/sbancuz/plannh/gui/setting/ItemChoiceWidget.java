package com.sbancuz.plannh.gui.setting;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.integration.recipeviewer.RecipeViewerGhostIngredientSlot;
import com.cleanroommc.modularui.integration.recipeviewer.RecipeViewerIngredientProvider;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.setting.ChoiceSettingDef;
import com.sbancuz.plannh.gui.common.ItemSelectionGrid;
import com.sbancuz.plannh.gui.common.SelectionPicker;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;

/** Client-only item selection: NEI drops edit the plan, never an inventory or server-side slot. */
public final class ItemChoiceWidget extends ButtonWidget<ItemChoiceWidget>
    implements RecipeViewerGhostIngredientSlot<ItemStack>, RecipeViewerIngredientProvider {

    private final Supplier<ItemStack> selection;
    private final Predicate<ItemStack> accepts;
    private final Consumer<ItemStack> select;
    private final Consumer<Runnable> edit;
    private Supplier<List<ItemStack>> choices;
    private Runnable reset;
    private final ItemDrawable item = new ItemDrawable();
    private ItemStack displayedSelection;
    private SelectionPicker picker;
    private String hint = "plannh.settings.item_drop_hint";
    private boolean shiftHeld;
    private BiConsumer<RichTooltip, ItemStack> pickerTooltip;
    private IntConsumer scrollHandler;
    private IntUnaryOperator selectionCount;

    /** Binds native item choices to their stored value, including non-item settings. */
    public <T> ItemChoiceWidget(final ChoiceSettingDef<T> setting, final MachineConfig cfg, final RecipeContext ctx,
        final Consumer<Runnable> edit, final Supplier<List<ItemStack>> choices, final Function<T, ItemStack> display,
        final Function<ItemStack, T> value) {
        this(
            setting,
            () -> display.apply(cfg.value(setting, ctx)),
            stack -> value.apply(stack) != null,
            stack -> cfg.set(setting, value.apply(stack)),
            edit);
        selectionCount = count -> setting.isCounted()
            ? Math.clamp(count, Math.max(1, setting.minimumCount(cfg, ctx)), setting.maximumCount(cfg, ctx))
            : 1;
        picker(choices, () -> cfg.reset(setting));
        hint("plannh.settings.item_choice_hint");
        tooltipBuilder(
            tooltip -> TooltipBuilder.create(tooltip)
                .langRow(cfg.isExplicit(setting) ? "plannh.settings.choice_manual" : "plannh.settings.choice_auto")
                .flush());
    }

    public ItemChoiceWidget(final ChoiceSettingDef<?> anchor, final Supplier<ItemStack> selection,
        final Predicate<ItemStack> accepts, final Consumer<ItemStack> select, final Consumer<Runnable> edit) {
        this.selection = selection;
        this.accepts = accepts;
        this.select = select;
        this.edit = edit;
        selectionCount = count -> anchor.isCounted()
            ? Math.clamp(count, Math.max(1, anchor.getMinCount()), anchor.getMaxCount())
            : 1;
        updateItemStack();
        name("settings.item_choice");
        size(18).padding(1)
            .background(GuiTextures.SLOT_ITEM)
            .overlay(
                item.asIcon()
                    .size(16),
                IKey.dynamic(
                    () -> item.getItem() != null && item.getItem().stackSize != 1
                        ? Integer.toString(item.getItem().stackSize)
                        : "")
                    .alignment(Alignment.BottomRight)
                    .color(0xFFFFFFFF)
                    .shadow(true)
                    .asIcon()
                    .size(16))
            .onUpdateListener(_ -> {
                updateItemStack();
                final boolean currentShift = Interactable.hasShiftDown();
                if (currentShift != shiftHeld) {
                    shiftHeld = currentShift;
                    markTooltipDirty();
                }
            })
            .tooltipBuilder(tooltip -> {
                final ItemStack selected = item.getItem();
                if (selected != null) tooltip.addFromItem(selected);
                TooltipBuilder.create(tooltip)
                    .langRow(hint)
                    .flush();
            })
            .tooltipAutoUpdate(true);
    }

    public ItemChoiceWidget hint(final String hint) {
        this.hint = hint;
        markTooltipDirty();
        return this;
    }

    private void updateItemStack() {
        final ItemStack selected = selection.get();
        if (ItemStack.areItemStacksEqual(displayedSelection, selected)) return;
        displayedSelection = selected == null ? null : selected.copy();
        item.setItem(selected == null ? null : selected.copy());
        markTooltipDirty();
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

    public boolean hasScrollHandler() {
        return scrollHandler != null;
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
        updateItemStack();
        return item.getItem();
    }
}
