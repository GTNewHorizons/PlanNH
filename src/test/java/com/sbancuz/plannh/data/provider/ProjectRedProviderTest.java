package com.sbancuz.plannh.data.provider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import codechicken.nei.recipe.FurnaceRecipeHandler;
import mrtjp.projectred.expansion.InductiveFurnaceRecipeLib$;

class ProjectRedProviderTest {

    @BeforeAll
    static void initializeMinecraftFixture() {
        ProviderTestFixture.bootstrap();
    }

    @Test
    void inductiveFurnaceRequiresRegistryInputAndExactOutput() {
        final ItemStack input = new ItemStack(Items.iron_ingot);
        final ItemStack output = new ItemStack(Items.gold_ingot);
        InductiveFurnaceRecipeLib$.MODULE$.addRecipe(input, output, 37);

        final ProjectRedProvider provider = new ProjectRedProvider();

        assertTrue(provider.canCraft(furnace(input, output), 0));
        assertFalse(provider.canCraft(furnace(input, new ItemStack(Items.diamond)), 0));
    }

    private static FurnaceRecipeHandler furnace(final ItemStack input, final ItemStack output) {
        final FurnaceRecipeHandler handler = ProviderTestFixture.allocate(FurnaceRecipeHandler.class);
        handler.arecipes = new ArrayList<>();
        handler.arecipes.add(handler.new SmeltingPair(input.copy(), output.copy()));
        FurnaceRecipeHandler.afuels = new ArrayList<>();
        FurnaceRecipeHandler.afuels.add(new FurnaceRecipeHandler.FuelPair(new ItemStack(Items.coal), 1600));
        return handler;
    }
}
