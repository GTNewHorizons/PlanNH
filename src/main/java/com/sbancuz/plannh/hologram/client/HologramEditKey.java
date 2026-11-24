package com.sbancuz.plannh.hologram.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.input.Keyboard;

import com.sbancuz.plannh.hologram.BlockPin;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;

/** The key that opens the pin picker for the block face the player is looking at. */
public class HologramEditKey {

    private static final KeyBinding EDIT_KEY = new KeyBinding(
        "key.plannh.hologram.edit",
        Keyboard.KEY_H,
        "key.categories.neiflowchart");

    public static void init() {
        ClientRegistry.registerKeyBinding(EDIT_KEY);
        FMLCommonHandler.instance()
            .bus()
            .register(new HologramEditKey());
    }

    @SubscribeEvent
    public void onKeyInput(final InputEvent.KeyInputEvent event) {
        if (!EDIT_KEY.isPressed()) return;
        final Minecraft mc = Minecraft.getMinecraft();
        final MovingObjectPosition hit = mc.objectMouseOver;
        if (!PlanReadiness.isReady() || mc.currentScreen != null
            || mc.theWorld == null
            || mc.thePlayer == null
            || hit == null
            || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
        // The air block in front of the face; looking at a hologram hits the block behind it, so this re-targets it.
        final ForgeDirection side = ForgeDirection.getOrientation(hit.sideHit);
        PinPickerScreen.open(
            new BlockPin(
                mc.thePlayer.dimension,
                hit.blockX + side.offsetX,
                hit.blockY + side.offsetY,
                hit.blockZ + side.offsetZ));
    }
}
