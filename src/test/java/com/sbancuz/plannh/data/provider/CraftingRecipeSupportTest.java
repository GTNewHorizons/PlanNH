package com.sbancuz.plannh.data.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.flowchart.resource.LongItemStack;

import codechicken.nei.recipe.ShapedRecipeHandler;

class CraftingRecipeSupportTest {

    @BeforeAll
    static void initializeMinecraftFixture() {
        ProviderTestFixture.bootstrap();
    }

    @Test
    void returnedContainersAreOutputsInsteadOfBeingLostByAutomaticCrafting() {
        final ItemStack consumed = new ItemStack(Items.water_bucket);
        final ShapedRecipeHandler handler = handler(consumed, new ItemStack(Items.iron_ingot));
        final RecipeContext.Builder recipe = RecipeContext.builder();
        new CraftingRecipeSupport(handler, 0).addContainerReturns(recipe);
        final var outputs = recipe.build()
            .outputs();
        assertEquals(1, outputs.size());
        assertEquals(
            1,
            outputs.getFirst()
                .getAmount());
        assertSame(
            consumed.getItem()
                .getContainerItem(consumed)
                .getItem(),
            ((LongItemStack) outputs.getFirst()
                .getValue()).stack.getItem());
    }

    @Test
    void selectsMissingDynamicOutputPermutations() {
        final ItemStack output = new ItemStack(Items.iron_ingot);
        final ItemStack alternateOutput = new ItemStack(Items.gold_ingot);
        final ShapedRecipeHandler dynamicHandler = handler(
            new ItemStack(Items.redstone),
            Arrays.asList(output, alternateOutput));

        final CraftingRecipeSupport support = new CraftingRecipeSupport(dynamicHandler, 0);
        assertTrue(support.isEligible());
        dynamicHandler.getResultStack(0).item = null;
        support.selectMissingPermutations();
        assertTrue(dynamicHandler.getResultStack(0).item != null);
    }

    private static ShapedRecipeHandler handler(final ItemStack input, final ItemStack output) {
        final ShapedRecipeHandler handler = ProviderTestFixture.allocate(ShapedRecipeHandler.class);
        handler.arecipes = new ArrayList<>();
        handler.arecipes.add(handler.new CachedShapedRecipe(1, 1, new Object[] { input }, output));
        return handler;
    }

    private static ShapedRecipeHandler handler(final ItemStack input, final java.util.List<ItemStack> outputs) {
        final ShapedRecipeHandler handler = ProviderTestFixture.allocate(ShapedRecipeHandler.class);
        handler.arecipes = new ArrayList<>();
        handler.arecipes.add(handler.new CachedShapedRecipe(1, 1, new Object[] { input }, outputs.get(0)));
        handler.getResultStack(0).items = outputs.toArray(new ItemStack[0]);
        return handler;
    }
}
