package com.sbancuz.plannh.data.setting;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.text.TextRenderer;
import com.cleanroommc.modularui.value.IntValue;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.gui.setting.ItemChoiceWidget;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;
import com.sbancuz.plannh.gui.tooltips.TooltipTheme;

import lombok.Getter;

@Getter
public class IntegerSettingDef extends SettingDef<Integer> {

    private static final int PADDING = 10;

    private final int min;
    private final int max;

    public IntegerSettingDef(final String key, final int def, final int min, final int max) {
        super(key, def);
        this.min = min;
        this.max = max;
    }

    @Override
    protected TooltipTheme.Role valueRole() {
        return TooltipTheme.Role.TUNABLE;
    }

    public static int fieldWidth(final int min, final int max) {
        return fieldWidth(Long.toString(Math.max(Math.abs((long) min), Math.abs((long) max))));
    }

    /** The same field, measured against a value that is not an int - a rate, say. */
    public static int fieldWidth(final String widest) {
        return TextRenderer.getFontRenderer()
            .getStringWidth(widest) + PADDING;
    }

    /** The item chooses the upgrade; the wheel edits its persisted, profile-bounded count. */
    protected IWidget itemCountWidget(final MachineConfig cfg, final RecipeContext ctx, final Consumer<Runnable> edit,
        final Supplier<ItemStack> unit) {
        final int minimum = cfg.getProfile()
            .minimum(this);
        final int maximum = cfg.getProfile()
            .maximum(this);
        final ItemStack item = unit.get();
        final ItemChoiceWidget slot = new ItemChoiceWidget(
            this,
            cfg,
            ctx,
            edit,
            () -> item == null ? List.of() : List.of(item),
            count -> {
                if (item == null) return null;
                final ItemStack display = item.copy();
                display.stackSize = count;
                return display;
            },
            stack -> item != null && item.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(item, stack) ? 1 : null)
                .name("settings." + getKey());

        slot.scrollHandler(direction -> {
            final int current = cfg.value(this, ctx);
            final int next = Math.clamp(current + direction, minimum, maximum);
            if (next != current) edit.accept(() -> cfg.set(this, next));
        })
            .tooltipBuilder(
                tooltip -> TooltipBuilder.create(tooltip)
                    .langRow("plannh.settings.item_count_scroll")
                    .flush());
        return slot;
    }

    @Override
    public IWidget settingsWidget(final MachineConfig cfg, final RecipeContext ctx, final Consumer<Runnable> edit) {
        final int minimum = cfg.getProfile()
            .minimum(this);
        final int maximum = cfg.getProfile()
            .maximum(this);
        return new TextFieldWidget().name("settings.int")
            .width(fieldWidth(minimum, maximum))
            .value(
                new IntValue.Dynamic(
                    () -> cfg.getProfile()
                        .value(this, cfg, ctx),
                    val -> edit.accept(() -> cfg.set(this, val))))
            .numbersInt(minimum, maximum)
            .formatAsInteger(true);
    }

    @Override
    protected Class<?> valueType() {
        return Integer.class;
    }
}
