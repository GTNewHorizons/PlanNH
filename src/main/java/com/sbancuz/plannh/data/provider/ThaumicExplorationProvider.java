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
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.setting.Settings;

import codechicken.nei.recipe.IRecipeHandler;
import flaxbeard.thaumicexploration.ThaumicExploration;
import flaxbeard.thaumicexploration.integration.nei.ReplicatorHandler;

public final class ThaumicExplorationProvider implements PropertyProvider {

    private final MachineProfile PROFILE = MachineProfile.builder("tx:replicator")
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
        return ThaumicExploration.replicator == null ? GuiTextures.PROCESSOR
            : new ItemDrawable(ThaumicExploration.replicator);
    }

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(ReplicatorHandler.class, this);

    }

    // TODO: open an upstream PR to expose this copied constant.
    // Thaumic Replicator: base countdown time (ticks) from TileReplicator
    private static final int REPLICATOR_COUNTDOWN = 100;

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        return switch (handler) {
            case ReplicatorHandler _ -> List.of(PROFILE);
            default -> List.of();
        };
    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler, final int recipeIndex) {
        PropertyProvider.super.populate(recipe, handler, recipeIndex);
        recipe.property(RecipePropertyAPI.DURATION_TICKS, REPLICATOR_COUNTDOWN);
    }
}
