package com.sbancuz.plannh.data.provider.gregtech;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.sbancuz.plannh.PlanNH;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMap;

/**
 * Mode count of a machine, and the mode each recipe runs in. Several multiblocks are two machines behind
 * one controller. Where the machine declares its modes, the mode is derived from the recipe. A user-picked
 * mode could pair tower mode with a distillery recipe, modelling a machine that cannot run it with no
 * error raised.
 *
 * <p>
 * The count bounds the row even where the mapping is ambiguous. Both come from the prototype's
 * {@code getMachineModeCount()} and {@code getMachineModeFor(RecipeMap)}.
 */
public final class GTMachineModes {

    private GTMachineModes() {}

    private static final Modes SINGLE = new Modes(1, null);

    /**
     * @param count   how many modes the machine cycles through, at least one.
     * @param machine the registry prototype, or null for a single-mode machine.
     */
    public record Modes(int count, @Nullable MTEMultiBlockBase machine) {

        /** Mode that runs this recipemap, or -1 when the user has to pick it. */
        public int modeFor(@Nullable final RecipeMap<?> recipeMap) {
            if (machine == null || recipeMap == null) return -1;
            return machine.getMachineModeFor(recipeMap);
        }
    }

    /** Reads the modes off the registry prototype, without switching it through them. */
    @Nonnull
    public static Modes of(@Nonnull final IMetaTileEntity prototype) {
        if (!(prototype instanceof final MTEMultiBlockBase machine) || !machine.supportsMachineModeSwitch()) {
            return SINGLE;
        }
        try {
            final int count = machine.getMachineModeCount();
            return count < 2 ? SINGLE : new Modes(count, machine);
        } catch (final RuntimeException | LinkageError e) {
            PlanNH.LOG.debug("PlanNH: {} would not report its modes", machine.getClass(), e);
            return SINGLE;
        }
    }
}
