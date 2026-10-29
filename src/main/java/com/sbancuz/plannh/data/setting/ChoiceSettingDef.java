package com.sbancuz.plannh.data.setting;

import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.MCHelper;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.text.TextRenderer;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.gui.setting.ItemChoiceWidget;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;
import com.sbancuz.plannh.gui.tooltips.TooltipTheme;

/** A registered value selected from a runtime registry, rather than a Java enum. */
public class ChoiceSettingDef<T> extends SettingDef<T> {

    private final Class<T> type;
    private final Supplier<List<T>> choices;
    private final Function<T, String> display;
    private final BiPredicate<T, T> equality;

    public ChoiceSettingDef(final String key, final T defaultValue, final Class<T> type,
        final Supplier<List<T>> choices, final Function<T, String> display) {
        this(key, defaultValue, type, choices, display, Objects::equals);
    }

    public ChoiceSettingDef(final String key, final T defaultValue, final Class<T> type,
        final Supplier<List<T>> choices, final Function<T, String> display, final BiPredicate<T, T> equality) {
        super(key, defaultValue);
        this.type = type;
        this.choices = choices;
        this.display = display;
        this.equality = equality;
    }

    @Override
    public Consumer<TooltipBuilder> tooltip(final T value) {
        if (value instanceof ItemStack item) return out -> {
            final List<String> lines = MCHelper.getItemToolTip(item);
            if (lines.isEmpty()) out.entry(getLabel(), TooltipTheme.Role.TUNABLE, display.apply(value));
            else for (final String line : lines) out.row(IKey.str(line));
        };
        return out -> out.entry(getLabel(), TooltipTheme.Role.TUNABLE, display.apply(value));
    }

    @Override
    public IWidget settingsWidget(final MachineConfig cfg, final RecipeContext ctx, final Consumer<Runnable> edit) {
        final List<T> values = choices.get();
        final boolean itemChoice = type == ItemStack.class;
        final int width = itemChoice ? 18
            : Math.max(
                40,
                values.stream()
                    .map(display)
                    .mapToInt(
                        label -> TextRenderer.getFontRenderer()
                            .getStringWidth(label) + 10)
                    .max()
                    .orElse(40));
        final ButtonWidget<?> button = (itemChoice ? new ItemChoiceWidget(
            () -> (ItemStack) cfg.getProfile()
                .value(this, cfg, ctx),
            this::acceptsItem,
            stack -> cfg.set(this, type.cast(stack)),
            edit).picker(
                () -> choices.get()
                    .stream()
                    .map(value -> (ItemStack) value)
                    .toList(),
                () -> cfg.reset(this))
            : new ButtonWidget<>()).name("settings.choice")
                .width(width)
                .tooltipBuilder(tooltip -> {
                    final T value = cfg.getProfile()
                        .value(this, cfg, ctx);
                    final TooltipBuilder out = TooltipBuilder.create(tooltip);
                    if (!itemChoice || value == null) tooltip(value).accept(out);
                    out.langRow(cfg.isExplicit(this) ? "plannh.settings.choice_manual" : "plannh.settings.choice_auto");
                    out.langRow(itemChoice ? "plannh.settings.item_choice_hint" : "plannh.settings.choice_hint")
                        .flush();
                })
                .tooltipAutoUpdate(true);
        if (!itemChoice) {
            button.overlay(
                IKey.dynamic(
                    () -> display.apply(
                        cfg.getProfile()
                            .value(this, cfg, ctx))))
                .onMousePressed(mouseButton -> {
                    if (mouseButton == 2) {
                        edit.accept(() -> cfg.reset(this));
                        return true;
                    }
                    if (mouseButton != 0 && mouseButton != 1 || values.isEmpty()) return false;
                    int index = -1;
                    final T current = cfg.getProfile()
                        .value(this, cfg, ctx);
                    for (int i = 0; i < values.size(); i++) {
                        if (equality.test(values.get(i), current)) {
                            index = i;
                            break;
                        }
                    }
                    final int nextIndex = index < 0 ? (mouseButton == 0 ? 0 : values.size() - 1)
                        : Math.floorMod(index + (mouseButton == 0 ? 1 : -1), values.size());
                    final T next = values.get(nextIndex);
                    edit.accept(() -> cfg.set(this, next));
                    return true;
                });
        }
        return button;
    }

    protected boolean acceptsItem(final ItemStack stack) {
        if (type != ItemStack.class || stack == null) return false;
        return choices.get()
            .stream()
            .anyMatch(
                value -> value instanceof ItemStack item && item.isItemEqual(stack)
                    && ItemStack.areItemStackTagsEqual(item, stack));
    }

    @Override
    protected Class<?> valueType() {
        return type;
    }
}
