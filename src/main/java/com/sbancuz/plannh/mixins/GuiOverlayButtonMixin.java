package com.sbancuz.plannh.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.sbancuz.plannh.gui.FlowchartScreen;

import codechicken.nei.recipe.GuiOverlayButton;

@Mixin(value = GuiOverlayButton.class, remap = false)
public class GuiOverlayButtonMixin {

    @Inject(method = "drawItemOverlay", at = @At("HEAD"), cancellable = true)
    private void plannh$onDrawItemOverlay(final CallbackInfo ci) {
        final GuiOverlayButton self = (GuiOverlayButton) (Object) this;
        if (self.firstGui instanceof GuiContainerWrapper
            && ((GuiContainerWrapper) self.firstGui).getScreen() instanceof FlowchartScreen) {
            ci.cancel();
        }
    }
}
