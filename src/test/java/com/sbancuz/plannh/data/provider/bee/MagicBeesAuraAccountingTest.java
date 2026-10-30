package com.sbancuz.plannh.data.provider.bee;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import net.bdew.neiaddons.forestry.bees.BeeProduceHandler;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.EffectResult;
import com.sbancuz.plannh.data.flowchart.Port;
import com.sbancuz.plannh.data.flowchart.resource.LongItemStack;
import com.sbancuz.plannh.data.flowchart.resource.MagicResources;
import com.sbancuz.plannh.data.provider.ProviderTestFixture;
import com.sbancuz.plannh.data.setting.Settings;

import forestry.api.apiculture.BeeManager;
import forestry.api.apiculture.DefaultBeeModifier;
import forestry.api.apiculture.EnumBeeChromosome;
import forestry.api.apiculture.IAlleleBeeSpecies;
import forestry.api.apiculture.IBeeGenome;
import forestry.api.apiculture.IBeeRoot;
import forestry.api.apiculture.IBeekeepingMode;
import forestry.api.core.ForestryAPI;
import forestry.api.core.IErrorStateRegistry;
import forestry.api.genetics.AlleleManager;
import forestry.api.genetics.IAllele;
import forestry.api.genetics.IAlleleFloat;
import forestry.api.genetics.IAlleleInteger;
import forestry.api.genetics.IAlleleRegistry;

class MagicBeesAuraAccountingTest {

    private static final String SPECIES_UID = "plannh:test_species";
    private static final String SPEED_UID = "plannh:test_speed";
    private static final String LIFESPAN_UID = "plannh:test_lifespan";

    private static MachineProfile magicApiary;
    private static IBeeRoot previousBeeRoot;
    private static IBeeRoot previousMagicBeesBeeRoot;
    private static IAlleleRegistry previousAlleleRegistry;
    private static IErrorStateRegistry previousErrorStateRegistry;

    @BeforeAll
    static void initializeMinecraftFixture() {
        ProviderTestFixture.item("redstone");
        previousBeeRoot = BeeManager.beeRoot;
        previousMagicBeesBeeRoot = magicbees.bees.BeeManager.beeRoot;
        previousAlleleRegistry = AlleleManager.alleleRegistry;
        previousErrorStateRegistry = ForestryAPI.errorStateRegistry;

        final IAlleleFloat speed = allele(IAlleleFloat.class, SPEED_UID, 1f);
        final IAlleleInteger lifespan = allele(IAlleleInteger.class, LIFESPAN_UID, 1);
        final IAlleleBeeSpecies species = species();
        AlleleManager.alleleRegistry = alleleRegistry(speed, lifespan);
        BeeManager.beeRoot = beeRoot(species, speed, lifespan);
        magicbees.bees.BeeManager.beeRoot = BeeManager.beeRoot;
        ForestryAPI.errorStateRegistry = errorStateRegistry();
        magicApiary = new MagicBeesProvider(MagicBeesAuraAccountingTest::beekeepingMode)
            .getProfiles(new BeeProduceHandler(), 0)
            .getFirst();
    }

    @AfterAll
    static void restoreMinecraftFixture() {
        BeeManager.beeRoot = previousBeeRoot;
        magicbees.bees.BeeManager.beeRoot = previousMagicBeesBeeRoot;
        AlleleManager.alleleRegistry = previousAlleleRegistry;
        ForestryAPI.errorStateRegistry = previousErrorStateRegistry;
    }

    @Test
    void auraDemandUsesFinalizedDurationOncePerOperation() {
        final RecipeContext recipe = auraRecipe();
        final EffectResult nominal = config(true, false, 100, 2, 3).computeEffect(recipe);
        final EffectResult effect = config(true, false, 50, 2, 3).computeEffect(recipe);

        assertEquals(Math.round(nominal.durationTicks() * 2d), effect.durationTicks());
        assertEquals(effect.durationTicks(), effect.inputMultiplier(1));
        assertEquals(0d, effect.inputMultiplier(2));
        assertEquals(1d, effect.inputMultiplier(0));
        assertEquals(6, effect.throughputFactor());
    }

    @Test
    void disabledAurasHaveZeroDemandWithoutChangingBeeInputs() {
        final EffectResult effect = config(false, false, 100, 1, 1).computeEffect(auraRecipe());

        assertEquals(0d, effect.inputMultiplier(1));
        assertEquals(0d, effect.inputMultiplier(2));
        assertEquals(1d, effect.inputMultiplier(0));
    }

    private static MachineConfig config(final boolean productionAura, final boolean lifespanAura,
        final int tickModifier, final int machines, final int parallels) {
        final MachineConfig cfg = new MachineConfig(magicApiary);
        cfg.set(BeeSettings.MAGIC_PRODUCTION_AURA, productionAura);
        cfg.set(BeeSettings.MAGIC_LIFESPAN_AURA, lifespanAura);
        cfg.set(Settings.TICK_MODIFIER, tickModifier);
        cfg.set(Settings.MACHINES, machines);
        cfg.set(Settings.PARALLELS, parallels);
        cfg.set(BeeSettings.SPEED, SPEED_UID);
        cfg.set(BeeSettings.LIFESPAN, LIFESPAN_UID);
        return cfg;
    }

    private static RecipeContext auraRecipe() {
        return RecipeContext.builder()
            .property(RecipePropertyAPI.DURATION_TICKS, 400)
            .property(BeeProvider.SPECIES, SPECIES_UID)
            .input(
                new Port<>(
                    RecipePropertyAPI.ITEM,
                    new LongItemStack(new ItemStack(ProviderTestFixture.item("redstone")), 0),
                    1f,
                    null))
            .input(
                new Port<>(
                    MagicResources.AURA_CHARGE,
                    new MagicResources.Amount("production", "Production Aura Charge", 0xF0D05A, 1),
                    1f,
                    null))
            .input(
                new Port<>(
                    MagicResources.AURA_CHARGE,
                    new MagicResources.Amount("death", "Death/Lifespan Aura Charge", 0x9A62C8, 1),
                    1f,
                    null))
            .build();
    }

    private static IAlleleBeeSpecies species() {
        return proxy(IAlleleBeeSpecies.class, (proxy, method, args) -> switch (method.getName()) {
            case "getUID" -> SPECIES_UID;
            case "getProductChances", "getSpecialtyChances" -> Map.of();
            case "getName", "getUnlocalizedName" -> SPECIES_UID;
            case "isDominant" -> false;
            default -> throw new UnsupportedOperationException(method.toString());
        });
    }

    private static <T extends IAllele> T allele(final Class<T> type, final String uid, final Object value) {
        return proxy(type, (proxy, method, args) -> switch (method.getName()) {
            case "getUID" -> uid;
            case "getName", "getUnlocalizedName" -> uid;
            case "isDominant" -> false;
            case "getValue" -> value;
            default -> throw new UnsupportedOperationException(method.toString());
        });
    }

    private static IAlleleRegistry alleleRegistry(final IAlleleFloat speed, final IAlleleInteger lifespan) {
        final Map<String, IAllele> alleles = Map.of(SPEED_UID, speed, LIFESPAN_UID, lifespan);
        return proxy(IAlleleRegistry.class, (proxy, method, args) -> switch (method.getName()) {
            case "getAllele" -> alleles.get(args[0]);
            case "getRegisteredAlleles" -> args[0] == EnumBeeChromosome.SPEED ? List.of(speed)
                : args[0] == EnumBeeChromosome.LIFESPAN ? List.of(lifespan) : List.of();
            default -> throw new UnsupportedOperationException(method.toString());
        });
    }

    private static IBeeRoot beeRoot(final IAlleleBeeSpecies species, final IAlleleFloat speed,
        final IAlleleInteger lifespan) {
        final IBeeGenome genome = proxy(IBeeGenome.class, (proxy, method, args) -> switch (method.getName()) {
            case "getPrimary", "getSecondary" -> species;
            case "getSpeed" -> 1f;
            case "getLifespan" -> 1;
            default -> throw new UnsupportedOperationException(method.toString());
        });
        final IAllele[] template = new IAllele[EnumBeeChromosome.values().length];
        template[EnumBeeChromosome.SPECIES.ordinal()] = species;
        template[EnumBeeChromosome.SPEED.ordinal()] = speed;
        template[EnumBeeChromosome.LIFESPAN.ordinal()] = lifespan;
        return proxy(IBeeRoot.class, (proxy, method, args) -> switch (method.getName()) {
            case "getTemplate" -> template;
            case "templateAsGenome" -> genome;
            case "createBeeHousingModifier" -> new DefaultBeeModifier();
            case "createBeekeepingLogic" -> null;
            default -> throw new UnsupportedOperationException(method.toString());
        });
    }

    private static IErrorStateRegistry errorStateRegistry() {
        return proxy(IErrorStateRegistry.class, (proxy, method, args) -> null);
    }

    private static IBeekeepingMode beekeepingMode() {
        return proxy(IBeekeepingMode.class, (proxy, method, args) -> switch (method.getName()) {
            case "getBeeModifier" -> new DefaultBeeModifier();
            default -> throw new UnsupportedOperationException(method.toString());
        });
    }

    private static <T> T proxy(final Class<T> type, final InvocationHandler handler) {
        final InvocationHandler objectSafe = (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "equals" -> proxy == (args == null ? null : args[0]);
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> type.getSimpleName() + " fixture";
                    default -> throw new UnsupportedOperationException(method.toString());
                };
            }
            return handler.invoke(proxy, method, args);
        };
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, objectSafe));
    }
}
