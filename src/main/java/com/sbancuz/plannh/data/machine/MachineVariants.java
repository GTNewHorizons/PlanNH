package com.sbancuz.plannh.data.machine;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.NodeState;
import com.sbancuz.plannh.data.RecipeContext;

/**
 * The machines that can run a node's recipe, across the installed mods.
 *
 * <p>
 * One picker row and one stored value per node, whichever provider the machines come from. A mod
 * registers which machines run a recipe and gets the picker row, the per-machine settings rows and
 * persistence without writing any of them.
 *
 * <p>
 * Static like {@code MachineProfileRegistry} and reset with it from {@code Compat.init}, because
 * providers register into it during the same pass.
 */
public final class MachineVariants {

    private MachineVariants() {}

    /**
     * A provider's lookup for its machines. A lookup, not a list passed at registration: the GregTech
     * source builds its entries by cloning and probing every multiblock, which is too slow for mod init,
     * and caches and orders them itself.
     */
    public interface Source {

        /**
         * The machines that can run this recipe, best first. The widget uses the first as the value of
         * an unset setting, so the order sets which machine an untouched node is planned with. Empty when
         * this source has no machine for the recipe.
         */
        @Nonnull
        List<? extends MachineVariant> candidates(RecipeContext ctx);

        /** A machine by its persisted id, whatever recipe it was stored against, or null when unknown. */
        @Nullable
        MachineVariant byId(String id);
    }

    private static final List<Source> sources = new ArrayList<>();

    public static void register(@Nonnull final Source source) {
        sources.add(source);
    }

    public static void reset() {
        sources.clear();
        lastCandidates = null;
        lastStored = null;
        lastSelected = null;
    }

    /**
     * The first non-empty answer, not merged. A node's recipe comes from one mod's recipe list, so two
     * non-empty sources would mean one of them is wrong. Concatenating would also allocate on a path that
     * runs every frame.
     */
    @Nonnull
    public static List<? extends MachineVariant> candidates(final RecipeContext ctx) {
        for (final Source source : sources) {
            final List<? extends MachineVariant> found = source.candidates(ctx);
            if (!found.isEmpty()) return found;
        }
        return List.of();
    }

    @Nullable
    public static MachineVariant byId(final String id) {
        for (final Source source : sources) {
            final MachineVariant found = source.byId(id);
            if (found != null) return found;
        }
        return null;
    }

    /**
     * The last result of {@link #selected}, memoized on the identity of the candidate list, not its
     * contents. A source returns the same list instance until its candidates change.
     */
    @Nullable
    private static List<? extends MachineVariant> lastCandidates;
    @Nullable
    private static String lastStored;
    @Nullable
    private static MachineVariant lastSelected;

    /**
     * The node's machine: the stored one, or the best candidate when none is stored, so a node on the
     * default stores nothing. A stored id missing from the installed pack resolves to null, not to a
     * different machine.
     */
    @Nullable
    public static MachineVariant selected(final RecipeContext ctx, @Nullable final String storedId) {
        final List<? extends MachineVariant> candidates = candidates(ctx);
        if (candidates.isEmpty()) return null;

        final String stored = storedId == null ? "" : storedId;
        if (candidates == lastCandidates && stored.equals(lastStored)) return lastSelected;

        lastCandidates = candidates;
        lastStored = stored;
        lastSelected = resolve(candidates, stored);
        return lastSelected;
    }

    /** The node's machine, by the id its profile's node state stores. */
    @Nullable
    public static MachineVariant selected(final RecipeContext ctx, final MachineConfig config) {
        final NodeState state = config.getNodeState();
        return selected(ctx, state == null ? null : state.machineId());
    }

    @Nullable
    private static MachineVariant resolve(final List<? extends MachineVariant> candidates, final String stored) {
        if (stored.isEmpty()) return candidates.getFirst();
        for (final MachineVariant candidate : candidates) {
            if (candidate.id()
                .equals(stored)) return candidate;
        }
        return null;
    }
}
