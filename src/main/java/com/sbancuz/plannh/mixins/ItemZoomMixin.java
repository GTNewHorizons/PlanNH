package com.sbancuz.plannh.mixins;

import net.minecraft.client.gui.inventory.GuiContainer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.sbancuz.plannh.gui.FlowchartScreen;

import codechicken.nei.ItemZoom;
import codechicken.nei.NEIClientUtils;

@Mixin(value = ItemZoom.class, remap = false)
public class ItemZoomMixin {

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private void plannh$hideZoom(final int mouseX, final int mouseY, final CallbackInfo ci) {
        if (NEIClientUtils.getGuiContainer() instanceof GuiContainerWrapper wrapper
            && wrapper.getScreen() instanceof FlowchartScreen) {
            ci.cancel();
        }
    }

    @Inject(method = "resize", at = @At("HEAD"), cancellable = true)
    private void plannh$skipZoomLayout(final GuiContainer gui, final CallbackInfo ci) {
        if (gui instanceof GuiContainerWrapper wrapper && wrapper.getScreen() instanceof FlowchartScreen) {
            ci.cancel();
        }
    }

    @Inject(method = "lastKeyTyped", at = @At("HEAD"), cancellable = true)
    private void plannh$ignoreZoomKeys(final GuiContainer gui, final char keyChar, final int keyCode,
        final CallbackInfoReturnable<Boolean> cir) {
        if (gui instanceof GuiContainerWrapper wrapper && wrapper.getScreen() instanceof FlowchartScreen) {
            cir.setReturnValue(false);
        }
    }
}
