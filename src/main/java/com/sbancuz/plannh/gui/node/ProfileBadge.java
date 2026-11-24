package com.sbancuz.plannh.gui.node;

import javax.annotation.Nullable;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.utils.Alignment;
import com.sbancuz.plannh.PlanNH;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.NodeBadge;
import com.sbancuz.plannh.data.flowchart.Node;
import com.sbancuz.plannh.gui.common.FlowchartFlow;
import com.sbancuz.plannh.gui.common.FlowchartTextWidget;
import com.sbancuz.plannh.gui.common.ItemWidget;
import com.sbancuz.plannh.gui.theme.PlannhThemes;

/** {@link ProfileBadge} shows the item and the code of the {@link NodeBadge} of the selected profile. */
public final class ProfileBadge extends FlowchartFlow {

    private static final int ITEM = 12;

    private final NodeWidget node;
    private long builtVersion = Long.MIN_VALUE;

    public ProfileBadge(final NodeWidget node) {
        super(GuiAxis.X, node);
        this.node = node;
        name("node.options.badge");
        coverChildren();
        padding(2, 1);
        background(GuiTextures.SLOT_ITEM);
        childPadding(1);
        crossAxisAlignment(Alignment.CrossAxis.CENTER);
        rebuild();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        rebuild();
    }

    /** Rebuild the badge when the graph's version changed. An edit of a setting changes the version. */
    private void rebuild() {
        final long version = node.getCanvas()
            .getGraph()
            .getVersion();
        if (version == builtVersion) return;
        builtVersion = version;

        removeAll();
        final NodeBadge badge = readBadge();
        setEnabled(badge != null);
        scheduleResize();
        if (badge == null) return;

        final String code = badge.code();
        if (badge.item() != null) child(
            new ItemWidget<>(badge.item()).name("node.badge.item")
                .size(ITEM)
                .disableHoverThemeBackground(true));
        // `TextRenderer` centres the capitals half a pixel above the item. The text shadow is one pixel lower, so the
        // capitals and the shadow centre on the item
        if (!code.isEmpty()) child(
            new FlowchartTextWidget(IKey.str(code), node).name("node.badge.code")
                .widgetTheme(PlannhThemes.CARD_BADGE)
                .height(ITEM));
    }

    private @Nullable NodeBadge readBadge() {
        final Node data = node.getData();
        final MachineConfig cfg = data.getMachineConfig();
        try {
            return cfg.getProfile()
                .getBadge()
                .apply(cfg, data.recipeContext());
        } catch (RuntimeException | LinkageError failure) {
            PlanNH.LOG.warn("node.badge.failed machine={}", data.getMachineName(), failure);
            return null;
        }
    }
}
