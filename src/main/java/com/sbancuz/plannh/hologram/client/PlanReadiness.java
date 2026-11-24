package com.sbancuz.plannh.hologram.client;

import java.lang.ref.WeakReference;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Tells whether NEI has loaded the current world, so that {@code Plan.getInstance()} reads the right file.
 * NEI sets its world path in the START phase of a client tick and never clears it, while the world itself is
 * set mid-tick during packet handling; a render frame in between would see a stale path.
 */
public final class PlanReadiness {

    private static WeakReference<World> startWorld = new WeakReference<>(null);
    private static WeakReference<World> readyWorld = new WeakReference<>(null);

    public static void init() {
        FMLCommonHandler.instance()
            .bus()
            .register(new PlanReadiness());
    }

    public static boolean isReady() {
        final World world = Minecraft.getMinecraft().theWorld;
        return world != null && world == readyWorld.get();
    }

    @SubscribeEvent
    public void onClientTick(final TickEvent.ClientTickEvent event) {
        final World world = Minecraft.getMinecraft().theWorld;
        if (event.phase == TickEvent.Phase.START) {
            startWorld = new WeakReference<>(world);
        } else if (world == null) {
            readyWorld = new WeakReference<>(null);
        } else if (world == startWorld.get()) {
            readyWorld = new WeakReference<>(world);
        }
    }
}
