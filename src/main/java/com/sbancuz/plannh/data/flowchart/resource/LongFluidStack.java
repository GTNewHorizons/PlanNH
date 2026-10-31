package com.sbancuz.plannh.data.flowchart.resource;

import javax.annotation.Nonnull;

import net.minecraftforge.fluids.FluidStack;

public final class LongFluidStack {

    @Nonnull
    public final FluidStack stack;
    public long amount;

    public LongFluidStack(@Nonnull final FluidStack stack, final long amount) {
        this.stack = stack;
        this.amount = amount;
    }
}
