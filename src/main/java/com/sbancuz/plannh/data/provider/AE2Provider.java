package com.sbancuz.plannh.data.provider;

import java.util.List;

import javax.annotation.Nonnull;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.sbancuz.plannh.Compat;
import com.sbancuz.plannh.annotation.Versioned;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.Effects;
import com.sbancuz.plannh.data.effect.steps.CoFHCompat;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.setting.Settings;

import appeng.api.AEApi;
import appeng.api.features.IGrinderEntry;
import appeng.integration.modules.NEIHelpers.NEIAEShapedRecipeHandler;
import appeng.integration.modules.NEIHelpers.NEIAEShapelessRecipeHandler;
import appeng.integration.modules.NEIHelpers.NEIGrinderRecipeHandler;
import appeng.integration.modules.NEIHelpers.NEIInscriberRecipeHandler;
import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.ShapedRecipeHandler;

@Versioned.Mod(modId = Compat.IDs.AE2, sinceVersion = "rv3-beta-1017-GTNH")
public final class AE2Provider implements PropertyProvider {

    private final MachineProfile QUARTZ_GRINDER = MachineProfile.builder("ae2:quartz_grinder")
        .setting(Settings.MACHINES)
        .setting(Settings.TICK_MODIFIER)
        .setting(Settings.DURATION_TICKS)
        .effect(DefaultProvider::noopEffect)
        .provider(this)
        .icon(this::icon)
        .build();

    private final MachineProfile INSCRIBER = MachineProfile.builder("ae2:inscriber")
        .setting(Settings.MACHINES)
        .setting(Settings.TICK_MODIFIER)
        .setting(Settings.CATALYST_ACCEL_CARD)
        .effect(Effects.durationFromFormula((cfg, ctx) -> {
            final int cards = cfg.getOrDefault(Settings.CATALYST_ACCEL_CARD);
            final int speedFactor = TileInscriber.BASE_SPEED + cards;
            return Math.max(1, (TileInscriber.MAX_PROCESSING_TIME + speedFactor - 1) / speedFactor);
        })
            .withCostPerT(CoFHCompat.RF_PER_T, (current, cfg, ctx) -> {
                final int cards = cfg.getOrDefault(Settings.CATALYST_ACCEL_CARD);
                final int speedFactor = TileInscriber.BASE_SPEED + cards;
                return (long) TileInscriber.BASE_POWER_PER_TICK * speedFactor;
            })
            .applyParallelism()
            .computeTotal(CoFHCompat.RF_COST))
        .provider(this)
        .icon(this::icon)
        .build();

    private final MachineProfile MOLECULAR_ASSEMBLER = MachineProfile.builder("ae2:molecular_assembler")
        .setting(Settings.MACHINES)
        .setting(Settings.TICK_MODIFIER)
        .setting(Settings.CATALYST_ACCEL_CARD)
        .effect(Effects.durationFromFormula((cfg, ctx) -> {
            final int cards = Math
                .min(cfg.getOrDefault(Settings.CATALYST_ACCEL_CARD), TileMolecularAssembler.SPEED.length - 1);
            final int speed = TileMolecularAssembler.SPEED[cards];
            return Math.max(1, (TileMolecularAssembler.MAX_PROCESSING_TIME + speed - 1) / speed);
        })
            .andThen((current, cfg, ctx) -> {
                final int cards = Math
                    .min(cfg.getOrDefault(Settings.CATALYST_ACCEL_CARD), TileMolecularAssembler.SPEED.length - 1);
                final int speed = TileMolecularAssembler.SPEED[cards];
                final double tax = TileMolecularAssembler.ACCELERATION_TAX[cards];
                current.energyPerT(Math.round(speed * tax));
                return current;
            })
            .applyParallelism())
        .provider(this)
        .icon(this::icon)
        .build();

    @Nonnull
    private IDrawable icon(final IRecipeHandler handler, final int recipeIndex) {
        if (handler instanceof NEIGrinderRecipeHandler) {
            final var stack = AEApi.instance()
                .definitions()
                .blocks()
                .grindStone()
                .maybeStack(1)
                .orNull();
            return stack == null ? GuiTextures.MATERIAL : new ItemDrawable(stack);
        }
        if (handler instanceof NEIInscriberRecipeHandler) {
            final var stack = AEApi.instance()
                .definitions()
                .blocks()
                .inscriber()
                .maybeStack(1)
                .orNull();
            return stack == null ? GuiTextures.MATERIAL : new ItemDrawable(stack);
        }
        if (handler instanceof NEIAEShapedRecipeHandler || handler instanceof NEIAEShapelessRecipeHandler
            || handler instanceof ShapedRecipeHandler) {
            final var stack = AEApi.instance()
                .definitions()
                .blocks()
                .molecularAssembler()
                .maybeStack(1)
                .orNull();
            return stack == null ? GuiTextures.MATERIAL : new ItemDrawable(stack);
        }
        return GuiTextures.MATERIAL;
    }

    @Versioned.Class
    public static class TileInscriber {

        public static final int MAX_PROCESSING_TIME = 100;
        public static final int BASE_POWER_PER_TICK = 10;
        public static final int BASE_SPEED = 1;
    }

    @Versioned.Class
    public static class TileMolecularAssembler {

        public static final int MAX_PROCESSING_TIME = 100;
        public static final int[] SPEED = { 10, 13, 17, 20, 25, 50 };
        public static final double[] ACCELERATION_TAX = { 1.0, 1.3, 1.7, 2.0, 2.5, 5.0 };
    }

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(NEIGrinderRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(NEIInscriberRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(NEIAEShapedRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(NEIAEShapelessRecipeHandler.class, this);
        // ShapelessRecipeHandler extends this class, covering vanilla shaped and shapeless recipes.
        RecipePropertyAPI.registerExtractor(ShapedRecipeHandler.class, this);

    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        return switch (handler) {
            case NEIGrinderRecipeHandler _ -> List.of(QUARTZ_GRINDER);
            case NEIInscriberRecipeHandler _-> List.of(INSCRIBER);
            case NEIAEShapedRecipeHandler _, NEIAEShapelessRecipeHandler _, ShapedRecipeHandler _ ->
                List.of(MOLECULAR_ASSEMBLER);
            default -> List.of();
        };
    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler, final int recipeIndex) {
        PropertyProvider.super.populate(recipe, handler, recipeIndex);

        if (handler instanceof NEIGrinderRecipeHandler) {
            final int duration = findGrinderDuration(handler, recipeIndex);
            if (duration > 0) {
                recipe.property(RecipePropertyAPI.DURATION_TICKS, duration);
            }
        }
    }

    private static int findGrinderDuration(final IRecipeHandler handler, final int recipeIndex) {
        final PositionedStack result = handler.getResultStack(recipeIndex);
        if (result == null || result.item == null) return 0;

        for (final IGrinderEntry entry : AEApi.instance()
            .registries()
            .grinder()
            .getRecipes()) {
            if (NEIServerUtils.areStacksSameTypeCrafting(entry.getOutput(), result.item)) {
                return entry.getEnergyCost();
            }
        }
        return 0;
    }
}
