package com.sbancuz.plannh.data;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

/**
 * A fact about a machine beside the profile icon: an item, a code such as {@code HV 2A}, or both. The code has no
 * words, and may have formatting codes.
 */
public record NodeBadge(@Nullable ItemStack item, String code) {}
