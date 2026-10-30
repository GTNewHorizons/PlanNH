package com.sbancuz.plannh.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sbancuz.plannh.data.effect.EffectResult;
import com.sbancuz.plannh.data.flowchart.Edge;
import com.sbancuz.plannh.data.flowchart.Graph;
import com.sbancuz.plannh.data.flowchart.Node;
import com.sbancuz.plannh.data.flowchart.balancer.BalanceMode;
import com.sbancuz.plannh.data.flowchart.balancer.Balancer;
import com.sbancuz.plannh.data.flowchart.balancer.ModelData;
import com.sbancuz.plannh.data.setting.Settings;
import com.sbancuz.plannh.harness.TestIngredients;

import it.unimi.dsi.fastutil.ints.IntIntMutablePair;

class MachineProfileTimedInputsTest {

    @Test
    void fallbackQueriesUseTheirOwnRule() {
        final MachineProfile profile = MachineProfile.builder("fallback-rules")
            .effect((cfg, ctx) -> new EffectResult(20, 0, 1))
            .inputMultiplier((effect, cfg, ctx, index) -> 2d)
            .outputMultiplier((effect, cfg, ctx, index) -> 3d)
            .build();
        final MachineConfig cfg = new MachineConfig(profile);
        final RecipeContext ctx = RecipeContext.builder()
            .build();

        assertEquals(2d, profile.inputMultiplier(cfg, ctx, 0));
        assertEquals(3d, profile.outputMultiplier(cfg, ctx, 0));
    }

    @Test
    void resultAwareCallbackSeesFinalDurationAndThroughputFactor() {
        final MachineProfile profile = MachineProfile.builder("timed-input")
            .effect((cfg, ctx) -> new EffectResult(100, 0, 1))
            .applyParallelism()
            .inputMultiplier((effect, cfg, ctx, index) -> effect.durationTicks() * 0.5d)
            .build();
        final MachineConfig config = new MachineConfig(profile);
        config.set(Settings.MACHINES, 2);
        config.set(Settings.PARALLELS, 3);
        config.set(Settings.TICK_MODIFIER, 50);

        final EffectResult result = config.computeEffect(recipe(1, 0));

        assertEquals(200, result.durationTicks());
        assertEquals(6, result.throughputFactor());
        assertEquals(100d, result.inputMultiplier(0));
    }

    @Test
    void tickModifierLeavesRecipeAmountFixedWhileTimedMultiplierChanges() {
        final MachineProfile profile = MachineProfile.builder("recipe-amount")
            .effect((cfg, ctx) -> new EffectResult(100, 0, 1))
            .inputMultiplier((effect, cfg, ctx, index) -> effect.durationTicks() * 0.5d)
            .build();
        final RecipeContext context = RecipeContext.builder()
            .input(TestIngredients.port("input", 2))
            .build();
        final MachineConfig config = new MachineConfig(profile);

        config.set(Settings.TICK_MODIFIER, 100);
        final EffectResult normal = config.computeEffect(context);
        config.set(Settings.TICK_MODIFIER, 50);
        final EffectResult slowed = config.computeEffect(context);

        assertEquals(
            2d,
            TestIngredients.quantityOf(
                context.inputs()
                    .getFirst()));
        assertEquals(50d, normal.inputMultiplier(0));
        assertEquals(100d, slowed.inputMultiplier(0));
    }

    @Test
    void modelDataAppliesThroughputAfterTheResultAwareMultiplier() {
        final MachineProfile targetProfile = MachineProfile.builder("model-timed-input")
            .effect((cfg, ctx) -> new EffectResult(100, 0, 1))
            .applyParallelism()
            .inputMultiplier((effect, cfg, ctx, index) -> effect.durationTicks() * 0.5d)
            .build();
        final Node source = syntheticNode(
            "source",
            MachineProfile.builder("source-profile")
                .effect((cfg, ctx) -> new EffectResult(1, 0, 1))
                .build(),
            RecipeContext.builder()
                .output(TestIngredients.port("ore", 1))
                .build());
        final Node target = syntheticNode(
            "target",
            targetProfile,
            RecipeContext.builder()
                .input(TestIngredients.port("ore", 2))
                .build());
        final MachineConfig targetConfig = target.getMachineConfig();
        targetConfig.set(Settings.MACHINES, 2);
        targetConfig.set(Settings.PARALLELS, 3);
        targetConfig.set(Settings.TICK_MODIFIER, 50);

        final Graph graph = new Graph();
        graph.addNode(source);
        graph.addNode(target);
        graph.addEdge(
            new Edge(
                UUID.randomUUID(),
                source.getId(),
                target.getId(),
                new IntIntMutablePair(0, 0),
                new IntIntMutablePair(0, 0)));

        final ModelData model = Balancer.run(BalanceMode.NONE, graph)
            .ctx().model;
        final int targetMachine = model.machineIndex.get(target.getId());
        final ModelData.ConnectedPort input = model.connectedPorts.stream()
            .filter(port -> port.machine() == targetMachine && port.input())
            .findFirst()
            .orElseThrow();

        assertEquals(
            2d,
            TestIngredients.quantityOf(
                target.getInputs()
                    .getFirst()));
        assertEquals(1200d, input.qtyPerCraft());
    }

    @Test
    void resultAwareRulesRejectNonFiniteAndNegativeValues() {
        for (final double value : new double[] { Double.NaN, Double.POSITIVE_INFINITY, -1d }) {
            assertInvalidMultiplier(value, false);
            assertInvalidMultiplier(value, true);
        }
    }

    private static void assertInvalidMultiplier(final double value, final boolean output) {
        final MachineProfile.Builder builder = MachineProfile.builder("invalid-" + (output ? "output" : "input"))
            .effect((cfg, ctx) -> new EffectResult(1, 0, 1));
        if (output) builder.outputMultiplier((effect, cfg, ctx, index) -> value);
        else builder.inputMultiplier((effect, cfg, ctx, index) -> value);
        final MachineProfile profile = builder.build();

        assertThrows(IllegalArgumentException.class, () -> new MachineConfig(profile).computeEffect(recipe(1, 1)));
    }

    private static Node syntheticNode(final String name, final MachineProfile profile, final RecipeContext context) {
        return new Node(UUID.randomUUID(), name, 0, 0, profile, context);
    }

    private static RecipeContext recipe(final int inputs, final int outputs) {
        final RecipeContext.Builder builder = RecipeContext.builder();
        for (int i = 0; i < inputs; i++) builder.input(TestIngredients.port("input-" + i, 1));
        for (int i = 0; i < outputs; i++) builder.output(TestIngredients.port("output-" + i, 1));
        return builder.build();
    }
}
