package com.sbancuz.plannh.nei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;

import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.sbancuz.plannh.PlanNH;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.gui.CanvasWidget;
import com.sbancuz.plannh.gui.FlowchartScreen;

import codechicken.nei.PositionedStack;
import codechicken.nei.api.IOverlayHandler;
import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.Recipe;
import codechicken.nei.recipe.RecipeHandlerRef;

public class PlanOverlayHandler implements IOverlayHandler {

    @Override
    public void overlayRecipe(final GuiContainer firstGui, final IRecipeHandler recipe, final int recipeIndex,
        final boolean maxTransfer) {
        addRecipe(firstGui, recipe, recipeIndex);
    }

    @Override
    public int transferRecipe(final GuiContainer firstGui, final IRecipeHandler recipe, final int recipeIndex,
        final int multiplier) {
        addRecipe(firstGui, recipe, recipeIndex);
        return 0;
    }

    @Override
    public boolean canCraft(final GuiContainer firstGui, final IRecipeHandler handler, final int recipeIndex) {
        if (!canResolve(handler, recipeIndex)) return false;
        for (final PropertyProvider p : RecipePropertyAPI.getExtractors(handler.getClass())) {
            if (p.canCraft(handler, recipeIndex)) return true;
        }
        return false;
    }

    @Override
    public boolean craft(final GuiContainer firstGui, final IRecipeHandler handler, final int recipeIndex,
        final int multiplier) {
        return canCraft(firstGui, handler, recipeIndex);
    }

    /** Only offer recipes that NEI can reconstruct when the saved node is loaded. */
    public static boolean canResolve(final IRecipeHandler handler, final int recipeIndex) {
        return RecipeHandlerRef.of(Recipe.RecipeId.of(handler, recipeIndex)) != null;
    }

    private static void addRecipe(final GuiContainer firstGui, final IRecipeHandler handler, final int recipeIndex) {
        if (!canResolve(handler, recipeIndex)) {
            PlanNH.LOG.warn("Cannot add unresolved recipe: handler={}, recipeIndex={}", handler.getHandlerId(), recipeIndex);
            return;
        }
        if (firstGui instanceof final GuiContainerWrapper wrapper
            && wrapper.getScreen() instanceof final FlowchartScreen screen) {
            CanvasWidget canvas =  screen.getCanvas();
            canvas.addNode(canvas.getCanvasScreenCenterX(), canvas.getCanvasScreenCenterY(), handler, recipeIndex);
        }
    }

    @Override
    public List<GuiOverlayButton.ItemOverlayState> presenceOverlay(final GuiContainer firstGui,
        final IRecipeHandler recipe, final int recipeIndex) {
        final List<GuiOverlayButton.ItemOverlayState> itemPresenceSlots = new ArrayList<>();
        final List<PositionedStack> ingredients = recipe.getIngredientStacks(recipeIndex);

        for (final PositionedStack stack : ingredients) {
            itemPresenceSlots.add(new GuiOverlayButton.ItemOverlayState(stack, true));
        }

        return itemPresenceSlots;
    }
}
