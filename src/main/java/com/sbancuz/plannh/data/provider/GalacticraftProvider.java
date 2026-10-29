package com.sbancuz.plannh.data.provider;

import java.util.List;

import javax.annotation.Nonnull;

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

import codechicken.nei.recipe.IRecipeHandler;
import micdoodle8.mods.galacticraft.core.blocks.BlockMachine2;
import micdoodle8.mods.galacticraft.core.blocks.GCBlocks;
import micdoodle8.mods.galacticraft.core.nei.CircuitFabricatorRecipeHandler;
import micdoodle8.mods.galacticraft.core.nei.RefineryRecipeHandler;

public final class GalacticraftProvider implements PropertyProvider {

    private static final String PROFILE_ID = "gc:basic";

    private final MachineProfile PROFILE = MachineProfile.builder(PROFILE_ID)
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
        if (GCBlocks.machineBase2 == null || GCBlocks.machineBase == null || GCBlocks.refinery == null)
            return GuiTextures.BUBBLE;
        return handler instanceof CircuitFabricatorRecipeHandler
            ? new ItemDrawable(GCBlocks.machineBase2, BlockMachine2.CIRCUIT_FABRICATOR_METADATA)
            : new ItemDrawable(GCBlocks.refinery);
    }

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(RefineryRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(CircuitFabricatorRecipeHandler.class, this);

    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        return switch (handler) {
            case RefineryRecipeHandler _, CircuitFabricatorRecipeHandler _ -> List.of(PROFILE);
            default -> List.of();
        };
    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler, final int recipeIndex) {
        PropertyProvider.super.populate(recipe, handler, recipeIndex);
    }
}
