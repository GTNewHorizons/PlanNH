package com.sbancuz.plannh.data.provider;

import java.util.List;

import javax.annotation.Nonnull;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.RecipeHandlerAccess;
import com.sbancuz.plannh.data.effect.Effects;
import com.sbancuz.plannh.data.effect.steps.CoFHCompat;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.setting.Settings;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;
import tonius.neiintegration.mods.railcraft.RecipeHandlerBlastFurnace;
import tonius.neiintegration.mods.railcraft.RecipeHandlerBlastFurnace.CachedBlastFurnaceRecipe;
import tonius.neiintegration.mods.railcraft.RecipeHandlerCokeOven;
import tonius.neiintegration.mods.railcraft.RecipeHandlerCokeOven.CachedCokeOvenRecipe;
import tonius.neiintegration.mods.railcraft.RecipeHandlerRockCrusher;
import tonius.neiintegration.mods.railcraft.RecipeHandlerRollingMachine;

// TODO: Make a PR to expose these values from NEI cached recipes
// Rock Crusher and Rolling Machine have no time/energy in their cached recipe classes
public class RailcraftProvider implements PropertyProvider {

    private final MachineProfile PROFILE = MachineProfile.builder("railcraft")
        .setting(Settings.MACHINES)
        .setting(Settings.TICK_MODIFIER)
        .effect(
            Effects.durationFromHandler()
                .amortizeCost(CoFHCompat.RF_COST)
                .applyParallelism())
        .provider(this)
        .icon(this::icon)
        .build();

    @Nonnull
    private IDrawable icon(final IRecipeHandler handler, final int recipeIndex) {
        return GuiTextures.LEFTLOAD;
    }

    @Override
    public StackRole otherStackRole(final IRecipeHandler handler, final int recipeIndex, final PositionedStack stack) {
        return handler instanceof RecipeHandlerBlastFurnace ? StackRole.IGNORE
            : PropertyProvider.super.otherStackRole(handler, recipeIndex, stack);
    }

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(RecipeHandlerCokeOven.class, this);
        RecipePropertyAPI.registerExtractor(RecipeHandlerBlastFurnace.class, this);
        RecipePropertyAPI.registerExtractor(RecipeHandlerRockCrusher.class, this);
        RecipePropertyAPI.registerExtractor(RecipeHandlerRollingMachine.class, this);

    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler,
        final int recipeIndex) {
        PropertyProvider.super.populate(recipe, handler, recipeIndex);
        if (!(handler instanceof final TemplateRecipeHandler trh)) return;

        final List<TemplateRecipeHandler.CachedRecipe> recipes = RecipeHandlerAccess.getArecipes(trh);
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) return;

        final var cached = recipes.get(recipeIndex);

        if (handler instanceof final RecipeHandlerCokeOven rco) {
            final var r = (CachedCokeOvenRecipe) cached;
            if (r.cookTime > 0) {
                recipe.property(RecipePropertyAPI.DURATION_TICKS, r.cookTime);
            }
        } else if (handler instanceof final RecipeHandlerBlastFurnace rbf) {
            final var r = (CachedBlastFurnaceRecipe) cached;
            if (r.cookTime > 0) {
                recipe.property(RecipePropertyAPI.DURATION_TICKS, r.cookTime);
            }
        }
        // Rock Crusher and Rolling Machine: no time/RF in cached recipe — values would be estimates

    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        return switch (handler) {
            case RecipeHandlerCokeOven _, RecipeHandlerBlastFurnace _,
                 RecipeHandlerRockCrusher _, RecipeHandlerRollingMachine _ -> List.of(PROFILE);
            default -> List.of();
        };
    }
}
