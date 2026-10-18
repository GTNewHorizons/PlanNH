package com.sbancuz.plannh.data.provider.gregtech;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.sbancuz.plannh.data.NodeState;
import com.sbancuz.plannh.data.RecipeContext;

/**
 * A GregTech node's own settings, in the spec's terms: the machine, its mode, the energy hatch, a value per
 * {@link gregtech.api.logic.ModifierKind} the machine reads, and overrides per {@link GTOverride}. Every field left
 * unset follows the machine, the recipe, or the chart's floors.
 *
 * <p>
 * Saved with plain strings, never class names or enum ordinals: kind ids such as {@code gregtech:coil}, the voltage
 * name, and each override under its quantity's name.
 */
public final class GTNodeState implements NodeState {

    @Nullable
    String machine;
    @Nullable
    Integer mode;
    /** A GregTech voltage name, such as {@code "IV"}. */
    @Nullable
    String voltage;
    @Nullable
    Integer amps;
    /** By kind id. */
    final SortedMap<String, Long> structure = new TreeMap<>();
    /** {@link Integer} or {@link Boolean}, as the override takes. */
    final EnumMap<GTOverride, Object> overrides = new EnumMap<>(GTOverride.class);
    /** Shows the override rows in place of the structure rows. Changes no number on its own. */
    boolean advanced;

    @Nonnull
    public static GTNodeState load(final JsonObject saved) {
        final GTNodeState state = new GTNodeState();
        state.machine = string(saved, "machine");
        state.mode = integer(saved, "mode");
        state.voltage = string(saved, "voltage");
        state.amps = integer(saved, "amps");
        if (saved.get("structure") instanceof final JsonObject structure) {
            for (final Map.Entry<String, JsonElement> kind : structure.entrySet()) {
                if (kind.getValue() instanceof final JsonPrimitive value && value.isNumber())
                    state.structure.put(kind.getKey(), value.getAsLong());
            }
        }
        for (final GTOverride override : GTOverride.values()) {
            if (!(saved.get(override.quantity.name()) instanceof final JsonObject group)) continue;
            if (!(group.get(override.key) instanceof final JsonPrimitive value)) continue;
            if (override.isFlag() && value.isBoolean()) state.overrides.put(override, value.getAsBoolean());
            else if (!override.isFlag() && value.isNumber()) state.overrides.put(override, value.getAsInt());
        }
        state.advanced = saved.get("advanced") instanceof final JsonPrimitive value && value.isBoolean()
            && value.getAsBoolean();
        return state;
    }

    @Override
    @Nonnull
    public JsonObject save() {
        final JsonObject saved = new JsonObject();
        if (machine != null) saved.addProperty("machine", machine);
        if (mode != null) saved.addProperty("mode", mode);
        if (voltage != null) saved.addProperty("voltage", voltage);
        if (amps != null) saved.addProperty("amps", amps);
        if (!structure.isEmpty()) {
            final JsonObject kinds = new JsonObject();
            structure.forEach(kinds::addProperty);
            saved.add("structure", kinds);
        }
        overrides.forEach((override, value) -> {
            final String group = override.quantity.name();
            if (!saved.has(group)) saved.add(group, new JsonObject());
            final JsonObject values = saved.getAsJsonObject(group);
            if (value instanceof final Boolean on) values.addProperty(override.key, on);
            else values.addProperty(override.key, (Integer) value);
        });
        if (advanced) saved.addProperty("advanced", true);
        return saved;
    }

    @Override
    @Nonnull
    public GTNodeState copy() {
        return load(save());
    }

    @Override
    @Nullable
    public String machineId() {
        return machine;
    }

    @Override
    @Nonnull
    public List<Row> rows(final RecipeContext ctx, final Consumer<Runnable> edit) {
        return GTSettings.rows(ctx, this, edit);
    }

    /**
     * Another machine has different coils, parallel ceiling and overclock rules, so the values set for the last one
     * give numbers this one can't reach. Voltage stays: it is the power the node is given, not part of the machine.
     */
    public void pickMachine(@Nullable final String id) {
        machine = id;
        mode = null;
        amps = null;
        structure.clear();
        overrides.clear();
    }

    @Nullable
    public String getVoltage() {
        return voltage;
    }

    public void setVoltage(@Nullable final String voltage) {
        this.voltage = voltage;
    }

    @Nullable
    public Integer getAmps() {
        return amps;
    }

    public void setAmps(@Nullable final Integer amps) {
        this.amps = amps;
    }

    @Nullable
    public Integer getMode() {
        return mode;
    }

    public void setMode(@Nullable final Integer mode) {
        this.mode = mode;
    }

    /** A structure value, by kind id, or null when the node follows the chart and the recipe. */
    @Nullable
    public Long getStructure(final String kindId) {
        return structure.get(kindId);
    }

    public void setStructure(final String kindId, @Nullable final Long value) {
        if (value == null) structure.remove(kindId);
        else structure.put(kindId, value);
    }

    @Nullable
    public Object getOverride(final GTOverride override) {
        return overrides.get(override);
    }

    /** {@code value} is an {@link Integer} or a {@link Boolean}, as the override takes. Null follows the machine. */
    public void setOverride(final GTOverride override, @Nullable final Object value) {
        if (value == null) overrides.remove(override);
        else overrides.put(override, value);
    }

    public boolean isAdvanced() {
        return advanced;
    }

    public void setAdvanced(final boolean advanced) {
        this.advanced = advanced;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof final GTNodeState other && Objects.equals(machine, other.machine)
            && Objects.equals(mode, other.mode)
            && Objects.equals(voltage, other.voltage)
            && Objects.equals(amps, other.amps)
            && structure.equals(other.structure)
            && overrides.equals(other.overrides)
            && advanced == other.advanced;
    }

    @Override
    public int hashCode() {
        return Objects.hash(machine, mode, voltage, amps, structure, overrides, advanced);
    }

    @Nullable
    private static String string(final JsonObject saved, final String key) {
        return saved.get(key) instanceof final JsonPrimitive value && value.isString() ? value.getAsString() : null;
    }

    @Nullable
    private static Integer integer(final JsonObject saved, final String key) {
        return saved.get(key) instanceof final JsonPrimitive value && value.isNumber() ? value.getAsInt() : null;
    }
}
