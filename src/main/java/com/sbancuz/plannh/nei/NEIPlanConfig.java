package com.sbancuz.plannh.nei;

import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.sbancuz.plannh.PlanNH;
import com.sbancuz.plannh.Tags;
import com.sbancuz.plannh.gui.FlowchartScreen;

import codechicken.nei.NEIClientUtils;
import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.config.OptionCycled;
import codechicken.nei.config.OptionIntegerField;
import codechicken.nei.config.OptionTextField;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.GuiRecipeButton.UpdateRecipeButtonsEvent;
import codechicken.nei.recipe.RecipeHandlerRef;
import codechicken.nei.recipe.RecipeInfo;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class NEIPlanConfig implements IConfigureNEI {

    public static class ConfigItemColumns {

        public static String KEY = "plannh.item_columns";
        public static int min = 6;
        public static int max = Integer.MAX_VALUE;
        public static int defVal = 9;
    }

    // Hash binds carry their modifiers in the stored int and compare exactly, so every accepted
    // combination needs its own ident.
    public static class ConfigUndoKey {

        public static String KEY = "plannh.undo";
        public static int defVal = Keyboard.KEY_Z + NEIClientUtils.CTRL_HASH;
    }

    public static class ConfigRedoKey {

        public static String KEY = "plannh.redo";
        public static int defVal = Keyboard.KEY_Z + NEIClientUtils.CTRL_HASH + NEIClientUtils.SHIFT_HASH;
    }

    public static class ConfigRedoAltKey {

        public static String KEY = "plannh.redo_alt";
        public static int defVal = Keyboard.KEY_Y + NEIClientUtils.CTRL_HASH;
    }

    public static class ConfigBlurStrength {

        public static String KEY = "plannh.blur_strength";
        public static int min = 0;
        public static int max = 255;
        public static int defVal = 16;
    }

    public static class ConfigShowGrid {

        public static String KEY = "plannh.show_grid";
        public static int OFF = 0;
        public static int ON = 1;
    }

    public static class ConfigBackgroundColor {

        public static String KEY = "plannh.background_color";
    }

    private static final PlanOverlayHandler HANDLER = new PlanOverlayHandler();

    @Override
    public void loadConfig() {
        API.registerNEIGuiHandler(new FlowchartGuiHandler());
        API.addLayoutStyle(0, new FlowchartLayoutStyle());
        MinecraftForge.EVENT_BUS.register(this);
        API.addHashBind(ConfigUndoKey.KEY, ConfigUndoKey.defVal);
        API.addHashBind(ConfigRedoKey.KEY, ConfigRedoKey.defVal);
        API.addHashBind(ConfigRedoAltKey.KEY, ConfigRedoAltKey.defVal);

        API.addOption(new OptionIntegerField(ConfigItemColumns.KEY, ConfigItemColumns.min, ConfigItemColumns.max));
        API.addOption(new OptionIntegerField(ConfigBlurStrength.KEY, ConfigBlurStrength.min, ConfigBlurStrength.max));
        API.addOption(new OptionCycled(ConfigShowGrid.KEY, 2));
        API.addOption(new OptionTextField(ConfigBackgroundColor.KEY));
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
