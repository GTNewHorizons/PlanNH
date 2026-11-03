package com.sbancuz.plannh.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "com.gtnewhorizons.aspectrecipeindex.nei.InfusionRecipeHandler$InfusionCachedRecipe", remap = false)
public interface InfusionCachedRecipeAccessor {

    @Accessor("instability")
    int plannh$getInstability();
}
