package com.sbancuz.plannh.data.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.EffectResult;
import com.sbancuz.plannh.data.flowchart.Port;
import com.sbancuz.plannh.data.flowchart.resource.LongFluidStack;
import com.sbancuz.plannh.data.setting.Settings;
import com.sbancuz.plannh.harness.TestIngredients;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.FurnaceRecipeHandler;
import sun.misc.Unsafe;

class RailcraftProviderTest {

    private static final Unsafe UNSAFE = unsafe();

    @BeforeAll
    static void initializeMinecraftFixture() {
        ProviderTestFixture.bootstrap();
    }

    @Test
    void steamOvenModelsSelectedBatchAndIndependentMachineCount() {
        final MachineProfile profile = new RailcraftProvider().getProfiles(allocate(FurnaceRecipeHandler.class), 0)
            .getFirst();
        final MachineConfig config = new MachineConfig(profile);
        config.set(RailcraftProvider.STEAM_OVEN_BATCH_SIZE, 9);
        config.set(Settings.MACHINES, 3);

        final RecipeContext recipe = RecipeContext.builder()
            .input(TestIngredients.port("ore", 1))
            .output(TestIngredients.port("ingot", 1))
            .build();
        final EffectResult effect = config.computeEffect(recipe);

        assertEquals("railcraft:steam_oven", profile.id());
        assertEquals(1, RailcraftProvider.STEAM_OVEN_BATCH_SIZE.getMin());
        assertEquals(9, RailcraftProvider.STEAM_OVEN_BATCH_SIZE.getMax());
        assertEquals(256, effect.durationTicks());
        assertEquals(3, effect.throughputFactor());
        assertEquals(9d, effect.inputMultiplier(0));
        assertEquals(9d, effect.outputMultiplier(0));
    }

    @Test
    void partialAndFullBatchesConsumeTheSameSteamWithoutScalingItAsAnItem() {
        final MachineProfile profile = new RailcraftProvider().getProfiles(allocate(FurnaceRecipeHandler.class), 0)
            .getFirst();
        final MachineConfig config = new MachineConfig(profile);
        final FluidStack steamDisplay = new FluidStack(FluidRegistry.WATER, 8000);
        final RecipeContext recipe = RecipeContext.builder()
            .input(TestIngredients.port("ore", 1))
            .input(
                new Port<>(
                    RecipePropertyAPI.FLUID,
                    new LongFluidStack(steamDisplay, 8000),
                    1f,
                    new PositionedStack(new ItemStack(Items.water_bucket), 0, 0)))
            .output(TestIngredients.port("ingot", 1))
            .build();
        for (final int batch : new int[] { 1, 4, 9 }) {
            config.set(RailcraftProvider.STEAM_OVEN_BATCH_SIZE, batch);
            final EffectResult effect = config.computeEffect(recipe);
            assertEquals(batch, effect.inputMultiplier(0));
            assertEquals(batch, effect.outputMultiplier(0));
            assertEquals(1d, effect.inputMultiplier(1));
            assertEquals(
                8000,
                recipe.inputs()
                    .get(1)
                    .getAmount());
        }
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
