package com.sbancuz.plannh.data.provider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.FurnaceRecipeHandler;
import mrtjp.projectred.expansion.InductiveFurnaceRecipeLib$;
import sun.misc.Unsafe;

class ProjectRedProviderTest {

    private static final Unsafe UNSAFE = unsafe();

    @BeforeAll
    static void initializeMinecraftFixture() {
        ProviderTestFixture.bootstrap();
    }

    @Test
    void inductiveFurnaceRequiresRegistryInputAndExactOutput() {
        final Item inputItem = ProviderTestFixture.item("projectred_input");
        final Item outputItem = ProviderTestFixture.item("projectred_output");
        final Item otherOutputItem = ProviderTestFixture.item("projectred_other_output");
        final ItemStack input = new ItemStack(inputItem);
        final ItemStack output = new ItemStack(outputItem);
        InductiveFurnaceRecipeLib$.MODULE$.addRecipe(input, output, 37);

        final ProjectRedProvider provider = new ProjectRedProvider();

        assertTrue(provider.canCraft(furnace(input, output), 0));
        assertFalse(provider.canCraft(furnace(input, new ItemStack(otherOutputItem)), 0));
    }

    @Test
    void inductiveFurnaceRejectsInputPermutationsWithDifferentRuntimeRecipes() {
        final ItemStack input = new ItemStack(ProviderTestFixture.item("projectred_permuted_input"));
        final ItemStack output = new ItemStack(ProviderTestFixture.item("projectred_permuted_output"));
        final ItemStack unsupported = new ItemStack(ProviderTestFixture.item("projectred_unsupported_input"));
        InductiveFurnaceRecipeLib$.MODULE$.addRecipe(input, output, 37);
        final FurnaceRecipeHandler handler = furnace(input, output);
        final PositionedStack ingredient = handler.getIngredientStacks(0)
            .getFirst();
        ingredient.items = new ItemStack[] { input, unsupported };

        assertFalse(new ProjectRedProvider().canCraft(handler, 0));
    }

    private static FurnaceRecipeHandler furnace(final ItemStack input, final ItemStack output) {
        final FurnaceRecipeHandler handler = allocate(FurnaceRecipeHandler.class);
        handler.arecipes = new ArrayList<>();
        handler.arecipes.add(handler.new SmeltingPair(input.copy(), output.copy()));
        FurnaceRecipeHandler.afuels = new ArrayList<>();
        FurnaceRecipeHandler.afuels.add(new FurnaceRecipeHandler.FuelPair(new ItemStack(Items.coal), 1600));
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
