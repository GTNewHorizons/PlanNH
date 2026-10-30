package com.sbancuz.plannh;

import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.provider.AE2Provider;
import com.sbancuz.plannh.data.provider.AvaritiaProvider;
import com.sbancuz.plannh.data.provider.BinnieProvider;
import com.sbancuz.plannh.data.provider.BloodMagicProvider;
import com.sbancuz.plannh.data.provider.BotaniaProvider;
import com.sbancuz.plannh.data.provider.DefaultProvider;
import com.sbancuz.plannh.data.provider.DraconicEvolutionProvider;
import com.sbancuz.plannh.data.provider.EFRProvider;
import com.sbancuz.plannh.data.provider.EnderIOProvider;
import com.sbancuz.plannh.data.provider.ForestryProvider;
import com.sbancuz.plannh.data.provider.GTProvider;
import com.sbancuz.plannh.data.provider.GalacticraftProvider;
import com.sbancuz.plannh.data.provider.HarvestCraftProvider;
import com.sbancuz.plannh.data.provider.LogisticsPipesProvider;
import com.sbancuz.plannh.data.provider.ProjectBlueProvider;
import com.sbancuz.plannh.data.provider.ProjectRedProvider;
import com.sbancuz.plannh.data.provider.RailcraftProvider;
import com.sbancuz.plannh.data.provider.RandomThingsProvider;
import com.sbancuz.plannh.data.provider.ThaumcraftProvider;
import com.sbancuz.plannh.data.provider.ThaumicExplorationProvider;
import com.sbancuz.plannh.data.provider.ThaumicTinkererProvider;
import com.sbancuz.plannh.data.provider.TinkersConstructProvider;
import com.sbancuz.plannh.data.provider.TwilightForestProvider;
import com.sbancuz.plannh.data.provider.VanillaProvider;
import com.sbancuz.plannh.data.provider.WitchingGadgetsProvider;
import com.sbancuz.plannh.data.provider.bee.BeeProvider;
import com.sbancuz.plannh.data.provider.bee.ExtraBeesProvider;
import com.sbancuz.plannh.data.provider.bee.GendustryProvider;
import com.sbancuz.plannh.data.provider.bee.MagicBeesProvider;

import cpw.mods.fml.common.Loader;
import lombok.Getter;

public enum Compat {

    AVARITIA(IDs.AVARITIA, AvaritiaProvider.class),
    APPLIEDENERGISTICS2(IDs.AE2, AE2Provider.class),
    BINNIE(IDs.BINNIECORE, BinnieProvider.class),
    BLOODMAGIC(IDs.BLOODMAGIC, BloodMagicProvider.class),
    BOTANIA(IDs.BOTANIA, BotaniaProvider.class),
    DRACONICEVOLUTION(IDs.DRACONICEVOLUTION, DraconicEvolutionProvider.class),
    ENDERIO(IDs.ENDERIO, EnderIOProvider.class),
    FORESTRY(IDs.FORESTRY, ForestryProvider.class),
    GALACTICRAFT(IDs.GALACTICRAFT, GalacticraftProvider.class),
    GENDUSTRY(IDs.GENDUSTRY, GendustryProvider.class),
    GREGTECH(IDs.GREGTECH, GTProvider.class),
    HARVESTCRAFT(IDs.HARVESTCRAFT, HarvestCraftProvider.class),
    LOGISTICSPIPES(IDs.LOGISTICSPIPES, LogisticsPipesProvider.class),
    PROJECTBLUE(IDs.PROJECTBLUE, ProjectBlueProvider.class),
    PROJECTRED(IDs.PROJECTRED, ProjectRedProvider.class),
    RAILCRAFT(IDs.RAILCRAFT, RailcraftProvider.class),
    RANDOMTHINGS(IDs.RANDOMTHINGS, RandomThingsProvider.class),
    THAUMCRAFT(IDs.THAUMCRAFT, ThaumcraftProvider.class),
    THAUMICEXPLORATION(IDs.THAUMICEXPLORATION, ThaumicExplorationProvider.class),
    THAUMICTINKERER(IDs.THAUMICTINKERER, ThaumicTinkererProvider.class),
    TINKERSCONSTRUCT(IDs.TINKERSCONSTRUCT, TinkersConstructProvider.class),
    TWILIGHTFOREST(IDs.TWILIGHTFOREST, TwilightForestProvider.class),
    WITCHINGGADGETS(IDs.WITCHINGGADGETS, WitchingGadgetsProvider.class),
    EFR(IDs.EFR, EFRProvider.class),
    NEIADDONS(IDs.NEIADDONS, BeeProvider.class, IDs.FORESTRY),
    EXTRABEES(IDs.EXTRABEES, ExtraBeesProvider.class, IDs.NEIADDONS, IDs.FORESTRY, IDs.BINNIECORE),
    MAGICBEES(IDs.MAGICBEES, MagicBeesProvider.class, IDs.NEIADDONS, IDs.FORESTRY);

    public static class IDs {

        public static final String AVARITIA = "Avaritia";
        public static final String AE2 = "appliedenergistics2";
        public static final String BLOODMAGIC = "AWWayofTime";
        public static final String BOTANIA = "Botania";
        public static final String DRACONICEVOLUTION = "DraconicEvolution";
        public static final String ENDERIO = "EnderIO";
        public static final String FORESTRY = "Forestry";
        public static final String GALACTICRAFT = "Galacticraft";
        public static final String GENDUSTRY = "gendustry";
        public static final String GREGTECH = "gregtech";
        public static final String GTNHLIB = "gtnhlib";
        public static final String HARVESTCRAFT = "harvestcraft";
        public static final String LOGISTICSPIPES = "LogisticsPipes";
        public static final String MODULARUI2 = "modularui2";
        public static final String NEI = "NotEnoughItems";
        public static final String PROJECTBLUE = "ProjectBlue";
        public static final String PROJECTRED = "ProjRed|Expansion";
        public static final String RAILCRAFT = "Railcraft";
        public static final String RANDOMTHINGS = "RandomThings";
        public static final String THAUMCRAFT = "Thaumcraft";
        public static final String THAUMICEXPLORATION = "ThaumicExploration";
        public static final String THAUMICTINKERER = "ThaumicTinkerer";
        public static final String TINKERSCONSTRUCT = "TConstruct";
        public static final String TWILIGHTFOREST = "TwilightForest";
        public static final String WITCHINGGADGETS = "WitchingGadgets";
        public static final String EFR = "etfuturum";
        public static final String NEIADDONS = "NEIAddons";
        public static final String EXTRABEES = "ExtraBees";
        public static final String MAGICBEES = "MagicBees";
        public static final String BINNIECORE = "BinnieCore";
        public static final String GENETICS = "Genetics";
        public static final String EXTRATREES = "ExtraTrees";
    }

    public final String modid;
    public final boolean isLoaded;

    @Nullable
    @Getter
    private PropertyProvider extractor;
    @Nonnull
    private final Class<? extends PropertyProvider> providerFactory;

    Compat(final String modid, final Class<? extends PropertyProvider> providerFactory, final String... dependencies) {
        this.modid = modid;
        this.isLoaded = Loader.isModLoaded(modid) && Arrays.stream(dependencies)
            .allMatch(Loader::isModLoaded);
        this.providerFactory = providerFactory;
    }

    @Nullable
    private static <T extends PropertyProvider> PropertyProvider create(@Nonnull final Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor()
                .newInstance();
        } catch (Exception | LinkageError failure) {
            PlanNH.LOG.warn("Could not initialize recipe provider {}", clazz.getName(), failure);
        }
        return null;
    }

    public static void init() {
        RecipePropertyAPI.reset();
        DefaultProvider.INSTANCE.register();
        new VanillaProvider().register();
        for (final Compat mod : values()) {
            // Extra Bees supplies the same BeeProduceHandler choices with an enriched Alveary.
            // Keep one extractor owner so forestry:alveary cannot be duplicated.
            if (mod == NEIADDONS && EXTRABEES.isLoaded) continue;
            if (mod.isLoaded) {
                mod.extractor = create(mod.providerFactory);
                if (mod.extractor != null) {
                    try {
                        mod.extractor.register();
                    } catch (RuntimeException | LinkageError failure) {
                        PlanNH.LOG.warn("Could not register recipe provider for {}", mod.modid, failure);
                    }
                }
            }
        }
    }

}
