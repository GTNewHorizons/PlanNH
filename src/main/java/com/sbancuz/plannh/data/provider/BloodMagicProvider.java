package com.sbancuz.plannh.data.provider;

import java.lang.reflect.Field;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.RecipeHandlerAccess;
import com.sbancuz.plannh.data.effect.Effects;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.properties.RecipeProperty;
import com.sbancuz.plannh.data.properties.SummaryProperty;
import com.sbancuz.plannh.data.setting.Settings;

import WayofTime.alchemicalWizardry.client.nei.NEIAlchemyRecipeHandler;
import WayofTime.alchemicalWizardry.client.nei.NEIAlchemyRecipeHandler.CachedAlchemyRecipe;
import WayofTime.alchemicalWizardry.client.nei.NEIAltarRecipeHandler;
import WayofTime.alchemicalWizardry.client.nei.NEIAltarRecipeHandler.CachedAltarRecipe;
import WayofTime.alchemicalWizardry.client.nei.NEIBindingRitualHandler;
import WayofTime.alchemicalWizardry.client.nei.NEIBloodOrbShapedHandler;
import WayofTime.alchemicalWizardry.client.nei.NEIBloodOrbShapelessHandler;
import WayofTime.alchemicalWizardry.client.nei.NEICalcinatorHandler;
import WayofTime.alchemicalWizardry.client.nei.NEIMeteorRecipeHandler;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;

public final class BloodMagicProvider implements PropertyProvider {

    @Nonnull
    private IDrawable icon(final IRecipeHandler handler, final int recipeIndex) {
        return GuiTextures.BUBBLE;
    }

    public static final RecipeProperty<Integer> LP_AMOUNT = SummaryProperty.builder("bloodmagic.lp_amount", 0)
        .build();

    public static final RecipeProperty<Integer> LP_TIER = RecipeProperty.builder("bloodmagic.lp_tier", 0)
        .build();

    private final MachineProfile PROFILE = MachineProfile.builder("bloodmagic:altar")
        .setting(Settings.MACHINES)
        .setting(Settings.LP_PER_TICK)
        .setting(Settings.TICK_MODIFIER)
        .effect(
            Effects.durationFromTotal(LP_AMOUNT, Settings.LP_PER_TICK)
                .amortizeCost(LP_AMOUNT)
                .applyParallelism())
        .provider(this)
        .icon(this::icon)
        .build();

    @Nullable
    private static Field ALTAR_LP;

    @Nullable
    private static Field ALTAR_TIER;

    @Nullable
    private static Field ALCHEMY_LP;

    static {
        try {
            ALTAR_LP = CachedAltarRecipe.class.getDeclaredField("lp_amount");
            ALTAR_LP.setAccessible(true);
            ALTAR_TIER = CachedAltarRecipe.class.getDeclaredField("tier");
            ALTAR_TIER.setAccessible(true);
            ALCHEMY_LP = CachedAlchemyRecipe.class.getDeclaredField("lp");
            ALCHEMY_LP.setAccessible(true);
        } catch (final Exception ignored) {}
    }

    @Override
    public StackRole otherStackRole(final IRecipeHandler handler, final int recipeIndex, final PositionedStack stack) {
        // The blood orb is retained by the alchemy table, not produced or consumed per operation.
        return handler instanceof NEIAlchemyRecipeHandler ? StackRole.IGNORE
            : PropertyProvider.super.otherStackRole(handler, recipeIndex, stack);
    }

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(NEIAlchemyRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(NEIAltarRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(NEIBindingRitualHandler.class, this);
        RecipePropertyAPI.registerExtractor(NEIBloodOrbShapedHandler.class, this);
        RecipePropertyAPI.registerExtractor(NEIBloodOrbShapelessHandler.class, this);
        RecipePropertyAPI.registerExtractor(NEICalcinatorHandler.class, this);
        RecipePropertyAPI.registerExtractor(NEIMeteorRecipeHandler.class, this);

    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        return switch (handler) {
            case NEIAlchemyRecipeHandler _, NEIAltarRecipeHandler _, NEIBindingRitualHandler _,
                 NEIBloodOrbShapedHandler _, NEIBloodOrbShapelessHandler _, NEICalcinatorHandler _,
                 NEIMeteorRecipeHandler _ -> List.of(PROFILE);
            default -> List.of();
        };
    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler,
        final int recipeIndex) {
        PropertyProvider.super.populate(recipe, handler, recipeIndex);
        if (!(handler instanceof final TemplateRecipeHandler trh)) return;

        final List<TemplateRecipeHandler.CachedRecipe> recipes = RecipeHandlerAccess.getArecipes(trh);
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) return;

        final TemplateRecipeHandler.CachedRecipe cached = recipes.get(recipeIndex);

        if (cached instanceof final CachedAltarRecipe r) {
            final int lp = getIntField(ALTAR_LP, r);
            final int tier = getIntField(ALTAR_TIER, r);
            if (lp > 0) recipe.property(LP_AMOUNT, lp);
            if (tier > 0) recipe.property(LP_TIER, tier);
        } else if (cached instanceof final CachedAlchemyRecipe r) {
            final int lp = getIntField(ALCHEMY_LP, r);
            if (lp > 0) recipe.property(LP_AMOUNT, lp);
        }
    }

    private static int getIntField(@Nullable final Field field, final Object instance) {
        if (field == null) return 0;
        try {
            return field.getInt(instance);
        } catch (final Exception ignored) {
            return 0;
        }
    }
}
