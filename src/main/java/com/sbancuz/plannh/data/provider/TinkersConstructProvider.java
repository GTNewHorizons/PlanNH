package com.sbancuz.plannh.data.provider;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.Effects;
import com.sbancuz.plannh.data.flowchart.Port;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.setting.Settings;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;
import tconstruct.armor.TinkerArmor;
import tconstruct.library.crafting.CastingRecipe;
import tconstruct.plugins.nei.RecipeHandlerAlloying;
import tconstruct.plugins.nei.RecipeHandlerCastingBase;
import tconstruct.plugins.nei.RecipeHandlerCastingBase.CachedCastingRecipe;
import tconstruct.plugins.nei.RecipeHandlerCastingBasin;
import tconstruct.plugins.nei.RecipeHandlerCastingTable;
import tconstruct.plugins.nei.RecipeHandlerDryingRack;
import tconstruct.plugins.nei.RecipeHandlerDryingRack.CachedDryingRackRecipe;
import tconstruct.plugins.nei.RecipeHandlerMelting;
import tconstruct.plugins.nei.RecipeHandlerToolMaterials;
import tconstruct.smeltery.TinkerSmeltery;
import tconstruct.tools.TinkerTools;

public final class TinkersConstructProvider implements PropertyProvider {

    private final MachineProfile PROFILE = MachineProfile.builder("tconstruct:basic")
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
        switch (handler) {
            case RecipeHandlerDryingRack recipeHandlerDryingRack -> {
                return TinkerArmor.dryingRack == null ? GuiTextures.PROCESSOR : new ItemDrawable(TinkerArmor.dryingRack);
            }
            case RecipeHandlerToolMaterials recipeHandlerToolMaterials -> {
                return TinkerTools.toolForge == null ? GuiTextures.PROCESSOR : new ItemDrawable(TinkerTools.toolForge);
            }
            case RecipeHandlerCastingTable recipeHandlerCastingTable -> {
                return TinkerSmeltery.searedBlock == null ? GuiTextures.PROCESSOR
                    : new ItemDrawable(TinkerSmeltery.searedBlock, 0);
            }
            case RecipeHandlerCastingBasin recipeHandlerCastingBasin -> {
                return TinkerSmeltery.searedBlock == null ? GuiTextures.PROCESSOR
                    : new ItemDrawable(TinkerSmeltery.searedBlock, 2);
            }
            default -> {
            }
        }
        if (TinkerSmeltery.smeltery == null) return GuiTextures.PROCESSOR;
        return new ItemDrawable(TinkerSmeltery.smeltery, 0);
    }

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(RecipeHandlerAlloying.class, this);
        RecipePropertyAPI.registerExtractor(RecipeHandlerCastingBasin.class, this);
        RecipePropertyAPI.registerExtractor(RecipeHandlerCastingTable.class, this);
        RecipePropertyAPI.registerExtractor(RecipeHandlerDryingRack.class, this);
        RecipePropertyAPI.registerExtractor(RecipeHandlerMelting.class, this);
        RecipePropertyAPI.registerExtractor(RecipeHandlerToolMaterials.class, this);

    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        return switch (handler) {
            case RecipeHandlerAlloying _, RecipeHandlerCastingBasin _, RecipeHandlerCastingTable _,
                 RecipeHandlerDryingRack _, RecipeHandlerMelting _, RecipeHandlerToolMaterials _ -> List.of(PROFILE);
            default -> List.of();
        };
    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler,
        final int recipeIndex) {
        PropertyProvider.super.populate(recipe, handler, recipeIndex);
        if (!(handler instanceof final TemplateRecipeHandler trh)) return;

        final List<TemplateRecipeHandler.CachedRecipe> recipes = trh.arecipes;
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) return;

        final TemplateRecipeHandler.CachedRecipe cached = recipes.get(recipeIndex);

        if (cached instanceof final CachedDryingRackRecipe r && r.time > 0) {
            recipe.property(RecipePropertyAPI.DURATION_TICKS, r.time);
        }

        if (handler instanceof final RecipeHandlerCastingBase cb) {
            extractCasting(recipe, cb, cached);
        }
    }

    private static void extractCasting(final RecipeContext.Builder recipe, final RecipeHandlerCastingBase handler,
        final TemplateRecipeHandler.CachedRecipe cached) {
        recipe.inputs().clear();
        recipe.outputs().clear();
        if (!(cached instanceof final CachedCastingRecipe casting)) return;

        final PositionedStack result = casting.getResult();
        final CastingRecipe match = findCastingRecipe(handler, casting, result);
        if (match == null) return;

        if (match.castingMetal.amount > 0) {
            final var position = casting.getFluidTanks().getFirst().position;
            recipe.input(Port.fluid(match.castingMetal, 1f, position.x, position.y));
        }
        if (match.cast != null) {
            final var cast = Port.itemPort(casting.getIngredients().getFirst());
            if (!match.consumeCast) cast.getValue().amount = 0;
            recipe.input(cast);
        }
        recipe.output(Port.itemPort(result));
        if (match.coolTime > 0) recipe.property(RecipePropertyAPI.DURATION_TICKS, match.coolTime);
    }

    /** Match the cached recipe's complete backing recipe, not merely its output item. */
    private static CastingRecipe findCastingRecipe(final RecipeHandlerCastingBase handler,
        final CachedCastingRecipe cached, final PositionedStack result) {
        final List<PositionedStack> ingredients = cached.getIngredients();
        final ItemStack cast = ingredients.isEmpty() ? null : ingredients.getFirst().item;
        final var cachedMetal = cached.getFluidTanks()
            .getFirst().fluid;
        for (final CastingRecipe candidate : handler.getCastingRecipes()) {
            if (!ItemStack.areItemStacksEqual(candidate.output, result.item)) continue;
            if (candidate.castingMetal.amount != cachedMetal.amount || !candidate.matches(cachedMetal, cast)) continue;
            return candidate;
        }
        return null;
    }
}
