package com.sbancuz.plannh.gui.group;

import static com.sbancuz.plannh.data.flowchart.Group.GROUP_MIN_H;
import static com.sbancuz.plannh.data.flowchart.Group.GROUP_MIN_W;

import java.util.List;

import com.cleanroommc.modularui.api.layout.IViewportStack;
import com.cleanroommc.modularui.api.widget.IDragResizeable;
import com.cleanroommc.modularui.api.widget.IGuiAction;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.api.widget.ResizeDragArea;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.sbancuz.plannh.api.PlanAPI;
import com.sbancuz.plannh.data.flowchart.Graph;
import com.sbancuz.plannh.data.flowchart.Group;
import com.sbancuz.plannh.gui.common.FlowchartWidget;
import com.sbancuz.plannh.gui.common.IFlowchartDraggable;

public class GroupAreaWidget extends ParentWidget<GroupAreaWidget> implements IFlowchartDraggable, IDragResizeable {

    private final GroupWidget<?> parent;
    private final Group data;
    private String resizeEditToken;
    private Graph resizeGraph;
    private boolean resizeFinished;

    public GroupAreaWidget(GroupWidget<?> parent) {
        this.parent = parent;
        data = parent.getData();
        name("group.area");
        listenGuiAction((IGuiAction.MouseReleased) mouseButton -> {
            if (resizeEditToken != null) resizeFinished = true;
            return false;
        });
        configureCoverChildren();
    }

    @Override
    public GroupWidget<?> getFlowchartParent() {
        return parent;
    }

    @Override
    public boolean isCurrentlyResizable() {
        return !data.isCoverChildren();
    }

    public boolean isResizing() {
        return resizeEditToken != null;
    }

    @Override
    public int getDragAreaSize() {
        return 10;
    }

    @Override
    public int getMinDragWidth() {
        return Math.max(
            GROUP_MIN_W,
            getChildren().stream()
                .filter(w -> w instanceof FlowchartWidget<?, ?>)
                .map(IWidget::getArea)
                .mapToInt(a -> a.rx + a.width)
                .max()
                .orElse(0));
    }

    @Override
    public int getMinDragHeight() {
        return Math.max(
            GROUP_MIN_H,
            getChildren().stream()
                .filter(w -> w instanceof FlowchartWidget<?, ?>)
                .map(IWidget::getArea)
                .mapToInt(a -> a.ry + a.height)
                .max()
                .orElse(0));
    }

    @Override
    public boolean keepPosOnDragResize() {
        return false;
    }

    @Override
    public Object getAdditionalHoverInfo(IViewportStack viewportStack, int mouseX, int mouseY) {
        // Keep the content origin fixed; the group's header controls its position.
        Object superInfo = super.getAdditionalHoverInfo(viewportStack, mouseX, mouseY);
        if (superInfo instanceof ResizeDragArea resizeDragArea && (resizeDragArea.top || resizeDragArea.left))
            return null;
        return superInfo;
    }

    @Override
    public void onDragResize() {
        if (resizeEditToken == null) {
            resizeGraph = parent.getCanvas()
                .getGraph();
            resizeEditToken = resizeGraph.getUndoHistory()
                .beginEdit(resizeGraph);
        }
        left(0);
        parent.scheduleResize();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!resizeFinished) return;
        resizeFinished = false;
        // Reparent after event dispatch: removing groups unregisters their GUI listeners.
        for (IWidget sibling : List.copyOf(
            parent.getParent()
                .getChildren())) {
            if (sibling instanceof FlowchartWidget<?, ?>widget && parent.canContain(widget)) widget.moveTo(this);
        }
        finishResizeEdit();
    }

    private void finishResizeEdit() {
        resizeGraph.getUndoHistory()
            .commitEdit(resizeEditToken, resizeGraph);
        resizeEditToken = null;
        resizeGraph.bumpVersion();
        resizeGraph = null;
        parent.getCanvas()
            .needsReroute();
        PlanAPI.save();
    }

    @Override
    public void dispose() {
        if (resizeEditToken != null) finishResizeEdit();
        resizeFinished = false;
        super.dispose();
    }

    @Override
    public void onResized() {
        Area area = getArea();
        if (data.getWidth() != area.width || data.getHeight() != area.height) {
            data.setWidth(area.width);
            data.setHeight(area.height);
        }
    }

    @Override
    public void postResize() {
        super.postResize();
        if (data.isCoverChildren()) return;
        final int width = Math.max(getArea().width, getMinDragWidth());
        final int height = Math.max(getArea().height, getMinDragHeight());
        if (width == getArea().width && height == getArea().height) return;
        // Fixed-size groups must still grow when an expanded child no longer fits.
        size(width, height);
        parent.scheduleResize();
    }

    public void configureCoverChildren() {
        if (data.isCoverChildren()) {
            coverChildren(GROUP_MIN_W, GROUP_MIN_H);
            scheduleResize();
        } else {
            disableCoverChildren();
            size(Math.max(data.getWidth(), getMinDragWidth()), Math.max(data.getHeight(), getMinDragHeight()));
        }
    }

    @Override
    protected void onChildRemove(IWidget child) {
        if (child instanceof FlowchartWidget<?, ?>flowchartWidget) parent.leaveGroup(flowchartWidget);
    }
}
