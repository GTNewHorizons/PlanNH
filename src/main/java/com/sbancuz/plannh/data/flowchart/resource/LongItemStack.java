package com.sbancuz.plannh.data.flowchart.resource;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

public final class LongItemStack {

    @Nonnull
    public final ItemStack stack;
    public long amount;

    public LongItemStack(@Nonnull final ItemStack stack, final long amount) {
        this.stack = stack;
        this.amount = amount;
    }
}
