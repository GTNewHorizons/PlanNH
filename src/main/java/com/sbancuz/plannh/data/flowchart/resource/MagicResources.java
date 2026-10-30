package com.sbancuz.plannh.data.flowchart.resource;

import java.util.Objects;

import javax.annotation.Nonnull;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.sbancuz.plannh.data.flowchart.Port;
import com.sbancuz.plannh.data.properties.ResourceProperty;
import com.sbancuz.plannh.gui.PlannhColors;

import codechicken.nei.PositionedStack;

/** Shared typed resources used by the Thaumcraft-family integrations. */
public final class MagicResources {

    public static final ResourceProperty<Amount> ESSENTIA = type(
        "thaumcraft.essentia",
        new Amount("thaumcraft:aer", "Aer", 0xFFFFFF, 0));
    public static final ResourceProperty<Amount> STORED_VIS = type(
        "thaumcraft.stored_vis",
        new Amount("thaumcraft:aer", "Aer", 0xFFFFFF, 0));
    public static final ResourceProperty<Amount> NETWORK_VIS = type(
        "thaumcraft.network_vis",
        new Amount("thaumcraft:ignis", "Ignis", 0xFF5555, 0));
    public static final ResourceProperty<Amount> AURA_CHARGE = type(
        "magicbees.aura_charge",
        new Amount("magicbees:aura", "Aura", 0xAA55FF, 0));

    private MagicResources() {}

    @Nonnull
    public static Port<Amount> port(final ResourceProperty<Amount> type, final String id, final String name,
        final int color, final long amount, final PositionedStack display) {
        return new Port<>(type, new Amount(id, name, color, amount), 1f, display.copy());
    }

    /** ARI's recipe display variant: discovered rendering, aspect NBT, and the recipe amount. */
    public static PositionedStack aspectDisplay(final Item aspectItem, final String aspectTag, final long amount,
        final int x, final int y) {
        if (aspectItem == null) throw new IllegalStateException("Aspect Recipe Index aspect item is unavailable");
        final ItemStack stack = new ItemStack(aspectItem, (int) Math.clamp(amount, 1L, Integer.MAX_VALUE), 1);
        stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound()
            .setString("Aspect", aspectTag);
        return new PositionedStack(stack, x, y, false);
    }

    public static String unitKey(final ResourceProperty<?> type) {
        if (NETWORK_VIS.equals(type)) return "plannh.resources.unit.centivis";
        if (AURA_CHARGE.equals(type)) return "plannh.resources.unit.aura";
        return null;
    }

    private static ResourceProperty<Amount> type(final String key, final Amount defaultValue) {
        return ResourceProperty.<Amount>builder(key, defaultValue)
            .displayFormatter(value -> value.name)
            .amountExtractor(value -> value.amount)
            .amountUpdater((value, amount) -> value.amount = amount)
            .connectionChecker((a, b) -> Objects.equals(a.getValue().id, b.getValue().id))
            .hashCodeExtractor(value -> Objects.hashCode(value.id))
            .colorProvider(value -> value.color & 0xFFFFFF)
            .pinInputColor(PlannhColors.PIN_INPUT.getColor())
            .pinOutputColor(PlannhColors.PIN_OUTPUT.getColor())
            .arrowColor(PlannhColors.ARROW_ITEM.getColor())
            .build();
    }

    public static final class Amount {

        public final String id;
        public final String name;
        public final int color;
        public long amount;

        public Amount(final String id, final String name, final int color, final long amount) {
            this.id = Objects.requireNonNull(id);
            this.name = Objects.requireNonNull(name);
            this.color = color;
            this.amount = amount;
        }

        public String id() {
            return id;
        }

        public String name() {
            return name;
        }

        public int color() {
            return color;
        }

        public long amount() {
            return amount;
        }

        public void amount(final long newAmount) {
            amount = newAmount;
        }
    }
}
