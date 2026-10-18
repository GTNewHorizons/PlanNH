package com.sbancuz.plannh.data;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.google.gson.JsonObject;

/**
 * What a mod keeps on a node besides the shared settings: the knobs its own machines have, such as the coils a
 * GregTech multiblock is planned with or the capacitor in an EnderIO machine.
 *
 * <p>
 * The shared {@link com.sbancuz.plannh.data.setting.Settings} are the same for every machine of a profile, so they
 * are stored whole. These come from the picked machine, so a value left unset follows that machine: an untouched
 * node stores nothing here and keeps up when its machine or the chart changes.
 */
public interface NodeState {

    /** The picked machine's id, or null to plan with the best machine for the recipe. */
    @Nullable
    String machineId();

    /** The values set on this node. Keyed by plain strings, so a save outlives the mod's class layout. */
    @Nonnull
    JsonObject save();

    @Nonnull
    NodeState copy();

    /**
     * Rows drawn after the shared settings. {@code edit} runs a change so undo reaches it and the chart
     * re-solves, as in {@link com.sbancuz.plannh.data.setting.SettingDef#settingsWidget}.
     */
    @Nonnull
    List<Row> rows(RecipeContext ctx, Consumer<Runnable> edit);

    /** One settings row: what it sets, and the control that sets it. */
    record Row(String label, IWidget control) {}
}
