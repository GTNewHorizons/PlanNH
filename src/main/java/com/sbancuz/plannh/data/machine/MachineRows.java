package com.sbancuz.plannh.data.machine;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.text.TextRenderer;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.value.IntValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.sbancuz.plannh.data.NodeState;

/**
 * Settings rows for values that come from a node's machine, for a {@link NodeState}. An unset value shows the
 * machine's number and follows it. Editing stores a value, and the reset button beside it returns the row to the
 * machine's number.
 */
public final class MachineRows {

    private MachineRows() {}

    private static final int PADDING = 10;
    /** Machine names run far longer than a node is wide, so the picker shortens them to this. */
    private static final int MAX_PICKER_WIDTH = 100;
    /** A longer range is typed in, not cycled through. */
    private static final int MAX_CYCLED = 32;

    /**
     * A value a node may set over its machine's.
     *
     * @param <T> {@link Integer} or {@link Boolean}
     */
    public interface Followed<T> {

        /** The node's value, or null when it follows the machine. */
        @Nullable
        T stored();

        @Nonnull
        T machine();

        /** Null returns the row to the machine's value. */
        void set(@Nullable T value);

        @Nonnull
        default T effective() {
            final T stored = stored();
            return stored != null ? stored : machine();
        }
    }

    /**
     * Cycles through the machines that can run the recipe, best first. An unset picker is planned with the first,
     * so a node on the default stores nothing.
     */
    @Nonnull
    public static NodeState.Row picker(final String label, final List<? extends MachineVariant> candidates,
        final Followed<String> machineId, final Consumer<Runnable> edit) {
        final CycleButtonWidget button = new CycleButtonWidget().name("settings.machine")
            .stateCount(candidates.size())
            .value(
                new IntValue.Dynamic(
                    () -> Math.max(0, indexOf(candidates, machineId.effective())),
                    index -> edit.accept(
                        () -> machineId.set(
                            candidates.get(index)
                                .id()))))
            .width(
                Math.min(
                    MAX_PICKER_WIDTH,
                    widest(
                        candidates.stream()
                            .map(MachineVariant::label)
                            .toList())));
        for (int i = 0; i < candidates.size(); i++) {
            button.stateOverlay(
                i,
                IKey.str(
                    shortened(
                        candidates.get(i)
                            .label(),
                        MAX_PICKER_WIDTH)));
        }
        return new NodeState.Row(label, withReset(button, machineId, edit));
    }

    private static int indexOf(final List<? extends MachineVariant> candidates, final String id) {
        for (int i = 0; i < candidates.size(); i++) {
            if (candidates.get(i)
                .id()
                .equals(id)) return i;
        }
        return -1;
    }

    /**
     * A tier drawn as its name, such as a coil or a voltage. A range too long to cycle through is typed in as a
     * number.
     */
    @Nonnull
    public static NodeState.Row tier(final String label, final int min, final int max, final IntFunction<String> name,
        final Followed<Integer> value, final Consumer<Runnable> edit) {
        if (max - min >= MAX_CYCLED) return number(label, min, max, value, edit);
        final CycleButtonWidget button = new CycleButtonWidget().name("settings.tier")
            .stateCount(max - min + 1)
            .value(
                new IntValue.Dynamic(
                    () -> clamp(value.effective(), min, max) - min,
                    index -> edit.accept(() -> value.set(min + index))));
        int width = 0;
        for (int tier = min; tier <= max; tier++) {
            final String text = name.apply(tier);
            button.stateOverlay(tier - min, IKey.str(text));
            width = Math.max(width, textWidth(text));
        }
        return new NodeState.Row(label, withReset(button.width(width), value, edit));
    }

    /** A number typed in, within {@code min} and {@code max}. */
    @Nonnull
    public static NodeState.Row number(final String label, final int min, final int max, final Followed<Integer> value,
        final Consumer<Runnable> edit) {
        final TextFieldWidget field = new TextFieldWidget().name("settings.int")
            .width(Math.max(textWidth(Integer.toString(min)), textWidth(Integer.toString(max))))
            .value(new IntValue.Dynamic(value::effective, typed -> edit.accept(() -> value.set(typed))))
            .numbersInt(min, max)
            .formatAsInteger(true);
        return new NodeState.Row(label, withReset(field, value, edit));
    }

    /** An on/off value. */
    @Nonnull
    public static NodeState.Row flag(final String label, final Followed<Boolean> value, final Consumer<Runnable> edit) {
        final ToggleButton toggle = new ToggleButton().name("settings.toggle")
            .value(new BoolValue.Dynamic(value::effective, on -> edit.accept(() -> value.set(on))))
            .overlay(false, IKey.str("[ ]"))
            .overlay(true, IKey.str("[✓]"));
        return new NodeState.Row(label, withReset(toggle, value, edit));
    }

    /** The control, then a reset button shown while the row holds a value of its own. */
    private static IWidget withReset(final IWidget control, final Followed<?> value, final Consumer<Runnable> edit) {
        return Flow.row()
            .coverChildren()
            .childPadding(2)
            .child(control)
            .child(
                new ButtonWidget<>().name("settings.reset")
                    .overlay(IKey.str("×"))
                    .size(textWidth("×") + 4, 10)
                    .setEnabledIf(w -> value.stored() != null)
                    .tooltipStatic(t -> t.addLine(IKey.lang("plannh.settings.follow_machine", new Object[] {})))
                    .onMousePressed(button -> {
                        edit.accept(() -> value.set(null));
                        return true;
                    }));
    }

    /** {@code text}, cut with an ellipsis to fit {@code width}. */
    private static String shortened(final String text, final int width) {
        if (textWidth(text) <= width) return text;
        String cut = text;
        while (!cut.isEmpty() && textWidth(cut + "…") > width) cut = cut.substring(0, cut.length() - 1);
        return cut + "…";
    }

    private static int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int widest(final List<String> texts) {
        int width = 0;
        for (final String text : texts) width = Math.max(width, textWidth(text));
        return width;
    }

    private static int textWidth(final String text) {
        return TextRenderer.getFontRenderer()
            .getStringWidth(text) + PADDING;
    }
}
