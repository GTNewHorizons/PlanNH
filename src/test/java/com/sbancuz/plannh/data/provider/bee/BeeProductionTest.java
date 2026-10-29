package com.sbancuz.plannh.data.provider.bee;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

import com.gtnewhorizon.gtnhlib.util.data.ItemId;

import forestry.apiculture.genetics.Bee;

class BeeProductionTest {

    @Test
    void fractionalLifespanUsesTheNativeStoppingTimeEstimator() {
        assertEquals(10d, BeeProvider.expectedWorkCycles(10, 1f), 1e-9);
        assertEquals(20d, BeeProvider.expectedWorkCycles(10, 2f), 1e-9);
        assertEquals(1d, BeeProvider.expectedWorkCycles(10, 0f), 1e-9);
        assertEquals(1.75d, BeeProvider.expectedWorkCycles(2, 0.8f), 1e-9);
    }

    @Test
    void sharedProductsApplyChanceBeforeSummingAndIgnoreStackSizeInIdentity() {
        final Item item = new Item();
        final Map<ItemId, Double> amounts = new HashMap<>();
        BeeProvider.addExpectedProducts(amounts, Map.of(new ItemStack(item, 2, 0), 0.25f), 1, 1, 2);
        BeeProvider.addExpectedProducts(amounts, Map.of(new ItemStack(item, 3, 0), 0.25f), 0.5f, 1, 2);

        assertEquals(1, amounts.size());
        assertEquals(
            2d * Bee.getFinalChance(0.25f, 1, 2, 1) + 3d * Bee.getFinalChance(0.125f, 1, 2, 1),
            amounts.get(ItemId.create(item, 0, null)),
            1e-9);
    }

}
