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
import thaumic.tinkerer.client.nei.NEINecromancyHandler;
import thaumic.tinkerer.common.ThaumicTinkerer;
import thaumic.tinkerer.common.item.ItemSoulMould;

public final class ThaumicTinkererProvider implements PropertyProvider {

    private final MachineProfile PROFILE = MachineProfile.builder("tt:necromancy_soul_mould")
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
        final var item = ThaumicTinkerer.registry == null ? null
            : ThaumicTinkerer.registry.getFirstItemFromClass(ItemSoulMould.class);
        return item == null ? GuiTextures.PROCESSOR : new ItemDrawable(item);
    }

    // TODO: Open an upstream PR to expose the Soul Mould scan interval from TileSoulMould.
    private static final int SCAN_INTERVAL = 300;

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(NEINecromancyHandler.class, this);

    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        return switch (handler) {
            case NEINecromancyHandler _ -> List.of(PROFILE);
            default -> List.of();
        };
    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler, final int recipeIndex) {
        PropertyProvider.super.populate(recipe, handler, recipeIndex);
        recipe.property(RecipePropertyAPI.DURATION_TICKS, SCAN_INTERVAL);
    }
}
