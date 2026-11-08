package com.sbancuz.plannh.nei;

import net.minecraftforge.common.MinecraftForge;

import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.sbancuz.plannh.PlanNH;
import com.sbancuz.plannh.Tags;
import com.sbancuz.plannh.gui.FlowchartScreen;
import com.sbancuz.plannh.nei.config.NEISettings;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.GuiRecipeButton.UpdateRecipeButtonsEvent;
import codechicken.nei.recipe.RecipeHandlerRef;
import codechicken.nei.recipe.RecipeInfo;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class NEIPlanConfig implements IConfigureNEI {

    private static final PlanOverlayHandler HANDLER = new PlanOverlayHandler();

    @Override
    public void loadConfig() {
        API.registerNEIGuiHandler(new FlowchartGuiHandler());
        API.addLayoutStyle(0, new FlowchartLayoutStyle());
        MinecraftForge.EVENT_BUS.register(this);

        NEISettings.register();
    }

    @SubscribeEvent
    public void onPreButtonUpdate(final UpdateRecipeButtonsEvent.Pre event) {
        final GuiRecipe<?> gui = (GuiRecipe<?>) event.gui;
        if (!(gui.firstGui instanceof final GuiContainerWrapper wrapper
            && wrapper.getScreen() instanceof FlowchartScreen)) return;

        final RecipeHandlerRef ref = event.handlerRef;
        if (!PlanOverlayHandler.canResolve(ref.handler, ref.recipeIndex)) return;

        final String ident = ref.handler.getOverlayIdentifier();
        if (ident != null && !ident.isEmpty()
            && !RecipeInfo.hasOverlayHandler(GuiContainerWrapper.class, ident)) {
            API.registerGuiOverlayHandler(GuiContainerWrapper.class, HANDLER, ident);
        }
    }

    @SubscribeEvent
    public void onPostButtonUpdate(final UpdateRecipeButtonsEvent.Post event) {
        final GuiRecipe<?> gui = (GuiRecipe<?>) event.gui;
        if (!(gui.firstGui instanceof final GuiContainerWrapper wrapper
            && wrapper.getScreen() instanceof FlowchartScreen)) return;
        final RecipeHandlerRef ref = event.handlerRef;
        if (!HANDLER.canCraft(wrapper, ref.handler, ref.recipeIndex)) return;

        // Informational handlers can omit an overlay identifier or suppress the default '+' entirely.
        PlanRecipeButton.configure(event.buttonList, ref, event.width, event.height, () -> {
            wrapper.mc.displayGuiScreen(wrapper);
            HANDLER.overlayRecipe(wrapper, ref.handler, ref.recipeIndex, false);
        });
    }

    @Override
    public String getName() {
        return PlanNH.MODID;
    }

    @Override
    public String getVersion() {
        return Tags.VERSION;
    }
}
