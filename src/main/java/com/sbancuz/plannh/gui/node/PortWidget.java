package com.sbancuz.plannh.gui.node;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IDraggable;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.sbancuz.plannh.PlanNH;
import com.sbancuz.plannh.api.PlanAPI;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.effect.EffectResult;
import com.sbancuz.plannh.data.flowchart.Edge;
import com.sbancuz.plannh.data.flowchart.Node;
import com.sbancuz.plannh.data.flowchart.Plan;
import com.sbancuz.plannh.data.flowchart.Port;
import com.sbancuz.plannh.data.flowchart.balancer.Balancer;
import com.sbancuz.plannh.data.flowchart.resource.LongFluidStack;
import com.sbancuz.plannh.data.flowchart.resource.LongItemStack;
import com.sbancuz.plannh.gui.CanvasWidget;
import com.sbancuz.plannh.gui.GuiHelper;
import com.sbancuz.plannh.gui.GuiHelper.RateUnit;
import com.sbancuz.plannh.gui.PlannhColors;
import com.sbancuz.plannh.gui.common.ItemSelectionGrid;
import com.sbancuz.plannh.gui.common.SelectionPicker;
import com.sbancuz.plannh.gui.edge.ArrowWidget;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;
import com.sbancuz.plannh.gui.tooltips.TooltipTheme;
import com.sbancuz.plannh.mixins.PositionedStackAccessor;

import codechicken.nei.KeyManager;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiUsageRecipe;
import codechicken.nei.recipe.StackInfo;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import lombok.Getter;

public class PortWidget extends Widget<PortWidget> implements Interactable, IDraggable {

    private static final int RECIPE_KEYCODE = KeyManager.getKeyCode("recipe.recipe");
    private static final int USAGE_KEYCODE = KeyManager.getKeyCode("recipe.usage");

    private static final String LANG = "plannh.gui.node.port.";

    // needed for proper positioning of ports
    static final int PORT_OFFSET_X = 1;
    static final int PORT_OFFSET_Y = -1;
    static final int PORT_SIZE = 18;

    private final CanvasWidget canvas;

    private static boolean isCreatingEdge = false;
    public static ArrowWidget arrowWidgetInCreation;

    // saved, so we can toggle permutation
    private final PositionedStack stack;
    @Getter
    private final PortType portType;
    @Getter
    private final Port<?> port;
    @Getter
    private final IntIntPair index;
    @Getter
    private final Node node;
    /**
     * Which side of the machine this port is on. A {@link PortType} cannot answer it: a catalyst is
     * the same type either way, and the balance keeps inputs and outputs in separate maps.
     */
    @Getter
    private final boolean input;

    private final NodeWidget parent;
    private final Map<IntIntPair, PortWidget> siblingPortWidgets;
    private SelectionPicker picker;
    private boolean inactiveDemand;

    public PortWidget(NodeWidget parent, IntIntPair index, boolean isInput, PortType portType, int yShift,
        Map<IntIntPair, PortWidget> siblingPortWidgets) {
        this.parent = parent;
        this.index = index;
        this.portType = portType;
        this.input = isInput;
        this.siblingPortWidgets = siblingPortWidgets;

        // One name per port per node, so the debug overlay can say which stack of which port is
        // under the mouse instead of just "port".
        name((isInput ? "port.in." : "port.out.") + index.leftInt() + "." + index.secondInt());

        canvas = parent.getCanvas();
        node = parent.getData();
        port = (isInput ? node.getInputs() : node.getOutputs()).get(index.firstInt());
        stack = port.getAllStacks()
            .get(index.secondInt());

        final int color = portType == PortType.CATALYST ? portType.borderColor : port.getPinColor(input);
        refreshInactiveState();

        background(
            new DynamicDrawable(
                () -> new Rectangle().color(
                    inactiveDemand ? PlannhColors.PORT_INACTIVE_OVERLAY.getColor()
                        : PlannhColors.TRANSPARENT.getColor())),
            new DynamicDrawable(
                () -> new Rectangle().color(inactiveDemand ? PlannhColors.PORT_INACTIVE_BORDER.getColor() : color)
                    .hollow()));
        hoverOverlay(new Rectangle().color(Color.argb(255, 255, 255, 128)));
        pos(stack.relx + PORT_OFFSET_X, stack.rely + PORT_OFFSET_Y + yShift);
        size(PORT_SIZE);

        tooltipBuilder(this::writeTooltip);
        tooltipAutoUpdate(true);

        if (configurable()) {
            overlay(
                GuiTextures.GEAR.asIcon()
                    .size(9)
                    .alignment(Alignment.TopRight));
        }

        onUpdateListener(w -> { refreshInactiveState(); }, true);
    }

    private void selectConfiguration(@Nullable final ItemStack itemStack) {
        parent.commitEdit(() -> {
            final List<ArrowWidget> arrows = parent.getArrowWidgets();
            final List<ArrowWidget> removed = arrows.stream()
                .filter(
                    arrow -> arrow.getEdge()
                        .getTargetInputIndex()
                        .leftInt() == index.leftInt())
                .toList();
            removed.forEach(ArrowWidget::removeFromGraph);
            arrows.removeAll(removed);
            if (itemStack == null) enablePermutations();
            else setPermutationToStack(itemStack);
        });
        if (picker != null) picker.close();
    }

    @Override
    public @NotNull Result onKeyPressed(char typedChar, int keyCode) {
        if (keyCode == RECIPE_KEYCODE) {
            GuiCraftingRecipe.openRecipeGui("item", stack.item);
            if (portType.isSupportsEdge() && !portType.isEdgeSource()) canvas.setNeiTransferSource(this);
            return Result.ACCEPT;
        }
        if (keyCode == USAGE_KEYCODE) {
            GuiUsageRecipe.openRecipeGui("item", stack.item);
            if (portType.isSupportsEdge() && portType.isEdgeSource()) canvas.setNeiTransferSource(this);
            return Result.ACCEPT;
        }
        return Interactable.super.onKeyPressed(typedChar, keyCode);
    }

    @Override
    public @NotNull Result onMousePressed(int mouseButton) {
        if (mouseButton == 1 && configurable()) {
            if (picker == null) picker = new SelectionPicker(
                this,
                () -> new ItemSelectionGrid(List.of(stack.items), this::selectConfiguration));
            picker.toggle();
            return Result.SUCCESS;
        }
        return Result.ACCEPT;
    }

    @SuppressWarnings("unchecked")
    private void setPermutationToStack(ItemStack itemStack) {
        if (port.getType() == RecipePropertyAPI.ITEM) {
            ((Port<LongItemStack>) port).setValue(new LongItemStack(itemStack.copy(), port.getAmount()));
        }
        if (port.getType() == RecipePropertyAPI.FLUID) {
            FluidStack fluid = StackInfo.getFluid(itemStack);
            if (fluid == null) return;
            ((Port<LongFluidStack>) port).setValue(new LongFluidStack(fluid.copy(), port.getAmount()));
        }

        port.getAllStacks()
            .forEach(ps -> {
                PositionedStackAccessor psa = (PositionedStackAccessor) ps;
                psa.setPermutated(true);
                ps.setPermutationToRender(itemStack);
                psa.setPermutated(false);
            });

        node.getInputConfigurations()
            .put(index.firstInt(), itemStack);
    }

    private void enablePermutations() {
        port.getAllStacks()
            .forEach(ps -> {
                PositionedStackAccessor psa = (PositionedStackAccessor) ps;
                psa.setPermutated(true);
            });

        node.getInputConfigurations()
            .remove(index.firstInt());
    }

    public boolean canConnect(PortWidget other) {
        return node.isAvailable() && other.node.isAvailable()
            && portType.canConnect(other.portType)
            && port.canConnect(other.port);
    }

    private boolean configurable() {
        return stack.items.length > 1 && !portType.edgeSource;
    }

    private boolean notConfigured() {
        return ((PositionedStackAccessor) stack).getPermutated();
    }

    private void writeTooltip(final RichTooltip tooltip) {
        refreshInactiveState();
        final EffectResult tooltipEffect = currentEffect();
        if (stack.item != null && stack.item.getItem() != null) tooltip.addFromItem(stack.item);
        else tooltip.addLine(IKey.str(port.getDisplayName()));

        final TooltipBuilder out = TooltipBuilder.create(tooltip);
        if (configurable()) out.langRow(LANG + "configure_hint");
        if (inactiveDemand) out.langRow(LANG + "inactive");

        final Balancer.NodeBalance balance = parent.balance();
        writeThroughput(out, balance, tooltipEffect);
        out.flush();
        port.getType()
            .addTooltip(tooltip, port.getValue());
    }

    private void writeThroughput(final TooltipBuilder out, final @Nullable Balancer.NodeBalance balance,
        final @Nullable EffectResult throughputEffect) {
        final double perCycle = perCycle(balance);
        if (perCycle <= 0) return;

        writeThroughput(out, node, port, input, index.leftInt(), perCycle, balance.durationPerOp(), throughputEffect);
    }

    public static void writeThroughput(final TooltipBuilder out, final Node node, final Port<?> port,
        final boolean input, final int portIndex, final double perCycle, final int duration) {
        writeThroughput(out, node, port, input, portIndex, perCycle, duration, node.computeEffect());
    }

    private static void writeThroughput(final TooltipBuilder out, final Node node, final Port<?> port,
        final boolean input, final int portIndex, final double perCycle, final int duration,
        final @Nullable EffectResult effect) {
        if (effect == null) return;
        final double bonus = input ? effect.inputMultiplier(portIndex) : effect.outputMultiplier(portIndex);

        // A rule between this and what came before it - unless it is the first thing in the tooltip,
        // where a rule would be a heading over nothing.
        if (!out.isEmpty()) out.separator();
        out.entry(
            IKey.lang(LANG + "rate"),
            TooltipTheme.Role.RATE,
            IKey.str(rateText(port, perCycle, duration, rateUnit())));
        out.entry(
            IKey.lang(input ? LANG + "consumption" : LANG + "productivity"),
            TooltipTheme.Role.TUNABLE,
            TooltipBuilder.multiple(bonus));

        if (GuiHelper.shiftHeld()) {
            out.group(LANG + "per_unit", rows -> {
                for (final RateUnit unit : RateUnit.VALUES) {
                    rows.entry(IKey.EMPTY, TooltipTheme.Role.RATE, IKey.str(rateText(port, perCycle, duration, unit)));
                }
            });
        } else out.langRow(LANG + "shift_hint");
    }

    private double perCycle(final @Nullable Balancer.NodeBalance balance) {
        if (balance == null || balance.durationPerOp() <= 0) return 0;

        final Double perCycle = (input ? balance.effectiveInputs() : balance.effectiveOutputs()).get(index.leftInt());
        return perCycle == null ? 0 : perCycle;
    }

    /** This port's rate in the given unit, as the corner, the row and the tooltip all state it. */
    static String rateText(final Port<?> port, final double perCycle, final int duration, final RateUnit unit) {
        return TooltipBuilder.rate(
            port.getType()
                .formatAmount(GuiHelper.rate(perCycle, duration, unit)),
            unit);
    }

    private static RateUnit rateUnit() {
        return Plan.getInstance()
            .getSummary()
            .getRateUnit();
    }

    private boolean configuredInactiveDemand() {
        if (!input || portType != PortType.INPUT) return false;
        final EffectResult current = currentEffect();
        return current != null && isInactiveDemand(portType, current.inputMultiplier(index.leftInt()));
    }

    static boolean isInactiveDemand(final PortType portType, final double multiplier) {
        return portType == PortType.INPUT && multiplier == 0d;
    }

    private @Nullable EffectResult currentEffect() {
        return parent.effectSnapshot();
    }

    private void refreshInactiveState() {
        final boolean next = configuredInactiveDemand();
        if (next == inactiveDemand) return;
        inactiveDemand = next;
        markTooltipDirty();
    }

    static int portRight(final int stackX) {
        return stackX + PORT_OFFSET_X + PORT_SIZE;
    }

    static int portBottom(final int stackY, final int yShift) {
        return stackY + PORT_OFFSET_Y + yShift + PORT_SIZE;
    }

    @Override
    public boolean onDragStart(int button) {
        if (button == 0 && portType.supportsEdge && (picker == null || !picker.isOpen())) {
            arrowWidgetInCreation = new ArrowWidget(canvas);
            canvas.child(arrowWidgetInCreation);

            return true;
        }
        return false;
    }

    @Override
    public void onDragEnd(boolean successful) {
        canvas.remove(arrowWidgetInCreation);
        arrowWidgetInCreation = null;

        if (successful) {
            PortWidget source;
            PortWidget target;
            if (portType.edgeSource) {
                source = this;
                target = (PortWidget) getContext().getTopHovered();
            } else {
                source = (PortWidget) getContext().getTopHovered();
                target = this;
            }

            if (source == null || target == null) {
                PlanNH.LOG.warn("Edge creation unsuccessful: ports were null");
                return;
            }

            PlanAPI.recordEdit(canvas.getGraph(), () -> addArrow(canvas, source, target));
        }
    }

    public static void addArrow(CanvasWidget canvas, PortWidget source, PortWidget target) {
        if (!source.canConnect(target)) return;
        if (canvas.getNodeWidgets()
            .get(source.node.getId()) != source.parent
            || canvas.getNodeWidgets()
                .get(target.node.getId()) != target.parent) {
            PlanAPI.showMessage("plannh.error.connection");
            return;
        }

        final SortedMap<UUID, Edge> edges = canvas.getGraph()
            .getEdges();
        final List<Edge> replaced = edges.values()
            .stream()
            .filter(
                edge -> edge.getSourceOutputIndex()
                    .leftInt() == source.index.leftInt() && edge.getSourceNodeId()
                        .equals(source.node.getId())
                    && edge.getTargetInputIndex()
                        .leftInt() == target.index.leftInt()
                    && edge.getTargetNodeId()
                        .equals(target.node.getId()))
            .toList();
        final Edge edge = new Edge(source, target);
        final ArrowWidget arrow = new ArrowWidget(canvas, edge);
        if (target.notConfigured() && target.configurable()) target.setPermutationToStack(source.stack.item);

        for (final Edge old : replaced) {
            final ArrowWidget oldArrow = canvas.getArrowWidgets()
                .get(old.getId());
            if (oldArrow == null) {
                edges.remove(old.getId());
                continue;
            }
            source.parent.getArrowWidgets()
                .remove(oldArrow);
            target.parent.getArrowWidgets()
                .remove(oldArrow);
            oldArrow.removeFromGraph();
        }
        canvas.getGraph()
            .addEdge(edge);
        canvas.child(arrow);
    }

    @Override
    public void onDrag(int mouseButton, long timeSinceLastClick) {
        Area startArea = getArea();

        int startX = canvas.getCanvasPosX(startArea.x + startArea.width / 2);
        int startY = canvas.getCanvasPosY(startArea.y + startArea.height / 2);
        int endX = canvas.getCanvasMouseX();
        int endY = canvas.getCanvasMouseY();

        List<int[]> coords = List
            .of(new int[] { startX, startY }, new int[] { endX, startY }, new int[] { endX, endY });

        arrowWidgetInCreation.setCoords(portType.edgeSource ? coords : coords.reversed());
    }

    @Override
    public boolean canDropHere(int x, int y, @Nullable IWidget widget) {
        return widget instanceof PortWidget other && canConnect(other);
    }

    @Override
    public boolean isMoving() {
        return isCreatingEdge;
    }

    @Override
    public void setMoving(boolean moving) {
        isCreatingEdge = moving;
    }

    @Override
    public void drawMovingState(ModularGuiContext context, float partialTicks) {}

    @Override
    public @Nullable Area getMovingArea() {
        return null;
    }

    @Override
    public void drawForeground(ModularGuiContext context) {
        RichTooltip tooltip = getTooltip();
        if (tooltip != null && isHoveringFor(tooltip.getShowUpTimer()) && (!context.hasDraggable() || isCreatingEdge)) {
            tooltip.draw(context);
        }
    }

    @Getter
    public enum PortType {

        INPUT(0, true, false),
        OUTPUT(0, true, true),
        CATALYST(Color.BLACK.main, false);

        private final int borderColor;
        private final boolean supportsEdge;
        private final boolean edgeSource;

        PortType(int borderColor, boolean supportsEdge, boolean edgeSource) {
            this.borderColor = borderColor;
            this.supportsEdge = supportsEdge;
            this.edgeSource = edgeSource;
        }

        PortType(int borderColor, boolean supportsEdge) {
            this(borderColor, supportsEdge, false);
        }

        private boolean canConnect(PortType other) {
            return switch (this) {
                case CATALYST -> false;
                case INPUT -> other == OUTPUT;
                case OUTPUT -> other == INPUT;
            };
        }
    }

    @Override
    public void onMouseStartHover() {
        // this will call super in the for each
        siblingPortWidgets.forEach(
            (index, portWidget) -> { if (this.index.leftInt() == index.leftInt()) portWidget.onSiblingStartHover(); });
    }

    private void onSiblingStartHover() {
        super.onMouseStartHover();
    }

    @Override
    public void onMouseEndHover() {
        // this will call super in the for each
        siblingPortWidgets.forEach(
            (index, portWidget) -> { if (this.index.leftInt() == index.leftInt()) portWidget.onSiblingEndHover(); });
    }

    private void onSiblingEndHover() {
        super.onMouseEndHover();
    }
}
