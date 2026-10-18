package com.sbancuz.plannh.gui.node;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.utils.Alignment;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.flowchart.Node;
import com.sbancuz.plannh.gui.common.FlowchartFlow;
import com.sbancuz.plannh.gui.common.FlowchartTextWidget;

class SettingsFold extends NodeFold {

    private final Node data;
    private final MachineConfig config;
    private MachineProfile savedProfile;

    public SettingsFold(final NodeWidget node) {
        super(
            node,
            () -> node.getData()
                .isSettingsOpen());
        name("fold.settings");

        data = node.getData();
        config = data.getMachineConfig();
        savedProfile = config.getProfile();

        // Only the profile changes which settings there are to show. The pinned target used to be
        // watched here as well, back when the machine setting was hidden behind a pin; it is a
        // different number now and hiding one behind the other made no sense.
        onUpdateListener(w -> {
            if (savedProfile != config.getProfile()) {
                savedProfile = config.getProfile();
                markStale();
            }
        }, true);
    }

    @Override
    protected void rebuild() {
        removeAll();

        final MachineConfig config = data.getMachineConfig();
        config.getProfile()
            .visibleSettings(new RecipeContext(data.getProperties()), config)
            .forEach(def -> child(row(def.getLabel(), def.settingsWidget(data.getMachineConfig(), change -> {
                commitEdit(change);
                markStale();
            }))));

        // A profile's own knobs, after the shared ones
        if (config.getNodeState() != null) {
            config.getNodeState()
                .rows(new RecipeContext(data.getProperties()), change -> {
                    commitEdit(change);
                    markStale();
                })
                .forEach(stateRow -> child(row(stateRow.label(), stateRow.control())));
        }

        scheduleResize();
    }

    private IWidget row(final String label, final IWidget control) {
        return FlowchartFlow.row(node)
            .name("settings.row")
            .fullWidth()
            .marginBottom(ROW_GAP)
            .coverChildrenHeight()
            .childPadding(2)
            .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
            .child(new FlowchartTextWidget(label, node).name("settings.row.label"))
            .child(control);
    }
}
