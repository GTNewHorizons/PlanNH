package com.sbancuz.plannh.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import codechicken.nei.guihook.GuiContainerManager;

/** Invokers for the protected methods of {@link GuiContainerManager}. */
@Mixin(value = GuiContainerManager.class, remap = false)
public interface GuiContainerManagerAccessor {

    @Invoker("drawBigStackSize")
    static void plannh$drawBigStackSize(int x, int y, String stackSize, float scale) {
        throw new AssertionError();
    }
}
