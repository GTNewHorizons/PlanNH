package com.sbancuz.plannh.data.provider.enderio;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.util.StatCollector;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.sbancuz.plannh.data.ChartMinimums;
import com.sbancuz.plannh.data.NodeState;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.machine.MachineRows;

/**
 * An EnderIO node's own setting: the capacitor its machine is built with. Unset follows the chart's floor, so one
 * chart-wide choice applies to every EnderIO node.
 *
 * <p>
 * EnderIO's recipe registry is keyed by machine, so a recipe has one machine and there is nothing to pick.
 */
public final class EnderIONodeState implements NodeState {

    @Nullable
    private Integer capacitor;

    @Nonnull
    public static EnderIONodeState load(final JsonObject saved) {
        final EnderIONodeState state = new EnderIONodeState();
        if (saved.get("capacitor") instanceof final JsonPrimitive value && value.isNumber())
            state.capacitor = value.getAsInt();
        return state;
    }

    @Override
    @Nonnull
    public JsonObject save() {
        final JsonObject saved = new JsonObject();
        if (capacitor != null) saved.addProperty("capacitor", capacitor);
        return saved;
    }

    @Override
    @Nonnull
    public EnderIONodeState copy() {
        return load(save());
    }

    @Override
    @Nullable
    public String machineId() {
        return null;
    }

    /** The capacitor tier the node runs with. */
    public int capacitorTier() {
        return capacitor != null ? capacitor
            : ChartMinimums.floor(EnderIOProfile.CAPACITOR_FLOOR, EnderIOCapacitors.highestTier());
    }

    @Override
    @Nonnull
    public List<Row> rows(final RecipeContext ctx, final Consumer<Runnable> edit) {
        final EnderIOMachines machine = ctx.getOrDefault(EnderIOMachines.MACHINE, null);
        if (machine == null || !machine.capacitorDriven()) return List.of();
        return List.of(
            MachineRows.tier(
                StatCollector.translateToLocal("plannh.settings.eio_capacitor"),
                0,
                EnderIOCapacitors.highestTier(),
                EnderIOCapacitors::label,
                new MachineRows.Followed<>() {

                    @Override
                    @Nullable
                    public Integer stored() {
                        return capacitor;
                    }

                    @Override
                    @Nonnull
                    public Integer machine() {
                        return capacitorTier();
                    }

                    @Override
                    public void set(@Nullable final Integer value) {
                        capacitor = value;
                    }
                },
                edit));
    }
}
