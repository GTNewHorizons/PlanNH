package com.sbancuz.plannh.client;

import java.util.ArrayDeque;
import java.util.Queue;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.sbancuz.plannh.PlanNH;
import com.sbancuz.plannh.gui.FlowchartScreen;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Client-side delivery for PlanAPI messages; notification failures never interrupt an operation. */
public final class MessageHandler {

    private static final int MAX_PENDING = 64;
    private static final Queue<String> PENDING = new ArrayDeque<>();

    public static void show(final IKey key) {
        try {
            final Minecraft mc = Minecraft.getMinecraft();
            if (mc == null) deliver(key);
            else mc.func_152344_a(() -> deliver(key));
        } catch (final Exception | LinkageError failure) {
            PlanNH.LOG.warn("Unable to queue PlanNH message", failure);
        }
    }

    private static void deliver(final IKey key) {
        final String message;
        try {
            message = key.getFormatted();
            synchronized (PENDING) {
                if (PENDING.size() == MAX_PENDING) PENDING.remove();
                PENDING.add(message);
            }
        } catch (final Exception | LinkageError failure) {
            PlanNH.LOG.warn("Unable to queue PlanNH message", failure);
            return;
        }
        try {
            if (ModularScreen.getCurrent() instanceof FlowchartScreen screen && screen.getMainPanel()
                .isOpen()) {
                screen.showMessage(message);
            }
        } catch (final Exception | LinkageError failure) {
            PlanNH.LOG.warn("Unable to display PlanNH popup", failure);
        }
    }

    @SubscribeEvent
    public void onClientTick(final TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            final Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) return;
            synchronized (PENDING) {
                while (!PENDING.isEmpty()) {
                    mc.thePlayer.addChatMessage(new ChatComponentText("[" + PlanNH.MODID + "] " + PENDING.remove()));
                }
            }
        } catch (final Exception | LinkageError failure) {
            PlanNH.LOG.warn("Unable to display PlanNH chat message", failure);
        }
    }
}
