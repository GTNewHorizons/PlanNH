package com.sbancuz.plannh.data.provider;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.Effects;
import com.sbancuz.plannh.data.effect.steps.CoFHCompat;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.setting.Settings;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;
import forestry.api.recipes.ICentrifugeRecipe;
import forestry.api.recipes.RecipeManagers;
import forestry.factory.blocks.BlockFactoryPlainType;
import forestry.factory.blocks.BlockFactoryTesrType;
import forestry.factory.recipes.nei.NEIHandlerBottler;
import forestry.factory.recipes.nei.NEIHandlerCarpenter;
import forestry.factory.recipes.nei.NEIHandlerCentrifuge;
import forestry.factory.recipes.nei.NEIHandlerCentrifuge.CachedCentrifugeRecipe;
import forestry.factory.recipes.nei.NEIHandlerFabricator;
import forestry.factory.recipes.nei.NEIHandlerFermenter;
import forestry.factory.recipes.nei.NEIHandlerMoistener;
import forestry.factory.recipes.nei.NEIHandlerSqueezer;
import forestry.factory.recipes.nei.NEIHandlerSqueezer.CachedSqueezerRecipe;
import forestry.factory.recipes.nei.NEIHandlerStill;
import forestry.plugins.PluginFactory;

public class ForestryProvider implements PropertyProvider {

    public ForestryProvider() {}

    @Nonnull
    private IDrawable icon(final IRecipeHandler handler,
        final int recipeIndex) {
        if (PluginFactory.blocks == null) return GuiTextures.MATERIAL;
        if (handler instanceof NEIHandlerFabricator) {
            return new ItemDrawable(
                PluginFactory.blocks.factoryPlain,
                BlockFactoryPlainType.FABRICATOR.getMeta());
        }
        final BlockFactoryTesrType type = switch (handler) {
            case NEIHandlerBottler _ -> BlockFactoryTesrType.BOTTLER;
            case NEIHandlerCarpenter _ -> BlockFactoryTesrType.CARPENTER;
            case NEIHandlerCentrifuge _ -> BlockFactoryTesrType.CENTRIFUGE;
            case NEIHandlerFermenter _ -> BlockFactoryTesrType.FERMENTER;
            case NEIHandlerMoistener _ -> BlockFactoryTesrType.MOISTENER;
            case NEIHandlerSqueezer _ -> BlockFactoryTesrType.SQUEEZER;
            case NEIHandlerStill _ -> BlockFactoryTesrType.STILL;
            default -> null;
        };
        return type == null ? GuiTextures.MATERIAL : new ItemDrawable(PluginFactory.blocks.factoryTESR, type.getMeta());
    }

    private static final String PROFILE_ID = "forestry:basic";

    // TODO: open an upstream PR to expose these copied constants.
    private static final int GAME_TICKS_PER_WORK_TICK = 5;

    // TileBottler: TICKS_PER_RECIPE_TIME=5, ENERGY_PER_RECIPE_TIME=1000
    private static final int BOTTLER_TICKS = 5;
    private static final int BOTTLER_RF = 1000;

    // TileCentrifuge: ENERGY_PER_RECIPE_TIME=160
    private static final int CENTRIFUGE_RF_PER_WORK_TICK = 160;

    // TileSqueezer: ENERGY_PER_RECIPE_TIME=200
    private static final int SQUEEZER_RF_PER_WORK_TICK = 200;

    @Override
    public StackRole otherStackRole(final IRecipeHandler handler, final int recipeIndex, final PositionedStack stack) {
        // These show a separate smelting/fuel cycle, not quantities consumed per displayed craft.
        if (handler instanceof NEIHandlerFabricator || handler instanceof NEIHandlerMoistener) return StackRole.IGNORE;
        return PropertyProvider.super.otherStackRole(handler, recipeIndex, stack);
    }

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(NEIHandlerBottler.class, this);
        RecipePropertyAPI.registerExtractor(NEIHandlerCarpenter.class, this);
        RecipePropertyAPI.registerExtractor(NEIHandlerCentrifuge.class, this);
        RecipePropertyAPI.registerExtractor(NEIHandlerFabricator.class, this);
        RecipePropertyAPI.registerExtractor(NEIHandlerFermenter.class, this);
        RecipePropertyAPI.registerExtractor(NEIHandlerMoistener.class, this);
        RecipePropertyAPI.registerExtractor(NEIHandlerSqueezer.class, this);
        RecipePropertyAPI.registerExtractor(NEIHandlerStill.class, this);
    }

    @Override
    public boolean canCraft(final IRecipeHandler handler, final int recipeIndex) {
        if (handler == null) return false;
        return switch (handler) {
            case NEIHandlerBottler _, NEIHandlerCarpenter _, NEIHandlerCentrifuge _,
                 NEIHandlerFabricator _, NEIHandlerFermenter _, NEIHandlerMoistener _,
                 NEIHandlerSqueezer _, NEIHandlerStill _ -> true;
            default -> false;
        };
    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        return canCraft(handler, recipeIndex) ? List.of(profile) : List.of();
    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler,
        final int recipeIndex) {
        PropertyProvider.super.populate(recipe, handler, recipeIndex);
        if (!(handler instanceof final TemplateRecipeHandler trh)) return;

        final List<TemplateRecipeHandler.CachedRecipe> recipes = trh.arecipes;
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) return;

        final TemplateRecipeHandler.CachedRecipe cached = recipes.get(recipeIndex);

        switch (handler) {
            case NEIHandlerBottler _ -> {
                recipe.property(RecipePropertyAPI.DURATION_TICKS, BOTTLER_TICKS);
                recipe.property(CoFHCompat.RF_COST, (long) BOTTLER_RF);
            }
            case NEIHandlerCentrifuge _ when cached instanceof final CachedCentrifugeRecipe c ->
                extractCentrifuge(recipe, c);
            case NEIHandlerSqueezer _ when cached instanceof final CachedSqueezerRecipe s ->
                extractSqueezer(recipe, s);
            default -> {}
        }
    }

    private static void extractSqueezer(final RecipeContext.Builder recipe, final CachedSqueezerRecipe s) {
        if (s.processingTime <= 0) return;
        recipe.property(RecipePropertyAPI.DURATION_TICKS, s.processingTime * GAME_TICKS_PER_WORK_TICK);
        recipe.property(CoFHCompat.RF_COST, (long) s.processingTime * SQUEEZER_RF_PER_WORK_TICK);
    }

    private static void extractCentrifuge(final RecipeContext.Builder recipe, final CachedCentrifugeRecipe c) {
        final int workTicks = lookupCentrifugeTime(c.inputs.item);
        if (workTicks <= 0) return;
        recipe.property(RecipePropertyAPI.DURATION_TICKS, workTicks * GAME_TICKS_PER_WORK_TICK);
        recipe.property(CoFHCompat.RF_COST, (long) workTicks * CENTRIFUGE_RF_PER_WORK_TICK);
    }

    private static int lookupCentrifugeTime(final @Nullable ItemStack input) {
        if (input == null) return 0;
        for (final ICentrifugeRecipe r : RecipeManagers.centrifugeManager.recipes()) {
            if (r.getInput() != null && r.getInput()
                .isItemEqual(input)) {
                return r.getProcessingTime();
            }
        }
        return 0;
    }

    private final MachineProfile profile = MachineProfile.builder(PROFILE_ID)
        .setting(Settings.MACHINES)
        .setting(Settings.TICK_MODIFIER)
        .effect(
            Effects.durationFromHandler()
                .amortizeCost(CoFHCompat.RF_COST)
                .applyParallelism())
        .icon(this::icon)
        .provider(this)
        .build();
}
