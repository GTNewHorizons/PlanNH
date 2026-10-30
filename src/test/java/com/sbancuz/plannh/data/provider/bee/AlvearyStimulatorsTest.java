package com.sbancuz.plannh.data.provider.bee;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import binnie.extrabees.apiary.machine.stimulator.CircuitType;
import binnie.extrabees.apiary.machine.stimulator.StimulatorCircuit;
import binnie.extrabees.apiary.machine.stimulator.StimulatorModifierComponent;
import forestry.api.apiculture.BeeManager;
import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.IBeeModifier;
import forestry.api.apiculture.IBeeRoot;
import forestry.apiculture.BeeHousingModifier;
import sun.misc.Unsafe;

class AlvearyStimulatorsTest {

    private static final Unsafe UNSAFE = unsafe();

    @Test
    void boardModifierMatchesNativeStimulatorCaps() throws Exception {
        final StimulatorCircuit[] highVoltage = circuits(CircuitType.HighVoltage, 4);
        final StimulatorCircuit[] life = circuits(CircuitType.Life, 4);
        final var previous = BeeManager.beeRoot;
        BeeManager.beeRoot = nativeRoot();
        try {
            final IBeeModifier nativeHigh = nativeStimulator(highVoltage);
            final IBeeModifier boardHigh = AlvearyStimulators.boardModifier(Arrays.asList(highVoltage));
            assertEquals(6, nativeHigh.getProductionModifier(null, 9), 0.0001);
            assertEquals(6, boardHigh.getProductionModifier(null, 9), 0.0001);

            final IBeeModifier nativeLife = nativeStimulator(life);
            final IBeeModifier boardLife = AlvearyStimulators.boardModifier(Arrays.asList(life));
            assertEquals(5, nativeLife.getLifespanModifier(null, null, 9), 0.0001);
            assertEquals(5, boardLife.getLifespanModifier(null, null, 9), 0.0001);
        } finally {
            BeeManager.beeRoot = previous;
        }
    }

    private static StimulatorCircuit[] circuits(final CircuitType type, final int count) throws Exception {
        final StimulatorCircuit[] circuits = new StimulatorCircuit[count];
        for (int index = 0; index < count; index++) {
            final StimulatorCircuit circuit = (StimulatorCircuit) UNSAFE.allocateInstance(StimulatorCircuit.class);
            field(StimulatorCircuit.class, "type").set(circuit, type);
            circuits[index] = circuit;
        }
        return circuits;
    }

    private static IBeeModifier nativeStimulator(final StimulatorCircuit[] circuits) throws Exception {
        final StimulatorModifierComponent component = (StimulatorModifierComponent) UNSAFE
            .allocateInstance(StimulatorModifierComponent.class);
        field(StimulatorModifierComponent.class, "powered").setBoolean(component, true);
        field(StimulatorModifierComponent.class, "modifiers").set(component, circuits);
        return component;
    }

    private static Field field(final Class<?> type, final String name) throws NoSuchFieldException {
        final Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
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

    private static IBeeRoot nativeRoot() {
        return (IBeeRoot) Proxy.newProxyInstance(
            IBeeRoot.class.getClassLoader(),
            new Class<?>[] { IBeeRoot.class },
            (proxy, method, args) -> {
                if (method.getName()
                    .equals("createBeeHousingModifier")) return new BeeHousingModifier((IBeeHousing) args[0]);
                throw new UnsupportedOperationException(method.toString());
            });
    }
}
