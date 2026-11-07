package com.sbancuz.plannh.data.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        dynamicHandler.getResultStack(0).item = null;
        assertTrue(support.isEligible());
        support.selectMissingPermutations();
        assertSame(Items.iron_ingot, dynamicHandler.getResultStack(0).item.getItem());
    }

    @Test
    void selectsMissingIngredientBeforeAccountingForContainerReturns() {
        final ShapedRecipeHandler handler = handler(new ItemStack(Items.water_bucket), new ItemStack(Items.iron_ingot));
        handler.getIngredientStacks(0)
            .getFirst().item = null;
        final CraftingRecipeSupport support = new CraftingRecipeSupport(handler, 0);

        assertTrue(support.isEligible());
        support.selectMissingPermutations();
        final RecipeContext.Builder recipe = RecipeContext.builder();
        support.addContainerReturns(recipe);

        assertSame(
            Items.water_bucket,
            handler.getIngredientStacks(0)
                .getFirst().item.getItem());
        assertEquals(
            1,
            recipe.outputs()
                .size());
        assertSame(
            Items.bucket,
            ((LongItemStack) recipe.outputs()
                .getFirst()
                .getValue()).stack.getItem());
    }

    @Test
    void keepsAnAlreadySelectedPermutation() {
        final ShapedRecipeHandler handler = handler(
            new ItemStack(Items.redstone),
            Arrays.asList(new ItemStack(Items.iron_ingot), new ItemStack(Items.gold_ingot)));
        handler.getResultStack(0)
            .setPermutationToRender(1);
        final ItemStack selected = handler.getResultStack(0).item;

        new CraftingRecipeSupport(handler, 0).selectMissingPermutations();

        assertSame(selected, handler.getResultStack(0).item);
    }

    @Test
    void skipsNullPermutationsAndRejectsOutputsWithoutUsablePermutations() {
        final ShapedRecipeHandler handler = handler(new ItemStack(Items.redstone), new ItemStack(Items.iron_ingot));
        final CraftingRecipeSupport support = new CraftingRecipeSupport(handler, 0);
        handler.getResultStack(0).items = new ItemStack[] { null, new ItemStack(Items.gold_ingot) };
        handler.getResultStack(0).item = null;

        assertTrue(support.isEligible());
        support.selectMissingPermutations();
        assertSame(Items.gold_ingot, handler.getResultStack(0).item.getItem());

        handler.getResultStack(0).item = null;
        handler.getResultStack(0).items = new ItemStack[] { null };
        assertFalse(support.isEligible());
        support.selectMissingPermutations();
        assertNull(handler.getResultStack(0).item);
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
