package com.sbancuz.plannh.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiErrorScreen;

import com.google.common.collect.ImmutableSortedMap;
import com.sbancuz.plannh.Compat;
import com.sbancuz.plannh.Tags;

import cpw.mods.fml.client.CustomModLoadingErrorDisplayException;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.versioning.ComparableVersion;

/**
 * The loading error for a pack older than PlanNH supports. Below the GregTech minimum, the required
 * GTNEIDefaultHandler mixin fails to apply, and the game freezes on the loading screen.
 *
 * <pre>
 *     PlanNH 0.1.8-git.2+863f3d3f7a-dirty does not support this version of GT New Horizons.
 *
 *     GregTech 5.09.51.482 is older than 5.09.54.192
 *
 *     Remove PlanNH from the mods folder, or update to GT New Horizons 2.9.0-RC-2 or newer.
 * </pre>
 * <p>
 * The text is English: FML logs the message before {@code refreshResources()} loads the lang files.
 */
public class UnsupportedPackException extends CustomModLoadingErrorDisplayException {

    private static final String HEADER_FORMAT = "PlanNH %s does not support this version of GT New Horizons.";
    private static final String FOOTER = "Remove PlanNH from the mods folder, or update to GT New Horizons 2.9.0-RC-2 or newer.";

    /** Oldest supported version of each mod, keyed by mod id. */
    private static final SortedMap<String, String> MINIMUM_VERSIONS = ImmutableSortedMap.<String, String>naturalOrder()
        // first version with GTNEIDefaultHandler.drawDescription(CachedDefaultRecipe, int)
        .put(Compat.IDs.GREGTECH, "5.09.54.192")
        // first version with RecipeWidget.getCyclingStacks
        .put(Compat.IDs.NEI, "2.8.148-GTNH")
        // The first versions with every class PlanNH references.
        // Assumed: each method PlanNH calls is as old as the method's class.
        .put(Compat.IDs.MODULARUI2, "2.3.55")
        .put(Compat.IDs.GTNHLIB, "0.11.5")
        .build();

    private final List<String> lines;
    private final List<String> wrapped = new ArrayList<>();

    private UnsupportedPackException(final List<String> lines) {
        super(String.join("\n", lines), null);
        this.lines = lines;
    }

    public static void throwIfPackUnsupported() {
        final List<String> outdated = describeOutdatedMods(
            Loader.instance()
                .getIndexedModList());
        if (outdated.isEmpty()) return;

        final List<String> lines = new ArrayList<>();
        lines.add(String.format(HEADER_FORMAT, Tags.VERSION));
        lines.add("");
        lines.addAll(outdated);
        lines.add("");
        lines.add(FOOTER);
        throw new UnsupportedPackException(lines);
    }

    public static List<String> describeOutdatedMods(final Map<String, ModContainer> modsById) {
        final List<String> outdated = new ArrayList<>();
        for (final Map.Entry<String, String> minimum : MINIMUM_VERSIONS.entrySet()) {
            final ModContainer mod = modsById.get(minimum.getKey());
            if (mod == null) continue;
            // GregTech's getVersion() returns the constant MC1710. getDisplayVersion() returns the mcmod.info version
            final String rawVersion = mod.getDisplayVersion();
            if (rawVersion == null) continue;

            final ComparableVersion version = new ComparableVersion(rawVersion);
            if (version.compareTo(new ComparableVersion(minimum.getValue())) < 0) {
                outdated.add(String.format("%s %s is older than %s", mod.getName(), rawVersion, minimum.getValue()));
            }
        }
        return outdated;
    }

    @Override
    public void initGui(final GuiErrorScreen errorScreen, final FontRenderer fontRenderer) {
        final int TEXT_MARGIN = 20;
        wrapped.clear();
        for (final String line : lines) {
            wrapped.addAll(fontRenderer.listFormattedStringToWidth(line, errorScreen.width - TEXT_MARGIN));
        }
    }

    /** Draw the lines that {@link #initGui} wrapped, centred on the screen. */
    @Override
    public void drawScreen(final GuiErrorScreen errorScreen, final FontRenderer fontRenderer, final int mouseRelX,
        final int mouseRelY, final float tickTime) {
        // The alternative, PlannhColors, would load GTNHLib's ColorResource.
        // GTNHLib before 0.11.5 has no ColorResource.
        final int TEXT_COLOR = 0xFFFFFF;
        int y = (errorScreen.height - wrapped.size() * fontRenderer.FONT_HEIGHT) / 2;
        for (final String line : wrapped) {
            errorScreen.drawCenteredString(fontRenderer, line, errorScreen.width / 2, y, TEXT_COLOR);
            y += fontRenderer.FONT_HEIGHT;
        }
    }
}
