package com.sbancuz.plannh.data.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import codechicken.nei.PositionedStack;

class MagicResourcesTest {

    @BeforeAll
    static void initializeMinecraftFixture() {
        ProviderTestFixture.bootstrap();
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

    private static PositionedStack display() {
        return new PositionedStack(new ItemStack(Items.redstone), 0, 0);
    }
}
