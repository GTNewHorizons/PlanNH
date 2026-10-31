package com.sbancuz.plannh.data.provider;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.sbancuz.plannh.Compat;
import com.sbancuz.plannh.annotation.Versioned;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.ProfileFactory;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.Effects;
import com.sbancuz.plannh.data.effect.steps.CoFHCompat;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.setting.ChoiceSettingDef;
import com.sbancuz.plannh.data.setting.Settings;

import appeng.api.AEApi;
import appeng.api.config.PowerUnits;
import appeng.api.definitions.IBlockDefinition;
import appeng.api.definitions.IBlocks;
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

    public static final ChoiceSettingDef<ItemStack> ACCELERATION_CARDS = new ChoiceSettingDef<>(
        "catalyst_accel_card",
        null,
        ItemStack.class,
        () -> {
            final ItemStack card = AEApi.instance()
                .definitions()
                .materials()
                .cardSpeed()
                .maybeStack(1)
                .orNull();
            return card == null ? List.of() : List.of(card);
        },
        stack -> stack == null ? "-" : stack.getDisplayName()).counts(0, 5);

    private static final Map<String, Function<IBlocks, IBlockDefinition>> ICONS = Map.of(
        "ae2:quartz_grinder",
        IBlocks::grindStone,
        "ae2:inscriber",
        IBlocks::inscriber,
        "ae2:advanced_inscriber",
        IBlocks::advancedInscriber,
        "ae2:molecular_assembler",
        IBlocks::molecularAssembler);

    private static double rfFromAe(final double ae) {
        return PowerUnits.AE.convertTo(PowerUnits.RF, ae);
    }

    private record InscriberModel(int maxCards, int maxProcessingTime, int basePowerPerTick, int baseSpeed,
        int animationTicks) {}

    private final MachineProfile QUARTZ_GRINDER = MachineProfile.builder("ae2:quartz_grinder")
        .setting(Settings.MACHINES)
        .setting(Settings.TICK_MODIFIER)
        .setting(Settings.DURATION_TICKS)
        .effect(
            Effects.durationFromConfigOrProperty()
                .applyParallelism())
        .provider(this)
        .icon((_, _) -> icon("ae2:quartz_grinder"))
        .build();

    private final ProfileFactory<InscriberModel> INSCRIBER_FACTORY = MachineProfile.template()
        .setting(Settings.MACHINES)
        .setting(Settings.TICK_MODIFIER)
        .provider(this)
        .asFactory(
            (builder, model) -> builder.setting(ACCELERATION_CARDS, 0, model.maxCards())
                .effect(Effects.duration((cfg, ctx) -> {
                    final int cards = ACCELERATION_CARDS.count(cfg, ctx);
                    final int speed = model.baseSpeed() + cards;
                    final int processingTicks = model.animationTicks() == 0
                        ? (model.maxProcessingTime() + speed - 1) / speed
                        : model.maxProcessingTime() / speed + 1;
                    return processingTicks + model.animationTicks();
                })
                    .withTotalCost(CoFHCompat.RF_COST, (current, cfg, ctx) -> {
                        final int cards = ACCELERATION_CARDS.count(cfg, ctx);
                        final int speed = model.baseSpeed() + cards;
                        final int processingTicks = current.durationTicks() - model.animationTicks();
                        return rfFromAe((long) model.basePowerPerTick() * speed * processingTicks);
                    })
                    .withCostPerT(CoFHCompat.RF_PER_T, (current, cfg, ctx) -> current.energyPerT())
                    .applyParallelism())
                .icon((_, _) -> icon(model.animationTicks() == 0 ? "ae2:advanced_inscriber" : "ae2:inscriber")));

    // TODO: Open an upstream AE2 PR to expose each machine's acceleration-card capacity.
    private final MachineProfile INSCRIBER = INSCRIBER_FACTORY
        .builder(
            "ae2:inscriber",
            new InscriberModel(
                3,
                TileInscriber.MAX_PROCESSING_TIME,
                TileInscriber.BASE_POWER_PER_TICK,
                TileInscriber.BASE_SPEED,
                16)) // TileInscriber performs its smash animation over 16 ticks.
        .build();

    private final MachineProfile ADVANCED_INSCRIBER = INSCRIBER_FACTORY
        .builder(
            "ae2:advanced_inscriber",
            new InscriberModel(
                5,
                TileAdvancedInscriber.MAX_PROCESSING_TIME,
                TileAdvancedInscriber.BASE_POWER_PER_TICK,
                TileAdvancedInscriber.BASE_SPEED,
                0))
        .build();

    private final MachineProfile MOLECULAR_ASSEMBLER = MachineProfile.builder("ae2:molecular_assembler")
        .setting(Settings.MACHINES)
        .setting(Settings.TICK_MODIFIER)
        .setting(ACCELERATION_CARDS, 0, 5)
        .effect(Effects.duration((cfg, ctx) -> {
            final int cards = ACCELERATION_CARDS.count(cfg, ctx);
            final int speed = TileMolecularAssembler.SPEED[cards];
            // TileMolecularAssembler completes when its progress reaches 100.
            return Math.max(1, (100 + speed - 1) / speed);
        })
            .andThen((current, cfg, ctx) -> {
                final int cards = ACCELERATION_CARDS.count(cfg, ctx);
                final int speed = TileMolecularAssembler.SPEED[cards];
                final double tax = TileMolecularAssembler.ACCELERATION_TAX[cards];
                current.energyPerT(rfFromAe(speed * tax));
                return current;
            })
            .applyParallelism())
        .provider(this)
        .icon((_, _) -> icon("ae2:molecular_assembler"))
        .build();

    @Nonnull
    private IDrawable icon(final String profileId) {
        final ItemStack stack = ICONS.get(profileId)
            .apply(
                AEApi.instance()
                    .definitions()
                    .blocks())
            .maybeStack(1)
            .orNull();
        return stack == null ? GuiTextures.MATERIAL : new ItemDrawable(stack);
    }

    // TODO: Open an upstream AE2 PR to expose these processing and power constants.
    @Versioned.Class
    public static class TileInscriber {

        public static final int MAX_PROCESSING_TIME = 100;
        public static final int BASE_POWER_PER_TICK = 10;
        public static final int BASE_SPEED = 1;
    }

    // TODO: Open an upstream AE2 PR to expose advanced-inscriber processing and power constants.
    @Versioned.Class
    public static class TileAdvancedInscriber {

        public static final int MAX_PROCESSING_TIME = 100;
        public static final int BASE_POWER_PER_TICK = 10;
        public static final int BASE_SPEED = 1;
    }

    // TODO: Open an upstream AE2 PR to expose assembler processing, speed and acceleration-tax constants.
    @Versioned.Class
    public static class TileMolecularAssembler {

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
    public boolean canCraft(final IRecipeHandler handler, final int recipeIndex) {
        if (handler instanceof NEIGrinderRecipeHandler || handler instanceof NEIInscriberRecipeHandler) return true;
        if (handler instanceof NEIAEShapedRecipeHandler || handler instanceof NEIAEShapelessRecipeHandler
            || handler instanceof ShapedRecipeHandler)
            return new CraftingRecipeSupport(handler, recipeIndex).isEligible();
        return false;
    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        if (handler instanceof NEIGrinderRecipeHandler) return List.of(QUARTZ_GRINDER);
        if (handler instanceof NEIInscriberRecipeHandler) return List.of(INSCRIBER, ADVANCED_INSCRIBER);
        if (handler instanceof NEIAEShapedRecipeHandler || handler instanceof NEIAEShapelessRecipeHandler
            || handler instanceof ShapedRecipeHandler) {
            return new CraftingRecipeSupport(handler, recipeIndex).isEligible() ? List.of(MOLECULAR_ASSEMBLER)
                : List.of();
        }
        return List.of();
    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler, final int recipeIndex) {
        final CraftingRecipeSupport crafting = new CraftingRecipeSupport(handler, recipeIndex);
        final boolean isCrafting = crafting.isEligible();
        if (isCrafting) crafting.selectMissingPermutations();
        PropertyProvider.super.populate(recipe, handler, recipeIndex);
        if (isCrafting) crafting.addContainerReturns(recipe);

        if (handler instanceof NEIGrinderRecipeHandler) {
            final PositionedStack result = handler.getResultStack(recipeIndex);
            if (result == null || result.item == null) return;
            for (final IGrinderEntry entry : AEApi.instance()
                .registries()
                .grinder()
                .getRecipes()) {
                if (NEIServerUtils.areStacksSameTypeCrafting(entry.getOutput(), result.item)) {
                    if (entry.getEnergyCost() > 0)
                        recipe.property(RecipePropertyAPI.DURATION_TICKS, entry.getEnergyCost());
                    break;
                }
            }
        }
    }
}
