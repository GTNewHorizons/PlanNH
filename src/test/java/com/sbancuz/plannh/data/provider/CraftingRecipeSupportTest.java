package com.sbancuz.plannh.data.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.flowchart.resource.LongItemStack;

import codechicken.nei.recipe.ShapedRecipeHandler;
import codechicken.nei.recipe.ShapelessRecipeHandler;
import sun.misc.Unsafe;

class CraftingRecipeSupportTest {

    private static final Unsafe UNSAFE = unsafe();

    @BeforeAll
    static void initializeMinecraftFixture() {
        ProviderTestFixture.bootstrap();
    }

    @Test
    void returnedContainersAreOutputsInsteadOfBeingLostByAutomaticCrafting() {
        final ItemStack consumed = new ItemStack(Items.water_bucket);
        final ShapedRecipeHandler handler = handler(
            consumed,
            new ItemStack(ProviderTestFixture.item("container_recipe_result")));
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
    void acceptsContainerReturnsAndDynamicOutputPermutations() {
        final ItemStack containerInput = new ItemStack(Items.water_bucket);
        final ItemStack output = new ItemStack(ProviderTestFixture.item("crafting_support_container_output"));
        final ShapedRecipeHandler containerHandler = handler(containerInput, output);

        final ItemStack alternateOutput = new ItemStack(ProviderTestFixture.item("crafting_support_alternate_output"));
        final ShapedRecipeHandler dynamicHandler = handler(
            new ItemStack(ProviderTestFixture.item("crafting_support_dynamic_input")),
            Arrays.asList(output, alternateOutput));

        assertTrue(new CraftingRecipeSupport(containerHandler, 0).isEligible());
        final CraftingRecipeSupport support = new CraftingRecipeSupport(dynamicHandler, 0);
        assertTrue(support.isEligible());
        dynamicHandler.getResultStack(0).item = null;
        support.selectMissingPermutations();
        assertTrue(dynamicHandler.getResultStack(0).item != null);
    }

    @Test
    void acceptsShapelessAndOrePermutationsFromNativeHandlers() {
        final ItemStack first = new ItemStack(ProviderTestFixture.item("crafting_support_ore_a"));
        final ItemStack second = new ItemStack(ProviderTestFixture.item("crafting_support_ore_b"));
        final ItemStack output = new ItemStack(ProviderTestFixture.item("crafting_support_shapeless_output"));
        final ShapelessRecipeHandler handler = allocate(ShapelessRecipeHandler.class);
        handler.stackorder = new int[][] { { 0, 0 }, { 1, 0 }, { 0, 1 }, { 1, 1 }, { 0, 2 }, { 1, 2 }, { 2, 0 },
            { 2, 1 }, { 2, 2 } };
        handler.arecipes = new ArrayList<>();
        handler.arecipes
            .add(handler.new CachedShapelessRecipe(new Object[] { Arrays.asList(first, second), second }, output));

        assertTrue(new CraftingRecipeSupport(handler, 0).isEligible());
    }

    @Test
    void rejectsARecipeThatDoesNotFitTheThreeByThreeGrid() {
        final ShapedRecipeHandler handler = allocate(ShapedRecipeHandler.class);
        handler.arecipes = new ArrayList<>();
        handler.arecipes.add(
            handler.new CachedShapedRecipe(4, 1,
                new Object[] { new ItemStack(ProviderTestFixture.item("crafting_support_9x9_a")),
                    new ItemStack(ProviderTestFixture.item("crafting_support_9x9_b")),
                    new ItemStack(ProviderTestFixture.item("crafting_support_9x9_c")),
                    new ItemStack(ProviderTestFixture.item("crafting_support_9x9_d")) },
                new ItemStack(ProviderTestFixture.item("crafting_support_9x9_output"))));

        assertFalse(new CraftingRecipeSupport(handler, 0).isEligible());
    }

    private static ShapedRecipeHandler handler(final ItemStack input, final ItemStack output) {
        final ShapedRecipeHandler handler = allocate(ShapedRecipeHandler.class);
        handler.arecipes = new ArrayList<>();
        handler.arecipes.add(handler.new CachedShapedRecipe(1, 1, new Object[] { input }, output));
        return handler;
    }

    private static ShapedRecipeHandler handler(final ItemStack input, final java.util.List<ItemStack> outputs) {
        final ShapedRecipeHandler handler = allocate(ShapedRecipeHandler.class);
        handler.arecipes = new ArrayList<>();
        handler.arecipes.add(handler.new CachedShapedRecipe(1, 1, new Object[] { input }, outputs.get(0)));
        handler.getResultStack(0).items = outputs.toArray(new ItemStack[0]);
        return handler;
    }

    @SuppressWarnings("unchecked")
    private static <T> T allocate(final Class<T> type) {
        try {
            return (T) UNSAFE.allocateInstance(type);
        } catch (final InstantiationException failure) {
            throw new AssertionError("Could not allocate native handler fixture", failure);
        }
    }

    private static Unsafe unsafe() {
        try {
            final Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (final ReflectiveOperationException failure) {
            throw new AssertionError("Could not access the test fixture runtime", failure);
        }
    }
}
