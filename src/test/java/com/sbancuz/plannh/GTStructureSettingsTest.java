package com.sbancuz.plannh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.properties.RecipeProperty;
import com.sbancuz.plannh.data.provider.gregtech.GTNodeState;
import com.sbancuz.plannh.data.provider.gregtech.GTSettings;
import com.sbancuz.plannh.data.provider.gregtech.StructureState;

import gregtech.api.logic.ModifierKind;

/**
 * Every structure row stores its kind's number in GregTech's numbering, so a coil row and a solenoid row work the same.
 * Charts that stored the coil row as GregTech's coil level name are migrated on load.
 */
class GTStructureSettingsTest {

    private static final RecipeContext EMPTY = new RecipeContext(new HashMap<RecipeProperty<?>, Object>());

    @Test
    void aStoredValueIsWhatThePlayerChose() {
        final GTNodeState node = GTNodeState.load(new JsonObject());
        node.setStructure(ModifierKind.COIL.id, 2L);
        final StructureState state = GTSettings.resolve(EMPTY, node, 5, 0);

        assertEquals(
            2L,
            state.structure()
                .get(ModifierKind.COIL));
    }

    /** Unset values open on the chart's floor, or the best value on a chart with no floor. */
    @Test
    void unsetValuesOpenOnTheChartsFloor() {
        final StructureState state = GTSettings.resolve(EMPTY, GTNodeState.load(new JsonObject()), 5, 0);

        assertTrue(
            state.structure()
                .isEmpty());
        assertEquals(
            ModifierKind.COIL.getRange()
                .max(),
            state.floors()
                .get(ModifierKind.COIL));
        assertEquals(
            ModifierKind.PIPE_CASING.getRange()
                .max(),
            state.floors()
                .get(ModifierKind.PIPE_CASING));
        assertFalse(
            state.floors()
                .containsKey(ModifierKind.LENGTH),
            "anything else reads as the spec's best");
        assertEquals(5, state.voltageTier());
    }
}
