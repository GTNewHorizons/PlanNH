package com.sbancuz.plannh.gui.node;

import java.util.List;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.flowchart.Node;
import com.sbancuz.plannh.data.setting.SettingDef;
import com.sbancuz.plannh.gui.common.FlowchartFlow;
import com.sbancuz.plannh.gui.common.FlowchartTextWidget;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;

public class SettingsFold extends NodeFold {

    private final Node data;
    private MachineConfig savedConfig;
    private MachineProfile savedProfile;
    private long savedVersion = Long.MIN_VALUE;

    public SettingsFold(final NodeWidget node) {
        super(
            node,
            () -> node.getData()
                .isSettingsOpen());
        name("fold.settings");

        data = node.getData();
        savedConfig = data.getMachineConfig();
        savedProfile = savedConfig.getProfile();

        onUpdateListener(w -> {
            final MachineConfig currentConfig = data.getMachineConfig();
            final MachineProfile currentProfile = currentConfig.getProfile();
            final long currentVersion = node.getCanvas()
                .getGraph()
                .getVersion();
            if (savedConfig != currentConfig || savedProfile != currentProfile || savedVersion != currentVersion) {
                savedConfig = currentConfig;
                savedProfile = currentProfile;
                savedVersion = currentVersion;
                markStale();
            }
        }, true);
    }

    @Override
    protected void rebuild() {
        removeAll();

        final MachineConfig cfg = data.getMachineConfig();
        final RecipeContext ctx = data.recipeContext();
        cfg.getProfile()
            .visibleSettings(cfg, ctx)
            .forEach(def -> addSetting(cfg, ctx, def));

        scheduleResize();
    }

    private <T> void addSetting(final MachineConfig cfg, final RecipeContext ctx, final SettingDef<T> def) {
        final IWidget editor = def.settingsWidget(cfg, ctx, change -> {
            commitEdit(change);
            markStale();
        });
        final List<? extends SettingDef<?>> edited = def.editedSettings();
        final SettingsRow row = new SettingsRow(node, cfg, edited, () -> {
            commitEdit(() -> edited.forEach(cfg::reset));
            markStale();
        });
        row.name(def.isFullWidthEditor() ? "settings.composite" : "settings.row")
            .fullWidth()
            .marginBottom(ROW_GAP)
            .coverChildrenHeight()
            .childPadding(2)
            .tooltipBuilder(tooltip -> {
                final boolean explicit = edited.stream()
                    .anyMatch(cfg::isExplicit);
                final TooltipBuilder out = TooltipBuilder.create(tooltip);
                if (!def.isFullWidthEditor()) def.tooltip(cfg.value(def, ctx))
                    .accept(out);
                out.langRow(explicit ? "plannh.settings.choice_manual" : "plannh.settings.choice_auto")
                    .langRow("plannh.settings.middle_reset_hint")
                    .flush();
            })
            .tooltipAutoUpdate(true);

        final String label = def.getEditorLabel();
        if (!def.isFullWidthEditor() || !label.isEmpty()) row.child(
            new FlowchartTextWidget(IKey.str(label), node).name("settings.row.label")
                .expanded());
        row.child(editor);
        child(row);
    }

    /** A settings row with the screen-level middle-click fallback reset. */
    public static final class SettingsRow extends FlowchartFlow implements Interactable {

        private final MachineConfig cfg;
        private final List<? extends SettingDef<?>> edited;
        private final Runnable reset;

        public SettingsRow(final NodeWidget node, final MachineConfig cfg, final List<? extends SettingDef<?>> edited,
            final Runnable reset) {
            super(GuiAxis.X, node);
            this.cfg = cfg;
            this.edited = edited;
            this.reset = reset;
        }

        /** Resets this row when no editor-owned middle-click handler consumed the event. */
        public boolean resetSelection() {
            if (!isValid()) return false;
            if (!edited.stream()
                .anyMatch(cfg::isExplicit)) return false;
            reset.run();
            return true;
        }

        @Override
        public Result onMousePressed(final int mouseButton) {
            return mouseButton == 2 && resetSelection() ? Result.SUCCESS : Result.IGNORE;
        }
    }

}
