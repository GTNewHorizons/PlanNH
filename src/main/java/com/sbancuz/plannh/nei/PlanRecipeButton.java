package com.sbancuz.plannh.nei;

import java.util.List;
import java.util.Map;

import net.minecraft.util.StatCollector;

import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.GuiRecipeButton;
import codechicken.nei.recipe.RecipeHandlerRef;

/** A planner action, not an inventory transfer, for recipes without a usable NEI overlay button. */
final class PlanRecipeButton extends GuiRecipeButton {

    private final Runnable addRecipe;

    private PlanRecipeButton(final RecipeHandlerRef ref, final int x, final int y, final Runnable addRecipe) {
        super(ref, x, y, ref.recipeIndex + 4, "+");
        this.addRecipe = addRecipe;
    }

    static void configure(final List<GuiRecipeButton> buttons, final RecipeHandlerRef ref, final int width,
        final int height, final Runnable addRecipe) {
        final String ident = ref.handler.getOverlayIdentifier();
        for (int i = 0; i < buttons.size(); i++) {
            final GuiRecipeButton button = buttons.get(i);
            if (button instanceof PlanRecipeButton) return;
            if (!(button instanceof GuiOverlayButton overlay)) continue;
            if (ident == null || ident.isEmpty()) {
                buttons.set(i, new PlanRecipeButton(ref, button.xPosition, button.yPosition, addRecipe));
            } else {
                overlay.setRequireShiftForOverlayRecipe(false);
            }
            return;
        }

        final int x = Math.min(166, width) - BUTTON_WIDTH;
        int y = height - BUTTON_HEIGHT - 6;
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
        currenttip.add(StatCollector.translateToLocal("plannh.gui.recipe.add_to_plan"));
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
