package com.sbancuz.plannh.hologram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

class HologramCodecTest {

    @Test
    void roundTripsHolograms() throws IOException {
        final List<Hologram> holograms = List.of(
            new Hologram(0, 1, 2, 3, "Iron line", 0xFF8800),
            new Hologram(-1, -5, 64, 7, "Nether", Hologram.DEFAULT_COLOR));
        final List<Hologram> decoded = HologramCodec.decode(HologramCodec.encode(holograms));
        assertEquals(2, decoded.size());
        assertEquals(holograms.get(0), decoded.get(1));
        assertEquals(holograms.get(1), decoded.get(0));
    }

    @Test
    void encodingIsSortedAndDeterministic() {
        final Hologram a = new Hologram(0, 5, 1, 1, "a", 1);
        final Hologram b = new Hologram(0, 1, 9, 1, "b", 2);
        final Hologram c = new Hologram(-1, 9, 9, 9, "c", 3);
        assertEquals(HologramCodec.encode(List.of(a, b, c)), HologramCodec.encode(List.of(c, a, b)));
        assertEquals(List.of(c, b, a), decodeUnchecked(HologramCodec.encode(List.of(a, b, c))));
    }

    @Test
    void rejectsBadVersionAndMalformedJson() {
        assertThrows(IOException.class, () -> HologramCodec.decode("{\"version\": 2, \"holograms\": []}"));
        assertThrows(IOException.class, () -> HologramCodec.decode("{\"holograms\": []}"));
        assertThrows(IOException.class, () -> HologramCodec.decode("{\"version\": \"x\", \"holograms\": []}"));
        assertThrows(IOException.class, () -> HologramCodec.decode("{not json"));
        assertThrows(IOException.class, () -> HologramCodec.decode("[]"));
    }

    @Test
    void skipsInvalidEntries() throws IOException {
        final String json = """
            {"version": 1, "holograms": [
              {"dim":0,"x":1,"y":2,"z":3,"text":"ok","color":"#00FF00"},
              {"dim":0,"x":1,"y":2,"text":"missing z","color":"#00FF00"},
              {"dim":0,"x":4,"y":2,"z":3,"text":"bad color","color":"green"},
              {"dim":0,"x":5,"y":2,"z":3,"text":"no color"}
            ]}
            """;
        final List<Hologram> decoded = HologramCodec.decode(json);
        assertEquals(List.of(new Hologram(0, 1, 2, 3, "ok", 0x00FF00)), decoded);
    }

    @Test
    void lastDuplicateWins() throws IOException {
        final String json = """
            {"version": 1, "holograms": [
              {"dim":0,"x":1,"y":2,"z":3,"text":"first","color":"#000001"},
              {"dim":0,"x":1,"y":2,"z":3,"text":"second","color":"#000002"}
            ]}
            """;
        assertEquals(List.of(new Hologram(0, 1, 2, 3, "second", 2)), HologramCodec.decode(json));
    }

    @Test
    void sanitizesTextAndMasksColor() {
        assertEquals("a b c", new Hologram(0, 0, 0, 0, "  a\nb\rc  ", 0).text());
        assertEquals(
            Hologram.MAX_TEXT_LENGTH,
            new Hologram(0, 0, 0, 0, "x".repeat(1000), 0).text()
                .length());
        assertEquals(0x123456, new Hologram(0, 0, 0, 0, "t", 0xFF123456).color());
    }

    private static List<Hologram> decodeUnchecked(final String json) {
        try {
            return HologramCodec.decode(json);
        } catch (final IOException e) {
            throw new AssertionError(e);
        }
    }
}
