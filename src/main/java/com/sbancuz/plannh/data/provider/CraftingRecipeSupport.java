package com.sbancuz.plannh.data.provider;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.item.ItemStack;

import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.flowchart.Port;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.ShapedRecipeHandler;

/** Shared validation for machines that execute a 3x3 crafting grid. */
record CraftingRecipeSupport(IRecipeHandler handler, int recipeIndex) {

    private static final String AE_HANDLER_SHAPED = "appeng.integration.modules.NEIHelpers.NEIAEShapedRecipeHandler";
    private static final String AE_HANDLER_SHAPELESS = "appeng.integration.modules.NEIHelpers.NEIAEShapelessRecipeHandler";

    boolean isEligible() {
        if (handler == null) return false;
        final String name = handler.getClass()
            .getName();
        if (!(handler instanceof ShapedRecipeHandler) && !AE_HANDLER_SHAPED.equals(name)
            && !AE_HANDLER_SHAPELESS.equals(name)) return false;

        try {
            final PositionedStack result = handler.getResultStack(recipeIndex);
            final List<PositionedStack> ingredients = handler.getIngredientStacks(recipeIndex);
            if (result == null || ingredients.isEmpty() || ingredients.size() > 9) return false;

            if (!hasUsableStack(result)) return false;

            final Set<Integer> slots = new HashSet<>();
            for (final PositionedStack ingredient : ingredients) {
                if (!hasUsableStack(ingredient)) return false;

                final int x = (ingredient.relx - 25) / 18;
                final int y = (ingredient.rely - 6) / 18;
                if (x < 0 || x >= 3
                    || y < 0
                    || y >= 3
                    || ingredient.relx != 25 + x * 18
                    || ingredient.rely != 6 + y * 18
                    || !slots.add(y * 3 + x)) return false;
            }
            return true;
        } catch (final RuntimeException ignored) {
            // A handler with an unavailable or malformed cached recipe is not usable here.
        }
        return false;
    }

    /** Select the first cached permutation when a handler did not select one for rendering. */
    void selectMissingPermutations() {
        final PositionedStack result = handler.getResultStack(recipeIndex);
        selectMissingPermutation(result);
        for (final PositionedStack ingredient : handler.getIngredientStacks(recipeIndex)) {
            selectMissingPermutation(ingredient);
        }
    }

    void addContainerReturns(final RecipeContext.Builder recipe) {
        int index = 0;
        for (final PositionedStack ingredient : handler.getIngredientStacks(recipeIndex)) {
            if (ingredient == null || !isUsable(ingredient.item)) continue;
            final ItemStack consumed = ingredient.item.copy();
            consumed.stackSize = 1;
            if (!consumed.getItem()
                .hasContainerItem(consumed)) continue;
            final ItemStack returned = consumed.getItem()
                .getContainerItem(consumed);
            if (!isUsable(returned) || returned.stackSize <= 0) continue;
            recipe.output(Port.itemPort(new PositionedStack(returned, 119 + index % 3 * 18, 46 + index / 3 * 18)));
            index++;
        }
    }

    private static boolean hasUsableStack(final PositionedStack stack) {
        if (stack == null) return false;
        if (isUsable(stack.item)) return true;
        if (stack.items == null) return false;
        for (final ItemStack permutation : stack.items) {
            if (isUsable(permutation)) return true;
        }
        return false;
    }

    private static void selectMissingPermutation(final PositionedStack stack) {
        if (stack == null || isUsable(stack.item) || stack.items == null) return;
        for (int index = 0; index < stack.items.length; index++) {
            final ItemStack permutation = stack.items[index];
            if (isUsable(permutation)) {
                stack.setPermutationToRender(index);
                return;
            }
        }
    }

    private static boolean isUsable(final ItemStack stack) {
        return stack != null && stack.getItem() != null;
    }
}
