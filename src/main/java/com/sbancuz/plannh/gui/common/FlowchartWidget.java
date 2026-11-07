package com.sbancuz.plannh.gui.common;

import java.util.Comparator;
import java.util.SortedMap;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.widget.IDraggable;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.utils.TreeUtil;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.sbancuz.plannh.api.PlanAPI;
import com.sbancuz.plannh.data.flowchart.GraphData;
import com.sbancuz.plannh.data.flowchart.Group;
import com.sbancuz.plannh.data.flowchart.Node;
import com.sbancuz.plannh.data.flowchart.Note;
import com.sbancuz.plannh.gui.CanvasWidget;
import com.sbancuz.plannh.gui.PlannhColors;
import com.sbancuz.plannh.gui.group.GroupAreaWidget;
import com.sbancuz.plannh.gui.group.GroupWidget;
import com.sbancuz.plannh.gui.node.NodeWidget;
import com.sbancuz.plannh.gui.note.NoteWidget;

import lombok.Getter;
import lombok.Setter;

public abstract class FlowchartWidget<T extends ParentWidget<T>, D extends GraphData> extends ParentWidget<T>
    implements Interactable, IDraggable {

    @Getter
    protected final D data;
    @Getter
    protected final CanvasWidget canvas;
    private boolean moving = false;
    private int dragStartMouseX, dragStartMouseY;
    private int dragStartX, dragStartY;
    private int lastWidth = -1, lastHeight = -1;
    @Getter
    @Setter
    private SortedMap<UUID, ? super D> dataContainer;
    private String dragEditToken;
    private boolean filterClick;
    private boolean filterClickMoved;

    protected FlowchartWidget(CanvasWidget canvas, D data) {
        this.canvas = canvas;
        this.data = data;
        dataContainer = getDefaultContainer();
        pos(data.getX(), data.getY());
        canvas.getFlowchartWidgets()
            .put(data.getId(), this);
        if (isObstacle()) canvas.needsReroute();
    }

    @Override
    public void drawMovingState(ModularGuiContext context, float partialTicks) {}

    @Override
    public void drawOverlay(final ModularGuiContext context, final WidgetThemeEntry<?> widgetTheme) {
        super.drawOverlay(context, widgetTheme);
        if (data.isSummaryFiltered()) {
            new Rectangle().hollow(1)
                .color(getSummaryFilterColor())
                .draw(
                    context,
                    -1,
                    -1,
                    getArea().width + 2,
                    getArea().height + 2,
                    getActiveWidgetTheme(widgetTheme, isHovering()));
        }
    }

    public int getSummaryFilterColor() {
        final int color = data instanceof Group group ? group.getColor()
            : data instanceof Node node ? PlannhColors.titleColor(node.getMachineName())
                : PlannhColors.TEXT_WHITE.getColor();
        return Color.withAlpha(color, 0xFF);
    }

    /** Only passive surfaces may filter; never promote an embedded control to its owning node. */
    @Nullable
    public static FlowchartWidget<?, ?> summaryFilterTarget(@Nullable final IWidget hovered) {
        final FlowchartWidget<?, ?> owner;
        switch (hovered) {
            case FlowchartWidget<?, ?> widget -> owner = widget;
            case HeaderTextWidget header when !header.isFocused() -> owner = header.getFlowchartParent();
            case IFlowchartDraggable surface when !(hovered instanceof Interactable) ->
                owner = surface.getFlowchartParent();
            case null, default -> {
                return null;
            }
        }
        return (owner.data instanceof Node || owner.data instanceof Group)
            && owner.canvas.getFlowchartWidgets().get(owner.data.getId()) == owner ? owner : null;
    }

    @Override
    public boolean onDragStart(int mouseButton) {
        return onDragStartWithOffset(mouseButton, 0, 0);
    }

    public boolean onDragStartWithOffset(int mouseButton, int x, int y) {
        if (mouseButton == 0 && canvas.isMouseInsideCanvas()) {
            ModularGuiContext context = getContext();
            dragStartMouseX = context.getAbsMouseX();
            dragStartMouseY = context.getAbsMouseY();
            filterClick = Interactable.hasControlDown() && summaryFilterTarget(context.getTopHovered()) == this;
            filterClickMoved = false;
            if (filterClick) return true;
            dragEditToken = PlanAPI.undoHistory()
                .beginEdit(canvas.getGraph());
            dragStartX = data.getX();
            dragStartY = data.getY();
            return true;
        }
        return false;
    }

    @Override
    public void onDragEnd(boolean successful) {
        if (filterClick) {
            filterClick = false;
            if (!isValid()) return;
            updateFilterClickMovement();
            if (!filterClickMoved && getContext().getMouseButton() == 0 && canvas.isMouseInsideCanvas()
                && summaryFilterTarget(getContext().getTopHovered()) == this) {
                data.setSummaryFiltered(!data.isSummaryFiltered());
            }
            return;
        }
        if (successful) {
            if (canvas.getGraph()
                .isSnapToGrid()) {
                data.setX((int) (Math.round((double) data.getX() / CanvasWidget.GRID_SIZE) * CanvasWidget.GRID_SIZE));
                data.setY((int) (Math.round((double) data.getY() / CanvasWidget.GRID_SIZE) * CanvasWidget.GRID_SIZE));
                reposition();
            }
            successful = canDropHere(0, 0, null);
            if (successful) adjustGroupMembership();
        }
        if (!successful) {
            data.setX(dragStartX);
            data.setY(dragStartY);
            reposition();
        }
        PlanAPI.undoHistory()
            .commitEdit(dragEditToken, canvas.getGraph());
        if (successful) {
            canvas.getGraph()
                .bumpVersion();
            PlanAPI.save();
        }
        dragEditToken = null;
    }

    @Override
    public void onDrag(int mouseButton, long timeSinceLastClick) {
        if (filterClick) {
            if (isValid()) updateFilterClickMovement();
            return;
        }
        final int dx = getContext().getAbsMouseX() - dragStartMouseX;
        final int dy = getContext().getAbsMouseY() - dragStartMouseY;
        final float z = canvas.getGraph()
            .getZoom();
        data.setX(dragStartX + Math.round(dx / z));
        data.setY(dragStartY + Math.round(dy / z));
        reposition();
    }

    private void updateFilterClickMovement() {
        filterClickMoved |= Math.abs(getContext().getAbsMouseX() - dragStartMouseX) > 3
            || Math.abs(getContext().getAbsMouseY() - dragStartMouseY) > 3;
    }

    @Override
    public @Nullable Area getMovingArea() {
        return null;
    }

    @Override
    public boolean isMoving() {
        return moving;
    }

    @Override
    public void setMoving(boolean moving) {
        this.moving = moving;
    }

    @Override
    public boolean canDropHere(int x, int y, @Nullable IWidget widget) {
        if (filterClick) return isValid() && canvas.isMouseInsideCanvas();
        // Only our descendants and groups fully containing us may overlap the drop.
        Area dropArea = getPositionedArea();
        return canvas.isMouseInsideCanvas() && canvas.getFlowchartWidgets()
            .values()
            .stream()
            .filter(
                f -> f.getArea()
                    .intersects(dropArea))
            .allMatch(
                f -> TreeUtil.findParent(f, w -> w == this) != null
                    || f instanceof GroupWidget<?>groupWidget && groupWidget.canContain(this));
    }

    public void removeFromGraph() {
        canvas.getFlowchartWidgets()
            .remove(data.getId());
        dataContainer.remove(data.getId());
        canvas.getGraph()
            .bumpVersion();
    }

    protected abstract SortedMap<UUID, ? super D> getDefaultContainer();

    public void reposition() {
        pos(data.getX(), data.getY());
        canvas.needsReroute();
    }

    @Override
    public void postResize() {
        super.postResize();
        final Area area = getArea();
        final int w = area.w();
        final int h = area.h();
        // I really don't know, this gets called when changing position for some reason so guard that :(
        if (w != lastWidth || h != lastHeight) {
            lastWidth = w;
            lastHeight = h;
            canvas.resolveOverlapsFrom(this);
        }
    }

    private void adjustGroupMembership() {
        ParentWidget<?> newParent = canvas.getFlowchartWidgets()
            .values()
            .stream()
            .filter(w -> w instanceof GroupWidget<?>group && group.canContain(this))
            .map(w -> (GroupWidget<?>) w)
            .min(Comparator.comparingLong(w -> (long) w.getArea().width * w.getArea().height))
            .<ParentWidget<?>>map(GroupWidget::getAreaWidget)
            .orElse(canvas);
        moveTo(newParent);
    }

    public Area getPositionedArea() {
        return new Area(
            getParent().getArea().x + data.getX(),
            getParent().getArea().y + data.getY(),
            getArea().width,
            getArea().height);
    }

    public void moveTo(ParentWidget<?> newParent) {
        ParentWidget<?> oldParent = (ParentWidget<?>) getParent();
        if (oldParent == newParent) return;
        if (newParent instanceof GroupAreaWidget groupArea && !groupArea.getFlowchartParent()
            .canContain(this)) return;

        Area area = getPositionedArea();
        int x = area.x - newParent.getArea().x;
        int y = area.y - newParent.getArea().y;
        dataContainer.remove(data.getId());
        oldParent.remove(this);
        data.setX(x);
        data.setY(y);
        if (newParent instanceof GroupAreaWidget groupArea) {
            groupArea.getFlowchartParent()
                .joinGroup(this);
        } else {
            dataContainer = getDefaultContainer();
            newParent.child(
                canvas.getArrowWidgets()
                    .size(),
                this);
        }
        dataContainer.put(data.getId(), data);
        reposition();
        oldParent.scheduleResize();
        newParent.scheduleResize();
    }

    public static FlowchartWidget<?, ?> getFlowchartWidgetFromData(CanvasWidget canvas, GraphData data) {
        return switch (data) {
            case Note note -> new NoteWidget(canvas, note);
            case Group group -> GroupWidget.of(canvas, group);
            case Node node -> new NodeWidget(canvas, node);
            default -> throw new IllegalArgumentException("Unsupported data type: " + data.getClass());
        };
    }

    public abstract boolean isObstacle();
}
