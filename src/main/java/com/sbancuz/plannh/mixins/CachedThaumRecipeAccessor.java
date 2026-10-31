package com.sbancuz.plannh.mixins;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.gtnewhorizons.aspectrecipeindex.nei.ResearchInfo;

import thaumcraft.api.aspects.AspectList;

@Pseudo
@Mixin(targets = "com.gtnewhorizons.aspectrecipeindex.nei.TemplateThaumHandler$CachedThaumRecipe", remap = false)
public interface CachedThaumRecipeAccessor {

    @Accessor("aspects")
    AspectList plannh$getAspects();

    @Accessor("prereqs")
    List<ResearchInfo> plannh$getPrereqs();
}
