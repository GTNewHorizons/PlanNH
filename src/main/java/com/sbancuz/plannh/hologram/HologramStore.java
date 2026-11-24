package com.sbancuz.plannh.hologram;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;

import com.sbancuz.plannh.PlanNH;
import com.sbancuz.plannh.api.PlanAPI;

/** Holograms of one world save file. Every change is written to disk immediately. */
public final class HologramStore {

    private static final String FILE_NAME = "holograms.json";

    @Nullable
    private static HologramStore instance;

    private final File file;
    private final Map<HologramPos, Hologram> holograms = new HashMap<>();
    private boolean primaryUnreadable;

    public HologramStore(final File file) {
        this.file = file;
    }

    /** Loads the store, falling back to the backup if the primary file is unreadable. */
    public static HologramStore load(final File file) {
        final HologramStore store = new HologramStore(file);
        if (!file.isFile()) return store;
        try {
            store.addAll(read(file));
            return store;
        } catch (final IOException | RuntimeException e) {
            PlanNH.LOG.error("Failed to load holograms from {}", file, e);
            store.primaryUnreadable = true;
            try {
                Files.copy(file.toPath(), corruptFile(file).toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (final IOException copyFailure) {
                PlanNH.LOG.error("Failed to preserve unreadable holograms", copyFailure);
            }
        }
        final File backup = backupFile(file);
        if (backup.isFile()) {
            try {
                store.addAll(read(backup));
            } catch (final IOException | RuntimeException e) {
                PlanNH.LOG.error("Failed to load hologram backup {}", backup, e);
            }
        }
        return store;
    }

    /** Whether loading had to discard the primary file and no save has happened since. */
    public boolean wasPrimaryUnreadable() {
        return primaryUnreadable;
    }

    @Nullable
    public Hologram get(final HologramPos pos) {
        return holograms.get(pos);
    }

    public Collection<Hologram> inDimension(final int dimension) {
        return Collections.unmodifiableList(
            holograms.values()
                .stream()
                .filter(hologram -> hologram.dimension() == dimension)
                .collect(Collectors.toList()));
    }

    public int size() {
        return holograms.size();
    }

    /** @return false if the change could not be written to disk; it is kept in memory regardless */
    public boolean put(final Hologram hologram) {
        if (hologram.equals(holograms.put(hologram.pos(), hologram))) return true;
        return save();
    }

    /** @return false if the change could not be written to disk; it is kept in memory regardless */
    public boolean remove(final HologramPos pos) {
        if (holograms.remove(pos) == null) return true;
        return save();
    }

    private void addAll(final List<Hologram> loaded) {
        for (final Hologram hologram : loaded) holograms.put(hologram.pos(), hologram);
    }

    private boolean save() {
        try {
            PlanAPI.atomicWrite(
                file,
                HologramCodec.encode(holograms.values()),
                primaryUnreadable ? null : backupFile(file));
            primaryUnreadable = false;
            return true;
        } catch (final IOException e) {
            PlanNH.LOG.error("Failed to save holograms to {}", file, e);
            return false;
        }
    }

    private static List<Hologram> read(final File source) throws IOException {
        return HologramCodec.decode(Files.readString(source.toPath(), StandardCharsets.UTF_8));
    }

    private static File backupFile(final File primary) {
        return new File(primary.getPath() + ".bak");
    }

    private static File corruptFile(final File primary) {
        return new File(primary.getPath() + ".corrupt");
    }

    public static File saveFileForCurrentWorld() {
        return new File(
            PlanAPI.getSaveFile()
                .getParentFile(),
            FILE_NAME);
    }

    /** The store of the world the client is in, or null outside a world. */
    @Nullable
    public static HologramStore current() {
        if (Minecraft.getMinecraft().theWorld == null) return null;
        try {
            final File file = saveFileForCurrentWorld();
            if (instance == null || !instance.file.equals(file)) {
                instance = load(file);
                if (instance.wasPrimaryUnreadable()) PlanAPI.showMessage("plannh.hologram.error.load");
            }
            return instance;
        } catch (final Exception | LinkageError e) {
            PlanNH.LOG.error("Failed to open holograms of the current world", e);
            return null;
        }
    }

    public static void unload() {
        instance = null;
    }
}
