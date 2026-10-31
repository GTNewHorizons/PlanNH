package com.sbancuz.plannh.data.provider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import codechicken.nei.recipe.ShapedRecipeHandler;

class AE2ProviderTest {

    @BeforeAll
    static void initializeMinecraftFixture() {
        ProviderTestFixture.bootstrap();
    }

    @Test
    void assemblerIsNotInheritedBySpecialShapedHandlers() {
        final AE2Provider provider = new AE2Provider();

        assertTrue(
            provider.getProfiles(ProviderTestFixture.allocate(SpecialShapedHandler.class), 0)
                .isEmpty());
        final ItemStack input = new ItemStack(Items.iron_ingot);
        final ItemStack output = new ItemStack(Items.gold_ingot);
        final IRecipe recipe = new ShapedRecipes(1, 1, new ItemStack[] { input.copy() }, output.copy());
        final ShapedRecipeHandler handler = shapedHandler(input, output);
        CraftingManager.getInstance()
            .getRecipeList()
            .add(recipe);
        try {
            assertFalse(
                provider.getProfiles(handler, 0)
                    .isEmpty());
        } finally {
            CraftingManager.getInstance()
                .getRecipeList()
                .remove(recipe);
        }
    }

    private static ShapedRecipeHandler shapedHandler(final ItemStack input, final ItemStack output) {
        final ShapedRecipeHandler handler = ProviderTestFixture.allocate(ShapedRecipeHandler.class);
        handler.arecipes = new ArrayList<>();
        handler.arecipes.add(handler.new CachedShapedRecipe(1, 1, new Object[] { input }, output));
        return handler;
    }

    static class SpecialShapedHandler extends ShapedRecipeHandler {
    }

}
