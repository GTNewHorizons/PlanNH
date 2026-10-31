package com.sbancuz.plannh.data.provider.gregtech;

import static com.sbancuz.plannh.data.provider.GTProvider.EU_PER_TICK;
import static com.sbancuz.plannh.data.provider.GTProvider.RECIPE_MAP;
import static com.sbancuz.plannh.data.provider.GTProvider.TOTAL_EU;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;

import javax.annotation.Nullable;

import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.EffectComputer;
import com.sbancuz.plannh.data.effect.EffectResult;
import com.sbancuz.plannh.data.effect.EffectStep;
import com.sbancuz.plannh.data.setting.IntegerSettingDef;
import com.sbancuz.plannh.data.setting.Settings;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.OverclockCalculator;

public class GTOverclockStep implements EffectStep, EffectComputer {

    private record Condition(BiPredicate<MachineConfig, RecipeContext> predicate,
        Consumer<GTOverclockStep> configurator) {}

    private GTOverclockStep() {}

    public static GTOverclockStep create() {
        return new GTOverclockStep();
    }

    private boolean forceHeat;
    private boolean forcePerfectOC;
    private final List<Condition> conditions = new ArrayList<>();
    private final Map<String, Consumer<GTOverclockStep>> routeModifiers = new HashMap<>();
    private IntegerSettingDef catalystSetting;
    private IntUnaryOperator catalystComputer;

    public GTOverclockStep withCatalyst(final IntegerSettingDef setting, final IntUnaryOperator computer) {
        this.catalystSetting = setting;
        this.catalystComputer = computer;
        return this;
    }

    public GTOverclockStep route(final String recipeMapId, final Consumer<GTOverclockStep> modifier) {
        routeModifiers.put(recipeMapId, modifier);
        return this;
    }

    public GTOverclockStep withHeat() {
        this.forceHeat = true;
        return this;
    }

    public GTOverclockStep withPerfectOC() {
        this.forcePerfectOC = true;
        return this;
    }

    public GTOverclockStep applyIf(final BiPredicate<MachineConfig, RecipeContext> predicate,
        final Consumer<GTOverclockStep> configurator) {
        conditions.add(new Condition(predicate, configurator));
        return this;
    }

    @Override
    public EffectResult compute(final MachineConfig cfg, final RecipeContext ctx) {
        final Object dur = ctx.properties().get(RecipePropertyAPI.DURATION_TICKS);
        final int d = dur instanceof final Number n ? n.intValue() : 0;
        return apply(new EffectResult(d, 0, 1), cfg, ctx);
    }

    @Override
    public EffectResult apply(final EffectResult current, final MachineConfig cfg, final RecipeContext ctx) {
        // Conditions configure a private evaluation copy. The registered effect is shared by all
        // recipe evaluations and must not retain route-specific state.
        final GTOverclockStep evaluation = copy();
        return evaluation.applyResolved(current, cfg, ctx);
    }

    private EffectResult applyResolved(final EffectResult current, final MachineConfig cfg, final RecipeContext ctx) {
        for (final Condition cond : conditions) {
            if (cond.predicate()
                .test(cfg, ctx)) {
                cond.configurator()
                    .accept(this);
            }
        }

        if (!routeModifiers.isEmpty()) {
            final RecipeMap<?> map = ctx.getOrDefault(RECIPE_MAP, null);
            if (map != null) {
                final String uid = map.unlocalizedName;
                final Consumer<GTOverclockStep> mod = routeModifiers.get(uid);
                if (mod != null) mod.accept(this);
            }
        }

        final int parallels;
        if (catalystSetting != null) {
            final int cat = cfg.getProfile()
                .value(catalystSetting, cfg, ctx);
            parallels = cat > 0 ? catalystComputer.applyAsInt(cat)
                : cfg.getProfile()
                    .value(Settings.PARALLELS, cfg, ctx);
        } else {
            parallels = cfg.getProfile()
                .value(Settings.PARALLELS, cfg, ctx);
        }
        final int machines = cfg.getProfile()
            .value(Settings.MACHINES, cfg, ctx);

        final long eut = recipeEUt(ctx, current);
        final int recipeDuration = current.durationTicks();

        final Settings.Voltage voltage = cfg.getProfile()
            .value(Settings.VOLTAGE, cfg, ctx);
        if (eut <= 0 || recipeDuration <= 0 || voltage == null || voltage == Settings.Voltage.OFF) {
            current.durationTicks(recipeDuration);
            current.energyPerT(eut);
            current.throughputFactor(Math.multiplyExact(parallels, machines));
            current.energyRateProperty(EU_PER_TICK);
            current.properties()
                .put(EU_PER_TICK, eut);
            current.properties()
                .put(TOTAL_EU, Math.multiplyExact(eut, (long) recipeDuration));
            return current;
        }

        final OverclockCalculator calc = buildGtCalc(cfg, ctx, eut, recipeDuration, parallels);

        if (forcePerfectOC) calc.enablePerfectOC();

        if (forceHeat) {
            final int machineHeat = cfg.getProfile()
                .value(Settings.MACHINE_HEAT, cfg, ctx);
            if (cfg.getProfile()
                .value(Settings.HEAT_OC, cfg, ctx) && machineHeat > 0) {
                final int recipeHeat = cfg.getProfile()
                    .value(Settings.RECIPE_HEAT, cfg, ctx);
                calc.setHeatOC(true)
                    .setRecipeHeat(recipeHeat > 0 ? recipeHeat : machineHeat)
                    .setMachineHeat(machineHeat);
                if (cfg.getProfile()
                    .value(Settings.HEAT_DISCOUNT, cfg, ctx)) calc.setHeatDiscount(true);
                final int hdMult = cfg.getProfile()
                    .value(Settings.HEAT_DISCOUNT_MULT, cfg, ctx);
                if (hdMult != 100) calc.setHeatDiscountMultiplier(hdMult / 100.0);
            }
        }

        calc.calculate();
        current.durationTicks(calc.getDuration());
        current.energyPerT(calc.getConsumption());
        current.throughputFactor(Math.multiplyExact(parallels, machines));
        current.energyRateProperty(EU_PER_TICK);
        current.properties()
            .put(EU_PER_TICK, calc.getConsumption());
        current.properties()
            .put(TOTAL_EU, Math.multiplyExact(calc.getConsumption(), (long) calc.getDuration()));
        return current;
    }

    public static long tierNameToVoltage(@Nullable final String name) {
        if (name == null || name.equals("OFF")) return 0;
        final String[] names = { "ULV", "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV", "UHV", "UEV", "UIV", "UMV",
            "UXV", "MAX" };
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(name)) return 8L * (long) Math.pow(4, i);
        }
        return 0;
    }

    private static OverclockCalculator buildGtCalc(final MachineConfig cfg, final RecipeContext ctx, final long eut,
        final int duration, final int parallels) {
        final long voltage = tierNameToVoltage(
            cfg.getProfile()
                .value(Settings.VOLTAGE, cfg, ctx)
                .name());
        final int amp = cfg.getProfile()
            .value(Settings.AMP, cfg, ctx);
        final int speed = cfg.getProfile()
            .value(Settings.SPEED, cfg, ctx);

        final OverclockCalculator calc = new OverclockCalculator().setRecipeEUt(eut)
            .setEUt(voltage)
            .setDuration(duration)
            .setAmperage(amp)
            .setDurationModifier(100.0 / speed)
            .setParallel(parallels)
            .setAmperageOC(true);

        if (cfg.getProfile()
            .value(Settings.PERFECT_OC, cfg, ctx)) calc.enablePerfectOC();
        if (cfg.getProfile()
            .value(Settings.LASER_OC, cfg, ctx)) calc.setLaserOC(true);
        if (cfg.getProfile()
            .value(Settings.NO_OVERCLOCK, cfg, ctx)) calc.setNoOverclock(true);

        final int eutDisc = cfg.getProfile()
            .value(Settings.EUT_DISCOUNT, cfg, ctx);
        if (eutDisc > 0) calc.setEUtDiscount(eutDisc / 100.0);

        final int ocMult = cfg.getProfile()
            .value(Settings.EUT_INCREASE_PER_OC, cfg, ctx);
        if (ocMult != 400) calc.setEUtIncreasePerOC(ocMult / 100.0);

        final int durMult = cfg.getProfile()
            .value(Settings.DURATION_DECREASE_PER_OC, cfg, ctx);
        if (durMult != 200) calc.setDurationDecreasePerOC(durMult / 100.0);

        final int maxOc = cfg.getProfile()
            .value(Settings.MAX_OVERCLOCKS, cfg, ctx);
        if (maxOc > 0) calc.setMaxOverclocks(maxOc);

        final int maxReg = cfg.getProfile()
            .value(Settings.MAX_REGULAR_OC, cfg, ctx);
        if (maxReg > 0) calc.setMaxRegularOverclocks(maxReg);

        final int skips = cfg.getProfile()
            .value(Settings.MAX_TIER_SKIPS, cfg, ctx);
        if (skips > 0) calc.setMaxTierSkips(skips);

        if (cfg.getProfile()
            .value(Settings.UNLIMITED_SKIPS, cfg, ctx)) calc.setUnlimitedTierSkips();

        return calc;
    }

    private GTOverclockStep copy() {
        final GTOverclockStep copy = new GTOverclockStep();
        copy.forceHeat = forceHeat;
        copy.forcePerfectOC = forcePerfectOC;
        copy.catalystSetting = catalystSetting;
        copy.catalystComputer = catalystComputer;
        copy.conditions.addAll(conditions);
        copy.routeModifiers.putAll(routeModifiers);
        return copy;
    }

    static long recipeEUt(RecipeContext ctx, EffectResult current) {
        final Long euPerTick = ctx.getOrDefault(EU_PER_TICK, null);
        if (euPerTick != null && euPerTick > 0) return euPerTick;
        final Long totalEu = ctx.getOrDefault(TOTAL_EU, null);
        if (totalEu != null && totalEu > 0 && current.durationTicks() > 0) return totalEu / current.durationTicks();
        return Math.round(current.energyPerT());
    }
}
