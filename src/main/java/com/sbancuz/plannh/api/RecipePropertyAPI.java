package com.sbancuz.plannh.api;

import java.util.LinkedHashSet;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.google.common.collect.ArrayListMultimap;
import com.sbancuz.plannh.Compat;
import com.sbancuz.plannh.data.flowchart.Port;
import com.sbancuz.plannh.data.flowchart.resource.LongFluidStack;
import com.sbancuz.plannh.data.flowchart.resource.LongItemStack;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.properties.RecipeProperty;
import com.sbancuz.plannh.data.properties.ResourceProperty;
import com.sbancuz.plannh.gui.GuiHelper;
import com.sbancuz.plannh.gui.IngredientColors;
import com.sbancuz.plannh.gui.PlannhColors;
import com.sbancuz.plannh.mixins.PositionedStackAccessor;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.StackInfo;
import gregtech.nei.GTNEIDefaultHandler;

public final class RecipePropertyAPI {

    private static final ArrayListMultimap<Class<?>, PropertyProvider> extractors = ArrayListMultimap.create();

    public static final RecipeProperty<Integer> DURATION_TICKS = RecipeProperty.<Integer>builder("duration_ticks", 0)
        .build();

    public static final ResourceProperty<LongItemStack> ITEM = ResourceProperty
        .builder("item", new LongItemStack(new ItemStack(Blocks.dirt), 0))
        .displayFormatter(value -> value.stack.getDisplayName())
        .amountFormatter(GuiHelper::formatRate)
        .amountExtractor(value -> value.amount)
        .amountUpdater((value, newAmount) -> value.amount = newAmount)
        .connectionChecker(RecipePropertyAPI::itemPortsMatch)
        .hashCodeExtractor(
            value -> 31 * value.stack.getItem()
                .hashCode() + value.stack.getItemDamage())
        .colorProvider(value -> IngredientColors.itemColor(value.stack))
        .pinInputColor(PlannhColors.PIN_INPUT.getColor())
        .pinOutputColor(PlannhColors.PIN_OUTPUT.getColor())
        .arrowColor(PlannhColors.ARROW_ITEM.getColor())
        .build();

    public static final ResourceProperty<LongFluidStack> FLUID = ResourceProperty
        .builder("fluid", new LongFluidStack(new FluidStack(FluidRegistry.WATER, 0, null), 0))
        .displayFormatter(value -> value.stack.getLocalizedName())
        // Buckets past a thousand litres, litres below, and the same suffix table over whichever
        // unit is in play - so a big line reads 1.0kB rather than running out of digits at 1000.0B.
        .amountFormatter(amount -> {
            if (amount >= 1000d) return GuiHelper.trimTrailingZeros(GuiHelper.formatRate(amount / 1000d)) + "B";
            return GuiHelper.trimTrailingZeros(GuiHelper.formatRate(amount)) + "mB";
        })
        .amountExtractor(value -> value.amount)
        .amountUpdater((value, newAmount) -> value.amount = newAmount)
        .connectionChecker(RecipePropertyAPI::fluidPortsMatch)
        .hashCodeExtractor(
            value -> value.stack.getFluid()
                .hashCode())
        .colorProvider(value -> IngredientColors.fluidColor(value.stack))
        .pinInputColor(PlannhColors.PIN_FLUID_IN.getColor())
        .pinOutputColor(PlannhColors.PIN_FLUID_OUT.getColor())
        .arrowColor(PlannhColors.ARROW_FLUID.getColor())
        .build();

    private static boolean itemPortsMatch(final Port<LongItemStack> pa, final Port<LongItemStack> pb) {
        final PositionedStack psa = pa.getAllStacks()
            .getFirst();
        final PositionedStack psb = pb.getAllStacks()
            .getFirst();

        ItemStack[] as = ((PositionedStackAccessor) psa).getPermutated() ? psa.items : new ItemStack[] { psa.item };
        ItemStack[] bs = ((PositionedStackAccessor) psb).getPermutated() ? psb.items : new ItemStack[] { psb.item };

        if (Compat.GREGTECH.isLoaded) {
            if (psa instanceof GTNEIDefaultHandler.FixedPositionedStack fpsa && !fpsa.isInput() && as.length > 1)
                as = new ItemStack[] { psa.item };
            if (psb instanceof GTNEIDefaultHandler.FixedPositionedStack fpsb && !fpsb.isInput() && bs.length > 1)
                bs = new ItemStack[] { psb.item };
        }

        for (ItemStack a : as) {
            for (ItemStack b : bs) {
                if (a.isItemEqual(b)) return true;
            }
        }

        return false;
    }

    private static boolean fluidPortsMatch(final Port<LongFluidStack> pa, final Port<LongFluidStack> pb) {
        final PositionedStack psa = pa.getAllStacks()
            .getFirst();
        final PositionedStack psb = pb.getAllStacks()
            .getFirst();
        final ItemStack[] as = ((PositionedStackAccessor) psa).getPermutated() ? psa.items
            : new ItemStack[] { psa.item };
        final ItemStack[] bs = ((PositionedStackAccessor) psb).getPermutated() ? psb.items
            : new ItemStack[] { psb.item };

        for (ItemStack a : as) {
            FluidStack fluidA = StackInfo.getFluid(a);
            if (fluidA == null) continue;
            for (ItemStack b : bs) {
                FluidStack fluidB = StackInfo.getFluid(b);
                if (fluidB != null && fluidA.isFluidEqual(fluidB)) return true;
            }
        }
        return false;
    }

    public static void registerExtractor(final Class<?> handlerClass, final PropertyProvider extractor) {
        extractors.put(handlerClass, extractor);
    }

    @Nonnull
    public static List<PropertyProvider> getExtractors(final Class<?> handlerClass) {
        final LinkedHashSet<PropertyProvider> result = new LinkedHashSet<>();
        Class<?> clazz = handlerClass;
        while (clazz != null) {
            result.addAll(extractors.get(clazz));
            clazz = clazz.getSuperclass();
        }
        return List.copyOf(result);
    }

    public static void reset() {
        extractors.clear();
    }
}
