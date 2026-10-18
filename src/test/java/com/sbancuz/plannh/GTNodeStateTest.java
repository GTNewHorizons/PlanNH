package com.sbancuz.plannh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.sbancuz.plannh.data.provider.gregtech.GTNodeState;
import com.sbancuz.plannh.data.provider.gregtech.GTOverride;

import gregtech.api.logic.ModifierKind;

/**
 * A GregTech node stores only what the player set. Everything else follows the machine, so a stored value is a
 * choice, and a save holds no number that goes stale when the machine or the chart changes.
 */
class GTNodeStateTest {

    private static GTNodeState fresh() {
        return GTNodeState.load(new JsonObject());
    }

    @Test
    void anUntouchedNodeSavesNothing() {
        assertTrue(
            fresh().save()
                .entrySet()
                .isEmpty());
    }

    /** A value equal to the machine's is still the player's: it stays when the machine changes. */
    @Test
    void aStoredValueIsKeptWhateverItEquals() {
        final GTNodeState node = fresh();
        node.setOverride(GTOverride.MAX_TIER_SKIPS, 0);

        assertEquals(
            0,
            GTNodeState.load(node.save())
                .getOverride(GTOverride.MAX_TIER_SKIPS));
    }

    /** Saved under the kind's id and the quantity's name, so a save does not depend on PlanNH's class layout. */
    @Test
    void aSaveUsesPlainKeys() {
        final GTNodeState node = fresh();
        node.setStructure(ModifierKind.COIL.id, 3L);
        node.setOverride(GTOverride.EU_PER_OC, 400);
        node.setVoltage("IV");

        final JsonObject saved = node.save();

        assertEquals(
            3L,
            saved.getAsJsonObject("structure")
                .get(ModifierKind.COIL.id)
                .getAsLong());
        assertEquals(
            400,
            saved.getAsJsonObject("OVERCLOCK")
                .get(GTOverride.EU_PER_OC.key)
                .getAsInt());
        assertEquals(node, GTNodeState.load(saved));
    }

    /**
     * Another machine has other coils, parallels and overclock rules, so values set for the last one would plan
     * numbers this one can't reach. Voltage is the power the node is given, so it stays.
     */
    @Test
    void pickingAnotherMachineKeepsOnlyTheVoltage() {
        final GTNodeState node = fresh();
        node.setVoltage("LuV");
        node.setAmps(4);
        node.setStructure(ModifierKind.COIL.id, 3L);
        node.setOverride(GTOverride.PARALLEL, 8);

        node.pickMachine("gt.blockmachines.multimachine.blastfurnace");

        assertEquals("LuV", node.getVoltage());
        assertNull(node.getAmps());
        assertNull(node.getStructure(ModifierKind.COIL.id));
        assertNull(node.getOverride(GTOverride.PARALLEL));
    }

    /** Machine groups copy one node's settings onto another. An edit to one must not reach the other. */
    @Test
    void aCopyIsEqualButIndependent() {
        final GTNodeState node = fresh();
        node.setStructure(ModifierKind.COIL.id, 3L);
        final GTNodeState copy = node.copy();

        assertEquals(node, copy);
        assertNotSame(node, copy);
        copy.setStructure(ModifierKind.COIL.id, 5L);
        assertEquals(3L, node.getStructure(ModifierKind.COIL.id));
    }
}
