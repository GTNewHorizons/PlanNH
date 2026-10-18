package com.sbancuz.plannh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.machine.MachineVariant;
import com.sbancuz.plannh.data.machine.MachineVariants;
import com.sbancuz.plannh.data.properties.RecipeProperty;

/**
 * The machine picker without GregTech. Every machine here is from a made-up mod: the layer is only
 * worth having if a provider can register machines and get the picker row and per-machine settings rows
 * with no PlanNH code specific to that mod.
 */
class MachineVariantsTest {

    private static final RecipeProperty<String> KIND = RecipeProperty.<String>builder("test_kind", "")
        .build();

    private record Fake(String id, String displayName, String label) implements MachineVariant {}

    private static final Fake SIMPLE = new Fake("mod:simple", "Simple Grinder", "Simple Grinder");
    private static final Fake FANCY = new Fake("mod:fancy", "Fancy Grinder", "Fancy Grinder (big)");

    /** Returns candidates for one recipe kind only, like a real provider. */
    private record FakeSource(String kind, List<Fake> offered) implements MachineVariants.Source {

        @Override
        public List<? extends MachineVariant> candidates(final RecipeContext ctx) {
            return kind.equals(ctx.getOrDefault(KIND, "")) ? offered : List.of();
        }

        @Override
        @Nullable
        public MachineVariant byId(final String id) {
            for (final Fake fake : offered) {
                if (fake.id()
                    .equals(id)) return fake;
            }
            return null;
        }
    }

    private static RecipeContext recipeOf(final String kind) {
        final Map<RecipeProperty<?>, Object> props = new HashMap<>();
        props.put(KIND, kind);
        return new RecipeContext(props);
    }

    @BeforeEach
    @AfterEach
    void isolate() {
        MachineVariants.reset();
    }

    @Test
    void aProviderThatDoesNotRecogniseTheRecipeOffersNothing() {
        MachineVariants.register(new FakeSource("grinding", List.of(SIMPLE, FANCY)));

        assertTrue(
            MachineVariants.candidates(recipeOf("smelting"))
                .isEmpty());
        assertNull(MachineVariants.selected(recipeOf("smelting"), (String) null));
    }

    /** Nothing stored is the normal state, so the first candidate must be usable as the selection. */
    @Test
    void anUnsetNodeUsesTheBestCandidate() {
        MachineVariants.register(new FakeSource("grinding", List.of(SIMPLE, FANCY)));

        assertSame(SIMPLE, MachineVariants.selected(recipeOf("grinding"), (String) null));
    }

    @Test
    void aStoredMachineIsHonoured() {
        MachineVariants.register(new FakeSource("grinding", List.of(SIMPLE, FANCY)));

        assertSame(FANCY, MachineVariants.selected(recipeOf("grinding"), "mod:fancy"));
    }

    /** In a pack without the mod, a stored id must not resolve to a different machine with other numbers. */
    @Test
    void aMachineThePackNoLongerHasResolvesToNothing() {
        MachineVariants.register(new FakeSource("grinding", List.of(SIMPLE, FANCY)));

        assertNull(MachineVariants.selected(recipeOf("grinding"), "mod:removed"));
    }

    /**
     * A node's recipe comes from one mod's recipe list, so the first source with candidates wins.
     * Merging would offer machines that can't run the recipe.
     */
    @Test
    void onlyTheProviderThatRecognisesTheRecipeAnswers() {
        MachineVariants.register(new FakeSource("smelting", List.of(SIMPLE)));
        MachineVariants.register(new FakeSource("grinding", List.of(FANCY)));

        assertEquals(List.of(FANCY), MachineVariants.candidates(recipeOf("grinding")));
    }

    /** Registration is per pass. A stale source would offer machines from a mod that is not loaded. */
    @Test
    void resetDropsEverySource() {
        MachineVariants.register(new FakeSource("grinding", List.of(SIMPLE)));
        MachineVariants.reset();

        assertTrue(
            MachineVariants.candidates(recipeOf("grinding"))
                .isEmpty());
    }
}
