package com.sbancuz.plannh.gui.node;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.flowchart.Node;
import com.sbancuz.plannh.data.setting.SettingDef;
import com.sbancuz.plannh.gui.LangScope;
import com.sbancuz.plannh.gui.common.FlowchartFlow;
import com.sbancuz.plannh.gui.common.FlowchartTextWidget;
import com.sbancuz.plannh.gui.common.MiddleClickReset;
import com.sbancuz.plannh.gui.theme.PlannhThemes;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;
import com.sbancuz.plannh.gui.tooltips.TooltipTheme;

public class SettingsFold extends NodeFold {

    private static final LangScope SETTINGS = LangScope.ROOT.child("gui.node.settings");

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
        final List<? extends SettingDef<?>> visible = cfg.getProfile()
            .visibleSettings(cfg, ctx)
            .toList();
        visible.forEach(def -> addSetting(cfg, ctx, def));

        final List<SettingDef<?>> edited = new ArrayList<>();
        for (final SettingDef<?> def : visible) {
            edited.addAll(def.editedSettings());
        }
        if (edited.stream()
            .anyMatch(cfg::isExplicit)) child(resetButton(cfg, edited));

        scheduleResize();
    }

    /**
     * Add the row of {@code def}. The row has a marker while a setting of the row is explicit, the editor, and a
     * tooltip with the description and the reset rows.
     */
    private <T> void addSetting(final MachineConfig cfg, final RecipeContext ctx, final SettingDef<T> def) {
        final IWidget editor = def.settingsWidget(cfg, ctx, change -> {
            commitEdit(change);
            markStale();
        });
        final List<? extends SettingDef<?>> edited = def.editedSettings();
        final String descriptionKey = "plannh.settings." + def.getKey() + ".desc";
        final SettingsRow row = new SettingsRow(node, cfg, edited, () -> {
            commitEdit(() -> edited.forEach(cfg::reset));
            markStale();
        });
        row.name(def.isFullWidthEditor() ? "settings.composite" : "settings.row")
            .fullWidth()
            .marginBottom(ROW_GAP)
            .coverChildrenHeight()
            .childPadding(2)
            .tooltipBuilder(TooltipBuilder.build(out -> {
                if (StatCollector.canTranslate(descriptionKey))
                    out.line(TooltipTheme.BODY.style(IKey.lang(descriptionKey)));
                out.resetHint(row.isExplicit());
            }))
            .tooltipAutoUpdate(true);

        if (row.isExplicit()) row.child(
            new FlowchartTextWidget(SETTINGS.lang("changed"), node).name("settings.row.changed")
                .widgetTheme(PlannhThemes.CARD_CHANGED));
        final String label = def.getEditorLabel();
        if (!def.isFullWidthEditor() || !label.isEmpty()) row.child(
            new FlowchartTextWidget(IKey.str(label), node).name("settings.row.label")
                .expanded());
        row.child(editor);
        child(row);
    }

    /** Return the button for a reset of each setting of {@code edited}. The reset is one undo step. */
    private IWidget resetButton(final MachineConfig cfg, final List<SettingDef<?>> edited) {
        final int height = 12;
        return new ButtonWidget<>().name("settings.reset")
            .widthRel(1f)
            .height(height)
            .overlay(SETTINGS.lang("reset"))
            .onMousePressed(clicked -> {
                if (clicked != 0) return false;
                commitEdit(() -> edited.forEach(cfg::reset));
                markStale();
                return true;
            })
            .tooltipBuilder(TooltipBuilder.build(SETTINGS, out -> out.text("reset.hint")));
    }

    /** A row of the settings fold that a middle-click resets. */
    public static final class SettingsRow extends FlowchartFlow implements MiddleClickReset {

        private final MachineConfig cfg;
        private final List<? extends SettingDef<?>> edited;
        private final Runnable resetAction;

        /** The row of the settings {@code edited}. {@code resetAction} resets those settings. */
        public SettingsRow(final NodeWidget node, final MachineConfig cfg, final List<? extends SettingDef<?>> edited,
            final Runnable resetAction) {
            super(GuiAxis.X, node);
            this.cfg = cfg;
            this.edited = edited;
            this.resetAction = resetAction;
        }

        @Override
        public boolean isExplicit() {
            return edited.stream()
                .anyMatch(cfg::isExplicit);
        }

        @Override
        public void reset() {
            resetAction.run();
        }
    }

}
