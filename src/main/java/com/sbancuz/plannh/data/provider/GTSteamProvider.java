package com.sbancuz.plannh.data.provider;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.Effects;
import com.sbancuz.plannh.data.effect.steps.GTSteamOverclockStep;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.properties.RecipeProperty;
import com.sbancuz.plannh.data.properties.SummaryProperty;
import com.sbancuz.plannh.data.setting.Settings;

import codechicken.nei.recipe.FurnaceRecipeHandler;
import codechicken.nei.recipe.IRecipeHandler;
import gregtech.api.enums.ItemList;
import gregtech.api.recipe.RecipeMap;
import gregtech.nei.GTNEIDefaultHandler;

public class GTSteamProvider implements PropertyProvider {

    @Nonnull
    private IDrawable icon(final IRecipeHandler handler, final int recipeIndex) {
        final String mapName;
        if (handler instanceof final FurnaceRecipeHandler furnace) {
            mapName = furnaceRecipeMap(furnace);
        } else if (handler instanceof final GTNEIDefaultHandler gt) {
            final RecipeMap<?> map = gt.getRecipeMap();
            mapName = map == null ? null : map.unlocalizedName;
        } else {
            mapName = null;
        }
        if (mapName == null) return GuiTextures.SOUND;
        return switch (mapName) {
            case "gt.recipe.furnace", "gt.recipe.efrsmelting" ->
                GTProvider.itemIcon(ItemList.Machine_Bronze_Furnace, GuiTextures.SOUND);
            case "gt.recipe.efrblasting" ->
                GTProvider.itemIcon(ItemList.Machine_Bricked_BlastFurnace, GuiTextures.SOUND);
            case "gt.recipe.macerator" ->
                GTProvider.itemIcon(ItemList.Machine_Bronze_Macerator, GuiTextures.SOUND);
            case "gt.recipe.compressor" ->
                GTProvider.itemIcon(ItemList.Machine_Bronze_Compressor, GuiTextures.SOUND);
            case "gt.recipe.hammer" -> GTProvider.itemIcon(ItemList.Machine_Bronze_Hammer, GuiTextures.SOUND);
            case "gt.recipe.alloysmelter" ->
                GTProvider.itemIcon(ItemList.Machine_Bronze_AlloySmelter, GuiTextures.SOUND);
            default -> GuiTextures.SOUND;
        };
    }

    public static final RecipeProperty<Long> STEAM_EU_PERT = SummaryProperty.<Long>builder("gt.steam_eu_pert", 0L)
        .perSec(true)
        .build();
    public static final RecipeProperty<Long> TOTAL_STEAM_EU = SummaryProperty.<Long>builder("gt.total_steam_eu", 0L)
        .build();

    private static final Set<String> STEAM_RECIPE_MAPS = Set.of(
        "gt.recipe.furnace",
        "gt.recipe.efrblasting",
        "gt.recipe.efrsmelting",
        "gt.recipe.macerator",
        "gt.recipe.compressor",
        "gt.recipe.hammer",
        "gt.recipe.alloysmelter",
        "gt.recipe.centrifuge",
        "gt.recipe.orewasher",
        "gtpp.recipe.multimixer",
        "gtpp.recipe.simplewasher");

    private final MachineProfile steamProfile = MachineProfile.builder("gregtech:steam")
        .setting(Settings.SPEED)
        .setting(Settings.PARALLELS)
        .setting(Settings.MACHINES)
        .setting(Settings.STEAM_EUT_DISCOUNT)
        .setting(Settings.STEAM_DURATION_MODIFIER)
        .effect(
            Effects.durationFromHandler()
                .andThen(GTSteamOverclockStep.create())
                .applyParallelism())
        .icon(this::icon)
        .provider(this)
        .build();

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(FurnaceRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(GTNEIDefaultHandler.class, this);
    }

    @Override
    public boolean canCraft(final IRecipeHandler handler, final int recipeIndex) {
        if (handler instanceof final FurnaceRecipeHandler fh) return furnaceRecipeMap(fh) != null;
        if (handler instanceof final GTNEIDefaultHandler gth) {
            final RecipeMap<?> map = gth.getRecipeMap();
            return map != null && STEAM_RECIPE_MAPS.contains(map.unlocalizedName);
        }
        return false;
    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        return canCraft(handler, recipeIndex) ? List.of(steamProfile) : List.of();
    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler, final int recipeIndex) {
        GTProvider.populateGTRecipe(recipe, handler, recipeIndex);
    }

    private static String furnaceRecipeMap(final FurnaceRecipeHandler handler) {
        return switch (handler.getOverlayIdentifier()) {
            case "smelting" -> "gt.recipe.furnace";
            case EFRProvider.SMOKER_OVERLAY -> "gt.recipe.efrsmelting";
            case EFRProvider.BLAST_FURNACE_OVERLAY -> "gt.recipe.efrblasting";
            default -> null;
        };
    }
}
