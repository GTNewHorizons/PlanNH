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
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.properties.SummaryProperty;
import com.sbancuz.plannh.data.setting.Settings;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.FurnaceRecipeHandler;
import codechicken.nei.recipe.IRecipeHandler;
import mrtjp.projectred.ProjectRedExpansion$;
import mrtjp.projectred.expansion.InductiveFurnaceRecipe;
import mrtjp.projectred.expansion.InductiveFurnaceRecipeLib$;

/** ProjectRed Expansion's registry-backed Inductive Furnace overlay. */
public final class ProjectRedProvider implements PropertyProvider {

    // TODO: open an upstream PR to expose this copied machine constant.
    private static final int POWER_UNITS_PER_TICK = 1100;

    static final SummaryProperty<Number> POWER_UNITS_PER_TICK_PROPERTY = SummaryProperty
        .<Number>builder("projectred.power_units_per_t", 0)
        .perSec(true)
        .build();

    private final MachineProfile profile = MachineProfile.builder("projectred:inductive_furnace")
        .setting(Settings.MACHINES)
        .setting(Settings.TICK_MODIFIER)
        .effect(
            Effects.durationFromHandler()
                .withCostPerT(POWER_UNITS_PER_TICK_PROPERTY, (result, cfg, ctx) -> POWER_UNITS_PER_TICK)
                .applyParallelism())
        .provider(this)
        .icon(this::icon)
        .build();

    @Nonnull
    private IDrawable icon(final IRecipeHandler handler, final int recipeIndex) {
        return ProjectRedExpansion$.MODULE$.machine1() == null ? GuiTextures.PROCESSOR
            : new ItemDrawable(ProjectRedExpansion$.MODULE$.machine1(), 0);
    }

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(FurnaceRecipeHandler.class, this);
    }

    @Override
    public boolean canCraft(final IRecipeHandler handler, final int recipeIndex) {
        return matchingRecipe(handler, recipeIndex) != null;
    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        final InductiveFurnaceRecipe recipe = matchingRecipe(handler, recipeIndex);
        return recipe == null ? List.of() : List.of(profile);
    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler, final int recipeIndex) {
        PropertyProvider.super.populate(recipe, handler, recipeIndex);
        final InductiveFurnaceRecipe inductive = matchingRecipe(handler, recipeIndex);
        if (inductive != null) recipe.property(RecipePropertyAPI.DURATION_TICKS, inductive.burnTime());
    }

    @Nullable
    static InductiveFurnaceRecipe matchingRecipe(final IRecipeHandler handler, final int recipeIndex) {
        if (!(handler instanceof FurnaceRecipeHandler furnace) || !"smelting".equals(furnace.getOverlayIdentifier()))
            return null;

        final List<PositionedStack> ingredients;
        final PositionedStack output;
        try {
            ingredients = handler.getIngredientStacks(recipeIndex);
            output = handler.getResultStack(recipeIndex);
        } catch (final RuntimeException ignored) {
            return null;
        }
        if (ingredients.size() != 1 || output == null || output.item == null) return null;
        if (output.items != null) {
            for (final ItemStack result : output.items) {
                if (result == null || !ItemStack.areItemStacksEqual(result, output.item)) return null;
            }
        }

        final PositionedStack input = ingredients.getFirst();
        final ItemStack[] candidates = input.items == null ? new ItemStack[] { input.item } : input.items;
        if (candidates.length == 0) return null;

        InductiveFurnaceRecipe selected = null;
        for (final ItemStack candidate : candidates) {
            if (candidate == null || candidate.getItem() == null) return null;
            final InductiveFurnaceRecipe recipe = InductiveFurnaceRecipeLib$.MODULE$.getRecipeFor(candidate);
            if (recipe == null || !ItemStack.areItemStacksEqual(recipe.createOutput(), output.item)) return null;
            if (selected == null) {
                selected = recipe;
            } else if (selected.burnTime() != recipe.burnTime()
                || !ItemStack.areItemStacksEqual(selected.createOutput(), recipe.createOutput())) {
                    return null;
                }
        }
        return selected;
    }
}
