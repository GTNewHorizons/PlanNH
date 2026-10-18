package com.sbancuz.plannh.data.provider.gregtech;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.sbancuz.plannh.PlanNH;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.logic.ModifierKind;
import gregtech.api.logic.ProcessingInputs;
import gregtech.api.logic.ProcessingSpec;
import gregtech.api.logic.ResolvedRecipe;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.util.GTRecipe;

/** A GregTech multiblock's {@link ProcessingSpec}, evaluated at a node's structure. */
public record GTMachineSpec(ProcessingSpec spec) {

    /** Evaluated once at indexing, so a spec PlanNH cannot evaluate fails at indexing and not on a chart. */
    private static final StructureState REFERENCE = StructureState.of(1, 0);

    /** Machine's spec, or null when getProcessingSpec() returns null or a spec PlanNH cannot evaluate. */
    @Nullable
    public static GTMachineSpec read(@Nonnull final IMetaTileEntity prototype) {
        if (!(prototype instanceof final MTEMultiBlockBase multi)) return null;
        final ProcessingSpec spec = multi.getProcessingSpec();
        if (spec == null) return null;
        try {
            return of(spec);
        } catch (final RuntimeException e) {
            PlanNH.LOG.warn("PlanNH: {} declares a spec PlanNH cannot evaluate", prototype.getClass().getName(), e);
            return null;
        }
    }

    /** @throws RuntimeException when the spec reads a value missing from its getModifiers() */
    @Nonnull
    public static GTMachineSpec of(@Nonnull final ProcessingSpec spec) {
        final GTMachineSpec machine = new GTMachineSpec(spec);
        spec.getMaxParallel(machine.inputs(REFERENCE));
        return machine;
    }

    /**
     * Inputs for this structure: one hatch at the node's tier and amperage, the player's values, the chart's floors
     * clamped to the machine's range, and the spec's best for every other kind.
     */
    @Nonnull
    public ProcessingInputs inputs(@Nonnull final StructureState state) {
        final ProcessingInputs.Builder inputs = spec.bestInputs()
            .energyHatch(ProcessingInputs.EnergyHatch.exotic(state.voltageTier(), state.amperage()))
            .mode(state.mode());
        state.floors()
            .forEach((kind, floor) -> {
                final ModifierKind.Range range = spec.getModifiers()
                    .get(kind);
                if (range != null) inputs.value(kind, Math.max(range.min(), Math.min(range.max(), floor)));
            });
        state.structure()
            .forEach(inputs::value);
        return inputs.build();
    }

    /**
     * As {@link #inputs(StructureState)}, for one recipe: each ordered value the player left unset is raised to the
     * lowest that passes GregTech's check for the recipe, such as a hot enough coil.
     */
    @Nonnull
    public ProcessingInputs inputs(@Nonnull final StructureState state, @Nonnull final GTRecipe recipe) {
        ProcessingInputs inputs = inputs(state);
        for (final ModifierKind modifier : spec.getModifiers()
            .keySet()) {
            // runtime kinds start at their best
            if (!(modifier instanceof final ModifierKind.IntKind kind) || !kind.ordered
                || kind.source == ModifierKind.Source.RUNTIME
                || state.structure()
                    .containsKey(kind))
                continue;
            final OptionalInt lowest = spec.lowestPassing(kind, recipe, inputs);
            if (lowest.isPresent() && lowest.getAsInt() > inputs.value(kind)) {
                inputs = inputs.toBuilder()
                    .value(kind, lowest.getAsInt())
                    .build();
            }
        }
        return inputs;
    }

    @Nonnull
    public ResolvedRecipe resolve(@Nonnull final GTRecipe recipe, @Nonnull final StructureState state) {
        return spec.resolve(recipe, inputs(state, recipe));
    }

    /**
     * Values a player builds or inserts, each with its range. Values built up while running, such as momentum, are
     * planned at their best and have no row.
     */
    @Nonnull
    public Map<ModifierKind, ModifierKind.Range> structure() {
        final Map<ModifierKind, ModifierKind.Range> structure = new LinkedHashMap<>();
        spec.getModifiers()
            .forEach((kind, range) -> { if (kind.source != ModifierKind.Source.RUNTIME) structure.put(kind, range); });
        return structure;
    }
}
