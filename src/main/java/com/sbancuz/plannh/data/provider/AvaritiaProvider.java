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
import com.sbancuz.plannh.data.flowchart.resource.LongItemStack;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.properties.RecipeProperty;
import com.sbancuz.plannh.data.properties.SummaryProperty;
import com.sbancuz.plannh.data.setting.Settings;

import codechicken.nei.recipe.IRecipeHandler;
import fox.spiteful.avaritia.blocks.LudicrousBlocks;
import fox.spiteful.avaritia.compat.nei.CompressionHandler;

public final class AvaritiaProvider implements PropertyProvider {

    @Nonnull
    private IDrawable icon(final IRecipeHandler handler, final int recipeIndex) {
        return LudicrousBlocks.compressor == null ? GuiTextures.PROCESSOR
            : new ItemDrawable(LudicrousBlocks.compressor);
    }

    public static final RecipeProperty<Integer> COMPRESSION_COST = SummaryProperty
        .<Integer>builder("avaritia.compression_cost", 0)
        .build();

    private final MachineProfile PROFILE = MachineProfile.builder("avaritia:neutronium_compressor")
        .setting(Settings.MACHINES)
        .setting(Settings.INPUTS_PER_TICK)
        .setting(Settings.TICK_MODIFIER)
        .effect(
            Effects.durationFromTotal(COMPRESSION_COST, Settings.INPUTS_PER_TICK)
                .amortizeCost(COMPRESSION_COST)
                .applyParallelism())
        .provider(this)
        .icon(this::icon)
        .build();

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(CompressionHandler.class, this);

    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        return switch (handler) {
            case CompressionHandler _ -> List.of(PROFILE);
            default -> List.of();
        };
    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler, final int recipeIndex) {
        PropertyProvider.super.populate(recipe, handler, recipeIndex);

        if (handler instanceof CompressionHandler && !recipe.inputs()
            .isEmpty()) {
            final LongItemStack stack = (LongItemStack) recipe.inputs()
                .getFirst()
                .getValue();
            if (stack != null && stack.amount > 0) {
                recipe.property(COMPRESSION_COST, (int) Math.min(stack.amount, Integer.MAX_VALUE));
            }
        }
    }
}
