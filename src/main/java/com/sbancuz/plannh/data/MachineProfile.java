package com.sbancuz.plannh.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.effect.EffectComputer;
import com.sbancuz.plannh.data.effect.EffectFunction;
import com.sbancuz.plannh.data.effect.EffectResult;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.properties.RecipeProperty;
import com.sbancuz.plannh.data.setting.SettingDef;
import com.sbancuz.plannh.data.setting.Settings;

import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.RecipeHandlerRef;

public final class MachineProfile {

    @FunctionalInterface
    public interface Default<T> {

        T apply(MachineConfig cfg, RecipeContext ctx);
    }

    @FunctionalInterface
    public interface PortMultiplier {

        double apply(MachineConfig cfg, RecipeContext ctx, int index);
    }

    public record Entry(SettingDef<?> def, BiPredicate<MachineConfig, RecipeContext> visible) {

        public boolean isVisible(final MachineConfig cfg, final RecipeContext ctx) {
            return visible.test(cfg, ctx);
        }
    }

    public static Entry entry(final SettingDef<?> def) {
        return new Entry(def, (_, _) -> true);
    }

    private final String id;
    private final PropertyProvider provider;
    private final BiFunction<IRecipeHandler, Integer, IDrawable> icon;
    private final Consumer<RichTooltip> iconTooltip;

    private final List<Entry> settings;
    private final EffectComputer effectComputer;
    private final Map<SettingDef<?>, Default<?>> defaults;
    private final PortMultiplier inputMultiplier;
    private final PortMultiplier outputMultiplier;

    private MachineProfile(final String id, final Definition<?> definition) {
        this.id = Objects.requireNonNull(id, "id");
        this.provider = definition.provider;
        this.icon = definition.icon;
        this.iconTooltip = definition.iconTooltip;
        this.settings = List.copyOf(definition.settings);
        this.effectComputer = definition.effectComputer;
        this.defaults = Map.copyOf(definition.defaults);
        this.inputMultiplier = definition.inputMultiplier;
        this.outputMultiplier = definition.outputMultiplier;
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
        return cfg.getOrDefault(def, () -> {
            final Default<T> rule = defaultRule(def);
            return rule == null ? def.getDefaultValue() : rule.apply(cfg, ctx);
        });
    }

    /** Evaluates a single-port query; consumers reading multiple values should reuse {@link #computeEffect}. */
    public double inputMultiplier(final MachineConfig cfg, final RecipeContext ctx, final int index) {
        return cfg.inputMultiplier(
            index,
            () -> computeEffect(cfg, ctx).inputMultiplier(index, () -> inputMultiplier.apply(cfg, ctx, index)));
    }

    /** Evaluates a single-port query; consumers reading multiple values should reuse {@link #computeEffect}. */
    public double outputMultiplier(final MachineConfig cfg, final RecipeContext ctx, final int index) {
        return cfg.outputMultiplier(
            index,
            () -> computeEffect(cfg, ctx).outputMultiplier(index, () -> outputMultiplier.apply(cfg, ctx, index)));
    }

    public EffectResult computeEffect(final MachineConfig cfg, final RecipeContext ctx) {
        final EffectResult result = effectComputer.compute(cfg, ctx);

        // Tick adjustments change duration and energy rate, not per-operation quantities.
        final int tickModifier = value(Settings.TICK_MODIFIER, cfg, ctx);
        if (tickModifier > 0 && tickModifier != 100) {
            final double factor = 100.0 / tickModifier;
            final double ticks = result.durationTicks() * factor;
            if (!Double.isFinite(ticks) || ticks < 0 || ticks > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Unrepresentable tick-modified duration: " + ticks);
            }
            result.durationTicks(Math.max(1, (int) Math.round(ticks)));
            result.energyPerT(result.energyPerT() / factor);
        }

        final var rateProperty = result.energyRateProperty();
        if (rateProperty != null) result.properties()
            .put(rateProperty, result.energyPerT());

        // Freeze effective port rules into this evaluation; explicit user overrides always win.
        final int inputCount = ctx.inputs()
            .size();
        for (int i = 0; i < inputCount; i++) {
            final int index = i;
            final DoubleSupplier rule = () -> inputMultiplier.apply(cfg, ctx, index);
            final double multiplier = cfg.inputMultiplier(index, () -> result.inputMultiplier(index, rule));
            result.inputMultiplier(index, multiplier);
        }

        final int outputCount = ctx.outputs()
            .size();
        for (int i = 0; i < outputCount; i++) {
            final int index = i;
            final DoubleSupplier rule = () -> outputMultiplier.apply(cfg, ctx, index);
            final double multiplier = cfg.outputMultiplier(index, () -> result.outputMultiplier(index, rule));
            result.outputMultiplier(index, multiplier);
        }

        return result;
    }

    public RecipeContext getRecipeContext(final RecipeHandlerRef ref) {
        return provider.extract(ref);
    }

    public IDrawable getIcon(final RecipeHandlerRef ref) {
        return icon.apply(ref.handler, ref.recipeIndex);
    }

    public void addIconTooltip(final IDrawable drawable, final RichTooltip tooltip) {
        if (iconTooltip != null) iconTooltip.accept(tooltip);
        else if (drawable instanceof ItemDrawable item && item.getItem() != null
            && item.getItem()
                .getItem() != null) {
                    tooltip.addFromItem(item.getItem());
                }
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
        private PropertyProvider provider;
        private BiFunction<IRecipeHandler, Integer, IDrawable> icon = (_, _) -> IDrawable.EMPTY;
        private Consumer<RichTooltip> iconTooltip;
        private EffectComputer effectComputer = (cfg,
            ctx) -> new EffectResult(ctx.get(RecipePropertyAPI.DURATION_TICKS), 0, 1);
        private PortMultiplier inputMultiplier = (_, _, _) -> 1d;
        private PortMultiplier outputMultiplier = (_, _, _) -> 1d;

        private Definition() {}

        private Definition(final Definition<?> source) {
            settings.addAll(source.settings);
            defaults.putAll(source.defaults);
            provider = source.provider;
            icon = source.icon;
            iconTooltip = source.iconTooltip;
            effectComputer = source.effectComputer;
            inputMultiplier = source.inputMultiplier;
            outputMultiplier = source.outputMultiplier;
        }

        protected abstract B self();

        public B provider(final PropertyProvider provider) {
            this.provider = Objects.requireNonNull(provider);
            return self();
        }

        public B icon(final BiFunction<IRecipeHandler, Integer, IDrawable> icon) {
            return icon(icon, null);
        }

        public B icon(final BiFunction<IRecipeHandler, Integer, IDrawable> icon, final Consumer<RichTooltip> tooltip) {
            this.icon = Objects.requireNonNull(icon);
            this.iconTooltip = tooltip;
            return self();
        }

        public B setting(final SettingDef<?> def) {
            return setting(def, (_, _) -> true);
        }

        public B setting(final SettingDef<?> def, final BiPredicate<MachineConfig, RecipeContext> visible) {
            settings.add(new Entry(def, visible));
            return self();
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
            defaults.put(def, Objects.requireNonNull(rule));
            return self();
        }

        /** The last matching conditional default wins; unmatched rules retain the previous default. */
        @SuppressWarnings("unchecked")
        public <T> B deriveIf(final SettingDef<T> def, final BiPredicate<MachineConfig, RecipeContext> condition,
            final Default<T> rule) {
            Objects.requireNonNull(condition);
            Objects.requireNonNull(rule);
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
            inputMultiplier = Objects.requireNonNull(rule);
            return self();
        }

        public B outputMultiplier(final PortMultiplier rule) {
            outputMultiplier = Objects.requireNonNull(rule);
            return self();
        }

        public B effect(final EffectComputer effect) {
            effectComputer = effect;
            return self();
        }

        /** An authoritative duration calculation, unlike handler-duration fallbacks. */
        public B duration(final ToIntBiFunction<MachineConfig, RecipeContext> rule) {
            effectComputer = (cfg, ctx) -> new EffectResult(rule.applyAsInt(cfg, ctx), 0, 1);
            return self();
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
            this.id = Objects.requireNonNull(id, "id");
        }

        Builder(final String id, final Definition<?> source) {
            super(source);
            this.id = Objects.requireNonNull(id, "id");
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
        return settings.stream()
            .filter(e -> e.isVisible(cfg, ctx))
            .map(Entry::def);
    }
}
