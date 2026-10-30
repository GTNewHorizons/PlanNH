package com.sbancuz.plannh.data.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.steps.CoFHCompat;
import com.sbancuz.plannh.data.setting.Settings;

import codechicken.nei.recipe.ShapedRecipeHandler;
import crazypants.enderio.config.Config;
import crazypants.enderio.nei.VatRecipeHandler;
import crazypants.enderio.power.Capacitors;
import sun.misc.Unsafe;

class EnderIOProviderTest {

    private static final Unsafe UNSAFE = unsafe();
    private static MachineProfile enderProfile;

    @BeforeAll
    static void initializeProfile() {
        ProviderTestFixture.bootstrap();
        enderProfile = new EnderIOProvider().getProfiles(allocate(VatRecipeHandler.class), 0)
            .getFirst();
    }

    @Test
    void capacitorChangesUpdateDerivedRateAndDuration() {
        final RecipeContext ctx = contextWithEnergy(100L);
        final MachineConfig cfg = new MachineConfig(enderProfile);
        cfg.set(Settings.EIO_CAPACITOR, 0);

        final int firstRate = enderProfile.value(Settings.RF_PER_TICK, cfg, ctx);
        cfg.set(Settings.EIO_CAPACITOR, 1);
        final int secondRate = enderProfile.value(Settings.RF_PER_TICK, cfg, ctx);

        assertEquals(Capacitors.VALUES[0].capacitor.getMaxEnergyExtracted(), firstRate);
        assertEquals(Capacitors.VALUES[1].capacitor.getMaxEnergyExtracted(), secondRate);
        assertEquals(ceil(100, secondRate), enderProfile.value(Settings.DURATION_TICKS, cfg, ctx));
    }

    @Test
    void childOverridesSurviveParentChangesAndResetRestoresDerivation() {
        final RecipeContext ctx = contextWithEnergy(100L);
        final MachineConfig cfg = new MachineConfig(enderProfile);
        cfg.set(Settings.EIO_CAPACITOR, 0);
        cfg.set(Settings.RF_PER_TICK, 7);

        cfg.set(Settings.EIO_CAPACITOR, 2);
        assertEquals(7, enderProfile.value(Settings.RF_PER_TICK, cfg, ctx));
        assertEquals(ceil(100, 7), enderProfile.value(Settings.DURATION_TICKS, cfg, ctx));
        assertEquals(
            ceil(100, 7),
            cfg.computeEffect(ctx)
                .durationTicks());

        cfg.reset(Settings.RF_PER_TICK);
        final int derivedRate = enderProfile.value(Settings.RF_PER_TICK, cfg, ctx);
        assertEquals(ceil(100, derivedRate), enderProfile.value(Settings.DURATION_TICKS, cfg, ctx));
    }

    @Test
    void crafterUsesStrictCapacitorIntervalsAndConfiguredTotalRf() {
        final ItemStack input = new ItemStack(Items.iron_ingot);
        final ItemStack output = new ItemStack(ProviderTestFixture.item("enderio_crafter_output"));
        final IRecipe actualRecipe = new ShapedRecipes(1, 1, new ItemStack[] { input.copy() }, output.copy());
        final ShapedRecipeHandler handler = shapedHandler(input, output);
        CraftingManager.getInstance()
            .getRecipeList()
            .add(actualRecipe);
        final MachineProfile crafter = new EnderIOProvider().getProfiles(handler, 0)
            .getFirst();
        final int previousRf = Config.crafterRfPerCraft;
        Config.crafterRfPerCraft = 2100;
        try {
            final RecipeContext ctx = RecipeContext.builder()
                .build();
            final MachineConfig cfg = new MachineConfig(crafter);
            cfg.set(Settings.EIO_CAPACITOR, 0);
            assertEquals(
                21,
                cfg.computeEffect(ctx)
                    .durationTicks());
            assertEquals(
                100d,
                cfg.computeEffect(ctx)
                    .energyPerT());

            cfg.set(Settings.EIO_CAPACITOR, 2);
            assertEquals(
                3,
                cfg.computeEffect(ctx)
                    .durationTicks());
            assertEquals(
                700d,
                cfg.computeEffect(ctx)
                    .energyPerT());
        } finally {
            Config.crafterRfPerCraft = previousRf;
            CraftingManager.getInstance()
                .getRecipeList()
                .remove(actualRecipe);
        }
    }

    @Test
    void crafterSupportsContainerReturnsAndRejectsOversizedHandlers() {
        final EnderIOProvider provider = new EnderIOProvider();
        assertTrue(provider.canCraft(shapedHandler(new ItemStack(Items.water_bucket)), 0));
        assertFalse(provider.canCraft(oversizedHandler(), 0));
    }

    private static ShapedRecipeHandler shapedHandler(final ItemStack ingredient) {
        return shapedHandler(ingredient, new ItemStack(ProviderTestFixture.item("enderio_crafter_output")));
    }

    private static ShapedRecipeHandler shapedHandler(final ItemStack ingredient, final ItemStack output) {
        final ShapedRecipeHandler handler = allocate(ShapedRecipeHandler.class);
        handler.arecipes = new ArrayList<>();
        handler.arecipes.add(handler.new CachedShapedRecipe(1, 1, new Object[] { ingredient }, output));
        return handler;
    }

    static class SpecialShapedHandler extends ShapedRecipeHandler {
    }

    private static ShapedRecipeHandler oversizedHandler() {
        final SpecialShapedHandler handler = allocate(SpecialShapedHandler.class);
        handler.arecipes = new ArrayList<>();
        handler.arecipes.add(
            handler.new CachedShapedRecipe(4, 1,
                new Object[] { new ItemStack(ProviderTestFixture.item("enderio_9x9_a")),
                    new ItemStack(ProviderTestFixture.item("enderio_9x9_b")),
                    new ItemStack(ProviderTestFixture.item("enderio_9x9_c")),
                    new ItemStack(ProviderTestFixture.item("enderio_9x9_d")) },
                new ItemStack(ProviderTestFixture.item("enderio_9x9_output"))));
        return handler;
    }

    private static RecipeContext contextWithEnergy(final long energy) {
        return RecipeContext.builder()
            .property(CoFHCompat.RF_COST, energy)
            .build();
    }

    private static int ceil(final long energy, final int rate) {
        return (int) ((energy - 1) / rate + 1);
    }

    @SuppressWarnings("unchecked")
    private static <T> T allocate(final Class<T> type) {
        try {
            return (T) UNSAFE.allocateInstance(type);
        } catch (final InstantiationException failure) {
            throw new AssertionError("Could not allocate native handler fixture", failure);
        }
    }

    private static Unsafe unsafe() {
        try {
            final Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (final ReflectiveOperationException failure) {
            throw new AssertionError("Could not access the test fixture runtime", failure);
        }
    }
}
