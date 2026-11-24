package com.sbancuz.plannh.hologram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HologramStoreTest {

    @TempDir
    Path dir;

    private static final Hologram ONE = new Hologram(0, 1, 2, 3, "one", 0xFF0000);
    private static final Hologram TWO = new Hologram(0, 4, 5, 6, "two", 0x00FF00);

    private File file() {
        return dir.resolve("holograms.json")
            .toFile();
    }

    @Test
    void putAndRemovePersist() {
        final HologramStore store = new HologramStore(file());
        assertTrue(store.put(ONE));
        assertEquals(
            ONE,
            HologramStore.load(file())
                .get(ONE.pos()));
        assertEquals(
            1,
            HologramStore.load(file())
                .inDimension(0)
                .size());
        assertTrue(store.remove(ONE.pos()));
        assertNull(
            HologramStore.load(file())
                .get(ONE.pos()));
        assertEquals(
            0,
            HologramStore.load(file())
                .size());
    }

    @Test
    void noOpChangesDoNotRewriteFile() throws IOException {
        final HologramStore store = new HologramStore(file());
        store.put(ONE);
        final FileTime old = FileTime.fromMillis(1_000_000L);
        Files.setLastModifiedTime(file().toPath(), old);
        store.put(ONE);
        store.remove(TWO.pos());
        assertEquals(old, Files.getLastModifiedTime(file().toPath()));
    }

    @Test
    void backupHoldsPreviousContent() throws IOException {
        final HologramStore store = new HologramStore(file());
        store.put(ONE);
        final String first = Files.readString(file().toPath(), StandardCharsets.UTF_8);
        store.put(TWO);
        assertEquals(first, Files.readString(dir.resolve("holograms.json.bak"), StandardCharsets.UTF_8));
    }

    @Test
    void corruptPrimaryFallsBackToBackup() throws IOException {
        final HologramStore store = new HologramStore(file());
        store.put(ONE);
        store.put(TWO);
        Files.writeString(file().toPath(), "{broken", StandardCharsets.UTF_8);
        final HologramStore loaded = HologramStore.load(file());
        assertTrue(loaded.wasPrimaryUnreadable());
        assertTrue(
            dir.resolve("holograms.json.corrupt")
                .toFile()
                .isFile());
        assertEquals(1, loaded.size());
        assertNotNull(loaded.get(ONE.pos()));
    }

    @Test
    void corruptPrimaryWithoutBackupIsEmpty() throws IOException {
        Files.writeString(file().toPath(), "{broken", StandardCharsets.UTF_8);
        final HologramStore loaded = HologramStore.load(file());
        assertEquals(0, loaded.size());
        assertTrue(loaded.wasPrimaryUnreadable());
    }

    @Test
    void saveAfterCorruptPrimaryKeepsBackup() throws IOException {
        final HologramStore store = new HologramStore(file());
        store.put(ONE);
        store.put(TWO);
        final Path backup = dir.resolve("holograms.json.bak");
        final String backupContent = Files.readString(backup, StandardCharsets.UTF_8);
        Files.writeString(file().toPath(), "{broken", StandardCharsets.UTF_8);

        final HologramStore loaded = HologramStore.load(file());
        loaded.put(new Hologram(0, 7, 8, 9, "three", 1));
        assertEquals(backupContent, Files.readString(backup, StandardCharsets.UTF_8));
        assertFalse(loaded.wasPrimaryUnreadable());
    }
}
