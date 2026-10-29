package com.sbancuz.plannh.data.provider;

import java.util.List;

import javax.annotation.Nonnull;

import net.bdew.gendustry.config.Fluids;
import net.bdew.gendustry.machines.extractor.MachineExtractor;
import net.bdew.gendustry.machines.imprinter.MachineImprinter;
import net.bdew.gendustry.machines.liquifier.MachineLiquifier;
import net.bdew.gendustry.machines.mproducer.MachineMutagenProducer;
import net.bdew.gendustry.machines.mutatron.MachineMutatron;
import net.bdew.gendustry.machines.replicator.MachineReplicator;
import net.bdew.gendustry.machines.sampler.MachineSampler;
import net.bdew.gendustry.machines.transposer.MachineTransposer;
import net.bdew.gendustry.nei.ExtractorHandler;
import net.bdew.gendustry.nei.ImprinterHandler;
import net.bdew.gendustry.nei.LiquifierHandler;
import net.bdew.gendustry.nei.MutagenProducerHandler;
import net.bdew.gendustry.nei.MutatronHandler;
import net.bdew.gendustry.nei.ReplicatorHandler;
import net.bdew.gendustry.nei.SamplerHandler;
import net.bdew.gendustry.nei.TransposerHandler;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.Effects;
import com.sbancuz.plannh.data.effect.steps.CoFHCompat;
import com.sbancuz.plannh.data.flowchart.Port;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.setting.Settings;

import codechicken.nei.recipe.IRecipeHandler;

public final class GendustryProvider implements PropertyProvider {

    private static final String PROFILE_ID = "gendustry:basic";
    private static final float RF_MJ_RATIO = 10f;

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

    @Nonnull
    private IDrawable icon(final IRecipeHandler handler, final int recipeIndex) {
        return GuiTextures.COLOR_WHEEL;
    }

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(MutatronHandler.class, this);
        RecipePropertyAPI.registerExtractor(MutagenProducerHandler.class, this);
        RecipePropertyAPI.registerExtractor(SamplerHandler.class, this);
        RecipePropertyAPI.registerExtractor(ImprinterHandler.class, this);
        RecipePropertyAPI.registerExtractor(ExtractorHandler.class, this);
        RecipePropertyAPI.registerExtractor(LiquifierHandler.class, this);
        RecipePropertyAPI.registerExtractor(ReplicatorHandler.class, this);
        RecipePropertyAPI.registerExtractor(TransposerHandler.class, this);

    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        return switch (handler) {
            case MutatronHandler _, MutagenProducerHandler _, SamplerHandler _,
                 ImprinterHandler _, ExtractorHandler _, LiquifierHandler _,
                 ReplicatorHandler _, TransposerHandler _ -> List.of(PROFILE);
            default -> List.of();
        };
    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler,
        final int recipeIndex) {
        PropertyProvider.super.populate(recipe, handler, recipeIndex);

        final float mj;
        final float rate;

        switch (handler) {
            case MutatronHandler _ -> {
                mj = MachineMutatron.mjPerItem();
                rate = MachineMutatron.powerUseRate() * MachineMutatron.maxStoredEnergy();
                recipe.input(Port.fluid(new FluidStack(Fluids.mutagen(), MachineMutatron.mutagenPerItem()), 1f, 32, 19));
            }
            case MutagenProducerHandler _ -> {
                mj = MachineMutagenProducer.mjPerItem();
                rate = MachineMutagenProducer.powerUseRate() * MachineMutagenProducer.maxStoredEnergy();
            }
            case SamplerHandler _ -> {
                mj = MachineSampler.mjPerItem();
                rate = MachineSampler.powerUseRate() * MachineSampler.maxStoredEnergy();
            }
            case ImprinterHandler _ -> {
                mj = MachineImprinter.mjPerItem();
                rate = MachineImprinter.powerUseRate() * MachineImprinter.maxStoredEnergy();
            }
            case ExtractorHandler _ -> {
                mj = MachineExtractor.mjPerItem();
                rate = MachineExtractor.powerUseRate() * MachineExtractor.maxStoredEnergy();
            }
            case LiquifierHandler _ -> {
                mj = MachineLiquifier.mjPerItem();
                rate = MachineLiquifier.powerUseRate() * MachineLiquifier.maxStoredEnergy();
            }
            case ReplicatorHandler _ -> {
                mj = MachineReplicator.mjPerItem();
                rate = MachineReplicator.powerUseRate() * MachineReplicator.maxStoredEnergy();
            }
            case TransposerHandler _ -> {
                mj = MachineTransposer.mjPerItem();
                rate = MachineTransposer.powerUseRate() * MachineTransposer.maxStoredEnergy();
            }
            default -> {
                return;
            }
        }

        final int ticks = (int) (mj / rate);
        recipe.property(RecipePropertyAPI.DURATION_TICKS, Math.max(1, ticks));
        recipe.property(CoFHCompat.RF_COST, (long) (mj * RF_MJ_RATIO));
    }
}
