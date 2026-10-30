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
import com.sbancuz.plannh.data.properties.RecipeProperty;
import com.sbancuz.plannh.data.properties.SummaryProperty;
import com.sbancuz.plannh.data.setting.Settings;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;
import vazkii.botania.client.integration.nei.recipe.RecipeHandlerBrewery;
import vazkii.botania.client.integration.nei.recipe.RecipeHandlerElvenTrade;
import vazkii.botania.client.integration.nei.recipe.RecipeHandlerFloatingFlowers;
import vazkii.botania.client.integration.nei.recipe.RecipeHandlerLexicaBotania;
import vazkii.botania.client.integration.nei.recipe.RecipeHandlerManaPool;
import vazkii.botania.client.integration.nei.recipe.RecipeHandlerManaPool.CachedManaPoolRecipe;
import vazkii.botania.client.integration.nei.recipe.RecipeHandlerPetalApothecary;
import vazkii.botania.client.integration.nei.recipe.RecipeHandlerPureDaisy;
import vazkii.botania.client.integration.nei.recipe.RecipeHandlerRunicAltar;
import vazkii.botania.client.integration.nei.recipe.RecipeHandlerRunicAltar.CachedRunicAltarRecipe;
import vazkii.botania.common.block.ModBlocks;
import vazkii.botania.common.item.ModItems;
import vazkii.botania.common.item.block.ItemBlockSpecialFlower;

public class BotaniaProvider implements PropertyProvider {

    @Nonnull
    private IDrawable icon(final IRecipeHandler handler, final int recipeIndex) {
        if (handler instanceof RecipeHandlerPureDaisy)
            return ModBlocks.specialFlower == null ? GuiTextures.PROCESSOR
                : new ItemDrawable(ItemBlockSpecialFlower.ofType("pureDaisy"));
        if (handler instanceof RecipeHandlerElvenTrade)
            return ModBlocks.alfPortal == null ? GuiTextures.PROCESSOR : new ItemDrawable(ModBlocks.alfPortal);
        if (handler instanceof RecipeHandlerLexicaBotania)
            return ModItems.lexicon == null ? GuiTextures.PROCESSOR : new ItemDrawable(ModItems.lexicon);

        final var block = switch (handler) {
            case RecipeHandlerFloatingFlowers _ -> ModBlocks.floatingFlower;
            case RecipeHandlerRunicAltar _ -> ModBlocks.runeAltar;
            case RecipeHandlerPetalApothecary _ -> ModBlocks.altar;
            case RecipeHandlerManaPool _ -> ModBlocks.pool;
            case RecipeHandlerBrewery _ -> ModBlocks.brewery;
            default -> null;
        };
        return block == null ? GuiTextures.PROCESSOR : new ItemDrawable(block);
    }

    public static final RecipeProperty<Integer> MANA_COST = SummaryProperty.builder("botania.mana_cost", 0)
        .build();

    private final MachineProfile PROFILE = MachineProfile.builder("botania:basic")
        .setting(Settings.MACHINES)
        .setting(Settings.MANA_PER_TICK)
        .setting(Settings.TICK_MODIFIER)
        .effect(
            Effects.durationFromTotal(MANA_COST, Settings.MANA_PER_TICK)
                .amortizeCost(MANA_COST)
                .applyParallelism())
        .provider(this)
        .icon(this::icon)
        .build();

    @Override
    public StackRole otherStackRole(final IRecipeHandler handler, final int recipeIndex, final PositionedStack stack) {
        // The alchemy/conjuration catalyst is a persistent block beneath the pool.
        return handler instanceof RecipeHandlerManaPool ? StackRole.IGNORE
            : PropertyProvider.super.otherStackRole(handler, recipeIndex, stack);
    }

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(RecipeHandlerFloatingFlowers.class, this);
        RecipePropertyAPI.registerExtractor(RecipeHandlerPetalApothecary.class, this);
        RecipePropertyAPI.registerExtractor(RecipeHandlerRunicAltar.class, this);
        RecipePropertyAPI.registerExtractor(RecipeHandlerManaPool.class, this);
        RecipePropertyAPI.registerExtractor(RecipeHandlerElvenTrade.class, this);
        RecipePropertyAPI.registerExtractor(RecipeHandlerBrewery.class, this);
        RecipePropertyAPI.registerExtractor(RecipeHandlerPureDaisy.class, this);
        RecipePropertyAPI.registerExtractor(RecipeHandlerLexicaBotania.class, this);

    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        if (!handler.getClass()
            .getName()
            .startsWith("vazkii.botania")) return List.of();
        return List.of(PROFILE);
    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler, final int recipeIndex) {
        PropertyProvider.super.populate(recipe, handler, recipeIndex);

        if (!(handler instanceof final TemplateRecipeHandler trh)) return;

        final List<TemplateRecipeHandler.CachedRecipe> recipes = trh.arecipes;
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) return;

        final TemplateRecipeHandler.CachedRecipe cached = recipes.get(recipeIndex);

        if (cached instanceof final CachedRunicAltarRecipe r) {
            if (r.manaUsage > 0) {
                recipe.property(MANA_COST, r.manaUsage);
            }
        } else if (cached instanceof final CachedManaPoolRecipe r) {
            if (r.mana > 0) {
                recipe.property(MANA_COST, r.mana);
            }
        }

    }
}
