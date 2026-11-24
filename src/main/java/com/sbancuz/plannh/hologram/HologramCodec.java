package com.sbancuz.plannh.hologram;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sbancuz.plannh.PlanNH;

/** JSON format of the hologram file. Pure and free of Minecraft dependencies. */
public final class HologramCodec {

    public static final int VERSION = 1;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .create();
    private static final Pattern COLOR = Pattern.compile("#[0-9a-fA-F]{6}");
    private static final Comparator<Hologram> ORDER = Comparator.comparingInt(Hologram::dimension)
        .thenComparingInt(Hologram::x)
        .thenComparingInt(Hologram::y)
        .thenComparingInt(Hologram::z);

    private HologramCodec() {}

    public static String encode(final Collection<Hologram> holograms) {
        final List<Hologram> sorted = new ArrayList<>(holograms);
        sorted.sort(ORDER);
        final JsonArray array = new JsonArray();
        for (final Hologram hologram : sorted) {
            final JsonObject entry = new JsonObject();
            entry.addProperty("dim", hologram.dimension());
            entry.addProperty("x", hologram.x());
            entry.addProperty("y", hologram.y());
            entry.addProperty("z", hologram.z());
            entry.addProperty("text", hologram.text());
            entry.addProperty("color", String.format("#%06X", hologram.color()));
            array.add(entry);
        }
        final JsonObject root = new JsonObject();
        root.addProperty("version", VERSION);
        root.add("holograms", array);
        return GSON.toJson(root);
    }

    /**
     * Decodes a hologram file; individually invalid entries are skipped, later duplicates win.
     *
     * @throws IOException if the JSON is malformed or its version is missing or unsupported
     */
    public static List<Hologram> decode(final String json) throws IOException {
        final JsonObject root;
        try {
            final JsonElement parsed = new JsonParser().parse(json);
            if (parsed == null || !parsed.isJsonObject()) throw new IOException("Hologram file is not a JSON object");
            root = parsed.getAsJsonObject();
            final JsonElement version = root.get("version");
            if (version == null || !version.isJsonPrimitive() || version.getAsInt() != VERSION) {
                throw new IOException("Unsupported hologram file version: " + version);
            }
        } catch (final RuntimeException e) {
            throw new IOException("Malformed hologram file", e);
        }

        final Map<HologramPos, Hologram> result = new LinkedHashMap<>();
        final JsonElement array = root.get("holograms");
        if (array == null || !array.isJsonArray()) return new ArrayList<>();
        for (final JsonElement element : array.getAsJsonArray()) {
            try {
                final Hologram hologram = decodeEntry(element.getAsJsonObject());
                result.put(hologram.pos(), hologram);
            } catch (final RuntimeException e) {
                PlanNH.LOG.warn("Skipping invalid hologram entry: {}", element, e);
            }
        }
        return new ArrayList<>(result.values());
    }

    private static Hologram decodeEntry(final JsonObject entry) {
        final String color = entry.get("color")
            .getAsString();
        if (!COLOR.matcher(color)
            .matches()) throw new IllegalArgumentException("Bad color " + color);
        return new Hologram(
            entry.get("dim")
                .getAsInt(),
            entry.get("x")
                .getAsInt(),
            entry.get("y")
                .getAsInt(),
            entry.get("z")
                .getAsInt(),
            entry.get("text")
                .getAsString(),
            Integer.parseInt(color.substring(1), 16));
    }
}
