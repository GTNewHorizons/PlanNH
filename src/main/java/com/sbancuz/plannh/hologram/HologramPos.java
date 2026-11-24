package com.sbancuz.plannh.hologram;

/** Block position in a dimension; the identity of a {@link Hologram}. */
public record HologramPos(int dimension, int x, int y, int z) {}
