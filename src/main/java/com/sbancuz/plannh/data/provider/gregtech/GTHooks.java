package com.sbancuz.plannh.data.provider.gregtech;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.utils.Color;

import gregtech.api.items.MetaGeneratedItem;

/** GT class references, isolated. Only call behind {@code Compat.GREGTECH.isLoaded}. */
public final class GTHooks {

    private GTHooks() {}

    /**
     * GT MetaGeneratedItem (dusts, ingots, cells, circuits, etc.) colors are added at draw time (`MaterialsInit.java`).
     * Therefore, need to use `.getRGBA` and not Minecraft `.getColorFromItemStack`
     */
    public static int materialTint(final ItemStack stack) {
        if (stack.getItem() instanceof final MetaGeneratedItem gtItem) {
            final short[] rgba = gtItem.getRGBa(stack);
            if (rgba != null && rgba.length >= 3) {
                return Color.rgb(rgba[0], rgba[1], rgba[2]);
            }
        }
        return 0xFFFFFF;
    }
}
