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

import lombok.Getter;

/** A registered value selected from a runtime registry, rather than a Java enum. */
public class ChoiceSettingDef<T> extends SettingDef<T> {

    private final Class<T> type;
    private final Supplier<List<T>> choices;
    private final Function<T, String> display;
    private final BiPredicate<T, T> equality;
    @Getter
    private int minCount;
    @Getter
    private int maxCount;
    @Getter
    private boolean counted;

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

    public ChoiceSettingDef<T> counts(final int min, final int max) {
        if (type != ItemStack.class) throw new IllegalArgumentException("Only item choices have stack counts");
        if (min < 0 || min > max) throw new IllegalArgumentException("Invalid choice count bounds");
        this.minCount = min;
        this.maxCount = max;
        this.counted = true;
        return this;
    }

    public int minimumCount(final MachineConfig cfg, final RecipeContext ctx) {
        return cfg.getProfile()
            .minimum(this);
    }

    public int maximumCount(final MachineConfig cfg, final RecipeContext ctx) {
        return cfg.getProfile()
            .maximum(this);
    }

    public int count(final MachineConfig cfg, final RecipeContext ctx) {
        final T value = cfg.value(this, ctx);
        if (!(value instanceof ItemStack stack)) return 0;
        final int maximum = maximumCount(cfg, ctx);
        return maximum < 1 ? 0 : Math.clamp(stack.stackSize, minimumCount(cfg, ctx), maximum);
    }

    public void setCount(final MachineConfig cfg, final RecipeContext ctx, final int count) {
        if (count <= 0 && minimumCount(cfg, ctx) == 0) {
            cfg.set(this, null);
            return;
        }
        final int maximum = maximumCount(cfg, ctx);
        if (maximum < 1) {
            cfg.set(this, null);
            return;
        }
        final int minimum = Math.max(1, minimumCount(cfg, ctx));
        final int bounded = Math.clamp(count, minimum, maximum);
        final T current = cfg.value(this, ctx);
        final ItemStack selected = current instanceof ItemStack item ? item.copy()
            : choices.get()
                .stream()
                .filter(ItemStack.class::isInstance)
                .map(ItemStack.class::cast)
                .filter(item -> item.getItem() != null)
                .findFirst()
                .map(ItemStack::copy)
                .orElse(null);
        if (selected == null) {
            cfg.set(this, null);
            return;
        }
        selected.stackSize = bounded;
        cfg.set(this, type.cast(selected));
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
        if (type == ItemStack.class) {
            final ItemChoiceWidget widget = new ItemChoiceWidget(
                this,
                () -> (ItemStack) cfg.value(this, ctx),
                this::acceptsItem,
                stack -> {
                    if (counted) stack.stackSize = Math
                        .clamp(stack.stackSize, Math.max(1, minimumCount(cfg, ctx)), maximumCount(cfg, ctx));
                    cfg.set(this, type.cast(stack));
                },
                edit).picker(
                    () -> choices.get()
                        .stream()
                        .map(value -> (ItemStack) value)
                        .toList(),
                    () -> cfg.reset(this))
                    .hint("plannh.settings.item_choice_hint")
                    .tooltipBuilder(
                        tooltip -> TooltipBuilder.create(tooltip)
                            .langRow(
                                cfg.isExplicit(this) ? "plannh.settings.choice_manual" : "plannh.settings.choice_auto")
                            .flush());

            if (counted) widget.scrollHandler(direction -> {
                final int current = count(cfg, ctx);
                final int next = Math.clamp(current + direction, minimumCount(cfg, ctx), maximumCount(cfg, ctx));
                if (next != current) edit.accept(() -> setCount(cfg, ctx, next));
            })
                .tooltipBuilder(
                    tooltip -> TooltipBuilder.create(tooltip)
                        .langRow("plannh.settings.item_count_scroll")
                        .flush());

            return widget;
        }
        final List<T> values = choices.get();
        final int width = Math.max(
            40,
            values.stream()
                .map(display)
                .mapToInt(
                    label -> TextRenderer.getFontRenderer()
                        .getStringWidth(label) + 10)
                .max()
                .orElse(40));
        final ButtonWidget<?> button = new ButtonWidget<>().name("settings.choice")
            .width(width)
            .tooltipBuilder(tooltip -> {
                final T value = cfg.getProfile()
                    .value(this, cfg, ctx);
                final TooltipBuilder out = TooltipBuilder.create(tooltip);
                tooltip(value).accept(out);
                out.langRow(cfg.isExplicit(this) ? "plannh.settings.choice_manual" : "plannh.settings.choice_auto");
                out.langRow("plannh.settings.choice_hint")
                    .flush();
            })
            .tooltipAutoUpdate(true);
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
