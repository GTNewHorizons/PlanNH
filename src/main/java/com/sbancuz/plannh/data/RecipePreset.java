package com.sbancuz.plannh.data;

import com.sbancuz.plannh.data.properties.PropertyProvider;

/** One applicable machine/profile binding for a recipe. Only its ID is saved in a node. */
public record RecipePreset(String id, MachineProfile profile, PropertyProvider provider) {}
