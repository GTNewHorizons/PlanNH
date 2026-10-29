package com.sbancuz.plannh.data.provider;

import java.util.List;

import javax.annotation.Nonnull;

import com.brandon3055.draconicevolution.integration.nei.ReactorNEIHandler;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.Effects;
import com.sbancuz.plannh.data.effect.steps.CoFHCompat;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.setting.Settings;

import codechicken.nei.recipe.IRecipeHandler;

public final class DraconicEvolutionProvider implements PropertyProvider {

    private final MachineProfile PROFILE = MachineProfile.builder("draconicevolution:fusion_crafter")
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
        return GuiTextures.SPHERE;
    }

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(ReactorNEIHandler.class, this);

    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        return switch (handler) {
            case ReactorNEIHandler _ -> List.of(PROFILE);
            default -> List.of();
        };
    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler, final int recipeIndex) {
        PropertyProvider.super.populate(recipe, handler, recipeIndex);
    }
}
