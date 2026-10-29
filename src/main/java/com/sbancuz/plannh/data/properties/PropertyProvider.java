package com.sbancuz.plannh.data.properties;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.sbancuz.plannh.Compat;
import com.sbancuz.plannh.PlanNH;
import com.sbancuz.plannh.data.flowchart.Node;
import com.sbancuz.plannh.data.flowchart.Port;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.FurnaceRecipeHandler;
import codechicken.nei.recipe.IRecipeHandler;
import gregtech.nei.GTNEIDefaultHandler;

public interface PropertyProvider {

    enum StackRole {
        INPUT,
        OUTPUT,
        IGNORE
    }

    void register();

    /** Other stacks can be products, ingredients, or display-only prerequisites, regardless of burn time. */
    default StackRole otherStackRole(final IRecipeHandler handler, final int recipeIndex, final PositionedStack stack) {
        return handler instanceof FurnaceRecipeHandler ? StackRole.IGNORE : StackRole.OUTPUT;
    }

    default Map<RecipeProperty<?>, Object> extract(final Node node, final IRecipeHandler handler,
        final int recipeIndex) {
        final List<PositionedStack> ins = handler.getIngredientStacks(recipeIndex);
        for (final PositionedStack ps : ins) if (ps != null && ps.item != null) node.getInputs()
            .add(Port.fromStack(ps));

        final PositionedStack result = handler.getResultStack(recipeIndex);
        if (result != null && result.item != null) {
            node.getOutputs()
                .add(Port.fromStack(result));
            // warn if we find any handlers outside gt that have multiple output permutations
            if (result.items.length > 1 && (!Compat.GREGTECH.isLoaded || !(handler instanceof GTNEIDefaultHandler)))
                PlanNH.LOG.warn("output found with permutations");
        }

        final List<PositionedStack> others = handler.getOtherStacks(recipeIndex);
        for (final PositionedStack ps : others) {
            if (ps != null && ps.item != null) {
                switch (otherStackRole(handler, recipeIndex, ps)) {
                    case INPUT -> node.getInputs()
                        .add(Port.fromStack(ps));
                    case OUTPUT -> node.getOutputs()
                        .add(Port.fromStack(ps));
                    case IGNORE -> {}
                }
            }
        }

        return Map.of();
    }

    @Nullable
    default String getProfileId(final IRecipeHandler handler, final int recipeIndex) {
        return null;
    }

    default boolean canCraft(final IRecipeHandler handler, final int recipeIndex) {
        return true;
    }

    @Nonnull
    default String getExtractorName() {
        return getClass().getSimpleName();
    }
}
