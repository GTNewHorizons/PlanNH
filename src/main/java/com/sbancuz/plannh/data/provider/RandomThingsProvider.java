package com.sbancuz.plannh.data.provider;

import java.util.List;

import javax.annotation.Nonnull;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.Effects;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.setting.Settings;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import lumien.randomthings.Handler.ModCompability.NEI.ImbuingStationRecipeHandler;

public final class RandomThingsProvider implements PropertyProvider {

    private final MachineProfile PROFILE = MachineProfile.builder("randomthings:imbuing_station")
        .setting(Settings.MACHINES)
        .setting(Settings.TICK_MODIFIER)
        .effect(
            Effects.clearCost()
                .applyParallelism())
        .provider(this)
        .icon(this::icon)
        .build();

    @Nonnull
    private IDrawable icon(final IRecipeHandler handler, final int recipeIndex) {
        return GuiTextures.SPHERE;
    }

    // TODO: Make a PR to expose this constant
    // Imbuing Station: base process length (ticks) from TileEntityImbuingStation
    private static final int IMBUING_LENGTH = 200;

    @Override
    public StackRole otherStackRole(final IRecipeHandler handler, final int recipeIndex, final PositionedStack stack) {
        return handler instanceof ImbuingStationRecipeHandler ? StackRole.INPUT
            : PropertyProvider.super.otherStackRole(handler, recipeIndex, stack);
    }

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(ImbuingStationRecipeHandler.class, this);

    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        return switch (handler) {
            case ImbuingStationRecipeHandler _ -> List.of(PROFILE);
            default -> List.of();
        };
    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler, final int recipeIndex) {
        PropertyProvider.super.populate(recipe, handler, recipeIndex);
        recipe.property(RecipePropertyAPI.DURATION_TICKS, IMBUING_LENGTH);
    }
}
