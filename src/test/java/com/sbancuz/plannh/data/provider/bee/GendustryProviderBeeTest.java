package com.sbancuz.plannh.data.provider.bee;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.bdew.gendustry.api.ApiaryModifiers;
import net.bdew.gendustry.api.items.IApiaryUpgrade;
import net.bdew.gendustry.nei.MutatronHandler;
import net.bdew.neiaddons.forestry.bees.BeeProduceHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.setting.ChoiceSettingDef;
import com.sbancuz.plannh.data.setting.IntegerSettingDef;

class GendustryProviderBeeTest {

    @Test
    void processingHandlersRemainAcceptedAlongsideBeeProduction() {
        final GendustryProvider provider = new GendustryProvider();
        final MutatronHandler handler = new MutatronHandler();
        assertTrue(provider.canCraft(handler, 0));
        assertEquals(
            "gendustry:basic",
            provider.getProfiles(handler, 0)
                .getFirst()
                .id());
    }

    @Test
    void industrialApiaryPowerUsesConfiguredMjToRfRatio() {
        assertEquals(20d, GendustryProvider.rfPerTick(2, 1, 10), 1e-9);
    }

    @Test
    void industrialApiaryValidatesUpgradeStacksAgainstTheirNativeLimit() {
        final UpgradeItem limited = new UpgradeItem(1);
        assertThrows(
            IllegalArgumentException.class,
            () -> GendustryProvider.validateUpgradeStacks(List.of(new ItemStack(limited), new ItemStack(limited))));

        final UpgradeItem roomy = new UpgradeItem(2);
        assertDoesNotThrow(
            () -> GendustryProvider.validateUpgradeStacks(List.of(new ItemStack(roomy), new ItemStack(roomy))));
    }

    @Test
    void upgradeSelectionClampsCountAndResetClearsItsHiddenCount() {
        final var profile = new GendustryProvider().getProfiles(new BeeProduceHandler(), 0)
            .getFirst();
        final var defs = profile.settings()
            .stream()
            .map(entry -> entry.def())
            .filter(
                def -> def.getKey()
                    .equals("gendustry_apiary_upgrade_1"))
            .findFirst()
            .orElseThrow()
            .editedSettings();
        @SuppressWarnings("unchecked")
        final ChoiceSettingDef<ItemStack> upgrade = (ChoiceSettingDef<ItemStack>) defs.stream()
            .filter(
                def -> def.getKey()
                    .equals("gendustry_apiary_upgrade_1"))
            .findFirst()
            .orElseThrow();
        final IntegerSettingDef count = (IntegerSettingDef) defs.stream()
            .filter(
                def -> def.getKey()
                    .equals("gendustry_apiary_upgrade_1_count"))
            .findFirst()
            .orElseThrow();
        final MachineConfig cfg = new MachineConfig(profile);
        final RecipeContext ctx = RecipeContext.builder()
            .build();
        final ItemStack selected = new ItemStack(new UpgradeItem(1), 1, 0);

        cfg.set(count, 8);
        GendustryProvider.selectUpgrade(0, cfg, ctx, selected);
        assertEquals(1, cfg.getOrDefault(count));
        assertTrue(cfg.isExplicit(upgrade));

        GendustryProvider.resetUpgrade(0, cfg);
        assertFalse(cfg.isExplicit(upgrade));
        assertFalse(cfg.isExplicit(count));
    }

    @Test
    void upgradePickerRejectsNativeTotalConflicts() {
        final var profile = new GendustryProvider().getProfiles(new BeeProduceHandler(), 0)
            .getFirst();
        final var defs = profile.settings()
            .stream()
            .map(entry -> entry.def())
            .filter(
                def -> def.getKey()
                    .equals("gendustry_apiary_upgrade_1"))
            .findFirst()
            .orElseThrow()
            .editedSettings();
        @SuppressWarnings("unchecked")
        final ChoiceSettingDef<ItemStack> first = (ChoiceSettingDef<ItemStack>) defs.get(0);
        @SuppressWarnings("unchecked")
        final ChoiceSettingDef<ItemStack> second = (ChoiceSettingDef<ItemStack>) defs.get(1);
        final MachineConfig cfg = new MachineConfig(profile);
        final RecipeContext ctx = RecipeContext.builder()
            .build();
        final ItemStack selected = new ItemStack(new UpgradeItem(1), 1, 0);

        cfg.set(first, selected);
        assertFalse(GendustryProvider.acceptsUpgrade(1, cfg, ctx, selected));
        assertTrue(cfg.isExplicit(first));
        assertFalse(cfg.isExplicit(second));
    }

    @Test
    void upgradeSlotsRejectOrdinaryItemsAndEmptyDrops() {
        final var profile = new GendustryProvider().getProfiles(new BeeProduceHandler(), 0)
            .getFirst();
        final MachineConfig cfg = new MachineConfig(profile);
        final RecipeContext ctx = RecipeContext.builder()
            .build();
        assertFalse(GendustryProvider.acceptsUpgrade(0, cfg, ctx, new ItemStack(new Item())));
        assertFalse(GendustryProvider.acceptsUpgrade(0, cfg, ctx, null));
    }

    private static final class UpgradeItem extends Item implements IApiaryUpgrade {

        private final int max;

        private UpgradeItem(final int max) {
            this.max = max;
        }

        @Override
        public String getDisplayName(final ItemStack stack) {
            return "test";
        }

        @Override
        public List<String> getDisplayDetails(final ItemStack stack) {
            return List.of();
        }

        @Override
        public long getStackingId(final ItemStack stack) {
            return 1;
        }

        @Override
        public int getMaxNumber(final ItemStack stack) {
            return max;
        }

        @Override
        public void applyModifiers(final ApiaryModifiers modifiers, final ItemStack stack) {}
    }
}
