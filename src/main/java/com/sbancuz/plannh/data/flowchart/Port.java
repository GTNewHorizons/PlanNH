package com.sbancuz.plannh.data.flowchart;

import static codechicken.nei.PositionedStack.CHANCE_FULL;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.utils.Color;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.flowchart.resource.LongFluidStack;
import com.sbancuz.plannh.data.flowchart.resource.LongItemStack;
import com.sbancuz.plannh.data.properties.ResourceProperty;

import codechicken.nei.PositionedStack;
import codechicken.nei.item.ItemFluidDisplay;
import codechicken.nei.recipe.StackInfo;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Port<T> {

    private final ResourceProperty<T> type;
    private T value;
    private float chance;
    private final List<PositionedStack> allStacks = new ArrayList<>();

    public Port(final ResourceProperty<T> type, final T value, final float chance, PositionedStack stack) {
        this.type = type;
        this.value = value;
        this.chance = chance;
        allStacks.add(stack);
    }

    public static Port<LongFluidStack> fluid(final FluidStack stack, final float chance, final int x, final int y) {
        return fluid(stack, stack.amount, chance, new PositionedStack(ItemFluidDisplay.createStack(stack), x, y));
    }

    public static Port<LongFluidStack> fluid(final FluidStack stack, final long amount, final float chance,
        final PositionedStack display) {
        return new Port<>(RecipePropertyAPI.FLUID, new LongFluidStack(stack.copy(), amount), chance, display);
    }

    public long getAmount() {
        return type.extractAmount(value);
    }

    public String getDisplayName() {
        return type.formatDisplayName(value);
    }

    public boolean hasVisibleAmount() {
        return type.extractAmount(value) > 0;
    }

    /** Pin color for this port's ingredient; the type's pin color when none is derivable. */
    public int getPinColor(final boolean input) {
        return colorOr(input ? type.getPinInputColor() : type.getPinOutputColor());
    }

    /** Edge-arrow color for this port's ingredient; the type's arrow color as fallback. */
    public int getArrowColor() {
        return colorOr(type.getArrowColor());
    }

    /** Representative color for this port's ingredient; {@code fallback}'s alpha is kept. */
    private int colorOr(final int fallback) {
        final int rgb = type.color(value);
        if (rgb == -1) return fallback;
        return Color.withAlpha(rgb, Color.getAlpha(fallback));
    }

    @SuppressWarnings("unchecked")
    public boolean canConnect(final Port<?> other) {
        if (!type.equals(other.type)) return false;
        return type.canConnect(this, (Port<T>) other);
    }

    public void merge(final Port<?> other) {
        final long newAmount = Math.addExact(getAmount(), other.getAmount());
        if (newAmount != 0) {
            final double a = getAmount();
            final double b = other.getAmount();
            chance = (float) ((a * chance + b * other.chance) / (a + b));
        }

        allStacks.addAll(other.allStacks);
        type.setAmount(value, newAmount);
    }

    public static Port<?> fromStack(final PositionedStack ps) {
        if (!StackInfo.isFluidDisplayItem(ps.item)) return itemPort(ps);
        final long amount = StackInfo.itemStackToNBT(ps.item)
            .getLong("Count");
        return fluid(StackInfo.getFluid(ps.item), amount, (float) ps.getChance() / CHANCE_FULL, ps.copy());
    }

    public static Port<LongItemStack> itemPort(final PositionedStack ps) {
        final ItemStack stack = ps.item.copy();
        return new Port<>(
            RecipePropertyAPI.ITEM,
            new LongItemStack(stack, stack.stackSize),
            (float) ps.getChance() / CHANCE_FULL,
            ps.copy());
    }

    /**
     * @param value a copy of the new value (item/fluid stack) to allow for modifications
     */
    public void setValue(T value) {
        type.setAmount(value, type.extractAmount(this.value));
        this.value = value;
    }
}
