package com.sbancuz.plannh.nei.config;

import javax.annotation.Nullable;

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
    public static final String PORT_DISPLAY_KEY = "plannh.port_display";

    private KeybindsConfig() {}

    public static void register() {
        API.addKeyBind(UNDO_KEY, Keyboard.KEY_Z);
        API.addKeyBind(REDO_KEY, Keyboard.KEY_Z);
        API.addKeyBind(REDO_ALT_KEY, Keyboard.KEY_Y);
        API.addKeyBind(PORT_DISPLAY_KEY, Keyboard.KEY_T);
    }

    public static boolean isPortDisplayToggle(final int eventKeyCode) {
        return matches(eventKeyCode, KeyManager.getKeyCode(PORT_DISPLAY_KEY), false);
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

    /** An NEI key binding, with the modifiers that the flowchart requires. */
    public enum Shortcut {

        UNDO(UNDO_KEY, NEIKeyboardUtils.CTRL_HASH),
        REDO(REDO_KEY, NEIKeyboardUtils.CTRL_HASH | NEIKeyboardUtils.SHIFT_HASH),
        REDO_ALT(REDO_ALT_KEY, NEIKeyboardUtils.CTRL_HASH),
        PORT_DISPLAY(PORT_DISPLAY_KEY, NEIKeyboardUtils.CTRL_HASH),
        RECIPE("recipe.recipe", 0),
        USAGE("recipe.usage", 0);

        private final String ident;
        private final int modifiers;

        Shortcut(final String ident, final int modifiers) {
            this.ident = ident;
            this.modifiers = modifiers;
        }

        /** NEI's name of the binding with the modifiers, or {@code null} when the binding has no key. */
        public @Nullable String keyName() {
            return KeyManager.getKeyName(ident, modifiers);
        }
    }
}
