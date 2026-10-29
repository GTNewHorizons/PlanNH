package com.sbancuz.plannh.data.provider;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.init.Blocks;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.Effects;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.setting.Settings;

import codechicken.nei.recipe.FurnaceRecipeHandler;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.ShapedRecipeHandler;

public class VanillaProvider implements PropertyProvider {

    @Nonnull
    private IDrawable icon(final IRecipeHandler handler, final int recipeIndex) {
        return handler instanceof FurnaceRecipeHandler ? new ItemDrawable(Blocks.furnace)
            : new ItemDrawable(Blocks.crafting_table);
    }

    // Vanilla furnace: base cook time of 200 ticks (10 seconds)
    // Matches net.minecraft.tileentity.TileEntityFurnace.furnaceCookTime
    private static final int FURNACE_COOK_TICKS = 200;

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(FurnaceRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(ShapedRecipeHandler.class, this);
    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        return canCraft(handler, recipeIndex) ? List.of(profile) : List.of();
    }

    @Override
    public boolean canCraft(final IRecipeHandler handler, final int recipeIndex) {
        return handler instanceof ShapedRecipeHandler
            || handler instanceof FurnaceRecipeHandler fh && "smelting".equals(fh.getOverlayIdentifier());
    }

    @Override
    public String getMachineId(final IRecipeHandler handler, final int recipeIndex) {
        if (handler instanceof FurnaceRecipeHandler) return "minecraft:furnace";
        if (handler instanceof ShapedRecipeHandler) return "minecraft:crafting";
        return PropertyProvider.super.getMachineId(handler, recipeIndex);
    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler, final int recipeIndex) {
        PropertyProvider.super.populate(recipe, handler, recipeIndex);
        if (handler instanceof FurnaceRecipeHandler)
            recipe.property(RecipePropertyAPI.DURATION_TICKS, FURNACE_COOK_TICKS);
    }

    private final MachineProfile profile = MachineProfile.builder("minecraft")
        .setting(Settings.MACHINES)
        .setting(Settings.TICK_MODIFIER)
        .effect(
            Effects.durationFromHandler()
                .applyParallelism())
        .icon(this::icon)
        .provider(this)
        .build();
}
