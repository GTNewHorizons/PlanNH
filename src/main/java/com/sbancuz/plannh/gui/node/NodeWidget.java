package com.sbancuz.plannh.gui.node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.sbancuz.plannh.PlanNH;
import com.sbancuz.plannh.api.PlanAPI;
import com.sbancuz.plannh.data.effect.EffectResult;
import com.sbancuz.plannh.data.flowchart.Node;
import com.sbancuz.plannh.data.flowchart.balancer.BalanceMode;
import com.sbancuz.plannh.data.flowchart.balancer.Balancer;
import com.sbancuz.plannh.gui.CanvasWidget;
import com.sbancuz.plannh.gui.PlannhColors;
import com.sbancuz.plannh.gui.common.CloseButtonWidget;
import com.sbancuz.plannh.gui.common.FlowchartFlow;
import com.sbancuz.plannh.gui.common.FlowchartWidget;
import com.sbancuz.plannh.gui.common.HeaderTextWidget;
import com.sbancuz.plannh.gui.edge.ArrowWidget;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;

import it.unimi.dsi.fastutil.ints.IntIntPair;
import lombok.Getter;

public class NodeWidget extends FlowchartWidget<NodeWidget, Node> {

    private static final String SETTINGS_LANG = "plannh.gui.node.settings";
    private static final String TARGET_LANG = "plannh.gui.node.target.rows";
    private static final String THROUGHPUT_LANG = "plannh.gui.node.throughput.rows";
    private static final String PROPERTIES_LANG = "plannh.gui.node.properties";

    /** Compact square toggles for the node's configuration folds. */
    private static final int FOLD_BUTTON = 12;

    private final RecipeAreaWidget recipeAreaWidget;
    private final CycleButtonWidget settingsFold;
    private final CycleButtonWidget targetFold;
    private final CycleButtonWidget throughputFold;
    private final CycleButtonWidget propertiesFold;
    private boolean lastSettingsOpen;
    private boolean lastTargetOpen;
    private boolean lastThroughputOpen;
    private boolean lastPropertiesOpen;
    private long effectVersion = Long.MIN_VALUE;
    private EffectResult effect;

    @Getter
    private final List<ArrowWidget> arrowWidgets = new ArrayList<>();

    private static final UITexture bg = UITexture.builder()
        .location("nei:textures/gui/recipebg.png")
        .imageSize(256, 256)
        .subAreaXYWH(4, 4, 176, 166)
        .adaptable(3)
        .build();

    public NodeWidget(CanvasWidget canvas, Node data) {
        super(canvas, data);
        canvas.getNodeWidgets()
            .put(data.getId(), this);

        coverChildren();
        background(bg);
        padding(5);
        name("node");

        recipeAreaWidget = new RecipeAreaWidget(this);
        Flow mainColumn = FlowchartFlow.col(this)
            .name("node.column")
            .coverChildrenHeight()
            .width(recipeAreaWidget.getRecipeWidth())
            .collapseDisabledChild()
            .childPadding(4);

        Flow topRow = FlowchartFlow.row(this)
            .name("node.header")
            .coverChildrenHeight()
            .childPadding(4)
            .fullWidth()
            .background(new Rectangle().color(PlannhColors.titleColor(data.getMachineName())))
            .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN);

        topRow.child(
            new HeaderTextWidget(this, PlannhColors.titleColor(data.getMachineName())).setScale(1)
                .background()
                .setTextColor(Color.BLACK.main));

        topRow.child(new CloseButtonWidget(this));
        mainColumn.child(topRow);
        mainColumn.child(recipeAreaWidget);

        if (data.isAvailable()) {
            throughputFold = fold(
                GuiTextures.PROCESSOR,
                THROUGHPUT_LANG,
                data::isThroughputOpen,
                data::setThroughputOpen);
            propertiesFold = fold(GuiTextures.FILE, PROPERTIES_LANG, data::isPropertiesOpen, data::setPropertiesOpen);
            targetFold = fold(GuiTextures.GRAPH, TARGET_LANG, data::isTargetOpen, data::setTargetOpen);
            settingsFold = fold(GuiTextures.GEAR, SETTINGS_LANG, data::isSettingsOpen, data::setSettingsOpen);

            // What this machine is, on the left as the folds that decide what else it says, and on the
            // right the profile it is being read as - which is what the settings fold below is a list of.
            Flow optionRow = FlowchartFlow.row(this)
                .name("node.options")
                .fullWidth()
                .coverChildrenHeight()
                .childPadding(4)
                .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN);

            optionRow.child(
                FlowchartFlow.row(this)
                    .name("node.options.folds")
                    .coverChildren()
                    .childPadding(4)
                    .child(throughputFold)
                    .child(propertiesFold)
                    .child(targetFold)
                    .child(settingsFold))
                .child(new ExtractorWidget(this));
            mainColumn.child(optionRow);
            mainColumn.child(new ThroughputInfoWidget(this));
            mainColumn.child(new ConfigurationAreaWidget(this));
        } else {
            throughputFold = null;
            propertiesFold = null;
            targetFold = null;
            settingsFold = null;
            if (!data.getAvailableProfiles()
                .isEmpty())
                mainColumn.child(
                    FlowchartFlow.row(this)
                        .fullWidth()
                        .coverChildrenHeight()
                        .mainAxisAlignment(Alignment.MainAxis.END)
                        .child(new ExtractorWidget(this)));
        }
        child(mainColumn);
    }

    private CycleButtonWidget fold(final UITexture icon, final String langKey, final BooleanSupplier open,
        final Consumer<Boolean> set) {
        return new CycleButtonWidget().size(FOLD_BUTTON)
            .name("fold.toggle")
            .stateCount(2)
            .stateOverlay(true, icon.asIcon())
            .stateOverlay(
                false,
                icon.withColorOverride(PlannhColors.TEXT_DARK.getColor())
                    .asIcon())
            .value(new BoolValue.Dynamic(open, set::accept))
            .tooltipBuilder(
                tooltip -> TooltipBuilder.create(tooltip)
                    .langRows(langKey, langKey + (open.getAsBoolean() ? ".hide_hint" : ".show_hint"))
                    .flush());
    }

    /** Records an edit to this node's machine, so undo can take it back and the chart is re-solved. */
    public void commitEdit(final Runnable change) {
        try {
            PlanAPI.recordEdit(canvas.getGraph(), change);
            canvas.getGraph()
                .bumpVersion();
            PlanAPI.save();
        } catch (final Exception | LinkageError failure) {
            PlanNH.LOG.error("Failed to edit recipe for " + data.getMachineName(), failure);
            PlanAPI.showMessage("plannh.error.recipe_edit", data.getMachineName());
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        final boolean target = data.isTargetOpen();
        if (target != lastTargetOpen) {
            lastTargetOpen = target;
            if (targetFold != null) targetFold.markTooltipDirty();
        }

        final boolean settings = data.isSettingsOpen();
        if (settings != lastSettingsOpen) {
            lastSettingsOpen = settings;
            if (settingsFold != null) settingsFold.markTooltipDirty();
        }

        final boolean throughput = data.isThroughputOpen();
        if (throughput != lastThroughputOpen) {
            lastThroughputOpen = throughput;
            if (throughputFold != null) throughputFold.markTooltipDirty();
        }

        final boolean properties = data.isPropertiesOpen();
        if (properties != lastPropertiesOpen) {
            lastPropertiesOpen = properties;
            if (propertiesFold != null) propertiesFold.markTooltipDirty();
        }
    }

    @Override
    public boolean isObstacle() {
        return true;
    }

    @Override
    protected SortedMap<UUID, Node> getDefaultContainer() {
        return canvas.getGraph()
            .getNodes();
    }

    @Override
    public void removeFromGraph() {
        super.removeFromGraph();
        // The graph's own removal, not just the widget's: it also takes this node's edges with it.
        canvas.getGraph()
            .removeNode(data.getId());
        canvas.getNodeWidgets()
            .remove(data.getId());
        arrowWidgets.forEach(ArrowWidget::removeFromGraph);
    }

    public Map<IntIntPair, PortWidget> getPortWidgets(boolean isInput) {
        return recipeAreaWidget.getPortWidgets(isInput);
    }

    public PortWidget getPortWidget(IntIntPair index, boolean isInput) {
        return getPortWidgets(isInput).get(index);
    }

    /** How the chart's last solve treated this machine, or null while it has never been solved. */
    public Balancer.NodeBalance balance() {
        return canvas.getGraph()
            .balance()
            .nodeBalances()
            .get(data.getId());
    }

    /** Port state and tooltip rows share one evaluation for the current graph revision. */
    public @Nullable EffectResult effectSnapshot() {
        if (!data.isAvailable()) return null;
        final long version = canvas.getGraph()
            .getVersion();
        if (effectVersion != version) {
            effectVersion = version;
            try {
                effect = data.computeEffect();
            } catch (RuntimeException | LinkageError failure) {
                effect = null;
                PlanNH.LOG.warn("Could not evaluate machine effect for {}", data.getMachineName(), failure);
            }
        }
        return effect;
    }

    /** The mode the whole chart is being solved in, which is what decides which pins exist at all. */
    public BalanceMode balanceMode() {
        return canvas.getGraph()
            .getBalanceMode();
    }
}
