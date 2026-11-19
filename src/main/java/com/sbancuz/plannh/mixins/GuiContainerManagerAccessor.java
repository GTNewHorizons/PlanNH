package com.sbancuz.plannh.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import codechicken.nei.guihook.GuiContainerManager;

/** Invokers for the protected methods of {@link GuiContainerManager}. */
@Mixin(value = GuiContainerManager.class, remap = false)
public interface GuiContainerManagerAccessor {

    /**
     * NEI's count text at {@code scale}, right-aligned on the bottom edge of the 16 pixel item at x, y.
     *
     * @throws AssertionError if Mixin did not apply the invoker
     */
    @Invoker("drawBigStackSize")
    static void plannh$drawBigStackSize(int x, int y, String stackSize, float scale) { // noqa: NAR130, NAR131
        throw new AssertionError();
    }
}
