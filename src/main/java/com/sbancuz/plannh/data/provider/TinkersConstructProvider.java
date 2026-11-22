package com.sbancuz.plannh.data.provider;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

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
import codechicken.nei.recipe.StackInfo;
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

        // TC lists the metal tank first, then the cast only when the recipe has one.
        final List<PositionedStack> ingredients = casting.getIngredients();
        if (!(ingredients.getFirst() instanceof final PositionedStack.Tank metal)) return;
        final FluidStack metalFluid = StackInfo.getFluid(metal.item);
        if (metalFluid == null) return;
        final List<PositionedStack> casts = ingredients.subList(1, ingredients.size());
        final ItemStack castItem = casts.isEmpty() ? null : casts.getFirst().item;

        // NEI's fluid display stack keeps the long amount under Count.
        final long metalAmount = StackInfo.itemStackToNBT(metal.item)
            .getLong("Count");
        final PositionedStack result = casting.getResult();

        // Match the complete backing recipe, not merely its output item.
        for (final CastingRecipe candidate : handler.getCastingRecipes()) {
            if (!ItemStack.areItemStacksEqual(candidate.output, result.item)) continue;
            if (candidate.castingMetal.amount != metalAmount || !candidate.matches(metalFluid, castItem)) continue;

            if (metalAmount > 0) recipe.input(Port.fluid(candidate.castingMetal, metalAmount, 1f, metal.copy()));
            // Molds are reused, so they never count as consumed.
            for (final PositionedStack cast : casts) {
                final var castPort = Port.itemPort(cast);
                castPort.getValue().amount = 0;
                recipe.input(castPort);
            }
            recipe.output(Port.itemPort(result));
            if (candidate.coolTime > 0) recipe.property(RecipePropertyAPI.DURATION_TICKS, candidate.coolTime);
            return;
        }
    }
}
