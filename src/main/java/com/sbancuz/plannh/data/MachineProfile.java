package com.sbancuz.plannh.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToIntBiFunction;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.sbancuz.plannh.data.effect.EffectComputer;
import com.sbancuz.plannh.data.effect.EffectFunction;
import com.sbancuz.plannh.data.effect.EffectResult;
import com.sbancuz.plannh.data.effect.Effects;
import com.sbancuz.plannh.data.flowchart.Port;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.properties.RecipeProperty;
import com.sbancuz.plannh.data.setting.ChoiceSettingDef;
import com.sbancuz.plannh.data.setting.IntegerSettingDef;
import com.sbancuz.plannh.data.setting.SettingDef;
import com.sbancuz.plannh.data.setting.Settings;
import com.sbancuz.plannh.gui.GuiIcon;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;

import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.RecipeHandlerRef;
import lombok.Getter;

public final class MachineProfile {

    @FunctionalInterface
    public interface Default<T> {

        T apply(MachineConfig cfg, RecipeContext ctx);
    }

    @FunctionalInterface
    public interface PortMultiplier {

        double apply(EffectResult effect, MachineConfig cfg, RecipeContext ctx, int index);
    }

    @FunctionalInterface
    public interface BadgeRule {

        @Nullable
        NodeBadge apply(MachineConfig cfg, RecipeContext ctx);
    }

    /** PlanNH computes an {@code override} setting from the recipe or another setting. */
    public record Entry(SettingDef<?> def, BiPredicate<MachineConfig, RecipeContext> visible, boolean override) {

        public boolean isVisible(final MachineConfig cfg, final RecipeContext ctx) {
            return visible.test(cfg, ctx);
        }
    }

    public static Entry entry(final SettingDef<?> def) {
        return new Entry(def, (_, _) -> true, false);
    }

    private final String id;
    private final PropertyProvider provider;
    private final BiFunction<IRecipeHandler, Integer, IDrawable> icon;
    private final Consumer<TooltipBuilder> iconTooltip;

    private final List<Entry> settings;
    private final EffectComputer effectComputer;
    private final Map<SettingDef<?>, Default<?>> defaults;
    private final Map<SettingDef<?>, int[]> settingBounds;
    private final PortMultiplier inputMultiplier;
    private final PortMultiplier outputMultiplier;
    @Getter
    private final BadgeRule badge;

    private MachineProfile(final String id, final Definition<?> definition) {
        this.id = id;
        this.provider = definition.provider;
        this.icon = definition.icon;
        this.iconTooltip = definition.iconTooltip;
        this.settings = List.copyOf(definition.settings);
        this.effectComputer = definition.effectComputer;
        this.defaults = Map.copyOf(definition.defaults);
        this.settingBounds = Map.copyOf(definition.settingBounds);
        this.inputMultiplier = definition.inputMultiplier;
        this.outputMultiplier = definition.outputMultiplier;
        this.badge = definition.badge;
    }

    public String id() {
        return id;
    }

    public List<Entry> settings() {
        return settings;
    }

    @SuppressWarnings("unchecked")
    public <T> Default<T> defaultRule(final SettingDef<T> def) {
        return (Default<T>) defaults.get(def);
    }

    public <T> T value(final SettingDef<T> def, final MachineConfig cfg, final RecipeContext ctx) {
        final T value = cfg.getOrDefault(def, () -> {
            final Default<T> rule = defaultRule(def);
            return rule == null ? def.getDefaultValue() : rule.apply(cfg, ctx);
        });
        if (def instanceof IntegerSettingDef integerDef && settingBounds.containsKey(integerDef)) {
            @SuppressWarnings("unchecked")
            final T bounded = (T) Integer
                .valueOf(Math.clamp((Integer) value, minimum(integerDef), maximum(integerDef)));
            return bounded;
        }
        return value;
    }

    public int minimum(final IntegerSettingDef def) {
        final int[] bounds = settingBounds.get(def);
        return bounds == null ? def.getMin() : bounds[0];
    }

    public int maximum(final IntegerSettingDef def) {
        final int[] bounds = settingBounds.get(def);
        return bounds == null ? def.getMax() : bounds[1];
    }

    public int minimum(final ChoiceSettingDef<?> def) {
        final int[] bounds = settingBounds.get(def);
        return bounds == null ? def.getMinCount() : bounds[0];
    }

    public int maximum(final ChoiceSettingDef<?> def) {
        final int[] bounds = settingBounds.get(def);
        return bounds == null ? def.getMaxCount() : bounds[1];
    }

    /**
     * Evaluate one recipe on this machine: duration, energy, and the multiplier of every port. A port's multiplier is
     * the port's chance per craft: the user's chance when set, else the recipe chance times the profile's multiplier.
     *
     * @throws IllegalArgumentException for a duration over {@link Integer#MAX_VALUE} ticks, or a multiplier that is
     *                                  negative or not finite
     */
    public EffectResult computeEffect(final MachineConfig cfg, final RecipeContext ctx) {
        final EffectResult result = effectComputer.compute(cfg, ctx);

        // Tick adjustments change duration and energy rate, not per-operation quantities.
        final int tickModifier = value(Settings.TICK_MODIFIER, cfg, ctx);
        if (tickModifier > 0 && tickModifier != 100) {
            final double factor = 100.0 / tickModifier;
            final double ticks = result.durationTicks() * factor;
            if (ticks < 0 || ticks > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Unrepresentable tick-modified duration: " + ticks);
            }
            result.durationTicks(Math.max(1, (int) Math.round(ticks)));
            result.energyPerT(result.energyPerT() / factor);
        }

        final var rateProperty = result.energyRateProperty();
        if (rateProperty != null) result.properties()
            .put(rateProperty, result.energyPerT());

        for (final boolean input : new boolean[] { true, false }) {
            final List<Port<?>> ports = input ? ctx.inputs() : ctx.outputs();
            final PortMultiplier portRule = input ? inputMultiplier : outputMultiplier;
            for (int i = 0; i < ports.size(); i++) {
                final int index = i;
                final DoubleSupplier rule = () -> portRule.apply(result, cfg, ctx, index);
                final double chance = ports.get(index)
                    .getChance() * result.multiplier(input, index, rule);
                result.setMultiplier(input, index, resolveMultiplier(cfg.chanceOverride(input, index), chance));
            }
        }

        return result;
    }

    private static double resolveMultiplier(@Nullable final Double override, final double profileMultiplier) {
        final double multiplier = override != null ? override : profileMultiplier;
        if (!Double.isFinite(multiplier) || multiplier < 0)
            throw new IllegalArgumentException("Invalid port multiplier: " + multiplier);
        return multiplier;
    }

    public RecipeContext getRecipeContext(final RecipeHandlerRef ref) {
        return provider.extract(ref);
    }

    public IDrawable getIcon(final RecipeHandlerRef ref) {
        final IDrawable drawable = icon == null ? null : icon.apply(ref.handler, ref.recipeIndex);
        if (drawable == null || drawable instanceof ItemDrawable item && !hasItem(item)
            || !IDrawable.isVisible(drawable)) return GuiIcon.MACHINE;
        return drawable;
    }

    /** The tooltip of an icon that is not an item. {@code ItemWidget} writes the tooltip of an item. */
    public void addIconTooltip(final TooltipBuilder out) {
        if (iconTooltip != null) iconTooltip.accept(out);
        if (!out.isEmpty()) return;

        final String name = id == null || id.isBlank() ? null
            : id.replace(':', ' ')
                .replace('_', ' ')
                .replace('.', ' ');
        out.title(name == null ? IKey.lang("plannh.gui.node.machine") : IKey.str(name));
    }

    private static boolean hasItem(final ItemDrawable drawable) {
        return drawable.getItem() != null && drawable.getItem()
            .getItem() != null;
    }

    @Nonnull
    public static Builder builder(final String id) {
        return new Builder(id);
    }

    @Nonnull
    public static Template template() {
        return new Template();
    }

    /** Fluent configuration shared by ID-free templates and identified profile builders. */
    public abstract static class Definition<B extends Definition<B>> {

        private final List<Entry> settings = new ArrayList<>();
        private final Map<SettingDef<?>, Default<?>> defaults = new HashMap<>();
        private final Map<SettingDef<?>, int[]> settingBounds = new HashMap<>();
        private PropertyProvider provider;
        private BiFunction<IRecipeHandler, Integer, IDrawable> icon = (_, _) -> IDrawable.EMPTY;
        private Consumer<TooltipBuilder> iconTooltip;
        private EffectComputer effectComputer = Effects.durationFromHandler();
        private PortMultiplier inputMultiplier = (_, _, _, _) -> 1d;
        private PortMultiplier outputMultiplier = (_, _, _, _) -> 1d;
        private BadgeRule badge = (_, _) -> null;

        private Definition() {}

        private Definition(final Definition<?> source) {
            settings.addAll(source.settings);
            defaults.putAll(source.defaults);
            settingBounds.putAll(source.settingBounds);
            provider = source.provider;
            icon = source.icon;
            iconTooltip = source.iconTooltip;
            effectComputer = source.effectComputer;
            inputMultiplier = source.inputMultiplier;
            outputMultiplier = source.outputMultiplier;
            badge = source.badge;
        }

        protected abstract B self();

        public B provider(final PropertyProvider provider) {
            this.provider = provider;
            return self();
        }

        public B icon(final BiFunction<IRecipeHandler, Integer, IDrawable> icon) {
            return icon(icon, null);
        }

        public B icon(final BiFunction<IRecipeHandler, Integer, IDrawable> icon,
            final Consumer<TooltipBuilder> tooltip) {
            this.icon = icon;
            this.iconTooltip = tooltip;
            return self();
        }

        public B setting(final SettingDef<?> def) {
            return setting(def, (_, _) -> true);
        }

        public B setting(final SettingDef<?> def, final BiPredicate<MachineConfig, RecipeContext> visible) {
            settings.add(new Entry(def, visible, false));
            return self();
        }

        public B override(final SettingDef<?> def) {
            return override(def, (_, _) -> true);
        }

        /** Add a setting that PlanNH computes, and that the player sets only when the recipe is wrong. */
        public B override(final SettingDef<?> def, final BiPredicate<MachineConfig, RecipeContext> visible) {
            settings.add(new Entry(def, visible, true));
            return self();
        }

        /** Adds an integer setting with bounds specialized for this profile. */
        public B setting(final IntegerSettingDef def, final int min, final int max) {
            if (min > max || min < def.getMin() || max > def.getMax())
                throw new IllegalArgumentException("Invalid integer setting bounds for " + def.getKey());
            settingBounds.put(def, new int[] { min, max });
            return setting(def);
        }

        /** Adds a counted choice with bounds specialized for this profile. */
        public B setting(final ChoiceSettingDef<?> def, final int min, final int max) {
            if (!def.isCounted() || min > max || min < def.getMinCount() || max > def.getMaxCount())
                throw new IllegalArgumentException("Invalid choice count bounds for " + def.getKey());
            settingBounds.put(def, new int[] { min, max });
            return setting(def);
        }

        public B settings(final Consumer<B> consumer) {
            return configure(consumer);
        }

        public B settings(final Iterable<? extends SettingDef<?>> defs) {
            for (final SettingDef<?> def : defs) setting(def);
            return self();
        }

        public <S> B settings(final Iterable<S> sources, final Function<? super S, ? extends SettingDef<?>> setting) {
            for (final S source : sources) setting(setting.apply(source));
            return self();
        }

        public B configure(final Consumer<B> consumer) {
            consumer.accept(self());
            return self();
        }

        public B configureIf(final boolean condition, final Consumer<B> consumer) {
            return condition ? configure(consumer) : self();
        }

        public <S, T> B derivedSettings(final Iterable<S> sources,
            final Function<? super S, ? extends SettingDef<T>> setting,
            final Function<? super S, ? extends Default<T>> rule) {
            for (final S source : sources) {
                final SettingDef<T> def = setting.apply(source);
                setting(def);
                derive(def, rule.apply(source));
            }
            return self();
        }

        public <T> B derive(final SettingDef<T> def, final Default<T> rule) {
            defaults.put(def, rule);
            return self();
        }

        /** The last matching conditional default wins; unmatched rules retain the previous default. */
        @SuppressWarnings("unchecked")
        public <T> B deriveIf(final SettingDef<T> def, final BiPredicate<MachineConfig, RecipeContext> condition,
            final Default<T> rule) {
            final Default<T> previous = (Default<T>) defaults.get(def);
            return derive(def, (cfg, ctx) -> {
                if (condition.test(cfg, ctx)) return rule.apply(cfg, ctx);
                return previous == null ? def.getDefaultValue() : previous.apply(cfg, ctx);
            });
        }

        public <T> B derive(final Map<? extends SettingDef<T>, ? extends Default<T>> rules) {
            rules.forEach(this::derive);
            return self();
        }

        public <S, T> B derive(final Iterable<S> sources, final Function<? super S, ? extends SettingDef<T>> setting,
            final Function<? super S, ? extends Default<T>> rule) {
            for (final S source : sources) derive(setting.apply(source), rule.apply(source));
            return self();
        }

        public B inputMultiplier(final PortMultiplier rule) {
            inputMultiplier = rule;
            return self();
        }

        public B outputMultiplier(final PortMultiplier rule) {
            outputMultiplier = rule;
            return self();
        }

        public B badge(final BadgeRule rule) {
            badge = rule;
            return self();
        }

        public B effect(final EffectComputer effect) {
            effectComputer = effect;
            return self();
        }

        /** An authoritative duration calculation, unlike handler-duration fallbacks. */
        public B duration(final ToIntBiFunction<MachineConfig, RecipeContext> rule) {
            return effect(Effects.duration(rule));
        }

        public B applyParallelism() {
            effectComputer = effectComputer.applyParallelism();
            return self();
        }

        /** Amortizes per-operation energy over duration, including already-applied parallelism. */
        public B totalCost(final RecipeProperty<? extends Number> costProperty,
            final RecipeProperty<? extends Number> rateProperty,
            final ToDoubleBiFunction<MachineConfig, RecipeContext> rule) {
            return totalCost(costProperty, rateProperty, (result, cfg, ctx) -> rule.applyAsDouble(cfg, ctx));
        }

        /** A cost rule may reuse values already calculated by the effect computer. */
        public B totalCost(final RecipeProperty<? extends Number> costProperty,
            final RecipeProperty<? extends Number> rateProperty, final EffectFunction<? extends Number> rule) {
            effectComputer = effectComputer.withTotalCost(costProperty, rule)
                .withCostPerT(rateProperty, (result, cfg, ctx) -> result.energyPerT() * result.throughputFactor());
            return self();
        }

        @Nonnull
        public <M> ProfileFactory<M> asFactory(final BiConsumer<Builder, M> specialize) {
            return new ProfileFactory<>(this, specialize);
        }
    }

    public static final class Template extends Definition<Template> {

        private Template() {}

        Template(final Definition<?> source) {
            super(source);
        }

        @Override
        protected Template self() {
            return this;
        }
    }

    public static final class Builder extends Definition<Builder> {

        private final String id;

        private Builder(final String id) {
            this.id = id;
        }

        Builder(final String id, final Definition<?> source) {
            super(source);
            this.id = id;
        }

        @Override
        protected Builder self() {
            return this;
        }

        @Nonnull
        public MachineProfile build() {
            return new MachineProfile(id, this);
        }
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof MachineProfile other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Nonnull
    public Stream<? extends SettingDef<?>> visibleSettings(final MachineConfig cfg, final RecipeContext ctx) {
        return visibleEntries(cfg, ctx).map(Entry::def);
    }

    @Nonnull
    public Stream<Entry> visibleEntries(final MachineConfig cfg, final RecipeContext ctx) {
        return settings.stream()
            .filter(e -> e.isVisible(cfg, ctx));
    }
}
