package com.sbancuz.plannh.gui.group;

import static com.sbancuz.plannh.data.flowchart.Group.GROUP_MIN_W;

import java.util.SortedMap;
import java.util.UUID;

import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.utils.TreeUtil;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ColorPickerDialog;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.sbancuz.plannh.api.PlanAPI;
import com.sbancuz.plannh.data.flowchart.Group;
import com.sbancuz.plannh.data.flowchart.MachineGroup;
import com.sbancuz.plannh.gui.CanvasWidget;
import com.sbancuz.plannh.gui.GuiIcon;
import com.sbancuz.plannh.gui.PlannhColors;
import com.sbancuz.plannh.gui.common.CloseButtonWidget;
import com.sbancuz.plannh.gui.common.FlowchartFlow;
import com.sbancuz.plannh.gui.common.FlowchartWidget;
import com.sbancuz.plannh.gui.common.HeaderTextWidget;

import lombok.Getter;

public abstract class GroupWidget<G extends Group> extends FlowchartWidget<GroupWidget<G>, G> {

    protected static final int HEADER_BUTTON_SIZE = 16;

    @Getter
    protected final GroupAreaWidget areaWidget;

    public static <G extends Group> GroupWidget<?> of(CanvasWidget canvas, G group) {
        if (group instanceof MachineGroup machineGroup) return new MachineGroupWidget(canvas, machineGroup);
        return new SimpleGroupWidget(canvas, group);
    }

    protected GroupWidget(CanvasWidget canvas, G data) {
        super(canvas, data);
        areaWidget = new GroupAreaWidget(this);

        coverChildren(GROUP_MIN_W, 0);

        background(
            new DynamicDrawable(
                () -> new Rectangle().hollow(2)
                    .color(Color.withAlpha(data.getColor(), 255))),
            new DynamicDrawable(
                () -> new Rectangle().hollow(1)
                    .color(PlannhColors.FRAME_BORDER.getColor())));

        data.getChildren()
            .values()
            .forEach(subData -> {
                FlowchartWidget<?, ?> widget = FlowchartWidget.getFlowchartWidgetFromData(canvas, subData);
                widget.setDataContainer(data.getChildren());
                areaWidget.child(widget);
            });

        IPanelHandler colorPicker = IPanelHandler.simple(
            canvas.getPanel(),
            (_, _) -> new ColorPickerDialog(
                color -> PlanAPI.recordEdit(canvas.getGraph(), () -> data.setColor(color)),
                data.getColor(),
                true),
            true);

        name("group");

        Flow mainColumn = FlowchartFlow.col(this)
            .name("group.column")
            .coverChildren(GROUP_MIN_W, 0)
            .collapseDisabledChild();

        Flow topRow = getTopRow();
        Flow buttonRow = getButtonRow(colorPicker);

        topRow.child(buttonRow);

        mainColumn.child(topRow);
        mainColumn.child(areaWidget);

        child(mainColumn);
    }

    protected Flow getTopRow() {
        return FlowchartFlow.row(this)
            .name("group.header")
            .coverChildrenHeight()
            .fullWidth()
            .childPadding(2)
            .padding(2, 0)
            .background(
                new DynamicDrawable(() -> new Rectangle().color(data.getColor())),
                new DynamicDrawable(
                    () -> new Rectangle().hollow(1)
                        .color(PlannhColors.FRAME_BORDER.getColor())))
            .child(
                new HeaderTextWidget(this, data::getColor).height(20)
                    .setScale(1.5f));
    }

    protected Flow getButtonRow(IPanelHandler colorPicker) {
        return FlowchartFlow.row(this)
            .name("group.buttons")
            .coverChildren()
            .childPadding(2)
            .reverseLayout()
            .child(
                new CloseButtonWidget(this).size(HEADER_BUTTON_SIZE)
                    .marginRight(0))
            .child(
                new ToggleButton().name("group.coverChildren")
                    .size(HEADER_BUTTON_SIZE)
                    .value(new BoolValue.Dynamic(data::isCoverChildren, val -> {
                        data.setCoverChildren(val);
                        areaWidget.configureCoverChildren();
                    }))
                    .overlay(
                        GuiIcon.COVER_CHILDREN.asIcon()
                            .size(GuiIcon.COVER_CHILDREN.size()))
                    .addTooltipLine("Toggle Cover Children"))
            .child(
                new ButtonWidget<>().name("group.color")
                    .size(HEADER_BUTTON_SIZE)
                    .overlay(
                        GuiTextures.COLOR_WHEEL.asIcon()
                            .size(10))
                    .onMousePressed(_ -> {
                        if (!colorPicker.isPanelOpen()) colorPicker.openPanel();
                        else colorPicker.closePanel();
                        return true;
                    })
                    .addTooltipLine("Open Color Picker"));
    }

    public boolean canContain(FlowchartWidget<?, ?> widget) {
        return TreeUtil.findParent(this, parent -> parent == widget) == null && areaWidget.getArea()
            .contains(widget.getPositionedArea()) && canAddToGroup(widget);
    }

    public abstract boolean canAddToGroup(FlowchartWidget<?, ?> widget);

    public void joinGroup(FlowchartWidget<?, ?> widget) {
        areaWidget.child(widget);
        widget.setDataContainer(data.getChildren());
    }

    public void leaveGroup(FlowchartWidget<?, ?> widget) {}

    @Override
    public void removeFromGraph() {
        areaWidget.getChildren()
            .stream()
            .filter(w -> w instanceof FlowchartWidget<?, ?>)
            .map(w -> (FlowchartWidget<?, ?>) w)
            .forEach(FlowchartWidget::removeFromGraph);
        super.removeFromGraph();
    }

    @Override
    protected SortedMap<UUID, ? super G> getDefaultContainer() {
        return canvas.getGraph()
            .getGroups();
    }

    public int contentAreaOffset() {
        return areaWidget.getArea().y - getArea().y;
    }

    @Override
    public boolean isObstacle() {
        return false;
    }
}
