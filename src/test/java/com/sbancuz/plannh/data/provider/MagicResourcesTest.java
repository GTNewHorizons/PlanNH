package com.sbancuz.plannh.data.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

import com.sbancuz.plannh.data.flowchart.resource.MagicResources;

import codechicken.nei.PositionedStack;

class MagicResourcesTest {

    @Test
    void nativeAspectDisplaysUseTheRecipeVariantAndOnlySpecialUnitsGetAFooter() {
        final var aspectItem = ProviderTestFixture.item("aspect_display");
        final var stack = MagicResources.aspectDisplay(aspectItem, "ignis", 4, 64, 66);
        assertEquals(aspectItem, stack.item.getItem());
        assertEquals(1, stack.item.getItemDamage());
        assertEquals(4, stack.item.stackSize);
        assertEquals(
            "ignis",
            stack.item.getTagCompound()
                .getString("Aspect"));
        final var port = MagicResources
            .port(MagicResources.NETWORK_VIS, "thaumcraft:ignis", "Ignis", 0xFF5555, 4, stack);
        assertEquals("Ignis", port.getDisplayName());
        assertEquals("plannh.resources.unit.centivis", MagicResources.unitKey(MagicResources.NETWORK_VIS));
        assertEquals("plannh.resources.unit.aura", MagicResources.unitKey(MagicResources.AURA_CHARGE));
        assertNull(MagicResources.unitKey(MagicResources.ESSENTIA));
        assertNull(MagicResources.unitKey(MagicResources.STORED_VIS));
    }

    @Test
    void identicalAspectNamesDoNotConnectDifferentResourceChannels() {
        final var channels = List.of(
            MagicResources.ESSENTIA,
            MagicResources.STORED_VIS,
            MagicResources.NETWORK_VIS,
            MagicResources.AURA_CHARGE);
        for (int a = 0; a < channels.size(); a++) {
            for (int b = 0; b < channels.size(); b++) {
                final var first = MagicResources.port(channels.get(a), "ignis", "Ignis", 0xFF5555, 1, display());
                final var second = MagicResources.port(channels.get(b), "ignis", "Ignis", 0xFF5555, 2, display());
                assertEquals(a == b, first.canConnect(second));
            }
        }
    }

    @Test
    void typedAmountsPreserveLongQuantitiesWhenMerged() {
        final long amount = (1L << 40) + 1;
        final var first = MagicResources.port(MagicResources.ESSENTIA, "ignis", "Ignis", 0xFF5555, amount, display());
        final var second = MagicResources.port(MagicResources.ESSENTIA, "ignis", "Ignis", 0xFF5555, 7, display());
        first.merge(second);
        assertEquals(amount + 7, first.getAmount());
        assertEquals(
            2,
            first.getAllStacks()
                .size());
    }

    private static PositionedStack display() {
        return new PositionedStack(new ItemStack(ProviderTestFixture.item("magic_resource_display")), 0, 0);
    }
}
