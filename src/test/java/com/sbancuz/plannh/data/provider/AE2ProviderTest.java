package com.sbancuz.plannh.data.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sbancuz.plannh.data.setting.Settings;

import appeng.integration.modules.NEIHelpers.NEIAEShapelessRecipeHandler;
import appeng.integration.modules.NEIHelpers.NEIInscriberRecipeHandler;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.ShapedRecipeHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;

class AE2ProviderTest {

    @BeforeAll
    static void initializeMinecraftFixture() {
        ProviderTestFixture.bootstrap();
    }

    @Test
    void normalAndAdvancedInscribersHaveDistinctSlotsAndTiming() {
        final AE2Provider provider = new AE2Provider();
        final var profiles = provider.getProfiles(ProviderTestFixture.allocate(NEIInscriberRecipeHandler.class), 0);

        assertEquals(2, profiles.size());
        assertEquals(
            "ae2:inscriber",
            profiles.get(0)
                .id());
        assertEquals(
            "ae2:advanced_inscriber",
            profiles.get(1)
                .id());
        assertEquals(
            3,
            profiles.get(0)
                .maximum(Settings.CATALYST_ACCEL_CARD));
        assertEquals(
            5,
            profiles.get(1)
                .maximum(Settings.CATALYST_ACCEL_CARD));
    }

    @Test
    void assemblerIsNotInheritedBySpecialShapedHandlers() {
        final AE2Provider provider = new AE2Provider();

        assertFalse(
            provider.getProfiles(ProviderTestFixture.allocate(SpecialShapedHandler.class), 0)
                .stream()
                .anyMatch(
                    profile -> profile.id()
                        .equals("ae2:molecular_assembler")));
        final ItemStack input = new ItemStack(ProviderTestFixture.item("ae2_assembler_input"));
        final ItemStack output = new ItemStack(ProviderTestFixture.item("ae2_assembler_output"));
        final IRecipe recipe = new ShapedRecipes(1, 1, new ItemStack[] { input.copy() }, output.copy());
        final ShapedRecipeHandler handler = shapedHandler(input, output);
        CraftingManager.getInstance()
            .getRecipeList()
            .add(recipe);
        try {
            assertTrue(
                provider.getProfiles(handler, 0)
                    .stream()
                    .anyMatch(
                        profile -> profile.id()
                            .equals("ae2:molecular_assembler")));
        } finally {
            CraftingManager.getInstance()
                .getRecipeList()
                .remove(recipe);
        }
    }

    @Test
    void assemblerAcceptsAnAeShapelessRecipeAndMissingSelections() {
        final AE2Provider provider = new AE2Provider();
        final NEIAEShapelessRecipeHandler handler = ProviderTestFixture.allocate(NEIAEShapelessRecipeHandler.class);
        handler.arecipes = new ArrayList<>();
        final ItemStack input = new ItemStack(ProviderTestFixture.item("ae2_shapeless_input"));
        final ItemStack alternate = new ItemStack(ProviderTestFixture.item("ae2_shapeless_alternate"));
        final ItemStack output = new ItemStack(ProviderTestFixture.item("ae2_shapeless_output"));
        handler.arecipes.add(
            new CachedRecipeFixture(
                handler,
                Arrays.asList(new PositionedStack(Arrays.asList(input, alternate), 25, 6, false)),
                new PositionedStack(output, 119, 24, false)));
        handler.getIngredientStacks(0)
            .get(0).item = null;

        assertEquals(
            "ae2:molecular_assembler",
            provider.getProfiles(handler, 0)
                .getFirst()
                .id());
        new CraftingRecipeSupport(handler, 0).selectMissingPermutations();
        assertTrue(
            handler.getIngredientStacks(0)
                .get(0).item != null);
    }

    private static ShapedRecipeHandler shapedHandler(final ItemStack input, final ItemStack output) {
        final ShapedRecipeHandler handler = ProviderTestFixture.allocate(ShapedRecipeHandler.class);
        handler.arecipes = new ArrayList<>();
        handler.arecipes.add(handler.new CachedShapedRecipe(1, 1, new Object[] { input }, output));
        return handler;
    }

    static class SpecialShapedHandler extends ShapedRecipeHandler {
    }

    private static final class CachedRecipeFixture extends TemplateRecipeHandler.CachedRecipe {

        private final ArrayList<PositionedStack> ingredients;
        private final PositionedStack result;

        private CachedRecipeFixture(final TemplateRecipeHandler handler,
            final java.util.List<PositionedStack> ingredients, final PositionedStack result) {
            handler.super();
            this.ingredients = new ArrayList<>(ingredients);
            this.result = result;
        }

        @Override
        public ArrayList<PositionedStack> getIngredients() {
            return ingredients;
        }

        @Override
        public PositionedStack getResult() {
            return result;
        }
    }

}
