package com.sbancuz.plannh.nei.config;

import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Keyboard;

import codechicken.nei.KeyManager;
import codechicken.nei.api.API;
import codechicken.nei.util.NEIKeyboardUtils;

/** NEI key bindings used by the flowchart. */
public final class KeybindsConfig {

    public static final String UNDO_KEY = "plannh.undo";
    public static final String REDO_KEY = "plannh.redo";
    public static final String REDO_ALT_KEY = "plannh.redo_alt";

    private KeybindsConfig() {}

    public static void register() {
        API.addKeyBind(UNDO_KEY, Keyboard.KEY_Z);
        API.addKeyBind(REDO_KEY, Keyboard.KEY_Z);
        API.addKeyBind(REDO_ALT_KEY, Keyboard.KEY_Y);
    }

    public static boolean isUndo(final int eventKeyCode) {
        return matches(eventKeyCode, KeyManager.getKeyCode(UNDO_KEY), false);
    }

    public static boolean isRedo(final int eventKeyCode) {
        return matches(eventKeyCode, KeyManager.getKeyCode(REDO_KEY), true)
            || matches(eventKeyCode, KeyManager.getKeyCode(REDO_ALT_KEY), false);
    }

    static boolean matches(final int eventKeyCode, final int bindingKeyCode, final boolean ctrlDown,
        final boolean shiftDown, final boolean altDown, final boolean requiredShift) {
        return bindingKeyCode > Keyboard.KEY_NONE && eventKeyCode == bindingKeyCode
            && ctrlDown
            && shiftDown == requiredShift
            && !altDown;
    }

    private static boolean matches(final int eventKeyCode, final int bindingKeyCode, final boolean requiredShift) {
        return matches(
            eventKeyCode,
            bindingKeyCode,
            GuiScreen.isCtrlKeyDown(),
            GuiScreen.isShiftKeyDown(),
            Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU),
            requiredShift);
    }

    public static String undoName() {
        return shortcutName(UNDO_KEY, "Ctrl");
    }

    public static String redoName() {
        return shortcutName(REDO_KEY, "Ctrl", "Shift");
    }

    public static String redoAltName() {
        return shortcutName(REDO_ALT_KEY, "Ctrl");
    }

    private static String shortcutName(final String ident, final String... modifiers) {
        final int keyCode = KeyManager.getKeyCode(ident);
        if (keyCode == Keyboard.KEY_NONE) return null;

        final StringBuilder name = new StringBuilder();
        for (final String modifier : modifiers) {
            if (name.length() > 0) name.append(" + ");
            name.append(modifier);
        }
        if (name.length() > 0) name.append(" + ");
        name.append(NEIKeyboardUtils.getKeyName(keyCode));
        return name.toString();
    }
}
