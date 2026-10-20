package com.sbancuz.plannh.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.util.StatCollector;

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
 * <p>
 * FML logs the message before {@code refreshResources()} loads the lang files, so the message is fixed English.
 * {@link #initGui} runs after that refresh and reads the screen text from the lang file.
 */
public class UnsupportedPackException extends CustomModLoadingErrorDisplayException {

    /** The first GT New Horizons release in which no mod is below {@link #MINIMUM_VERSIONS}. */
    private static final String MINIMUM_PACK = "2.9.0-RC-2";
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

    private final List<OutdatedMod> outdatedMods;
    private final List<String> wrapped = new ArrayList<>();

    private UnsupportedPackException(final List<OutdatedMod> outdatedMods) {
        super(
            String.format(
                "unsupported.pack plannh=%s minimumPack=%s outdated=%s",
                Tags.VERSION,
                MINIMUM_PACK,
                outdatedMods),
            null);
        this.outdatedMods = outdatedMods;
    }

    public static void throwIfPackUnsupported() {
        final List<OutdatedMod> outdatedMods = findOutdatedMods(
            Loader.instance()
                .getIndexedModList());
        if (!outdatedMods.isEmpty()) throw new UnsupportedPackException(outdatedMods);
    }

    public static List<OutdatedMod> findOutdatedMods(final Map<String, ModContainer> modsById) {
        final List<OutdatedMod> outdatedMods = new ArrayList<>();
        for (final Map.Entry<String, String> minimum : MINIMUM_VERSIONS.entrySet()) {
            final ModContainer mod = modsById.get(minimum.getKey());
            if (mod == null) continue;
            // GregTech's getVersion() returns the constant MC1710. getDisplayVersion() returns the mcmod.info version
            final String rawVersion = mod.getDisplayVersion();
            if (rawVersion == null) continue;

            final ComparableVersion version = new ComparableVersion(rawVersion);
            if (version.compareTo(new ComparableVersion(minimum.getValue())) < 0) {
                outdatedMods.add(new OutdatedMod(mod.getName(), rawVersion, minimum.getValue()));
            }
        }
        return outdatedMods;
    }

    @Override
    public void initGui(final GuiErrorScreen errorScreen, final FontRenderer fontRenderer) {
        final List<String> lines = new ArrayList<>();
        lines.add(StatCollector.translateToLocalFormatted("plannh.unsupported_pack.header", Tags.VERSION));
        lines.add("");
        for (final OutdatedMod mod : outdatedMods) {
            lines.add(
                StatCollector.translateToLocalFormatted(
                    "plannh.unsupported_pack.outdated_mod",
                    mod.name(),
                    mod.version(),
                    mod.minimumVersion()));
        }
        lines.add("");
        lines.add(StatCollector.translateToLocalFormatted("plannh.unsupported_pack.footer", MINIMUM_PACK));

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

    /** A mod older than its entry in {@link #MINIMUM_VERSIONS}. */
    public record OutdatedMod(String name, String version, String minimumVersion) {}
}
