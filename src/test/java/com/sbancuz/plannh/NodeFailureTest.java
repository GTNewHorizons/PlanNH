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
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
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
        final Node node = new Node(fakeHandler(), 0);

        assertFalse(node.isAvailable());
        assertNull(node.getHandlerRef());
    }

    @Test
    void profileExtractsThroughItsOwningProvider() {
        final Fixture fixture = fixture((recipe, handler, index) -> {
            recipe.property(GtnhFlowLoader.DURATION_TICKS, 17);
            recipe.input(TestIngredients.port("owned-input", 1));
        });

        final RecipeContext context = fixture.profile()
            .getRecipeContext(RecipeHandlerRef.of(fakeHandler(), 0));

        assertEquals(17, context.get(GtnhFlowLoader.DURATION_TICKS));
        assertEquals(
            "owned-input",
            TestIngredients.nameOf(
                context.inputs()
                    .getFirst()));
    }

    @Test
    void providerInstancesOwnIndependentProfilesEvenWhenIdsMatch() {
        final Fixture first = fixture((recipe, handler, index) -> {});
        final Fixture second = fixture((recipe, handler, index) -> {});

        assertNotSame(first.provider(), second.provider());
        assertNotSame(first.profile(), second.profile());
        assertEquals(
            first.profile()
                .id(),
            second.profile()
                .id());
    }

    @Test
    void unavailableInitLeavesSafeCollectionsAndDoesNotRetry() throws Exception {
        final Node node = syntheticNode(
            "unavailable",
            profile("unavailable"),
            RecipeContext.builder()
                .build());
        set(node, "selectedProfile", null);
        set(node, "initialized", false);
        set(node, "handlerRef", null);

        node.init();

        assertTrue(node.isInitialized());
        assertFalse(node.isAvailable());
        assertNull(node.getHandlerRef());
        assertNotNullCollections(node);
        final List<Port<?>> inputs = node.getInputs();

        node.init();

        assertSame(inputs, node.getInputs(), "a failed node must not retry init");
        assertNotNullCollections(node);
    }

    @Test
    void failedProfileSwitchDoesNotPublishPartialCandidate() throws Exception {
        final Fixture old = fixture((recipe, handler, index) -> {});
        final Fixture failing = fixture((recipe, handler, index) -> {
            recipe.input(TestIngredients.port("candidate-input", 1));
            recipe.output(TestIngredients.port("candidate-output", 1));
            recipe.property(GtnhFlowLoader.DURATION_TICKS, 99);
            throw new IllegalStateException("provider failed");
        });
        final Node node = configuredNode(old, failing);

        final List<Port<?>> inputs = node.getInputs();
        final List<Port<?>> outputs = node.getOutputs();
        final Map<RecipeProperty<?>, Object> properties = node.getProperties();
        final MachineConfig machineConfig = node.getMachineConfig();
        final Map<Integer, ItemStack> inputConfigurations = node.getInputConfigurations();
        final ItemStack configuredInput = inputConfigurations.get(0);
        final MachineProfile selected = node.getSelectedProfile();
        final boolean settingsOpen = node.isSettingsOpen();

        assertThrows(IllegalStateException.class, () -> node.selectProfile(1));

        assertSame(inputs, node.getInputs());
        assertSame(outputs, node.getOutputs());
        assertSame(properties, node.getProperties());
        assertSame(machineConfig, node.getMachineConfig());
        assertSame(inputConfigurations, node.getInputConfigurations());
        assertSame(
            configuredInput,
            node.getInputConfigurations()
                .get(0));
        assertSame(selected, node.getSelectedProfile());
        assertEquals(settingsOpen, node.isSettingsOpen());
        assertEquals(
            42,
            node.getProperties()
                .get(GtnhFlowLoader.DURATION_TICKS));
        assertEquals(
            7,
            node.getMachineConfig()
                .getOrDefault(Settings.MACHINES, Settings.MACHINES::getDefaultValue));
    }

    @Test
    void successfulProfileSwitchPublishesIsolatedCandidate() throws Exception {
        final Fixture old = fixture((recipe, handler, index) -> {});
        final Fixture replacement = fixture((recipe, handler, index) -> {
            recipe.input(TestIngredients.port("new-input", 2));
            recipe.output(TestIngredients.port("new-output", 3));
            recipe.property(GtnhFlowLoader.DURATION_TICKS, 99);
        });
        final Node node = configuredNode(old, replacement);

        final List<Port<?>> oldInputs = node.getInputs();
        final List<Port<?>> oldOutputs = node.getOutputs();
        final Map<RecipeProperty<?>, Object> oldProperties = node.getProperties();
        final MachineConfig oldMachineConfig = node.getMachineConfig();

        assertTrue(node.selectProfile(1));

        assertSame(replacement.profile(), node.getSelectedProfile());
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
        assertEquals(7, config.getOrDefault(Settings.MACHINES, Settings.MACHINES::getDefaultValue));
        assertEquals(88, config.getOrDefault(Settings.TICK_MODIFIER, Settings.TICK_MODIFIER::getDefaultValue));
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
        assertEquals(0.5, config.inputMultiplier(0), 1e-6);
        assertEquals(1.25, config.outputMultiplier(0), 1e-6);
    }

    @Test
    void extractionCombinesMatchingPortsWithoutChangingPreviousPorts() throws Exception {
        final Fixture original = fixture((recipe, handler, index) -> {});
        final Fixture replacement = fixture((recipe, handler, index) -> {
            recipe.input(TestIngredients.port("input", 2));
            recipe.input(TestIngredients.port("input", 3));
            recipe.output(TestIngredients.port("output", 4));
            recipe.output(TestIngredients.port("output", 5));
        });
        final Node node = configuredNode(original, replacement);
        final List<Port<?>> previousInputs = node.getInputs();

        assertTrue(node.selectProfile(1));

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

    @Test
    void pickerSelectsAnExplicitProfileWhileTheOldSelectionIsUnavailable() throws Exception {
        final Fixture original = fixture((recipe, handler, index) -> {});
        final Fixture replacement = fixture((recipe, handler, index) -> {});
        final Node node = configuredNode(original, replacement);
        set(node, "availableProfiles", List.of(original.profile(), original.profile(), replacement.profile()));
        set(node, "selectedProfile", null);

        final List<Port<?>> inputs = node.getInputs();
        final List<Port<?>> outputs = node.getOutputs();
        final Map<RecipeProperty<?>, Object> properties = node.getProperties();
        final MachineConfig config = node.getMachineConfig();
        final RecipeContext context = node.recipeContext();
        final RecipeHandlerRef handlerRef = node.getHandlerRef();

        assertFalse(node.selectProfile(-1));
        assertFalse(node.selectProfile(3));
        assertSame(inputs, node.getInputs());
        assertSame(outputs, node.getOutputs());
        assertSame(properties, node.getProperties());
        assertSame(config, node.getMachineConfig());
        assertSame(context, node.recipeContext());
        assertSame(handlerRef, node.getHandlerRef());
        assertNull(node.getSelectedProfile());

        assertTrue(node.selectProfile(2));

        assertSame(replacement.profile(), node.getSelectedProfile());
    }

    private static Node configuredNode(final Fixture old, final Fixture replacement) throws Exception {
        final RecipeContext.Builder recipe = RecipeContext.builder()
            .property(GtnhFlowLoader.DURATION_TICKS, 42)
            .input(TestIngredients.port("old-input", 1))
            .output(TestIngredients.port("old-output", 1));
        final Node node = syntheticNode("switchable", old.profile(), recipe.build());
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
        config.inputMultiplier(0, 0.5);
        config.outputMultiplier(0, 1.25);
        node.getInputConfigurations()
            .put(
                0,
                TestIngredients.port("configured", 1)
                    .getAllStacks()
                    .getFirst().item.copy());

        set(node, "handlerRef", RecipeHandlerRef.of(fakeHandler(), 0));
        set(node, "availableProfiles", List.of(old.profile(), replacement.profile()));
        set(node, "selectedProfile", old.profile());
        return node;
    }

    private static Node syntheticNode(final String name, final MachineProfile profile, final RecipeContext context) {
        return new Node(UUID.randomUUID(), name, 0, 0, profile, context);
    }

    private static Fixture fixture(final Populate populate) {
        final class FixtureProvider implements PropertyProvider {

            private MachineProfile profile;

            @Override
            public void register() {}

            @Override
            public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
                return List.of(profile);
            }

            @Override
            public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler,
                final int recipeIndex) {
                populate.apply(recipe, handler, recipeIndex);
            }
        }
        final FixtureProvider provider = new FixtureProvider();
        final MachineProfile profile = MachineProfile.builder("test-machine")
            .provider(provider)
            .build();
        provider.profile = profile;
        return new Fixture(profile, provider);
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

    private static MachineProfile profile(final String id) {
        return MachineProfile.builder(id)
            .setting(Settings.MACHINES)
            .setting(Settings.TICK_MODIFIER)
            .build();
    }

    private static void assertNotNullCollections(final Node node) {
        assertTrue(node.getInputs() != null);
        assertTrue(node.getOutputs() != null);
        assertTrue(node.getProperties() != null);
        assertTrue(node.getAvailableProfiles() != null);
        assertTrue(node.getInputConfigurations() != null);
    }

    private static void set(final Node node, final String name, final Object value) throws Exception {
        final Field field = Node.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(node, value);
    }

    private record Fixture(MachineProfile profile, PropertyProvider provider) {}

    @FunctionalInterface
    private interface Populate {

        void apply(RecipeContext.Builder recipe, IRecipeHandler handler, int recipeIndex);
    }
}
