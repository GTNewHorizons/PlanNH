package com.sbancuz.plannh.data.provider.gregtech;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.ChartMinimums;
import com.sbancuz.plannh.data.NodeState;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.EffectResult;
import com.sbancuz.plannh.data.machine.MachineRows;
import com.sbancuz.plannh.data.machine.MachineRows.Followed;
import com.sbancuz.plannh.data.provider.GTProvider;

import gregtech.api.enums.GTValues;
import gregtech.api.logic.ModifierKind;
import gregtech.api.logic.ProcessingInputs;
import gregtech.api.logic.ProcessingSpec;
import gregtech.api.logic.ResolvedRecipe;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.api.util.OverclockCalculator;

/**
 * A GregTech node's rows and the numbers behind them, read from its {@link GTNodeState}.
 *
 * <p>
 * Rows are the structure a player built (which coil, which energy hatch), not raw overclock arithmetic. A row appears
 * only when the picked machine's spec reads that value, so a node has rows for the few numbers that machine uses.
 * Advanced swaps the structure rows for one row per {@link GTOverride}.
 */
public final class GTSettings {

    private GTSettings() {}

    /** The chart floor key for voltage. Structure floors use the kind's id. */
    public static final String VOLTAGE_FLOOR = "voltage";

    /** Lang keys of the rows every GregTech machine has, under {@code plannh.settings.} */
    private static final String MACHINE_ROW = "machine";
    private static final String VOLTAGE_ROW = "voltage";
    private static final String AMP_ROW = "amp";
    private static final String MODE_ROW = "gt_mode";
    private static final String ADVANCED_ROW = "gt_advanced";

    /** Sixteen 4A hatches exceeds any real GregTech build. The row is a plan, not a limit. */
    private static final int MAX_AMPERAGE = 64;

    /** Recipe's EU/t, which bounds the voltage tiers the rows list. */
    public static long recipeEUt(final RecipeContext ctx) {
        return recipeEUt(ctx, ctx.getOrDefault(RecipePropertyAPI.DURATION_TICKS, 0));
    }

    /** As {@link #recipeEUt(RecipeContext)}, falling back on the chart's current EU/t when the recipe has none. */
    static long recipeEUt(final RecipeContext ctx, final EffectResult current) {
        final long fromRecipe = recipeEUt(ctx, current.durationTicks());
        return fromRecipe > 0 ? fromRecipe : current.energyPerT();
    }

    private static long recipeEUt(final RecipeContext ctx, final int duration) {
        final Long euPerTick = ctx.getOrDefault(GTProvider.EU_PER_TICK, null);
        if (euPerTick != null && euPerTick > 0) return euPerTick;
        final Long totalEu = ctx.getOrDefault(GTProvider.TOTAL_EU, null);
        if (totalEu != null && totalEu > 0 && duration > 0) return totalEu / duration;
        return 0;
    }

    public static int minimumVoltageTier(final RecipeContext ctx) {
        return minimumVoltageTier(recipeEUt(ctx));
    }

    /** The lowest tier whose voltage covers the recipe's EU/t. */
    public static int minimumVoltageTier(final long recipeEUt) {
        if (recipeEUt <= 0) return 0;
        return Math.min(GTUtility.getTier(recipeEUt), GTValues.VN.length - 2);
    }

    public static int voltageTier(final RecipeContext ctx, final GTNodeState state) {
        return voltageTier(recipeEUt(ctx), state.voltage);
    }

    /** Tier a node runs at: the stored tier raised to the recipe's minimum, else {@link #defaultVoltageTier}. */
    public static int voltageTier(final long recipeEUt, @Nullable final String stored) {
        final int minimum = minimumVoltageTier(recipeEUt);
        for (int tier = 0; tier < GTValues.VN.length; tier++) {
            if (GTValues.VN[tier].equals(stored)) return Math.max(tier, minimum);
        }
        return defaultVoltageTier(recipeEUt);
    }

    /**
     * Tier for a node with no stored voltage: the chart's minimum, raised to the lowest tier that can run this
     * recipe. An unset node is planned at the chart's tier, and a recipe too expensive for that still gets a working
     * hatch.
     */
    public static int defaultVoltageTier(final long recipeEUt) {
        return Math.max(minimumVoltageTier(recipeEUt), ChartMinimums.floor(VOLTAGE_FLOOR, 0));
    }

    /**
     * Values a GregTech chart can set a floor for. Registered, not listed in the panel that draws them, so the panel
     * references no mod and works on a pack without GregTech.
     */
    public static void registerChartMinimums() {
        for (final ChartFloor floor : CHART_FLOORS) {
            final ModifierKind.Range range = floor.kind()
                .getRange();
            ChartMinimums.register(
                ChartMinimums.Minimum.strongest(
                    floor.kind().id,
                    floor.label(),
                    (int) range.min(),
                    (int) range.max(),
                    floor.kind()::label));
        }
        // one below the top of GregTech's list, matching the voltage row's options
        ChartMinimums.register(
            ChartMinimums.Minimum.weakest(VOLTAGE_FLOOR, "Volt", 0, GTValues.VN.length - 2, tier -> GTValues.VN[tier]));
    }

    /**
     * Structure value with a chart-wide floor, because it follows the world's progression, not one machine. A chart
     * planned at Cupronickel that prints Eternal numbers is wrong on every node.
     *
     * @param label Short enough to fit beside the floor's two steppers
     */
    private record ChartFloor(ModifierKind.IntKind kind, String label) {}

    private static final List<ChartFloor> CHART_FLOORS = List
        .of(new ChartFloor(ModifierKind.COIL, "Coil"), new ChartFloor(ModifierKind.PIPE_CASING, "Pipe"));

    /** Selected machine's max parallels at the node's structure. */
    public static int machineMaxParallel(final RecipeContext ctx, final GTNodeState state) {
        return fromSpec(ctx, state, 1, ResolvedRecipe::maxParallel);
    }

    /**
     * GregTech's numbers for this node's recipe on its picked machine and structure. Null for a machine without a
     * spec, or a node without a GregTech recipe.
     */
    @Nullable
    public static ResolvedRecipe resolved(final RecipeContext ctx, final GTNodeState state) {
        final Planned planned = planned(ctx, state);
        return planned == null ? null : planned.resolved();
    }

    /** Whether the node's mode is one the machine runs outside recipe processing, which has no numbers to plan. */
    public static boolean inUncalculatedMode(final RecipeContext ctx, final GTNodeState state) {
        final Planned planned = planned(ctx, state);
        return planned != null && !planned.entry()
            .machine()
            .spec()
            .calculates(
                planned.inputs()
                    .mode());
    }

    /**
     * Node's plan: its machine's inputs and the numbers its recipe resolves to with them.
     *
     * @param floors     Chart floors read for this plan, in {@link #floor(int)} order
     * @param calculator Read by the rows, never modified
     */
    private record Planned(RecipeContext ctx, GTNodeState state, long[] floors, GTMachineIndex.MachineEntry entry,
        ProcessingInputs inputs, @Nullable ResolvedRecipe resolved, @Nullable OverclockCalculator calculator) {}

    /**
     * Last node's plan. Every row of a node's panel reads it, and a panel builds its rows together, so one slot is
     * enough, as in {@link GTMachineIndex#candidates}. Every input of the plan is part of the cache key.
     */
    @Nullable
    private static Planned lastPlanned;

    @Nullable
    private static Planned planned(final RecipeContext ctx, final GTNodeState state) {
        final GTMachineIndex.MachineEntry entry = GTMachineIndex.selected(ctx, state);
        if (entry == null || entry.machine() == null) return null;
        final Planned last = lastPlanned;
        if (last != null && last.ctx() == ctx
            && last.entry() == entry
            && last.state()
                .equals(state)
            && sameFloors(last.floors())) return last;

        final StructureState structure = resolve(ctx, state, voltageTier(ctx, state), mode(ctx, entry, state));
        final GTRecipe recipe = ctx.getOrDefault(GTProvider.GT_RECIPE, null);
        final ProcessingInputs inputs = recipe == null ? entry.machine()
            .inputs(structure)
            : entry.machine()
                .inputs(structure, recipe);
        final ProcessingSpec spec = entry.machine()
            .spec();
        final ResolvedRecipe resolved = recipe == null || !spec.calculates(structure.mode()) ? null
            : spec.resolve(recipe, inputs);
        final long[] floors = new long[CHART_FLOORS.size() + 1];
        for (int i = 0; i < floors.length; i++) floors[i] = floor(i);
        lastPlanned = new Planned(
            ctx,
            state.copy(),
            floors,
            entry,
            inputs,
            resolved,
            resolved == null ? null : resolved.toCalculator());
        return lastPlanned;
    }

    private static boolean sameFloors(final long[] floors) {
        for (int i = 0; i < floors.length; i++) if (floors[i] != floor(i)) return false;
        return true;
    }

    /** 0 is the voltage floor, then the {@link #CHART_FLOORS}. */
    private static long floor(final int index) {
        if (index == 0) return ChartMinimums.floor(VOLTAGE_FLOOR, 0);
        final ChartFloor floor = CHART_FLOORS.get(index - 1);
        return floorOf(floor);
    }

    private static int floorOf(final ChartFloor floor) {
        return ChartMinimums.floor(
            floor.kind().id,
            (int) floor.kind()
                .getRange()
                .max());
    }

    /**
     * Reads one number off the node's machine at its structure, so an untouched row draws the machine's value, not a
     * global default that is wrong for most machines.
     */
    private static int fromSpec(final RecipeContext ctx, final GTNodeState state, final int fallback,
        final ToIntFunction<ResolvedRecipe> reader) {
        final ResolvedRecipe resolved = resolved(ctx, state);
        return resolved == null ? fallback : reader.applyAsInt(resolved);
    }

    /** Row percentage for a value GregTech stores as a factor. */
    public static int percent(final double factor) {
        return (int) Math.round(100 * factor);
    }

    /**
     * Player's structure values, plus the chart's floors for the other kinds, or the best value where the chart sets no
     * floor. A value stored on a node is used for that node in place of the floor.
     */
    @Nonnull
    public static StructureState resolve(final RecipeContext ctx, final GTNodeState state, final int voltageTier,
        final int mode) {
        final Map<ModifierKind, Long> structure = new HashMap<>();
        for (final ModifierKind kind : ModifierKind.all()) {
            final Long value = state.getStructure(kind.id);
            if (value != null) structure.put(kind, value);
        }
        final Map<ModifierKind, Long> floors = new HashMap<>();
        for (final ChartFloor floor : CHART_FLOORS) floors.put(floor.kind(), (long) floorOf(floor));
        return new StructureState(voltageTier, state.amps == null ? 1 : state.amps, mode, structure, floors);
    }

    /**
     * Machine mode this node runs in. A controller with two machines behind it runs one per recipemap, and the node's
     * recipe came from one of those recipemaps, so the mode is derived from it. Other machines use the mode row.
     */
    public static int mode(final RecipeContext ctx, @Nullable final GTMachineIndex.MachineEntry entry,
        final GTNodeState state) {
        final int implied = entry == null ? -1 : entry.modeFor(ctx.getOrDefault(GTProvider.RECIPE_MAP, null));
        return implied >= 0 ? implied : state.mode == null ? 0 : state.mode;
    }

    /** The node's rows: the machine, its power and structure, then Advanced and the override rows. */
    @Nonnull
    static List<NodeState.Row> rows(final RecipeContext ctx, final GTNodeState state, final Consumer<Runnable> edit) {
        final List<NodeState.Row> rows = new ArrayList<>();
        final List<GTMachineIndex.MachineEntry> candidates = GTMachineIndex.candidates(ctx);
        if (!candidates.isEmpty()) rows.add(
            MachineRows.picker(
                label(MACHINE_ROW),
                candidates,
                followed(
                    () -> state.machine,
                    () -> candidates.getFirst()
                        .id(),
                    state::pickMachine),
                edit));

        final GTMachineIndex.MachineEntry entry = GTMachineIndex.selected(ctx, state);
        final boolean builtAround = entry == null || entry.tieredByBuild();
        final boolean overclocks = state.advanced || entry == null
            || entry.machine() == null
            || entry.machine()
                .spec()
                .overclocks();
        // A singleblock's tier and amperage are fixed by its block, and a machine that runs every recipe at the
        // recipe's voltage gains nothing from its hatches.
        if (builtAround && overclocks) {
            final int minimum = minimumVoltageTier(ctx);
            rows.add(
                MachineRows.tier(
                    label(VOLTAGE_ROW),
                    minimum,
                    GTValues.VN.length - 2,
                    tier -> GTValues.VN[tier],
                    followed(
                        () -> state.voltage == null ? null : voltageTier(ctx, state),
                        () -> defaultVoltageTier(recipeEUt(ctx)),
                        tier -> state.voltage = tier == null ? null : GTValues.VN[tier]),
                    edit));
            rows.add(
                MachineRows.number(
                    label(AMP_ROW),
                    1,
                    MAX_AMPERAGE,
                    followed(
                        () -> state.amps,
                        () -> entry == null ? 1 : Math.max(1, entry.amperage()),
                        a -> state.amps = a),
                    edit));
        }
        // planning for fewer parallels than the machine's maximum is common, so the cap is editable wherever there is
        // a choice
        final int maxParallel = machineMaxParallel(ctx, state);
        if (builtAround && (state.advanced || maxParallel > 1)) rows
            .add(overrideRow(GTOverride.PARALLEL, 1, Math.max(1, maxParallel), () -> maxParallel, ctx, state, edit));

        if (!state.advanced && entry != null) {
            // one row per value a player builds or inserts, visible when the machine reads it
            entry.structure()
                .forEach((kind, range) -> rows.add(structureRow(kind, range, ctx, state, edit)));
            if (entry.modeFor(ctx.getOrDefault(GTProvider.RECIPE_MAP, null)) < 0 && entry.machine() != null
                && entry.machine()
                    .spec()
                    .variesByMode()) {
                rows.add(
                    MachineRows.number(
                        label(MODE_ROW),
                        0,
                        entry.modes()
                            .count() - 1,
                        followed(() -> state.mode, () -> 0, m -> state.mode = m),
                        edit));
            }
        }

        rows.add(
            new NodeState.Row(
                label(ADVANCED_ROW),
                new ToggleButton().name("settings.toggle")
                    .value(new BoolValue.Dynamic(() -> state.advanced, on -> edit.accept(() -> state.advanced = on)))
                    .overlay(false, IKey.str("[ ]"))
                    .overlay(true, IKey.str("[✓]"))));
        if (state.advanced) {
            final OverclockCalculator calculator = calculator(ctx, state);
            for (final GTOverride override : GTOverride.values()) {
                if (override == GTOverride.PARALLEL) continue;
                if (override.isFlag()) rows.add(
                    MachineRows.flag(
                        label(override.key),
                        followed(
                            () -> (Boolean) state.getOverride(override),
                            () -> calculator != null && override.machineFlag(calculator),
                            on -> state.setOverride(override, on)),
                        edit));
                else rows.add(
                    overrideRow(
                        override,
                        override.min,
                        override.max,
                        () -> calculator == null ? override.min : override.machineInt(calculator),
                        ctx,
                        state,
                        edit));
            }
        }
        return rows;
    }

    private static NodeState.Row overrideRow(final GTOverride override, final int min, final int max,
        final IntSupplier machine, final RecipeContext ctx, final GTNodeState state, final Consumer<Runnable> edit) {
        return MachineRows.number(
            label(override.key),
            min,
            max,
            followed(
                () -> (Integer) state.getOverride(override),
                machine::getAsInt,
                v -> state.setOverride(override, v)),
            edit);
    }

    /** Labelled and valued with GregTech's names for the kind, and bounded by the machine's range. */
    private static NodeState.Row structureRow(final ModifierKind kind, final ModifierKind.Range range,
        final RecipeContext ctx, final GTNodeState state, final Consumer<Runnable> edit) {
        return MachineRows.tier(
            kind.getName(),
            (int) range.min(),
            (int) Math.min(Integer.MAX_VALUE, range.max()),
            kind::label,
            followed(() -> {
                final Long stored = state.getStructure(kind.id);
                return stored == null ? null : (int) (long) stored;
            }, () -> planned(ctx, state, kind), v -> state.setStructure(kind.id, v == null ? null : (long) v)),
            edit);
    }

    /**
     * Value an untouched structure row draws, the one the node is planned with: the chart's floor raised to the
     * lowest value that runs the recipe, else the spec's best, per {@link GTMachineSpec#inputs}.
     */
    private static int planned(final RecipeContext ctx, final GTNodeState state, final ModifierKind kind) {
        final Planned planned = planned(ctx, state);
        if (planned == null) return 0;
        return (int) Math.min(
            Integer.MAX_VALUE,
            planned.inputs()
                .value(kind));
    }

    /** The calculator for this node's recipe on its machine, read by the override rows. */
    @Nullable
    private static OverclockCalculator calculator(final RecipeContext ctx, final GTNodeState state) {
        final Planned planned = planned(ctx, state);
        return planned == null ? null : planned.calculator();
    }

    @Nonnull
    private static String label(final String key) {
        return StatCollector.translateToLocal("plannh.settings." + key);
    }

    private static <T> Followed<T> followed(final Supplier<T> stored, final Supplier<T> machine,
        final Consumer<T> set) {
        return new Followed<>() {

            @Override
            @Nullable
            public T stored() {
                return stored.get();
            }

            @Override
            @Nonnull
            public T machine() {
                return machine.get();
            }

            @Override
            public void set(@Nullable final T value) {
                set.accept(value);
            }
        };
    }
}
