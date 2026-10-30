package com.sbancuz.plannh.data.provider;

import java.util.List;

import javax.annotation.Nonnull;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.pam.harvestcraft.BlockRegistry;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.Effects;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.setting.Settings;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import tonius.neiintegration.mods.harvestcraft.RecipeHandlerApiary;
import tonius.neiintegration.mods.harvestcraft.RecipeHandlerChurn;
import tonius.neiintegration.mods.harvestcraft.RecipeHandlerOven;
import tonius.neiintegration.mods.harvestcraft.RecipeHandlerPresser;
import tonius.neiintegration.mods.harvestcraft.RecipeHandlerPresserOld;
import tonius.neiintegration.mods.harvestcraft.RecipeHandlerQuern;

// TODO: open an upstream PR to expose these copied constants in the GTNH HarvestCraft fork.
// Source: com.pam.harvestcraft.TileEntityPamPresser (combPresserCookTime hardcoded to 125)
// Source: com.pam.harvestcraft.TileEntityChurn (churnCookTime hardcoded to 200)
// Source: com.pam.harvestcraft.TileEntityOven (ovenCookTime hardcoded to 200)
// Source: com.pam.harvestcraft.TileEntityQuern (quernCookTime hardcoded to 200)
// Source: com.pam.harvestcraft.TileEntityPamApiary (getRunTime() returns 3500 base)
public class HarvestCraftProvider implements PropertyProvider {

    private static final String PROFILE_ID = "harvestcraft:basic";

    private final MachineProfile PROFILE = MachineProfile.builder(PROFILE_ID)
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
        final var block = switch (handler) {
            case RecipeHandlerPresser _, RecipeHandlerPresserOld _ -> BlockRegistry.pamPresser;
            case RecipeHandlerOven _ -> BlockRegistry.pamOven;
            case RecipeHandlerChurn _ -> BlockRegistry.pamChurn;
            case RecipeHandlerQuern _ -> BlockRegistry.pamQuern;
            case RecipeHandlerApiary _ -> BlockRegistry.pamApiary;
            default -> null;
        };
        return block == null ? GuiTextures.PROCESSOR : new ItemDrawable(block);
    }

    private static final int PRESSER_TICKS = 125;
    private static final int OVEN_TICKS = 200;
    private static final int CHURN_TICKS = 200;
    private static final int QUERN_TICKS = 200;
    private static final int APIARY_TICKS = 3500;

    @Override
    public StackRole otherStackRole(final IRecipeHandler handler, final int recipeIndex,
        final PositionedStack stack) {
        return switch (handler) {
            case RecipeHandlerOven _, RecipeHandlerChurn _, RecipeHandlerQuern _ -> StackRole.INPUT;
            default -> PropertyProvider.super.otherStackRole(handler, recipeIndex, stack);
        };
    }

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(RecipeHandlerPresser.class, this);
        RecipePropertyAPI.registerExtractor(RecipeHandlerPresserOld.class, this);
        RecipePropertyAPI.registerExtractor(RecipeHandlerOven.class, this);
        RecipePropertyAPI.registerExtractor(RecipeHandlerChurn.class, this);
        RecipePropertyAPI.registerExtractor(RecipeHandlerQuern.class, this);
        RecipePropertyAPI.registerExtractor(RecipeHandlerApiary.class, this);

    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        return switch (handler) {
            case RecipeHandlerPresser _, RecipeHandlerPresserOld _, RecipeHandlerOven _,
                 RecipeHandlerChurn _, RecipeHandlerQuern _, RecipeHandlerApiary _ -> List.of(PROFILE);
            default -> List.of();
        };
    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler,
        final int recipeIndex) {
        PropertyProvider.super.populate(recipe, handler, recipeIndex);

        switch (handler) {
            case RecipeHandlerPresser _, RecipeHandlerPresserOld _ -> recipe.property(RecipePropertyAPI.DURATION_TICKS, PRESSER_TICKS);
            case RecipeHandlerOven _ -> recipe.property(RecipePropertyAPI.DURATION_TICKS, OVEN_TICKS);
            case RecipeHandlerChurn _ -> recipe.property(RecipePropertyAPI.DURATION_TICKS, CHURN_TICKS);
            case RecipeHandlerQuern _ -> recipe.property(RecipePropertyAPI.DURATION_TICKS, QUERN_TICKS);
            case RecipeHandlerApiary _ -> recipe.property(RecipePropertyAPI.DURATION_TICKS, APIARY_TICKS);
            default -> {}
        }
    }
}
