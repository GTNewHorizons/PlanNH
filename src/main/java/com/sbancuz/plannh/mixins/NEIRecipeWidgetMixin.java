package com.sbancuz.plannh.mixins;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.sbancuz.plannh.gui.node.NEIRecipeWidgetAccessor;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.HandlerInfo;
import codechicken.nei.recipe.widget.RecipeWidget;

@Mixin(value = RecipeWidget.class, remap = false)
public abstract class NEIRecipeWidgetMixin implements NEIRecipeWidgetAccessor {

    @Shadow
    protected boolean update;
    @Shadow
    @Final
    protected HandlerInfo handlerInfo;

    // create separate lists for all widgets so they are separate from the recipe handler
    @Unique
    private List<PositionedStack> plannh$inputs;

    @Inject(method = "getCyclingStacks", at = @At("HEAD"), cancellable = true)
    private void plannh$getInputs(CallbackInfoReturnable<List<PositionedStack>> cir) {
        if (plannh$inputs != null) {
            cir.setReturnValue(plannh$inputs);
        }
    }

    @Override
    public void plannh$setUpdate(boolean update) {
        this.update = update;
    }

    @Override
    public HandlerInfo plannh$getHandlerInfo() {
        return handlerInfo;
    }

    @Override
    public void plannh$setInputs(List<PositionedStack> inputs) {
        plannh$inputs = inputs;
    }
}
