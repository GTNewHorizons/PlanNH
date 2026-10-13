package com.sbancuz.plannh.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiErrorScreen;

import com.sbancuz.plannh.Compat;
import com.sbancuz.plannh.Tags;

import cpw.mods.fml.client.CustomModLoadingErrorDisplayException;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.versioning.ComparableVersion;

/**
 * Halts loading on a pack older than PlanNH supports. Below these versions a required mixin fails when GregTech's init
 * loads its target, and the game freezes on the loading screen instead of reporting it. English only: the text doubles
 * as the log message, built before lang files load.
 */
public class UnsupportedPackException extends CustomModLoadingErrorDisplayException {

    private static final String HEADER = "PlanNH " + Tags.VERSION
        + " does not support this version of GT New Horizons.";
    private static final String FOOTER = "Remove PlanNH from the mods folder, or update to GT New Horizons 2.9.0 or newer.";

    /**
     * Mod id and the first release carrying everything PlanNH uses from it. GregTech's version is its mcmod.info
     * version: its {@code @Mod} version is the constant {@code MC1710}.
     */
    private static final String[][] MINIMUM_VERSIONS = {
        // GTNEIDefaultHandler.drawDescription(CachedDefaultRecipe, int)
        { Compat.IDs.GREGTECH, "5.09.54.192" },
        // RecipeWidget.getCyclingStacks
        { "NotEnoughItems", "2.8.148-GTNH" }, { "modularui2", "2.3.55" }, { "gtnhlib", "0.11.5" } };

    private final List<String> lines;
    private final List<String> wrapped = new ArrayList<>();

    private UnsupportedPackException(final List<String> lines) {
        super(String.join("\n", lines), null);
        this.lines = lines;
    }

    public static void throwIfUnsupported() {
        final List<String> outdated = outdated(
            Loader.instance()
                .getIndexedModList());
        if (outdated.isEmpty()) return;

        final List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        lines.add("");
        lines.addAll(outdated);
        lines.add("");
        lines.add(FOOTER);
        throw new UnsupportedPackException(lines);
    }

    public static List<String> outdated(final Map<String, ModContainer> mods) {
        final List<String> outdated = new ArrayList<>();
        for (final String[] minimum : MINIMUM_VERSIONS) {
            final ModContainer mod = mods.get(minimum[0]);
            if (mod == null || mod.getDisplayVersion() == null) continue;
            if (new ComparableVersion(mod.getDisplayVersion()).compareTo(new ComparableVersion(minimum[1])) < 0) {
                outdated.add(mod.getName() + " " + mod.getDisplayVersion() + " is older than " + minimum[1]);
            }
        }
        return outdated;
    }

    @Override
    public void initGui(final GuiErrorScreen errorScreen, final FontRenderer fontRenderer) {
        wrapped.clear();
        for (final String line : lines) {
            wrapped.addAll(fontRenderer.listFormattedStringToWidth(line, errorScreen.width - 20));
        }
    }

    @Override
    public void drawScreen(final GuiErrorScreen errorScreen, final FontRenderer fontRenderer, final int mouseRelX,
        final int mouseRelY, final float tickTime) {
        // Not PlannhColors: it loads GTNHLib's ColorResource, which the packs reported here may lack.
        int y = (errorScreen.height - wrapped.size() * fontRenderer.FONT_HEIGHT) / 2;
        for (final String line : wrapped) {
            errorScreen.drawCenteredString(fontRenderer, line, errorScreen.width / 2, y, 0xFFFFFF);
            y += fontRenderer.FONT_HEIGHT;
        }
    }
}
