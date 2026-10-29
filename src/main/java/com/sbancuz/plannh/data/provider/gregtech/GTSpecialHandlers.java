package com.sbancuz.plannh.data.provider.gregtech;

import static codechicken.nei.PositionedStack.CHANCE_FULL;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.flowchart.Node;
import com.sbancuz.plannh.data.flowchart.Port;
import com.sbancuz.plannh.data.flowchart.resource.LongFluidStack;
import com.sbancuz.plannh.data.flowchart.resource.LongItemStack;

import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.StackInfo;
import gregtech.api.enums.Materials;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import tectech.recipe.EyeOfHarmonyRecipe;

/** Recipe-map-specific port builders for recipes whose NEI stacks are placeholders. */
public final class GTSpecialHandlers {

    private GTSpecialHandlers() {}

    public interface GTSpecialHandler {

        void buildPorts(GTRecipe recipe, Node node, List<PositionedStack> inputs, List<PositionedStack> outputs);
    }

    private static final Map<String, GTSpecialHandler> HANDLERS = Map.of("gt.recipe.eyeofharmony", new EyeOfHarmony());

    @Nullable
    public static GTSpecialHandler get(@Nullable final RecipeMap<?> map) {
        return map == null ? null : HANDLERS.get(map.unlocalizedName);
    }

    private static final class EyeOfHarmony implements GTSpecialHandler {

        @Override
        public void buildPorts(final GTRecipe recipe, final Node node, final List<PositionedStack> inputs,
            final List<PositionedStack> outputs) {
            if (!(recipe.mSpecialItems instanceof final EyeOfHarmonyRecipe eoh)) return;

            final ItemStack trigger = eoh.getRecipeTriggerItem().copy();
            // The recipe trigger is rendered by NEI but is a catalyst, not a consumed input.
            node.getInputs().add(itemPort(trigger, 0L, findItem(inputs, trigger)));

            node.getInputs().add(fluidPort(Materials.Hydrogen.getGas(1), eoh.getHydrogenRequirement(),
                new PositionedStack(GTUtility.getFluidDisplayStack(Materials.Hydrogen.getGas(1), false), 7, 26)));
            node.getInputs().add(fluidPort(Materials.Helium.getGas(1), eoh.getHeliumRequirement(),
                new PositionedStack(GTUtility.getFluidDisplayStack(Materials.Helium.getGas(1), false), 25, 26)));

            for (final var output : eoh.getOutputItems()) {
                final ItemStack stack = output.itemStack.copy();
                node.getOutputs().add(itemPort(stack, output.getStackSize(), findItem(outputs, stack)));
            }
            for (final var output : eoh.getOutputFluids()) {
                final FluidStack fluid = output.fluidStack.copy();
                node.getOutputs().add(fluidPort(fluid, output.getFluidAmount(), findFluid(outputs, fluid)));
            }
        }

        private static Port<LongItemStack> itemPort(final ItemStack stack, final long amount,
            @Nullable final PositionedStack positioned) {
            final PositionedStack display = positioned == null ? new PositionedStack(stack.copy(), 0, 0)
                : positioned.copy();
            return new Port<>(RecipePropertyAPI.ITEM, new LongItemStack(stack, amount), chance(display), display);
        }

        private static Port<LongFluidStack> fluidPort(final FluidStack stack, final long amount,
            @Nullable final PositionedStack positioned) {
            final PositionedStack display = positioned == null
                ? new PositionedStack(GTUtility.getFluidDisplayStack(stack, false), 0, 0)
                : positioned.copy();
            return new Port<>(RecipePropertyAPI.FLUID, new LongFluidStack(stack, amount), chance(display), display);
        }

        private static float chance(final PositionedStack stack) {
            return (float) stack.getChance() / CHANCE_FULL;
        }

        @Nullable
        private static PositionedStack findItem(final List<PositionedStack> stacks, final ItemStack wanted) {
            for (final PositionedStack stack : stacks) {
                if (NEIServerUtils.areStacksSameTypeCrafting(stack.item, wanted)) return stack;
            }
            return null;
        }

        @Nullable
        private static PositionedStack findFluid(final List<PositionedStack> stacks, final FluidStack wanted) {
            for (final PositionedStack stack : stacks) {
                final FluidStack fluid = StackInfo.getFluid(stack.item);
                if (fluid != null && fluid.isFluidEqual(wanted)) return stack;
            }
            return null;
        }
    }
}
