package com.sbancuz.plannh.data.provider.gregtech;

import static codechicken.nei.PositionedStack.CHANCE_FULL;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.flowchart.Port;
import com.sbancuz.plannh.data.flowchart.resource.LongFluidStack;
import com.sbancuz.plannh.data.flowchart.resource.LongItemStack;
import com.sbancuz.plannh.data.properties.SummaryProperty;
import com.sbancuz.plannh.gui.GuiHelper;

import codechicken.nei.NEIClientUtils;
import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.item.ItemFluidDisplay;
import codechicken.nei.recipe.Badge;
import codechicken.nei.recipe.StackInfo;
import gregtech.api.enums.Materials;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;
import tectech.recipe.EyeOfHarmonyRecipe;

/** Recipe-map-specific port builders for recipes whose NEI stacks are placeholders. */
public final class GTSpecialHandlers {

    private GTSpecialHandlers() {}

    public interface GTSpecialHandler {

        void populate(GTRecipe recipe, RecipeContext.Builder builder, List<PositionedStack> inputs,
            List<PositionedStack> outputs);
    }

    private static final Map<String, GTSpecialHandler> HANDLERS = Map.of("gt.recipe.eyeofharmony", new EyeOfHarmony());

    @Nullable
    public static GTSpecialHandler get(@Nullable final RecipeMap<?> map) {
        return map == null ? null : HANDLERS.get(map.unlocalizedName);
    }

    private static final class EyeOfHarmony implements GTSpecialHandler {

        private static final int COLUMNS = 9;
        private static final int SLOT_PITCH = 18;

        private static final SummaryProperty<Long> START_EU = SummaryProperty.builder("gt.eoh.start_eu", 0L)
            .build();
        private static final SummaryProperty<Long> OUTPUT_EU = SummaryProperty.builder("gt.eoh.output_eu", 0L)
            .build();

        @Override
        public void populate(final GTRecipe recipe, final RecipeContext.Builder builder,
            final List<PositionedStack> inputs, final List<PositionedStack> outputs) {
            if (!(recipe.mSpecialItems instanceof final EyeOfHarmonyRecipe eoh)) return;

            final ItemStack trigger = eoh.getRecipeTriggerItem().copy();
            final PositionedStack triggerDisplay = findItem(inputs, trigger);
            // The recipe trigger is rendered by NEI but is a catalyst, not a consumed input.
            builder.input(itemPort(trigger, 0L, triggerDisplay));

            builder.input(fluidPort(Materials.Hydrogen.getGas(1), eoh.getHydrogenRequirement(),
                new PositionedStack(ItemFluidDisplay.createStack(Materials.Hydrogen.mGas, eoh.getHydrogenRequirement()),
                    triggerDisplay.relx - 2 * SLOT_PITCH, triggerDisplay.rely)));
            builder.input(fluidPort(Materials.Helium.getGas(1), eoh.getHeliumRequirement(),
                new PositionedStack(ItemFluidDisplay.createStack(Materials.Helium.mGas, eoh.getHeliumRequirement()),
                    triggerDisplay.relx + 2 * SLOT_PITCH, triggerDisplay.rely)));

            // NEI caps its EoH item grid. Keep the remaining outputs in additional rows, not at (0,0).
            final int overflowX = outputs.stream().mapToInt(stack -> stack.relx).min().orElseThrow();
            final int overflowY = outputs.stream().mapToInt(stack -> stack.rely).max().orElseThrow() + SLOT_PITCH;
            int overflow = 0;

            for (final var output : eoh.getOutputItems()) {
                final ItemStack stack = output.itemStack.copy();
                PositionedStack display = findItem(outputs, stack);
                if (display == null) {
                    display = new PositionedStack(stack.copy(), overflowX + overflow % COLUMNS * SLOT_PITCH,
                        overflowY + overflow / COLUMNS * SLOT_PITCH);
                    display.setBadges(List.of(new Badge(GuiHelper.formatCount(output.getStackSize()),
                        Long.toString(output.getStackSize())).setColor(0xffffff)
                        .setAlignment(NEIClientUtils.Alignment.BottomRight)));
                    overflow++;
                }
                builder.output(itemPort(stack, output.getStackSize(), display));
            }
            for (final var output : eoh.getOutputFluids()) {
                final FluidStack fluid = output.fluidStack.copy();
                PositionedStack display = findFluid(outputs, fluid);
                if (display == null) {
                    display = new PositionedStack(ItemFluidDisplay.createStack(fluid.getFluid(), output.getFluidAmount()),
                        overflowX + overflow % COLUMNS * SLOT_PITCH, overflowY + overflow / COLUMNS * SLOT_PITCH);
                    overflow++;
                }
                builder.output(fluidPort(fluid, output.getFluidAmount(), display));
            }
            builder.property(START_EU, eoh.getEUStartCost())
                .property(OUTPUT_EU, eoh.getEUOutput());
        }

        private static Port<LongItemStack> itemPort(final ItemStack stack, final long amount,
            final PositionedStack positioned) {
            final PositionedStack display = positioned.copy();
            return new Port<>(RecipePropertyAPI.ITEM, new LongItemStack(stack, amount), chance(display), display);
        }

        private static Port<LongFluidStack> fluidPort(final FluidStack stack, final long amount,
            final PositionedStack positioned) {
            final PositionedStack display = positioned.copy();
            return Port.fluid(stack, amount, chance(display), display);
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
