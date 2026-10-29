package com.sbancuz.plannh.data.provider;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.RecipeHandlerAccess;
import com.sbancuz.plannh.data.effect.EffectComputer;
import com.sbancuz.plannh.data.effect.EffectResult;
import com.sbancuz.plannh.data.effect.steps.CoFHCompat;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.properties.RecipeProperty;
import com.sbancuz.plannh.data.setting.Settings;

import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;
import crazypants.enderio.EnderIO;
import crazypants.enderio.config.Config;
import crazypants.enderio.nei.AlloySmelterRecipeHandler;
import crazypants.enderio.nei.AlloySmelterRecipeHandler.AlloySmelterRecipe;
import crazypants.enderio.nei.EnchanterRecipeHandler;
import crazypants.enderio.nei.SagMillRecipeHandler;
import crazypants.enderio.nei.SagMillRecipeHandler.MillRecipe;
import crazypants.enderio.nei.SliceAndSpliceRecipeHandler;
import crazypants.enderio.nei.SliceAndSpliceRecipeHandler.SliceAndSpliceRecipe;
import crazypants.enderio.nei.SoulBinderRecipeHandler;
import crazypants.enderio.nei.SoulBinderRecipeHandler.SoulBinderRecipeNEI;
import crazypants.enderio.nei.VatRecipeHandler;
import crazypants.enderio.nei.VatRecipeHandler.InnerVatRecipe;
import crazypants.enderio.power.Capacitors;

public class EnderIOProvider implements PropertyProvider {

    @Nonnull
    private IDrawable icon(final IRecipeHandler handler, final int recipeIndex) {
        if (handler instanceof AlloySmelterRecipeHandler && EnderIO.blockAlloySmelter != null)
            return new ItemDrawable(EnderIO.blockAlloySmelter);
        if (handler instanceof SagMillRecipeHandler && EnderIO.blockCrusher != null)
            return new ItemDrawable(EnderIO.blockCrusher);
        if (handler instanceof EnchanterRecipeHandler && EnderIO.blockEnchanter != null)
            return new ItemDrawable(EnderIO.blockEnchanter);
        if (handler instanceof SliceAndSpliceRecipeHandler && EnderIO.blockSliceAndSplice != null)
            return new ItemDrawable(EnderIO.blockSliceAndSplice);
        if (handler instanceof SoulBinderRecipeHandler && EnderIO.blockSoulFuser != null)
            return new ItemDrawable(EnderIO.blockSoulFuser);
        if (handler instanceof VatRecipeHandler && EnderIO.blockVat != null) return new ItemDrawable(EnderIO.blockVat);
        return GuiTextures.PROCESSOR;
    }

    private static final String PROFILE_ID = "enderio";
    private static final int[] CAPACITOR_RF_PER_T = Arrays.stream(Capacitors.VALUES)
        .mapToInt(capacitor -> capacitor.capacitor.getMaxEnergyExtracted())
        .distinct()
        .toArray();

    static final RecipeProperty<Integer> FIXED_RF_PER_TICK = RecipeProperty
        .<Integer>builder("enderio.fixed_rf_per_tick", 0)
        .build();

    private static int durationTicks(final MachineConfig cfg, final RecipeContext ctx) {
        final long energy = energy(ctx);
        final int rate = cfg.getProfile()
            .value(Settings.RF_PER_TICK, cfg, ctx);
        if (energy <= 0 || rate <= 0) return 0;
        return (int) Math.min(Integer.MAX_VALUE, (energy - 1) / rate + 1);
    }

    private static long energy(final RecipeContext ctx) {
        final Number cost = ctx.getOrDefault(CoFHCompat.RF_COST, 0L);
        return cost.longValue();
    }

    private static int rfPerTick(final MachineConfig cfg, final RecipeContext ctx) {
        final Integer fixed = ctx.getOrDefault(FIXED_RF_PER_TICK, null);
        if (fixed != null) return fixed;
        final int capacitor = cfg.getOrDefault(Settings.EIO_CAPACITOR);
        final int index = Math.clamp(capacitor, 0, CAPACITOR_RF_PER_T.length - 1);
        return CAPACITOR_RF_PER_T[index];
    }

    public static final RecipeProperty<Integer> EXPERIENCE = RecipeProperty.<Integer>builder("enderio.experience", 0)
        .build();

    @Nullable
    private static Field MILL_OUTPUT_CHANCE;

    private final MachineProfile profile = MachineProfile.builder(PROFILE_ID)
        .setting(Settings.MACHINES)
        .setting(Settings.TICK_MODIFIER)
        .setting(
            Settings.EIO_CAPACITOR,
            (cfg, ctx) -> energy(ctx) > 0 && ctx.getOrDefault(FIXED_RF_PER_TICK, null) == null)
        .setting(Settings.RF_PER_TICK, (cfg, ctx) -> energy(ctx) > 0)
        .setting(Settings.DURATION_TICKS, (cfg, ctx) -> energy(ctx) > 0)
        .derive(Settings.RF_PER_TICK, EnderIOProvider::rfPerTick)
        .derive(Settings.DURATION_TICKS, EnderIOProvider::durationTicks)
        .effect(
            ((EffectComputer) (cfg, ctx) -> new EffectResult(
                cfg.getProfile()
                    .value(Settings.DURATION_TICKS, cfg, ctx),
                0,
                1)).withCostPerT(
                    CoFHCompat.RF_PER_T,
                    (current, cfg, ctx) -> energy(ctx) > 0 ? (long) cfg.getProfile()
                        .value(Settings.RF_PER_TICK, cfg, ctx) : 0L)
                    .applyParallelism())
        .icon(this::icon)
        .provider(this)
        .build();

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(AlloySmelterRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(SagMillRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(VatRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(EnchanterRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(SliceAndSpliceRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(SoulBinderRecipeHandler.class, this);

        Field f = null;
        try {
            f = MillRecipe.class.getDeclaredField("outputChance");
            f.setAccessible(true);
        } catch (final Exception ignored) {}
        MILL_OUTPUT_CHANCE = f;
    }

    @Override
    public boolean canCraft(final IRecipeHandler handler, final int recipeIndex) {
        if (!(handler instanceof TemplateRecipeHandler trh)) return false;
        final String overlay = trh.getOverlayIdentifier();
        return overlay != null && (overlay.startsWith("EnderIO") || overlay.equals("EIOEnchanter"));
    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        return canCraft(handler, recipeIndex) ? List.of(profile) : List.of();
    }

    @Override
    public String getMachineId(final IRecipeHandler handler, final int recipeIndex) {
        if (handler instanceof AlloySmelterRecipeHandler) return "enderio:alloy_smelter";
        if (handler instanceof SagMillRecipeHandler) return "enderio:sag_mill";
        if (handler instanceof VatRecipeHandler) return "enderio:vat";
        if (handler instanceof EnchanterRecipeHandler) return "enderio:enchanter";
        if (handler instanceof SliceAndSpliceRecipeHandler) return "enderio:slice_and_splice";
        if (handler instanceof SoulBinderRecipeHandler) return "enderio:soul_binder";
        return PropertyProvider.super.getMachineId(handler, recipeIndex);
    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler,
        final int recipeIndex) {
        PropertyProvider.super.populate(recipe, handler, recipeIndex);
        if (!(handler instanceof final TemplateRecipeHandler trh)) return;

        final List<TemplateRecipeHandler.CachedRecipe> recipes = RecipeHandlerAccess.getArecipes(trh);
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) return;

        final TemplateRecipeHandler.CachedRecipe cached = recipes.get(recipeIndex);

        if (cached instanceof final AlloySmelterRecipe r) {
            applyEnergy(recipe, r.getEnergy());
        } else if (cached instanceof final MillRecipe r) {
            applyEnergy(recipe, r.getEnergy());
            applyMillChances(recipe, r);
        } else if (cached instanceof final SliceAndSpliceRecipe r) {
            applyEnergy(recipe, r.getEnergy());
        } else if (cached instanceof final SoulBinderRecipeNEI r) {
            applyEnergy(recipe, r.getEnergy());
            if (r.getExperience() > 0) recipe.property(EXPERIENCE, r.getExperience());
        } else if (cached instanceof final InnerVatRecipe r) {
            applyEnergy(recipe, r.getEnergy());
            recipe.property(FIXED_RF_PER_TICK, Config.vatPowerUserPerTickRF);
        }
    }

    private static void applyEnergy(final RecipeContext.Builder recipe, final int energy) {
        recipe.property(CoFHCompat.RF_COST, (long) energy);
    }

    private static void applyMillChances(final RecipeContext.Builder recipe, final MillRecipe r) {
        if (MILL_OUTPUT_CHANCE == null) return;
        try {
            final float[] chances = (float[]) MILL_OUTPUT_CHANCE.get(r);
            for (int i = 0; i < chances.length && i < recipe.outputs()
                .size(); i++) {
                recipe.outputs()
                    .get(i)
                    .setChance(chances[i]);
            }
        } catch (final Exception ignored) {}
    }

}
