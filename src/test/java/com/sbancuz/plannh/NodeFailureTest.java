package com.sbancuz.plannh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.flowchart.Node;
import com.sbancuz.plannh.data.flowchart.Port;
import com.sbancuz.plannh.data.flowchart.balancer.Pin;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.properties.RecipeProperty;
import com.sbancuz.plannh.data.setting.Settings;
import com.sbancuz.plannh.harness.GtnhFlowLoader;
import com.sbancuz.plannh.harness.TestIngredients;

import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.RecipeHandlerRef;

class NodeFailureTest {

    @Test
    void constructorRejectsUnresolvedInputlessRecipe() {
        final IRecipeHandler handler = fakeHandler();

        final Node node = new Node(handler, 0);
        assertFalse(node.isAvailable());
        assertNull(node.getHandlerRef());
    }

    @Test
    void unavailableInitLeavesSafeCollectionsAndDoesNotRetry() throws Exception {
        final Node node = harnessNode("unavailable");
        set(node, "initialized", false);
        set(node, "handlerRef", null);

        node.init();

        assertTrue(node.isInitialized());
        assertFalse(node.isAvailable());
        assertNull(node.getHandlerRef());
        assertNull(node.getExtractor());
        assertNotNullCollections(node);
        final List<Port<?>> inputs = node.getInputs();

        node.init();

        assertSame(inputs, node.getInputs(), "a failed node must not retry init");
        assertNotNullCollections(node);
    }

    @Test
    void failedExtractorSwitchDoesNotPublishPartialCandidate() throws Exception {
        final PropertyProvider oldExtractor = provider((node, handler, index) -> Map.of());
        final PropertyProvider failingExtractor = provider((node, handler, index) -> {
            node.getInputs()
                .add(TestIngredients.port("candidate-input", 1));
            node.getOutputs()
                .add(TestIngredients.port("candidate-output", 1));
            node.getProperties()
                .put(GtnhFlowLoader.DURATION_TICKS, 99);
            node.getMachineConfig()
                .set(Settings.MACHINES, 99);
            throw new IllegalStateException("extractor failed");
        });
        final Node node = configuredNode(oldExtractor, failingExtractor);

        final List<Port<?>> inputs = node.getInputs();
        final List<Port<?>> outputs = node.getOutputs();
        final Map<RecipeProperty<?>, Object> properties = node.getProperties();
        final MachineConfig machineConfig = node.getMachineConfig();
        final Map<Integer, ItemStack> inputConfigurations = node.getInputConfigurations();
        final ItemStack configuredInput = inputConfigurations.get(0);
        final int extractorIndex = node.getExtractorIndex();
        final boolean settingsOpen = node.isSettingsOpen();

        assertThrows(IllegalStateException.class, () -> node.switchExtractor(1));

        assertSame(inputs, node.getInputs());
        assertSame(outputs, node.getOutputs());
        assertSame(properties, node.getProperties());
        assertSame(machineConfig, node.getMachineConfig());
        assertSame(inputConfigurations, node.getInputConfigurations());
        assertSame(
            configuredInput,
            node.getInputConfigurations()
                .get(0));
        assertEquals(extractorIndex, node.getExtractorIndex());
        assertEquals(settingsOpen, node.isSettingsOpen());
        assertEquals(
            42,
            node.getProperties()
                .get(GtnhFlowLoader.DURATION_TICKS));
        assertEquals(
            7,
            node.getMachineConfig()
                .get(Settings.MACHINES));
    }

    @Test
    void successfulExtractorSwitchPublishesIsolatedCandidate() throws Exception {
        final PropertyProvider oldExtractor = provider((node, handler, index) -> Map.of());
        final PropertyProvider replacement = provider((node, handler, index) -> {
            node.getInputs()
                .add(TestIngredients.port("new-input", 2));
            node.getOutputs()
                .add(TestIngredients.port("new-output", 3));
            return Map.of(GtnhFlowLoader.DURATION_TICKS, 99);
        });
        final Node node = configuredNode(oldExtractor, replacement);

        final List<Port<?>> oldInputs = node.getInputs();
        final List<Port<?>> oldOutputs = node.getOutputs();
        final Map<RecipeProperty<?>, Object> oldProperties = node.getProperties();
        final MachineConfig oldMachineConfig = node.getMachineConfig();

        node.switchExtractor(1);

        assertEquals(1, node.getExtractorIndex());
        assertSame(replacement, node.getExtractor());
        assertNotSame(oldInputs, node.getInputs());
        assertNotSame(oldOutputs, node.getOutputs());
        assertNotSame(oldProperties, node.getProperties());
        assertNotSame(oldMachineConfig, node.getMachineConfig());
        assertEquals(
            "new-input",
            TestIngredients.nameOf(
                node.getInputs()
                    .get(0)));
        assertEquals(
            "new-output",
            TestIngredients.nameOf(
                node.getOutputs()
                    .get(0)));
        assertEquals(
            99,
            node.getProperties()
                .get(GtnhFlowLoader.DURATION_TICKS));
        assertTrue(
            node.getInputConfigurations()
                .isEmpty());

        final MachineConfig config = node.getMachineConfig();
        assertEquals(7, config.get(Settings.MACHINES));
        assertEquals(88, config.get(Settings.TICK_MODIFIER));
        assertEquals(
            4,
            config.getCopies()
                .copies());
        assertEquals(
            12.5,
            config.getRates()
                .rate(0),
            1e-9);
        assertEquals(Pin.TARGET_RATE, config.getTargetKind());
        assertEquals(0.5f, config.inputMultiplier(0), 1e-6f);
        assertEquals(1.25f, config.outputMultiplier(0), 1e-6f);
    }

    @Test
    void extractionCombinesMatchingPortsWithoutChangingPreviousPorts() throws Exception {
        final PropertyProvider original = provider((node, handler, index) -> Map.of());
        final PropertyProvider replacement = provider((node, handler, index) -> {
            node.getInputs()
                .add(TestIngredients.port("input", 2));
            node.getInputs()
                .add(TestIngredients.port("input", 3));
            node.getOutputs()
                .add(TestIngredients.port("output", 4));
            node.getOutputs()
                .add(TestIngredients.port("output", 5));
            return Map.of();
        });
        final Node node = configuredNode(original, replacement);
        final List<Port<?>> previousInputs = node.getInputs();

        assertTrue(node.switchExtractor(1));

        assertEquals(
            1,
            node.getInputs()
                .size());
        assertEquals(
            5,
            node.getInputs()
                .getFirst()
                .getAmount());
        assertEquals(
            2,
            node.getInputs()
                .getFirst()
                .getAllStacks()
                .size());
        assertEquals(
            1,
            node.getOutputs()
                .size());
        assertEquals(
            9,
            node.getOutputs()
                .getFirst()
                .getAmount());
        assertEquals(
            1,
            previousInputs.getFirst()
                .getAmount());
    }

    private static Node configuredNode(final PropertyProvider oldExtractor, final PropertyProvider replacement)
        throws Exception {
        final Node node = harnessNode("switchable");
        node.getInputs()
            .add(TestIngredients.port("old-input", 1));
        node.getOutputs()
            .add(TestIngredients.port("old-output", 1));
        node.getProperties()
            .put(GtnhFlowLoader.DURATION_TICKS, 42);
        node.setSettingsOpen(false);

        final MachineConfig config = node.getMachineConfig();
        config.set(Settings.MACHINES, 7);
        config.set(Settings.TICK_MODIFIER, 88);
        config.getCopies()
            .setCopies(4);
        config.getRates()
            .rates()
            .put(0, 12.5);
        config.setTargetKind(Pin.TARGET_RATE);
        config.getInputConsumption()
            .put(0, 0.5f);
        config.getOutputProductivity()
            .put(0, 1.25f);
        node.getInputConfigurations()
            .put(
                0,
                TestIngredients.port("configured", 1)
                    .getAllStacks()
                    .getFirst().item.copy());

        set(node, "handlerRef", RecipeHandlerRef.of(fakeHandler(), 0));
        set(node, "extractor", oldExtractor);
        set(node, "availableExtractors", List.of(oldExtractor, replacement));
        set(node, "extractorIndex", 0);
        return node;
    }

    private static Node harnessNode(final String name) {
        GtnhFlowLoader.ensureDefaultMachineProfile();
        return new Node(UUID.randomUUID(), name, 0, 0);
    }

    private static PropertyProvider provider(final Extract extraction) {
        return new PropertyProvider() {

            @Override
            public void register() {}

            @Override
            public Map<RecipeProperty<?>, Object> extract(final Node node, final IRecipeHandler handler,
                final int recipeIndex) {
                return extraction.apply(node, handler, recipeIndex);
            }
        };
    }

    private static IRecipeHandler fakeHandler() {
        return (IRecipeHandler) Proxy.newProxyInstance(
            IRecipeHandler.class.getClassLoader(),
            new Class<?>[] { IRecipeHandler.class },
            (proxy, method, args) -> switch (method.getName()) {
            case "getRecipeName" -> "inputless";
            case "getIngredientStacks", "getOtherStacks" -> Collections.emptyList();
            case "getResultStack" -> null;
            case "numRecipes" -> 1;
            default -> defaultValue(method.getReturnType());
            });
    }

    private static Object defaultValue(final Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return null;
    }

    private static void assertNotNullCollections(final Node node) {
        assertTrue(node.getInputs() != null);
        assertTrue(node.getOutputs() != null);
        assertTrue(node.getProperties() != null);
        assertTrue(node.getAvailableExtractors() != null);
        assertTrue(node.getInputConfigurations() != null);
    }

    private static void set(final Node node, final String name, final Object value) throws Exception {
        final Field field = Node.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(node, value);
    }

    @FunctionalInterface
    private interface Extract {

        Map<RecipeProperty<?>, Object> apply(Node node, IRecipeHandler handler, int recipeIndex);
    }
}
