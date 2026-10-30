package com.sbancuz.plannh.data.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sbancuz.plannh.data.MachineProfile;

import codechicken.nei.recipe.FurnaceRecipeHandler;
import sun.misc.Unsafe;

class GalacticraftProviderTest {

    private static final Unsafe UNSAFE = unsafe();

    @Test
    void vanillaSmeltingOffersBothGalacticraftFurnaces() {
        final List<MachineProfile> profiles = new GalacticraftProvider()
            .getProfiles(allocate(FurnaceRecipeHandler.class), 0);

        assertEquals(
            List.of("gc:electric_furnace", "gc:electric_arc_furnace"),
            profiles.stream()
                .map(MachineProfile::id)
                .toList());
    }

    @Test
    void arcFurnaceOnlyDoublesSourceQualifyingSmelts() {
        assertTrue(GalacticraftProvider.arcFurnaceDoubles("mod.oreCopper", "mod.ingotCopper"));
        assertTrue(GalacticraftProvider.arcFurnaceDoubles("mod.moonRock", "mod.ingotMoon"));
        assertFalse(GalacticraftProvider.arcFurnaceDoubles("mod.dustCopper", "mod.ingotCopper"));
        assertFalse(GalacticraftProvider.arcFurnaceDoubles("mod.oreCopper", "mod.nuggetCopper"));
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
