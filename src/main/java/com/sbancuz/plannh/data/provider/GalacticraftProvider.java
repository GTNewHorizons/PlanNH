package com.sbancuz.plannh.data.provider;

import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;

import javax.annotation.Nonnull;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.EffectComputer;
import com.sbancuz.plannh.data.effect.EffectResult;
import com.sbancuz.plannh.data.effect.Effects;
import com.sbancuz.plannh.data.effect.steps.CoFHCompat;
import com.sbancuz.plannh.data.flowchart.Port;
import com.sbancuz.plannh.data.flowchart.resource.LongItemStack;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.properties.RecipeProperty;
import com.sbancuz.plannh.data.properties.SummaryProperty;
import com.sbancuz.plannh.data.setting.IntegerSettingDef;
import com.sbancuz.plannh.data.setting.Settings;

import codechicken.nei.recipe.FurnaceRecipeHandler;
import codechicken.nei.recipe.IRecipeHandler;
import micdoodle8.mods.galacticraft.core.blocks.BlockMachine2;
import micdoodle8.mods.galacticraft.core.blocks.BlockMachineTiered;
import micdoodle8.mods.galacticraft.core.blocks.GCBlocks;
import micdoodle8.mods.galacticraft.core.nei.CircuitFabricatorRecipeHandler;
import micdoodle8.mods.galacticraft.core.nei.RefineryRecipeHandler;
import micdoodle8.mods.galacticraft.core.util.ConfigManagerCore;

public final class GalacticraftProvider implements PropertyProvider {

    private static final String PROFILE_ID = "gc:basic";
    // TODO: open an upstream PR to expose these copied machine constants.
    private static final int ELECTRIC_FURNACE_TICKS = 130;
    private static final int ARC_FURNACE_BASE_TICKS = 200;
    private static final int ELECTRIC_FURNACE_NORMAL_GJ_PER_TICK = 45;
    private static final int ELECTRIC_FURNACE_HARD_GJ_PER_TICK = 60;
    private static final int ARC_FURNACE_NORMAL_GJ_PER_TICK = 60;
    private static final int ARC_FURNACE_HARD_GJ_PER_TICK = 90;
    static final IntegerSettingDef ARC_POWER_TIER = new IntegerSettingDef("galacticraft.arc_power_tier", 1, 1, 2);

    static final RecipeProperty<Number> GJ_PER_TICK = SummaryProperty.<Number>builder("galacticraft.gj_per_t", 0)
        .perSec(true)
        .build();

    private final MachineProfile PROFILE = MachineProfile.builder(PROFILE_ID)
        .setting(Settings.MACHINES)
        .setting(Settings.TICK_MODIFIER)
        .effect(
            Effects.durationFromHandler()
                .amortizeCost(CoFHCompat.RF_COST)
                .applyParallelism())
        .provider(this)
        .icon(this::icon)
        .build();

    private final MachineProfile ELECTRIC_FURNACE_PROFILE = furnaceProfile(
        "gc:electric_furnace",
        (cfg, ctx) -> ELECTRIC_FURNACE_TICKS,
        this::electricFurnaceRate,
        this::electricFurnaceIcon,
        false);

    private final MachineProfile ARC_FURNACE_PROFILE = furnaceProfile(
        "gc:electric_arc_furnace",
        this::arcFurnaceDuration,
        this::arcFurnaceRate,
        this::electricArcFurnaceIcon,
        true);

    @Nonnull
    private IDrawable icon(final IRecipeHandler handler, final int recipeIndex) {
        if (GCBlocks.machineBase2 == null || GCBlocks.machineBase == null || GCBlocks.refinery == null)
            return GuiTextures.PROCESSOR;
        return handler instanceof CircuitFabricatorRecipeHandler
            ? new ItemDrawable(GCBlocks.machineBase2, BlockMachine2.CIRCUIT_FABRICATOR_METADATA)
            : new ItemDrawable(GCBlocks.refinery);
    }

    @Nonnull
    private IDrawable electricFurnaceIcon(final IRecipeHandler handler, final int recipeIndex) {
        return furnaceIcon(BlockMachineTiered.ELECTRIC_FURNACE_METADATA);
    }

    @Nonnull
    private IDrawable electricArcFurnaceIcon(final IRecipeHandler handler, final int recipeIndex) {
        return furnaceIcon(BlockMachineTiered.ELECTRIC_FURNACE_METADATA + 8);
    }

    @Nonnull
    private IDrawable furnaceIcon(final int metadata) {
        return GCBlocks.machineTiered == null ? GuiTextures.BLOCK : new ItemDrawable(GCBlocks.machineTiered, metadata);
    }

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(RefineryRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(CircuitFabricatorRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(FurnaceRecipeHandler.class, this);

    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        if (isVanillaSmelting(handler)) return List.of(ELECTRIC_FURNACE_PROFILE, ARC_FURNACE_PROFILE);
        return switch (handler) {
            case RefineryRecipeHandler _, CircuitFabricatorRecipeHandler _ -> List.of(PROFILE);
            default -> List.of();
        };
    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler, final int recipeIndex) {
        PropertyProvider.super.populate(recipe, handler, recipeIndex);
    }

    private MachineProfile furnaceProfile(final String id, final Duration duration, final Rate rate,
        final BiFunction<IRecipeHandler, Integer, IDrawable> icon, final boolean arc) {
        final MachineProfile.Builder builder = MachineProfile.builder(id)
            .setting(Settings.MACHINES)
            .setting(Settings.TICK_MODIFIER)
            .effect(
                ((EffectComputer) (cfg, ctx) -> new EffectResult(duration.get(cfg, ctx), rate.get(cfg), 1))
                    .withCostPerT(GJ_PER_TICK, (result, cfg, ctx) -> result.energyPerT())
                    .applyParallelism())
            .provider(this)
            .icon(icon);
        if (arc) builder.setting(ARC_POWER_TIER, 1, 2);
        return builder.outputMultiplier((_, cfg, ctx, index) -> arc && arcFurnaceDoubles(ctx, index) ? 2d : 1d)
            .build();
    }

    private int electricFurnaceRate(final MachineConfig cfg) {
        return ConfigManagerCore.hardMode ? ELECTRIC_FURNACE_HARD_GJ_PER_TICK : ELECTRIC_FURNACE_NORMAL_GJ_PER_TICK;
    }

    private int arcFurnaceRate(final MachineConfig cfg) {
        return ConfigManagerCore.hardMode ? ARC_FURNACE_HARD_GJ_PER_TICK : ARC_FURNACE_NORMAL_GJ_PER_TICK;
    }

    private int arcFurnaceDuration(final MachineConfig cfg, final RecipeContext ctx) {
        final int powerTier = cfg.value(ARC_POWER_TIER, ctx);
        return ARC_FURNACE_BASE_TICKS / (1 + powerTier);
    }

    private static boolean arcFurnaceDoubles(final RecipeContext ctx, final int outputIndex) {
        if (outputIndex < 0 || outputIndex >= ctx.outputs()
            .size()) return false;
        final Port<?> output = ctx.outputs()
            .get(outputIndex);
        if (output.getType() != RecipePropertyAPI.ITEM) return false;
        final String outputName = ((LongItemStack) output.getValue()).stack.getUnlocalizedName()
            .toLowerCase(Locale.ROOT);
        if (!outputName.contains("ingot")) return false;
        return ctx.inputs()
            .stream()
            .filter(input -> input.getType() == RecipePropertyAPI.ITEM)
            .map(
                input -> ((LongItemStack) input.getValue()).stack.getUnlocalizedName()
                    .toLowerCase(Locale.ROOT))
            .anyMatch(GalacticraftProvider::isArcQualifyingInput);
    }

    static boolean arcFurnaceDoubles(final String inputName, final String outputName) {
        return outputName.toLowerCase(Locale.ROOT)
            .contains("ingot") && isArcQualifyingInput(inputName.toLowerCase(Locale.ROOT));
    }

    private static boolean isArcQualifyingInput(final String inputName) {
        return inputName.contains("ore") || inputName.contains("raw")
            || inputName.contains("moon")
            || inputName.contains("mars")
            || inputName.contains("shard");
    }

    private boolean isVanillaSmelting(final IRecipeHandler handler) {
        return handler instanceof FurnaceRecipeHandler furnace && "smelting".equals(furnace.getOverlayIdentifier());
    }

    @FunctionalInterface
    private interface Rate {

        int get(MachineConfig cfg);
    }

    @FunctionalInterface
    private interface Duration {

        int get(MachineConfig cfg, RecipeContext ctx);
    }
}
