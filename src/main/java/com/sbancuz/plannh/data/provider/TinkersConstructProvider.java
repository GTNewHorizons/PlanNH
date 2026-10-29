package com.sbancuz.plannh.data.provider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.MachineProfileRegistry;
import com.sbancuz.plannh.data.RecipeHandlerAccess;
import com.sbancuz.plannh.data.effect.Effects;
import com.sbancuz.plannh.data.flowchart.Node;
import com.sbancuz.plannh.data.flowchart.Port;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.properties.RecipeProperty;
import com.sbancuz.plannh.data.setting.Settings;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;
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

public final class TinkersConstructProvider implements PropertyProvider {

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(RecipeHandlerAlloying.class, this);
        RecipePropertyAPI.registerExtractor(RecipeHandlerCastingBasin.class, this);
        RecipePropertyAPI.registerExtractor(RecipeHandlerCastingTable.class, this);
        RecipePropertyAPI.registerExtractor(RecipeHandlerDryingRack.class, this);
        RecipePropertyAPI.registerExtractor(RecipeHandlerMelting.class, this);
        RecipePropertyAPI.registerExtractor(RecipeHandlerToolMaterials.class, this);

        MachineProfileRegistry.register(
            MachineProfile.builder("tconstruct:basic", "Tinkers' Construct")
                .setting(Settings.MACHINES)
                .setting(Settings.TICK_MODIFIER)
                .effect(
                    Effects.clearCost()
                        .applyParallelism())
                .build());
    }

    @Override
    public boolean canCraft(final IRecipeHandler handler, final int recipeIndex) {
        return getProfileId(handler, recipeIndex) != null;
    }

    @Override
    @Nullable
    public String getProfileId(final IRecipeHandler handler, final int recipeIndex) {
        return switch (handler) {
            case RecipeHandlerAlloying _, RecipeHandlerCastingBasin _, RecipeHandlerCastingTable _,
                 RecipeHandlerDryingRack _, RecipeHandlerMelting _, RecipeHandlerToolMaterials _ -> "tconstruct:basic";
            default -> null;
        };
    }

    @Override
    @Nonnull
    public Map<RecipeProperty<?>, Object> extract(final Node node, final IRecipeHandler handler,
        final int recipeIndex) {
        final Map<RecipeProperty<?>, Object> props = new HashMap<>(
            PropertyProvider.super.extract(node, handler, recipeIndex));
        if (!(handler instanceof final TemplateRecipeHandler trh)) return props;

        final List<TemplateRecipeHandler.CachedRecipe> recipes = RecipeHandlerAccess.getArecipes(trh);
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) return props;

        final TemplateRecipeHandler.CachedRecipe cached = recipes.get(recipeIndex);

        if (cached instanceof final CachedDryingRackRecipe r && r.time > 0) {
            props.put(RecipePropertyAPI.DURATION_TICKS, r.time);
        }

        if (handler instanceof final RecipeHandlerCastingBase cb) {
            return extractCasting(node, cb, cached, props);
        }

        return props;
    }

    private static Map<RecipeProperty<?>, Object> extractCasting(final Node node,
        final RecipeHandlerCastingBase handler, final TemplateRecipeHandler.CachedRecipe cached,
        final Map<RecipeProperty<?>, Object> props) {
        node.getInputs().clear();
        node.getOutputs().clear();
        if (!(cached instanceof final CachedCastingRecipe casting)) return props;

        final PositionedStack result = casting.getResult();
        final CastingRecipe match = findCastingRecipe(handler, casting, result);
        if (match == null) return props;

        if (match.castingMetal.amount > 0) {
            final var position = casting.getFluidTanks().getFirst().position;
            node.getInputs().add(Port.fluid(match.castingMetal, 1f, position.x, position.y));
        }
        if (match.cast != null) {
            final var cast = Port.itemPort(casting.getIngredients().getFirst());
            if (!match.consumeCast) cast.getValue().amount = 0;
            node.getInputs().add(cast);
        }
        node.getOutputs().add(Port.itemPort(result));
        if (match.coolTime > 0) props.put(RecipePropertyAPI.DURATION_TICKS, match.coolTime);
        return props;
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
