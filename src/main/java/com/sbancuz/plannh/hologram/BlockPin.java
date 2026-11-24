package com.sbancuz.plannh.hologram;

/** A block position in a dimension that a plan element is pinned to. */
public record BlockPin(int dimension, int x, int y, int z) {}
