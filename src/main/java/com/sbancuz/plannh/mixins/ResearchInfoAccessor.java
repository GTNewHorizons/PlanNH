package com.sbancuz.plannh.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

import thaumcraft.api.research.ResearchItem;

@Pseudo
@Mixin(targets = "com.gtnewhorizons.aspectrecipeindex.nei.ResearchInfo", remap = false)
public interface ResearchInfoAccessor {

    @Accessor("researchItem")
    ResearchItem plannh$getResearchItem();
}
