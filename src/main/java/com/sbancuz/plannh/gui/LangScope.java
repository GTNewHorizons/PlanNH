package com.sbancuz.plannh.gui;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;

/**
 * A prefix of lang keys. {@link #lang} and {@link #translate} take the last part of a key.
 *
 * <pre>
 *     LangScope.ROOT.child("gui.node.port").lang("rate")   ->  plannh.gui.node.port.rate
 * </pre>
 */
public record LangScope(String prefix) {

    public static final LangScope ROOT = new LangScope("plannh.");

    public LangScope child(final String path) {
        return new LangScope(prefix + path + ".");
    }

    public IKey lang(final String name, final Object... args) {
        return IKey.lang(prefix + name, args);
    }

    public String translate(final String name, final Object... args) {
        return StatCollector.translateToLocalFormatted(prefix + name, args);
    }
}
