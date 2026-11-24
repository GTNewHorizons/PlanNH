package com.sbancuz.plannh.mixins;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.sbancuz.plannh.gui.node.RecipeWidgetAccessor;
import com.sbancuz.plannh.gui.node.StackDrawer;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.HandlerInfo;
import codechicken.nei.recipe.widget.RecipeWidget;

@Mixin(value = RecipeWidget.class, remap = false)
public abstract class RecipeWidgetMixin implements RecipeWidgetAccessor {

    @Shadow
    protected boolean update;
    @Shadow
    @Final
    protected HandlerInfo handlerInfo;

    // create separate lists for all widgets so they are separate from the recipe handler
    @Unique
    private List<PositionedStack> plannh$inputs;

    @Unique
    private List<PositionedStack> plannh$outputs;

    @Unique
    private StackDrawer plannh$stackDrawer;

    @Inject(method = "getCyclingStacks", at = @At("HEAD"), cancellable = true)
    private void plannh$getInputs(CallbackInfoReturnable<List<PositionedStack>> cir) {
        if (plannh$inputs != null) {
            cir.setReturnValue(plannh$inputs);
        }
    }

    @Inject(method = "getOutputs", at = @At("HEAD"), cancellable = true)
    private void plannh$getOutputs(CallbackInfoReturnable<List<PositionedStack>> cir) {
        if (plannh$outputs != null) cir.setReturnValue(plannh$outputs);
    }

    @Inject(method = "drawItem", at = @At("HEAD"), cancellable = true)
    private void plannh$drawItem(PositionedStack stack, int x, int y, int shift, boolean input, CallbackInfo ci) {
        if (plannh$stackDrawer != null && plannh$stackDrawer.drawStack(stack)) ci.cancel();
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

    @Override
    public void plannh$setOutputs(List<PositionedStack> outputs) {
        plannh$outputs = outputs;
    }

    @Override
    public void plannh$setStackDrawer(StackDrawer drawer) {
        plannh$stackDrawer = drawer;
    }
}
