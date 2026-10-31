package com.sbancuz.plannh.data.properties;

import java.util.List;

import com.sbancuz.plannh.PlanNH;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.flowchart.Port;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.FurnaceRecipeHandler;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.RecipeHandlerRef;

public interface PropertyProvider {

    enum StackRole {
        INPUT,
        OUTPUT,
        IGNORE
    }

    void register();

    default RecipeContext extract(final RecipeHandlerRef ref) {
        final RecipeContext.Builder recipe = RecipeContext.builder();
        populate(recipe, ref.handler, ref.recipeIndex);
        return recipe.build();
    }

    /** Other stacks can be products, ingredients, or display-only prerequisites, regardless of burn time. */
    default StackRole otherStackRole(final IRecipeHandler handler, final int recipeIndex, final PositionedStack stack) {
        return handler instanceof FurnaceRecipeHandler ? StackRole.IGNORE : StackRole.OUTPUT;
    }

    default void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler, final int recipeIndex) {
        final List<PositionedStack> ins = handler.getIngredientStacks(recipeIndex);
        for (final PositionedStack ps : ins) if (ps != null && ps.item != null) recipe.input(Port.fromStack(ps));

        final PositionedStack result = handler.getResultStack(recipeIndex);
        if (result != null && result.item != null) {
            recipe.output(Port.fromStack(result));
            if (result.items.length > 1) PlanNH.LOG.warn("output found with permutations");
        }

        final List<PositionedStack> others = handler.getOtherStacks(recipeIndex);
        for (final PositionedStack ps : others) {
            if (ps != null && ps.item != null) {
                switch (otherStackRole(handler, recipeIndex, ps)) {
                    case INPUT -> recipe.input(Port.fromStack(ps));
                    case OUTPUT -> recipe.output(Port.fromStack(ps));
                    case IGNORE -> {}
                }
            }
        }
    }

    default List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        return List.of();
    }

    default boolean canCraft(final IRecipeHandler handler, final int recipeIndex) {
        return true;
    }

}
