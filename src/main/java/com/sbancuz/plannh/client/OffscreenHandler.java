package com.sbancuz.plannh.client;

import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import com.sbancuz.offscreen.api.OffscreenAPI;
import com.sbancuz.offscreen.window.Window;
import com.sbancuz.plannh.gui.FlowchartScreen;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;

/** Offscreen class references, isolated. Only initialize when the offscreen mod is loaded. */
public final class OffscreenHandler {

    private static final KeyBinding toggleSecondScreenKey = new KeyBinding(
        "key.neiflowchart.secondscreen",
        Keyboard.KEY_F7,
        "key.categories.neiflowchart");

    private Window secondScreenWindow;

    private OffscreenHandler() {}

    public static void init() {
        ClientRegistry.registerKeyBinding(toggleSecondScreenKey);
        FMLCommonHandler.instance()
            .bus()
            .register(new OffscreenHandler());
    }

    @SubscribeEvent
    public void onKeyInput(final InputEvent.KeyInputEvent event) {
        if (toggleSecondScreenKey.isPressed()) {
            if (OffscreenAPI.isOpen(secondScreenWindow)) {
                OffscreenAPI.close(secondScreenWindow);
                secondScreenWindow = null;
            } else {
                secondScreenWindow = OffscreenAPI.open(FlowchartScreen::create);
                OffscreenAPI.setTitle(secondScreenWindow, "PlanNH Planner");
            }
        }
    }
}
