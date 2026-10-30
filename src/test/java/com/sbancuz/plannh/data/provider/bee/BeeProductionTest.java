package com.sbancuz.plannh.data.provider.bee;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import net.bdew.neiaddons.forestry.bees.BeeProduceHandler;

import org.junit.jupiter.api.Test;

class BeeProductionTest {

    @Test
    void forestryAndExtraBeesShareTheSameHousingChoices() {
        final BeeProduceHandler handler = new BeeProduceHandler();
        final List<String> expected = List.of("forestry:apiary", "forestry:beehouse", "forestry:alveary");

        assertEquals(expected, ids(new BeeProvider(() -> null), handler));
        assertEquals(expected, ids(new ExtraBeesProvider(() -> null), handler));
    }

    private static List<String> ids(final BeeProvider provider, final BeeProduceHandler handler) {
        return provider.getProfiles(handler, 0)
            .stream()
            .map(profile -> profile.id())
            .toList();
    }
}
