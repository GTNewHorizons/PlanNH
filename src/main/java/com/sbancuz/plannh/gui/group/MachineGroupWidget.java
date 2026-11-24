package com.sbancuz.plannh.gui.group;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.sbancuz.plannh.api.PlanAPI;
import com.sbancuz.plannh.data.flowchart.Graph;
import com.sbancuz.plannh.data.flowchart.Group;
import com.sbancuz.plannh.data.flowchart.MachineGroup;
import com.sbancuz.plannh.data.flowchart.Node;
import com.sbancuz.plannh.data.flowchart.balancer.BalanceResult;
import com.sbancuz.plannh.data.flowchart.balancer.Balancer;
import com.sbancuz.plannh.gui.CanvasWidget;
import com.sbancuz.plannh.gui.GuiHelper;
import com.sbancuz.plannh.gui.GuiIcon;
import com.sbancuz.plannh.gui.LangScope;
import com.sbancuz.plannh.gui.common.FlowchartWidget;
import com.sbancuz.plannh.gui.node.NodeWidget;
import com.sbancuz.plannh.gui.theme.PlannhThemes;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;
import com.sbancuz.plannh.gui.tooltips.TooltipTheme;

public class MachineGroupWidget extends GroupWidget<MachineGroup> {

    private static final LangScope GROUP = LangScope.ROOT.child("gui.group");

    @Nullable
    private Node mainNode;

    public MachineGroupWidget(CanvasWidget canvas, MachineGroup data) {
        super(canvas, data);
        name("group.machine");
        mainNode = data.getChildren()
            .values()
            .stream()
            .filter(graphData -> graphData instanceof Node)
            .map(graphData -> (Node) graphData)
            .filter(Node::isAvailable)
            .findFirst()
            .orElse(null);
    }

    @Override
    public boolean canAddToGroup(FlowchartWidget<?, ?> widget) {
        return switch (widget.getData()) {
            case Group _ -> false;
            case Node node -> acceptsNode(node);
            default -> true;
        };
    }

    /**
     * Whether a machine group would accept this node. The group is one machine, so every member
     * has to use the same handler name and profile, not the player's editable header.
     */
    public boolean acceptsNode(final Node node) {
        if (!node.isAvailable()) return true;
        if (mainNode == null) return true;

        return node.getMachineName()
            .equals(mainNode.getMachineName())
            && node.getMachineConfig()
                .getProfile()
                .equals(
                    mainNode.getMachineConfig()
                        .getProfile());
    }

    @Override
    public void joinGroup(FlowchartWidget<?, ?> widget) {
        super.joinGroup(widget);

        if (widget instanceof NodeWidget nodeWidget && nodeWidget.getData()
            .isAvailable()) {
            if (mainNode == null) mainNode = nodeWidget.getData();
            else if (nodeWidget.getData() != mainNode) nodeWidget.getData()
                .getMachineConfig()
                .copySettingsFrom(mainNode.getMachineConfig());
        }
    }

    @Override
    public void leaveGroup(FlowchartWidget<?, ?> widget) {
        mainNode = data.getChildren()
            .values()
            .stream()
            .filter(graphData -> graphData instanceof Node)
            .map(graphData -> (Node) graphData)
            .filter(Node::isAvailable)
            .findFirst()
            .orElse(null);
    }

    @Override
    protected Flow getTopRow() {
        return super.getTopRow().child(
            0,
            GuiIcon.MACHINE.asIcon()
                .size(12)
                .asWidget()
                .name("group.machine.badge")
                .size(16)
                .margin(2, 0, 0, 0)
                .tooltipPos(RichTooltip.Pos.NEXT_TO_MOUSE)
                .tooltipBuilder(TooltipBuilder.build(GROUP, this::writeMachineTooltip))
                .tooltipAutoUpdate(true));
    }

    @Override
    protected Flow getButtonRow(IPanelHandler colorPicker) {
        return super.getButtonRow(colorPicker).child(capacityButton("+", 1))
            .child(capacityButton("-", -1))
            .child(loadLabel());
    }

    /**
     * Steps the pool's capacity, floored at zero, which is the group's "as many machines as it
     * takes" and the state every chart starts in. The capacity is a solve input, so a step is an
     * edit like any other: recorded for undo and version-bumped so the chart re-balances under it.
     */
    private ButtonWidget<?> capacityButton(final String label, final int step) {
        return new ButtonWidget<>().name(step > 0 ? "group.capacity.add" : "group.capacity.remove")
            .size(HEADER_BUTTON_SIZE)
            .overlay(IKey.str(label))
            .onMousePressed(_ -> {
                PlanAPI.recordEdit(canvas.getGraph(), () -> {
                    data.setMachineCapacity(Math.max(0, data.getMachineCapacity() + step));
                    resolve();
                });
                return true;
            })
            .tooltipPos(RichTooltip.Pos.NEXT_TO_MOUSE)
            .tooltipBuilder(
                TooltipBuilder.build(
                    GROUP,
                    out -> writeCapacity(out.title(step > 0 ? "capacity.increase" : "capacity.decrease"))))
            .tooltipAutoUpdate(true);
    }

    /** Keep the header compact; the tooltip names the machine and explains its shared load. */
    private TextWidget<?> loadLabel() {
        return new TextWidget<>(IKey.dynamicKey(() -> {
            final double load = copyLoad();
            if (load <= 0) return IKey.EMPTY;
            return GuiHelper.ratio(GuiHelper.multiple(load), capacity());
        })).name("group.load")
            .widgetTheme(PlannhThemes.TEXT)
            .textAlign(Alignment.CenterRight)
            .tooltipPos(RichTooltip.Pos.NEXT_TO_MOUSE)
            .tooltipBuilder(TooltipBuilder.build(GROUP, this::writeMachineTooltip))
            .tooltipAutoUpdate(true);
    }

    private void writeMachineTooltip(final TooltipBuilder out) {
        out.title("machine.title")
            .entry(
                IKey.lang("plannh.gui.node.machine"),
                TooltipTheme.VALUE,
                mainNode == null ? GROUP.lang("machine.empty") : IKey.str(machineName()));
        if (canvas.getGraph()
            .balance() != null) out.entry("load", TooltipTheme.VALUE, GuiHelper.multiple(copyLoad()));
        else out.text("load.unbalanced");
        writeCapacity(out).separator()
            .text("machine.shared");
    }

    private TooltipBuilder writeCapacity(final TooltipBuilder out) {
        return out.entry("machine_capacity", TooltipTheme.TUNABLE, capacity());
    }

    /** The maximum machine count, or the unlimited mark when the group doesn't have a maximum. */
    private IKey capacity() {
        final int capacity = data.getMachineCapacity();
        return capacity > 0 ? IKey.str(Integer.toString(capacity)) : IKey.lang("plannh.gui.number.unlimited");
    }

    /**
     * Marks the chart for a fresh solve; a pool's capacity and membership are model inputs.
     */
    private void resolve() {
        canvas.getGraph()
            .bumpVersion();
    }

    /**
     * The pool's load: the machine counts of the framed nodes, summed. A node's count is already
     * machine time - a recipe filling a third of a machine reads 0.33 - so the sum is how much of
     * the one machine the group's recipes ask for between them. Every member runs the same handler
     * under the same settings, which is what makes the sum a single machine's worth of work rather
     * than an addition of unlike things.
     */
    private double copyLoad() {
        final Graph graph = canvas.getGraph();
        final BalanceResult balance = graph.balance();
        if (balance == null) return 0;
        double load = 0;
        for (final UUID nodeId : getData().getChildren()
            .keySet()) {
            final Balancer.NodeBalance nb = balance.nodeBalances()
                .get(nodeId);
            if (nb == null || nb.operations() <= 0) continue;
            load += nb.operations();
        }
        return load;
    }

    /**
     * The machine the group stands for, from any member; empty until a node is framed.
     */
    private String machineName() {
        return mainNode != null ? mainNode.getMachineName() : "";
    }
}
