package com.sbancuz.plannh.nei;

import java.util.List;
import java.util.Map;

import net.minecraft.util.StatCollector;

import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.GuiRecipeButton;
import codechicken.nei.recipe.GuiRecipeButton.UpdateRecipeButtonsEvent;
import codechicken.nei.recipe.RecipeHandlerRef;

/** {@code PlanRecipeButton} adds a recipe to the plan. */
final class PlanRecipeButton extends GuiRecipeButton {

    private final Runnable addRecipe;

    private PlanRecipeButton(final RecipeHandlerRef ref, final int x, final int y, final Runnable addRecipe) {
        super(ref, x, y, ref.recipeIndex + 4, "+");
        this.addRecipe = addRecipe;
    }

    /**
     * Replace the NEI overlay button in {@code event.buttonList} with a {@code PlanRecipeButton}, disabled unless
     * {@code canAdd}. When the list doesn't have an overlay button and {@code canAdd} is true, add a
     * {@code PlanRecipeButton} at the lowest free position on the right edge.
     */
    public static void configure(final UpdateRecipeButtonsEvent.Post event, final boolean canAdd,
        final Runnable addRecipe) {
        final List<GuiRecipeButton> buttons = event.buttonList;
        final RecipeHandlerRef ref = event.handlerRef;

        for (int i = 0; i < buttons.size(); i++) {
            final GuiRecipeButton button = buttons.get(i);
            if (button instanceof PlanRecipeButton) return;
            if (!(button instanceof GuiOverlayButton)) continue;

            final var replacement = new PlanRecipeButton(ref, button.xPosition, button.yPosition, addRecipe);
            replacement.enabled = canAdd;
            buttons.set(i, replacement);
            return;
        }
        if (!canAdd) return;

        final int x = Math.min(166, event.width) - BUTTON_WIDTH;
        int y = event.height - BUTTON_HEIGHT - 6;
        while (true) {
            final int candidateY = y;
            if (buttons.stream()
                .noneMatch(
                    button -> button.xPosition < x + BUTTON_WIDTH && button.xPosition + button.width > x
                        && button.yPosition < candidateY + BUTTON_HEIGHT
                        && button.yPosition + button.height > candidateY))
                break;
            y -= BUTTON_HEIGHT + 1;
        }
        buttons.add(new PlanRecipeButton(ref, x, y, addRecipe));
    }

    @Override
    public void mouseReleased(final int mouseX, final int mouseY) {
        if (enabled) addRecipe.run();
    }

    @Override
    public List<String> handleTooltip(final List<String> currenttip) {
        final String key = enabled ? "plannh.gui.recipe.add_to_plan" : "plannh.gui.recipe.cannot_add_to_plan";
        currenttip.add(StatCollector.translateToLocal(key));
        return currenttip;
    }

    @Override
    public Map<String, String> handleHotkeys(final int mouseX, final int mouseY, final Map<String, String> hotkeys) {
        return hotkeys;
    }

    @Override
    public void lastKeyTyped(final char keyChar, final int keyID) {}

    @Override
    public void drawItemOverlay() {}
}
