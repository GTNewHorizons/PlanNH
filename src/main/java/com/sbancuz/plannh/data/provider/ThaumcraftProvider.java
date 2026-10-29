package com.sbancuz.plannh.data.provider;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.gtnewhorizons.aspectrecipeindex.nei.AlchemyRecipeHandler;
import com.gtnewhorizons.aspectrecipeindex.nei.AspectCombinationHandler;
import com.gtnewhorizons.aspectrecipeindex.nei.InfusionRecipeHandler;
import com.gtnewhorizons.aspectrecipeindex.nei.ItemsContainingAspectHandler;
import com.gtnewhorizons.aspectrecipeindex.nei.arcaneworkbench.ShapedArcaneRecipeHandler;
import com.gtnewhorizons.aspectrecipeindex.nei.arcaneworkbench.ShapelessArcaneRecipeHandler;
import com.gtnewhorizons.aspectrecipeindex.nei.arcaneworkbench.WandRecipeHandler;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.RecipeHandlerAccess;
import com.sbancuz.plannh.data.effect.Effects;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.properties.RecipeProperty;
import com.sbancuz.plannh.data.properties.SummaryProperty;
import com.sbancuz.plannh.data.setting.Settings;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import thaumcraft.api.crafting.ShapelessArcaneRecipe;
import thaumcraft.common.config.ConfigBlocks;

public class ThaumcraftProvider implements PropertyProvider {

    @Nonnull
    private IDrawable icon(final IRecipeHandler handler, final int recipeIndex) {
        if (ConfigBlocks.blockTable == null || ConfigBlocks.blockStoneDevice == null) return GuiTextures.BOOKMARK;
        if (handler instanceof InfusionRecipeHandler) return new ItemDrawable(ConfigBlocks.blockStoneDevice, 2);
        if (handler instanceof ShapedArcaneRecipeHandler || handler instanceof ShapelessArcaneRecipeHandler
            || handler instanceof WandRecipeHandler) return new ItemDrawable(ConfigBlocks.blockTable);
        return GuiTextures.BOOKMARK;
    }

    public static final RecipeProperty<int[]> VIS_COST = SummaryProperty.builder("thaumcraft.vis_cost", new int[6])
        .build();

    public static final RecipeProperty<Integer> INSTABILITY = RecipeProperty
        .<Integer>builder("thaumcraft.instability", 0)
        .build();

    public static final RecipeProperty<Integer> TOTAL_VIS = RecipeProperty.<Integer>builder("thaumcraft.total_vis", 0)
        .build();

    public static final RecipeProperty<String> RESEARCH_KEY = RecipeProperty
        .<String>builder("thaumcraft.research_key", "")
        .build();

    public static final RecipeProperty<Integer> NUM_COMPONENTS = RecipeProperty
        .<Integer>builder("thaumcraft.num_components", 0)
        .build();

    private final MachineProfile ARCANE = MachineProfile.builder("thaumcraft:arcane")
        .setting(Settings.MACHINES)
        .setting(Settings.VIS_PER_TICK)
        .setting(Settings.TICK_MODIFIER)
        .effect(
            Effects.durationFromTotal(TOTAL_VIS, Settings.VIS_PER_TICK)
                .amortizeCost(TOTAL_VIS)
                .applyParallelism())
        .provider(this)
        .icon(this::icon)
        .build();

    private final MachineProfile INFUSION = MachineProfile.builder("thaumcraft:infusion")
        .setting(Settings.MACHINES)
        .setting(Settings.TICK_MODIFIER)
        .effect(Effects.durationFromFormula((cfg, ctx) -> {
            final int tv = ctx.getOrDefault(TOTAL_VIS, 0);
            final int nc = ctx.getOrDefault(NUM_COMPONENTS, 0);
            return tv * 10 + nc * 60;
        })
            .amortizeCost(TOTAL_VIS)
            .applyParallelism())
        .provider(this)
        .icon(this::icon)
        .build();

    private static final String[] PRIMAL_TAGS = { "aer", "terra", "ignis", "aqua", "ordo", "perditio" };

    @Override
    public StackRole otherStackRole(final IRecipeHandler handler, final int recipeIndex, final PositionedStack stack) {
        // Arcane handlers use other stacks to display vis costs, not item products.
        return handler instanceof ShapedArcaneRecipeHandler ? StackRole.IGNORE
            : PropertyProvider.super.otherStackRole(handler, recipeIndex, stack);
    }

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(ItemsContainingAspectHandler.class, this);
        RecipePropertyAPI.registerExtractor(AspectCombinationHandler.class, this);
        RecipePropertyAPI.registerExtractor(ShapedArcaneRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(WandRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(ShapelessArcaneRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(AlchemyRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(InfusionRecipeHandler.class, this);

    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        if (!canCraft(handler, recipeIndex)) return List.of();
        if (!(handler instanceof TemplateRecipeHandler)) return List.of();
        final String overlay = handler.getOverlayIdentifier();
        if (overlay == null) return List.of();
        return switch (overlay) {
            case "thaumcraft.arcane.shaped", "thaumcraft.arcane.shapeless", "thaumcraft.wands", "thaumcraft.alchemy" -> List
                .of(ARCANE);
            case "thaumcraft.infusion" -> List.of(INFUSION);
            default -> List.of();
        };
    }

    @Override
    public boolean canCraft(final IRecipeHandler handler, final int recipeIndex) {
        final String researchKey = findResearchKey(handler, recipeIndex);
        if (researchKey == null || researchKey.isEmpty()) return false;
        return ThaumcraftApiHelper.isResearchComplete(
            Minecraft.getMinecraft()
                .getSession()
                .getUsername(),
            researchKey);
    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler, final int recipeIndex) {
        PropertyProvider.super.populate(recipe, handler, recipeIndex);
        if (!(handler instanceof final TemplateRecipeHandler trh)) return;

        final var recipes = RecipeHandlerAccess.getArecipes(trh);
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) return;

        final TemplateRecipeHandler.CachedRecipe cached = recipes.get(recipeIndex);
        final String overlay = trh.getOverlayIdentifier();
        if (overlay == null) return;

        final var resultPos = cached.getResult();
        if (resultPos == null || resultPos.item == null) return;
        final var result = resultPos.item;

        switch (overlay) {
            case "thaumcraft.alchemy" -> {
                final CrucibleRecipe cr = ThaumcraftApi.getCrucibleRecipe(result);
                if (cr != null && cr.aspects != null) {
                    recipe.property(VIS_COST, aspectListToPrimals(cr.aspects));
                    recipe.property(TOTAL_VIS, sumVis(cr.aspects));
                }
            }
            case "thaumcraft.infusion" -> {
                final InfusionRecipe ir = ThaumcraftApi.getInfusionRecipe(result);
                if (ir != null) {
                    final AspectList aspects = ir.getAspects();
                    if (aspects != null) {
                        recipe.property(VIS_COST, aspectListToPrimals(aspects));
                        recipe.property(TOTAL_VIS, sumVis(aspects));
                    }
                    final int instability = ir.getInstability();
                    if (instability > 0) recipe.property(INSTABILITY, instability);
                    final ItemStack[] comps = ir.getComponents();
                    recipe.property(NUM_COMPONENTS, comps != null ? comps.length : 0);
                }
            }
            case "thaumcraft.arcane.shaped", "thaumcraft.arcane.shapeless", "thaumcraft.wands" -> {
                for (final Object obj : ThaumcraftApi.getCraftingRecipes()) {
                    if (obj instanceof final ShapedArcaneRecipe ar && result.isItemEqual(ar.getRecipeOutput())) {
                        final AspectList aspects = ar.getAspects();
                        if (aspects != null) {
                            recipe.property(VIS_COST, aspectListToPrimals(aspects));
                            recipe.property(TOTAL_VIS, sumVis(aspects));
                        }
                        break;
                    }
                    if (obj instanceof final ShapelessArcaneRecipe ar && result.isItemEqual(ar.getRecipeOutput())) {
                        final AspectList aspects = ar.getAspects();
                        if (aspects != null) {
                            recipe.property(VIS_COST, aspectListToPrimals(aspects));
                            recipe.property(TOTAL_VIS, sumVis(aspects));
                        }
                        break;
                    }
                }
            }
        }

        final String researchKey = findResearchKey(handler, recipeIndex);
        if (researchKey != null) recipe.property(RESEARCH_KEY, researchKey);
    }

    @Nullable
    private static String findResearchKey(final IRecipeHandler handler, final int recipeIndex) {
        if (!(handler instanceof final TemplateRecipeHandler trh)) return null;
        final String overlay = trh.getOverlayIdentifier();
        if (overlay == null) return null;
        if (!switch (overlay) {
            case "thaumcraft.arcane.shaped", "thaumcraft.arcane.shapeless", "thaumcraft.wands", "thaumcraft.alchemy", "thaumcraft.infusion" -> true;
            default -> false;
        }) return null;

        final var recipes = RecipeHandlerAccess.getArecipes(trh);
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) return null;

        final TemplateRecipeHandler.CachedRecipe cached = recipes.get(recipeIndex);
        final var resultPos = cached.getResult();
        if (resultPos == null || resultPos.item == null) return null;
        final var result = resultPos.item;

        return switch (overlay) {
            case "thaumcraft.alchemy" -> {
                final CrucibleRecipe cr = ThaumcraftApi.getCrucibleRecipe(result);
                yield cr != null ? cr.key : null;
            }
            case "thaumcraft.infusion" -> {
                final InfusionRecipe ir = ThaumcraftApi.getInfusionRecipe(result);
                yield ir != null ? ir.getResearch() : null;
            }
            case "thaumcraft.arcane.shaped", "thaumcraft.arcane.shapeless", "thaumcraft.wands" -> {
                String key = null;
                for (final Object obj : ThaumcraftApi.getCraftingRecipes()) {
                    if (obj instanceof final ShapedArcaneRecipe ar && result.isItemEqual(ar.getRecipeOutput())) {
                        key = ar.getResearch();
                        break;
                    }
                    if (obj instanceof final ShapelessArcaneRecipe ar && result.isItemEqual(ar.getRecipeOutput())) {
                        key = ar.getResearch();
                        break;
                    }
                }
                yield key;
            }
            default -> null;
        };
    }

    private static int sumVis(final @Nullable AspectList aspects) {
        if (aspects == null) return 0;
        int total = 0;
        for (final Aspect a : aspects.getAspects()) {
            if (a != null) total += aspects.getAmount(a);
        }
        return total;
    }

    @Nonnull
    private static int[] aspectListToPrimals(final @Nullable AspectList aspects) {
        final int[] result = new int[6];
        if (aspects == null) return result;
        final Aspect[] aspectArray = aspects.getAspects();
        if (aspectArray == null) return result;
        for (final Aspect aspect : aspectArray) {
            if (aspect == null) continue;
            final String tag = aspect.getTag();
            if (tag == null) continue;
            for (int i = 0; i < 6; i++) {
                if (PRIMAL_TAGS[i].equals(tag)) {
                    result[i] = aspects.getAmount(aspect);
                    break;
                }
            }
        }
        return result;
    }

}
