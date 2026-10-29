package com.sbancuz.plannh.data.provider;

import java.util.List;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.EffectResult;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.setting.Settings;

import codechicken.nei.recipe.IRecipeHandler;

public final class DefaultProvider implements PropertyProvider {

    public static final DefaultProvider INSTANCE = new DefaultProvider();

    private DefaultProvider() {}

    private IDrawable icon(final IRecipeHandler handler, final int recipeIndex) {
        return GuiTextures.PROCESSOR;
    }

    @Override
    public void register() {}

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        return canCraft(handler, recipeIndex) ? List.of(profile) : List.of();
    }

    public static EffectResult noopEffect(final MachineConfig cfg, final RecipeContext ctx) {
        int d = cfg.getOrDefault(Settings.DURATION_TICKS);
        if (d <= 0) {
            d = ctx.get(RecipePropertyAPI.DURATION_TICKS);
        }
        return new EffectResult(d, 0, cfg.getOrDefault(Settings.MACHINES));
    }

    private final MachineProfile profile = MachineProfile.builder("default")
        .setting(Settings.MACHINES)
        .setting(Settings.TICK_MODIFIER)
        .setting(Settings.DURATION_TICKS)
        .effect(DefaultProvider::noopEffect)
        .icon(this::icon)
        .provider(this)
        .build();
}
