package com.sbancuz.plannh.data.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;

import WayofTime.alchemicalWizardry.api.alchemy.energy.Reagent;
import WayofTime.alchemicalWizardry.client.nei.NEIAlchemyRecipeHandler;
import WayofTime.alchemicalWizardry.client.nei.NEIAltarRecipeHandler;
import WayofTime.alchemicalWizardry.client.nei.NEIBindingRitualHandler;
import WayofTime.alchemicalWizardry.client.nei.NEICalcinatorHandler;
import codechicken.nei.PositionedStack;

class BloodMagicProviderTest {

    @BeforeAll
    static void initializeMinecraftFixture() {
        ProviderTestFixture.bootstrap();
    }

    @Test
    void profilesKeepAltarIdentityAndSeparateWritingTableAndCalcinator() {
        final BloodMagicProvider provider = new BloodMagicProvider();

        assertEquals(
            "bloodmagic:altar",
            provider.getProfiles(new NEIAltarRecipeHandler(), 0)
                .getFirst()
                .id());
        assertEquals(
            "bloodmagic:alchemy",
            provider.getProfiles(new NEIAlchemyRecipeHandler(), 0)
                .getFirst()
                .id());
        assertEquals(
            "bloodmagic:calcinator",
            provider.getProfiles(new NEICalcinatorHandler(), 0)
                .getFirst()
                .id());
        assertTrue(
            provider.getProfiles(new NEIBindingRitualHandler(), 0)
                .isEmpty());
    }

    @Test
    void lpRatesAreSourceTotalsAmortizedOverEachMachineDuration() {
        final BloodMagicProvider provider = new BloodMagicProvider();
        final MachineProfile alchemy = provider.getProfiles(new NEIAlchemyRecipeHandler(), 0)
            .getFirst();
        final MachineProfile calcinator = provider.getProfiles(new NEICalcinatorHandler(), 0)
            .getFirst();

        final RecipeContext alchemyRecipe = RecipeContext.builder()
            .property(BloodMagicProvider.LP_AMOUNT, 2500)
            .property(RecipePropertyAPI.DURATION_TICKS, 100)
            .build();
        final RecipeContext calcinatorRecipe = RecipeContext.builder()
            .property(BloodMagicProvider.LP_AMOUNT, BloodMagicProvider.CALCINATOR_LP_AMOUNT)
            .property(RecipePropertyAPI.DURATION_TICKS, BloodMagicProvider.CALCINATOR_TICKS)
            .build();

        assertEquals(
            25d,
            new MachineConfig(alchemy).computeEffect(alchemyRecipe)
                .energyPerT());
        assertEquals(
            10d,
            new MachineConfig(calcinator).computeEffect(calcinatorRecipe)
                .energyPerT());
    }

    @Test
    void calcinatorReagentAmountIsATypedResourceOutput() {
        final Reagent reagent = new Reagent("test", 1, 2, 3, 255);
        final var port = BloodMagicProvider
            .reagentPort(reagent, 750, new PositionedStack(new ItemStack(Items.redstone), 0, 0));

        assertEquals(BloodMagicProvider.REAGENT, port.getType());
        assertEquals(750, port.getAmount());
        assertEquals("test", port.getDisplayName());
    }
}
