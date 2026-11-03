package com.sbancuz.plannh.data.provider.bee;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import net.bdew.gendustry.api.ApiaryModifiers;
import net.bdew.gendustry.api.items.IApiaryUpgrade;
import net.bdew.neiaddons.forestry.bees.BeeProduceHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.provider.ProviderTestFixture;
import com.sbancuz.plannh.data.setting.ChoiceSettingDef;

class GendustryProviderBeeTest {

    @BeforeAll
    static void initializeMinecraftFixture() {
        ProviderTestFixture.bootstrap();
    }

    @Test
    void upgradeSelectionKeepsCountAndResetClearsTheChoice() {
        final var profile = new GendustryProvider().getProfiles(new BeeProduceHandler(), 0)
            .getFirst();
        @SuppressWarnings("unchecked")
        final ChoiceSettingDef<ItemStack> upgrade = (ChoiceSettingDef<ItemStack>) profile.settings()
            .stream()
            .map(entry -> entry.def())
            .filter(def -> def instanceof ChoiceSettingDef<?>choice && choice.isCounted())
            .findFirst()
            .orElseThrow();
        final MachineConfig cfg = new MachineConfig(profile);
        final RecipeContext ctx = RecipeContext.builder()
            .build();
        final UpgradeItem item = new UpgradeItem(4);
        final ItemStack selected = new ItemStack(item, 8, 0);

        GendustryProvider.selectUpgrade(0, cfg, ctx, selected);
        assertEquals(4, cfg.value(upgrade, ctx).stackSize);
        upgrade.setCount(cfg, ctx, 2);
        assertEquals(2, cfg.value(upgrade, ctx).stackSize);
        assertEquals(8, selected.stackSize);

        GendustryProvider.resetUpgrade(0, cfg);
        assertEquals(0, upgrade.count(cfg, ctx));
    }

    @Test
    void upgradePickerRejectsNativeTotalConflicts() {
        final var profile = new GendustryProvider().getProfiles(new BeeProduceHandler(), 0)
            .getFirst();
        @SuppressWarnings("unchecked")
        final ChoiceSettingDef<ItemStack> first = (ChoiceSettingDef<ItemStack>) profile.settings()
            .stream()
            .map(entry -> entry.def())
            .filter(def -> def instanceof ChoiceSettingDef<?>choice && choice.isCounted())
            .findFirst()
            .orElseThrow();
        final MachineConfig cfg = new MachineConfig(profile);
        final RecipeContext ctx = RecipeContext.builder()
            .build();
        final ItemStack selected = new ItemStack(new UpgradeItem(1), 1, 0);

        cfg.set(first, selected);
        assertFalse(GendustryProvider.acceptsUpgrade(1, cfg, ctx, selected));
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
