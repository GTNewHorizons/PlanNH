package com.sbancuz.plannh.data.provider;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.sbancuz.plannh.Compat;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.Effects;
import com.sbancuz.plannh.data.effect.steps.CoFHCompat;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.setting.Settings;

import binnie.extratrees.machines.ExtraTreeMachine;
import binnie.extratrees.machines.lumbermill.Lumbermill;
import binnie.extratrees.nei.NEIHandlerLumbermill;
import binnie.genetics.Genetics;
import binnie.genetics.api.IIncubatorRecipe;
import binnie.genetics.genetics.Engineering;
import binnie.genetics.machine.AdvGeneticMachine;
import binnie.genetics.machine.GeneticMachine;
import binnie.genetics.machine.LaboratoryMachine;
import binnie.genetics.machine.analyser.Analyser;
import binnie.genetics.machine.genepool.Genepool;
import binnie.genetics.machine.incubator.Incubator;
import binnie.genetics.machine.inoculator.Inoculator;
import binnie.genetics.machine.isolator.Isolator;
import binnie.genetics.machine.polymeriser.Polymeriser;
import binnie.genetics.machine.splicer.Splicer;
import binnie.genetics.nei.AcclimatiserRecipeHandler;
import binnie.genetics.nei.AnalyserRecipeHandler;
import binnie.genetics.nei.DatabaseRecipeHandler;
import binnie.genetics.nei.GenepoolRecipeHandler;
import binnie.genetics.nei.IncubatorRecipeHandler;
import binnie.genetics.nei.InoculatorRecipeHandler;
import binnie.genetics.nei.IsolatorRecipeHandler;
import binnie.genetics.nei.PolymeriserRecipeHandler;
import binnie.genetics.nei.SequencerRecipeHandler;
import binnie.genetics.nei.SplicerRecipeHandler;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;
import cpw.mods.fml.common.Loader;

public final class BinnieProvider implements PropertyProvider {

    @Nonnull
    private IDrawable icon(final IRecipeHandler handler, final int recipeIndex) {
        final ItemStack stack = switch (handler) {
            case GenepoolRecipeHandler _ -> LaboratoryMachine.Genepool.get(1);
            case IsolatorRecipeHandler _ -> GeneticMachine.Isolator.get(1);
            case SequencerRecipeHandler _ -> GeneticMachine.Sequencer.get(1);
            case PolymeriserRecipeHandler _ -> GeneticMachine.Polymeriser.get(1);
            case InoculatorRecipeHandler _ -> GeneticMachine.Inoculator.get(1);
            case AnalyserRecipeHandler _ -> LaboratoryMachine.Analyser.get(1);
            case IncubatorRecipeHandler _ -> LaboratoryMachine.Incubator.get(1);
            case AcclimatiserRecipeHandler _ -> LaboratoryMachine.Acclimatiser.get(1);
            case SplicerRecipeHandler _ -> AdvGeneticMachine.Splicer.get(1);
            case NEIHandlerLumbermill _ -> ExtraTreeMachine.Lumbermill.get(1);
            default -> Genetics.database == null ? null : new ItemStack(Genetics.database);
        };
        return stack == null ? GuiTextures.PROCESSOR : new ItemDrawable(stack);
    }

    private static final String PROFILE_ID = "binnie:general";

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

    // TODO: open an upstream PR to expose these copied constants.
    // Sequencer: base process length (ticks) from SequencerComponentLogic.getProcessLength()
    private static final int SEQUENCER_BASE_LENGTH = 19200;
    // Sequencer: RF/t from SequencerComponentLogic.getProcessEnergy()
    private static final int SEQUENCER_RF_PER_TICK = 20;
    // Sequencer: getSequenceStrength() = 1.0 - (1.0 - (damage%6)/5.0)^2 * 0.75
    private static final float SEQUENCER_DAMAGE_MOD = 6.0f;
    private static final float SEQUENCER_STRENGTH_DIVISOR = 5.0f;
    private static final float SEQUENCER_STRENGTH_FACTOR = 0.75f;

    // Polymeriser: catalyst multiplier when gold present (getCatalyst())
    private static final float POLYMERISER_GOLD_FACTOR = 0.2f;

    // Splicer: gene scaling factor in getProcessLength/getProcessEnergy
    private static final float SPLICER_GENE_FACTOR = 0.5f;

    // Acclimatiser: estimated total RF (indefinite process, no RF_COST constant in mod)
    private static final int ACCLIMATISER_ESTIMATED_RF = 200;

    @Override
    public void register() {
        if (Loader.isModLoaded(Compat.IDs.GENETICS)) registerGenetics();
        if (Loader.isModLoaded(Compat.IDs.EXTRATREES)) registerExtraTrees();
    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        if (Loader.isModLoaded(Compat.IDs.GENETICS) && isGeneticsHandler(handler)) return List.of(PROFILE);
        if (Loader.isModLoaded(Compat.IDs.EXTRATREES) && handler instanceof NEIHandlerLumbermill)
            return List.of(PROFILE);
        return List.of();
    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler, final int recipeIndex) {
        PropertyProvider.super.populate(recipe, handler, recipeIndex);

        if (Loader.isModLoaded(Compat.IDs.GENETICS)) populateGenetics(recipe, handler, recipeIndex);
        if (Loader.isModLoaded(Compat.IDs.EXTRATREES)) populateExtraTrees(recipe, handler, recipeIndex);
    }

    private void registerGenetics() {
        RecipePropertyAPI.registerExtractor(GenepoolRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(IsolatorRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(SequencerRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(PolymeriserRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(InoculatorRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(AnalyserRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(IncubatorRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(AcclimatiserRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(SplicerRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(DatabaseRecipeHandler.class, this);
    }

    private void registerExtraTrees() {
        RecipePropertyAPI.registerExtractor(NEIHandlerLumbermill.class, this);
    }

    private static boolean isGeneticsHandler(final IRecipeHandler handler) {
        return switch (handler) {
            case GenepoolRecipeHandler _, IsolatorRecipeHandler _, SequencerRecipeHandler _,
                 PolymeriserRecipeHandler _, InoculatorRecipeHandler _, AnalyserRecipeHandler _,
                 IncubatorRecipeHandler _, AcclimatiserRecipeHandler _, SplicerRecipeHandler _,
                 DatabaseRecipeHandler _ -> true;
            default -> false;
        };
    }

    private static void populateGenetics(final RecipeContext.Builder recipe, final IRecipeHandler handler,
        final int recipeIndex) {
        switch (handler) {
            case GenepoolRecipeHandler _ -> {
                recipe.property(RecipePropertyAPI.DURATION_TICKS, Genepool.TIME_PERIOD);
                recipe.property(CoFHCompat.RF_COST, (long) Genepool.RF_COST);
            }
            case IsolatorRecipeHandler _ -> {
                recipe.property(RecipePropertyAPI.DURATION_TICKS, Isolator.TIME_PERIOD);
                recipe.property(CoFHCompat.RF_COST, (long) Isolator.RF_COST);
            }
            case AnalyserRecipeHandler _ -> {
                recipe.property(RecipePropertyAPI.DURATION_TICKS, Analyser.TIME_PERIOD);
                recipe.property(CoFHCompat.RF_COST, (long) Analyser.RF_COST);
            }
            case SequencerRecipeHandler r ->
                extractSequencer(recipe, r, recipeIndex);
            case PolymeriserRecipeHandler r ->
                extractPolymeriser(recipe, r, recipeIndex);
            case InoculatorRecipeHandler r ->
                extractInoculator(recipe, r, recipeIndex);
            case SplicerRecipeHandler r ->
                extractSplicer(recipe, r, recipeIndex);
            case IncubatorRecipeHandler r ->
                extractIncubator(recipe, r, recipeIndex);
            case AcclimatiserRecipeHandler _ ->
                recipe.property(CoFHCompat.RF_COST, (long) ACCLIMATISER_ESTIMATED_RF);
            default -> {}
        }
    }

    private static void populateExtraTrees(final RecipeContext.Builder recipe, final IRecipeHandler handler,
        final int recipeIndex) {
        if (handler instanceof NEIHandlerLumbermill) {
            recipe.property(RecipePropertyAPI.DURATION_TICKS, Lumbermill.TIME_PERIOD);
            recipe.property(CoFHCompat.RF_COST, (long) Lumbermill.RF_COST);
        }
    }

    private static void extractSequencer(final RecipeContext.Builder recipe, final TemplateRecipeHandler handler,
        final int recipeIndex) {
        final var recipes = handler.arecipes;
        if (recipeIndex >= recipes.size()) return;
        final var cached = (SequencerRecipeHandler.CachedSequencerRecipe) recipes.get(recipeIndex);
        if (cached.seq == null || cached.seq.item == null) return;
        final float temp = 1.0f
            - (cached.seq.item.getItemDamage() % (int) SEQUENCER_DAMAGE_MOD) / SEQUENCER_STRENGTH_DIVISOR;
        final float strength = 1.0f - temp * temp * SEQUENCER_STRENGTH_FACTOR;
        final int ticks = Math.max(1, (int) (SEQUENCER_BASE_LENGTH * strength));
        recipe.property(RecipePropertyAPI.DURATION_TICKS, ticks);
        recipe.property(CoFHCompat.RF_COST, (long) ticks * SEQUENCER_RF_PER_TICK);
    }

    private static void extractPolymeriser(final RecipeContext.Builder recipe, final TemplateRecipeHandler handler,
        final int recipeIndex) {
        final var recipes = handler.arecipes;
        if (recipeIndex >= recipes.size()) return;
        final var cached = (PolymeriserRecipeHandler.CachedPolymeriserRecipe) recipes.get(recipeIndex);
        if (cached.input == null || cached.input.item == null) return;
        final int geneCount = Engineering.getGenes(cached.input.item).length;
        final boolean hasGold = cached.goldNugget != null && cached.goldNugget.item != null;
        final double factor = Math.max(1, geneCount) * (hasGold ? POLYMERISER_GOLD_FACTOR : 1.0);
        recipe.property(RecipePropertyAPI.DURATION_TICKS, Math.max(1, (int) (Polymeriser.TIME_PERIOD * factor)));
        recipe.property(CoFHCompat.RF_COST, (long) Math.max(1, (int) (Polymeriser.RF_COST * factor)));
    }

    private static void extractInoculator(final RecipeContext.Builder recipe, final TemplateRecipeHandler handler,
        final int recipeIndex) {
        final var recipes = handler.arecipes;
        if (recipeIndex >= recipes.size()) return;
        final var cached = (InoculatorRecipeHandler.CachedInoculatorRecipe) recipes.get(recipeIndex);
        final int factor = Math.max(
            1,
            cached.serum != null && cached.serum.item != null ? Engineering.getGenes(cached.serum.item).length : 1);
        recipe.property(RecipePropertyAPI.DURATION_TICKS, Math.max(1, Inoculator.TIME_PERIOD * factor));
        recipe.property(CoFHCompat.RF_COST, (long) Math.max(1, Inoculator.RF_COST * factor));
    }

    private static void extractSplicer(final RecipeContext.Builder recipe, final TemplateRecipeHandler handler,
        final int recipeIndex) {
        final var recipes = handler.arecipes;
        if (recipeIndex >= recipes.size()) return;
        final var cached = (SplicerRecipeHandler.CachedSplicer) recipes.get(recipeIndex);
        final double factor = 1.0 + (Math.max(
            1,
            cached.serum != null && cached.serum.item != null ? Engineering.getGenes(cached.serum.item).length : 1) - 1)
            * SPLICER_GENE_FACTOR;
        recipe.property(RecipePropertyAPI.DURATION_TICKS, Math.max(1, (int) (Splicer.TIME_PERIOD * factor)));
        recipe.property(CoFHCompat.RF_COST, (long) Math.max(1, (int) (Splicer.RF_COST * factor)));
    }

    private static void extractIncubator(final RecipeContext.Builder recipe, final TemplateRecipeHandler handler,
        final int recipeIndex) {
        final var recipes = handler.arecipes;
        if (recipeIndex >= recipes.size()) return;
        final var cached = (IncubatorRecipeHandler.CachedIncubatorRecipe) recipes.get(recipeIndex);

        float tickChance = 1.0f;
        if (cached.input != null && cached.input.item != null) {
            for (final IIncubatorRecipe incubatorRecipe : Incubator.RECIPES) {
                final ItemStack recipeInput = incubatorRecipe.getInputStack();
                if (recipeInput != null && recipeInput.isItemEqual(cached.input.item)) {
                    tickChance = incubatorRecipe.getChance();
                    break;
                }
            }
        }
        if (tickChance <= 0) tickChance = 1.0f;
        final int ticks = Math.max(1, (int) (1.0f / tickChance));
        recipe.property(RecipePropertyAPI.DURATION_TICKS, ticks);
        recipe.property(CoFHCompat.RF_COST, (long) (Incubator.ENERGY_PER_TICK / tickChance));
    }
}
