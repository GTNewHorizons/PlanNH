package com.sbancuz.plannh.hologram.client;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;

import codechicken.core.ClientUtils;
import codechicken.nei.NEIClientConfig;

/**
 * Tells whether NEI points at the current world's directory, so that {@code Plan.getInstance()} reads the right
 * file. In singleplayer NEI receives its world path through a packet some time after the world appears, and it never
 * clears the path when leaving a world, so right after joining the path is either unset or the previous world's.
 * The expected path is built the way {@code NEICPH.getSaveName} builds it.
 */
public final class PlanReadiness {

    private PlanReadiness() {}

    public static boolean isReady() {
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || !NEIClientConfig.isLoaded()) return false;
        final String expected = expectedWorldPath(mc);
        return expected != null && expected.equals(NEIClientConfig.getWorldPath());
    }

    @Nullable
    private static String expectedWorldPath(final Minecraft mc) {
        try {
            if (mc.isSingleplayer()) {
                final String name = ClientUtils.getWorldSaveName();
                return name == null ? null : "local/" + name;
            }
            final String ip = ClientUtils.getServerIP();
            return ip == null ? null : "remote/" + ip.replace(':', '~');
        } catch (final Exception failure) {
            return null;
        }
    }
}
