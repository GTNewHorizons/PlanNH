package com.sbancuz.plannh.hologram;

/** A block position in a dimension that a plan element is attached to. */
public record BlockAnchor(int dimension, int x, int y, int z) {}
