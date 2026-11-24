package com.sbancuz.plannh.hologram.client;

import javax.annotation.Nullable;

import org.lwjgl.input.Keyboard;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.sbancuz.plannh.PlanNH;
import com.sbancuz.plannh.api.PlanAPI;
import com.sbancuz.plannh.hologram.Hologram;
import com.sbancuz.plannh.hologram.HologramPos;
import com.sbancuz.plannh.hologram.HologramStore;

/** Small client-only dialog to create, edit or delete the hologram at one position. */
public final class HologramEditorScreen extends ModularScreen {

    private static final int WIDTH = 220;
    private static final int HEIGHT = 70;
    private static final int MARGIN = 8;
    private static final int BUTTON_WIDTH = 64;

    private HologramEditorScreen(final ModularPanel panel) {
        super(PlanNH.MODID, panel);
    }

    /** Opens the editor for {@code pos}; the existing hologram, if any, is prefilled. */
    public static void open(final HologramPos pos) {
        final HologramStore store = HologramStore.current();
        if (store == null) return;
        final Hologram existing = store.get(pos);

        final ModularPanel panel = ModularPanel.defaultPanel("hologram_editor", WIDTH, HEIGHT);
        final EditField field = new EditField();
        final Runnable save = () -> {
            save(store, pos, existing, field.getText());
            panel.closeIfOpen();
        };
        field.onEnter = save;
        field.name("hologram.text")
            .value(new StringValue(existing == null ? "" : existing.text()))
            .setMaxLength(Hologram.MAX_TEXT_LENGTH)
            .setFocusOnGuiOpen(true)
            .pos(MARGIN, 20)
            .size(WIDTH - 2 * MARGIN, 14);

        panel.child(
            new TextWidget<>(IKey.lang("plannh.hologram.editor.title")).name("hologram.title")
                .pos(MARGIN, 7))
            .child(field)
            .child(button("hologram.save", "plannh.hologram.editor.save", save).pos(MARGIN, 46))
            .child(button("hologram.delete", "plannh.hologram.editor.delete", () -> {
                if (!store.remove(pos)) PlanAPI.showMessage("plannh.hologram.error.save");
                panel.closeIfOpen();
            }).left((WIDTH - BUTTON_WIDTH) / 2)
                .top(46)
                .setEnabledIf(_ -> existing != null))
            .child(
                button("hologram.cancel", "plannh.hologram.editor.cancel", panel::closeIfOpen).right(MARGIN)
                    .top(46));
        ClientGUI.open(new HologramEditorScreen(panel));
    }

    private static ButtonWidget<?> button(final String name, final String key, final Runnable action) {
        return new ButtonWidget<>().name(name)
            .size(BUTTON_WIDTH, 16)
            .overlay(IKey.lang(key))
            .onMousePressed(button -> {
                if (button != 0) return false;
                action.run();
                return true;
            });
    }

    private static void save(final HologramStore store, final HologramPos pos, @Nullable final Hologram existing,
        final String text) {
        final boolean saved;
        if (text.trim()
            .isEmpty()) {
            saved = store.remove(pos);
        } else {
            final int color = existing == null ? Hologram.DEFAULT_COLOR : existing.color();
            saved = store.put(new Hologram(pos.dimension(), pos.x(), pos.y(), pos.z(), text, color));
        }
        if (!saved) PlanAPI.showMessage("plannh.hologram.error.save");
    }

    /** Text field that saves on Enter instead of just losing focus. */
    private static final class EditField extends TextFieldWidget {

        private Runnable onEnter = () -> {};

        @Override
        public Interactable.Result onKeyPressed(final char typedChar, final int keyCode) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                onEnter.run();
                return Interactable.Result.SUCCESS;
            }
            return super.onKeyPressed(typedChar, keyCode);
        }
    }
}
