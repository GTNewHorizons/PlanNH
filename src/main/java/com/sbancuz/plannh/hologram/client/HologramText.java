package com.sbancuz.plannh.hologram.client;

import net.minecraft.util.StatCollector;

import org.apache.commons.lang3.StringUtils;

import com.sbancuz.plannh.data.flowchart.GraphData;
import com.sbancuz.plannh.data.flowchart.Group;
import com.sbancuz.plannh.data.flowchart.Node;
import com.sbancuz.plannh.data.flowchart.Note;

/** The one line a plan element shows as a hologram and in the pin picker. */
final class HologramText {

    static final int WHITE = 0xFFFFFF;

    private HologramText() {}

    /** The line for {@code data}, or an empty string if it has nothing to show. */
    static String of(final GraphData data) {
        if (data instanceof Note note) {
            for (final String line : note.getText()) {
                if (line != null && !line.isBlank()) return line.trim();
            }
            return "";
        }
        if (StringUtils.isNotBlank(data.getHeader())) return data.getHeader();
        if (data instanceof Group) return StatCollector.translateToLocal("plannh.hologram.unnamed_group");
        if (data instanceof Node node && node.getMachineName() != null) {
            return StatCollector.translateToLocal(node.getMachineName());
        }
        return "";
    }

    static int color(final GraphData data) {
        return data instanceof Group group ? group.getColor() & WHITE : WHITE;
    }
}
