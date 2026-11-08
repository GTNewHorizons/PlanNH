package com.sbancuz.plannh.gui;

import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.value.EnumValue;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.CycleButtonWidget;
import com.cleanroommc.modularui.widgets.Dialog;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.sbancuz.plannh.api.PlanAPI;
import com.sbancuz.plannh.data.flowchart.Graph;
import com.sbancuz.plannh.data.flowchart.Plan;
import com.sbancuz.plannh.data.flowchart.balancer.BalanceMode;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;
import com.sbancuz.plannh.gui.tooltips.TooltipTheme;
import com.sbancuz.plannh.nei.config.KeybindsConfig;

/** The graph controls that sit above a {@link CanvasWidget}. */
public final class FlowchartToolbar extends Flow {

    private static final int BUTTON_SIZE = FlowchartBounds.BUTTON_SIZE;

    public FlowchartToolbar(final CanvasWidget canvas) {
        super(GuiAxis.X);
        name("screen.toolbar").fullWidth()
            .coverChildrenHeight()
            .wrap()
            .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
            .childPadding(6)
            .crossAxisChildPadding(2)
            .crossAxisAlignment(Alignment.CrossAxis.START)
            .child(
                Flow.row()
                    .name("screen.toolbar.edit")
                    .coverChildren()
                    .childPadding(6)
                    .child(historyGroup(canvas))
                    .child(layoutGroup(canvas))
                    .child(addGroup(canvas))
                    .child(shareGroup(canvas)))
            .child(graphGroup(canvas));
    }

    private static Flow graphGroup(final CanvasWidget canvas) {
        final TextFieldWidget name = new TextFieldWidget().name("graph.name")
            .width(90)
            .height(BUTTON_SIZE)
            .padding(4, 2)
            .setTextColor(PlannhColors.TEXT_WHITE.getColor())
            .value(
                new StringValue.Dynamic(
                    () -> Plan.getActiveGraph()
                        .getName(),
                    value -> Plan.getActiveGraph()
                        .setName(value)));

        final IDrawable editingBackground = IDrawable.of(
            new Rectangle().color(0xFF000000),
            new Rectangle().hollow(1)
                .color(0xFFA0A0A0));
        name.background(new DynamicDrawable(() -> name.isFocused() ? editingBackground : IDrawable.EMPTY))
            .hoverBackground(editingBackground);

        return Flow.row()
            .name("screen.toolbar.graphs")
            .coverChildren()
            .childPadding(2)
            .child(name)
            .child(graphStep(canvas, -1))
            .child(
                new TextWidget<>(
                    IKey.dynamic(
                        () -> (Plan.getInstance()
                            .getActiveIndex() + 1) + "/"
                            + Plan.getInstance()
                                .getGraphs()
                                .size())).name("graph.index")
                                    .color(PlannhColors.TEXT_WHITE.getColor()))
            .child(graphStep(canvas, 1));
    }

    private static ButtonWidget<?> graphStep(final CanvasWidget canvas, final int direction) {
        final IPanelHandler deleteConfirmation = direction < 0
            ? IPanelHandler.simple(canvas.getPanel(), (_, _) -> deleteGraphDialog(canvas), true)
            : null;

        return leftButton(direction < 0 ? "graph.prev" : "graph.next", IKey.EMPTY, () -> {
            canvas.getContext()
                .removeFocus();
            if (!GuiHelper.shiftHeld()) cycleGraphs(canvas, direction);
            else if (direction < 0) {
                if (Plan.getInstance()
                    .getGraphs()
                    .size() <= 1 || deleteConfirmation.isPanelOpen()) return;
                deleteConfirmation.deleteCachedPanel();
                deleteConfirmation.openPanel();
            } else addGraph(canvas);
        }).overlay(IKey.dynamicKey(() -> {
            if (!GuiHelper.shiftHeld()) {
                return IKey.str(direction < 0 ? "<" : ">")
                    .color(PlannhColors.TEXT_WHITE.getColor());
            }
            return IKey.str(direction < 0 ? "-" : "+")
                .color(direction < 0 ? PlannhColors.ACCENT_RED.getColor() : PlannhColors.ACCENT_GREEN.getColor());
        }))
            .tooltipBuilder(tooltip -> {
                final boolean shift = GuiHelper.shiftHeld();
                final String action = shift ? (direction < 0 ? "delete" : "add")
                    : (direction < 0 ? "previous" : "next");
                tooltip.addLine(IKey.lang("plannh.gui.toolbar.graph." + action + ".title"));
                tooltip.addLine(
                    IKey.lang(
                        direction < 0 ? "plannh.gui.toolbar.graph.delete.shift" : "plannh.gui.toolbar.graph.add.shift")
                        .style(TooltipTheme.Role.TUNABLE.code()));
                if (shift && direction < 0) {
                    tooltip.addLine(
                        IKey.lang(
                            Plan.getInstance()
                                .getGraphs()
                                .size() <= 1 ? "plannh.gui.toolbar.graph.delete.last"
                                    : "plannh.gui.toolbar.graph.delete.warning")
                            .style(EnumChatFormatting.RED));
                }
            })
            .tooltipAutoUpdate(true);
    }

    private static Flow historyGroup(final CanvasWidget canvas) {
        return Flow.row()
            .name("screen.toolbar.history")
            .coverChildren()
            .childPadding(2)
            .child(leftButton("edit.undo", GuiIcon.UNDO, canvas::undoGraph).tooltipBuilder(tooltip -> {
                tooltip.addLine(IKey.lang("plannh.gui.toolbar.undo.title"));
                final String binding = KeybindsConfig.undoName();
                if (binding != null) tooltip.addLine(
                    IKey.lang("plannh.gui.toolbar.shortcut", binding)
                        .style(TooltipTheme.Role.TUNABLE.code()));
            }))
            .child(leftButton("edit.redo", GuiIcon.REDO, canvas::redoGraph).tooltipBuilder(tooltip -> {
                tooltip.addLine(IKey.lang("plannh.gui.toolbar.redo.title"));
                final String binding = KeybindsConfig.redoName();
                if (binding != null) tooltip.addLine(
                    IKey.lang("plannh.gui.toolbar.shortcut", binding)
                        .style(TooltipTheme.Role.TUNABLE.code()));
                final String alternate = KeybindsConfig.redoAltName();
                if (alternate != null) tooltip.addLine(
                    IKey.lang("plannh.gui.toolbar.alternate", alternate)
                        .style(TooltipTheme.Role.TUNABLE.code()));
            }));
    }

    private static Flow layoutGroup(final CanvasWidget canvas) {
        final ToggleButton snap = new ToggleButton();
        snap.name("edit.snapToGrid")
            .size(BUTTON_SIZE, BUTTON_SIZE)
            .background(false, GuiTextures.MC_BUTTON)
            .background(true, GuiTextures.MC_BUTTON_PRESSED)
            .hoverBackground(false, GuiTextures.MC_BUTTON_HOVERED)
            .hoverBackground(true, GuiTextures.MC_BUTTON_HOVERED_PRESSED)
            .value(
                new BoolValue.Dynamic(
                    () -> canvas.getGraph()
                        .isSnapToGrid(),
                    value -> canvas.getGraph()
                        .setSnapToGrid(value)))
            .overlay(
                false,
                GuiIcon.GRID.asIcon()
                    .size(12))
            .overlay(
                true,
                GuiIcon.GRID.asIcon()
                    .size(12))
            .addTooltipLine(IKey.lang("plannh.gui.toolbar.snap.title"));

        final CycleButtonWidget balance = new CycleButtonWidget().name("edit.balanceMode")
            .size(78, BUTTON_SIZE)
            .background(GuiTextures.MC_BUTTON)
            .hoverBackground(GuiTextures.MC_BUTTON_HOVERED)
            .value(
                new EnumValue.Dynamic<>(
                    BalanceMode.class,
                    () -> canvas.getGraph()
                        .getBalanceMode(),
                    value -> canvas.getGraph()
                        .setBalanceMode(value)))
            .stateOverlay(
                BalanceMode.NONE,
                IKey.lang("plannh.gui.toolbar.balance.label", IKey.lang("plannh.gui.toolbar.balance.off"))
                    .color(PlannhColors.TEXT_WHITE.getColor()))
            .stateOverlay(
                BalanceMode.INPUT,
                IKey.lang("plannh.gui.toolbar.balance.label", IKey.lang("plannh.gui.toolbar.balance.input"))
                    .color(PlannhColors.TEXT_WHITE.getColor()))
            .stateOverlay(
                BalanceMode.OUTPUT,
                IKey.lang("plannh.gui.toolbar.balance.label", IKey.lang("plannh.gui.toolbar.balance.output"))
                    .color(PlannhColors.TEXT_WHITE.getColor()))
            .stateOverlay(
                BalanceMode.AUTO,
                IKey.lang("plannh.gui.toolbar.balance.label", IKey.lang("plannh.gui.toolbar.balance.auto"))
                    .color(PlannhColors.TEXT_WHITE.getColor()))
            .tooltipBuilder(tooltip -> {
                final BalanceMode active = canvas.getGraph()
                    .getBalanceMode();
                final TooltipBuilder out = TooltipBuilder.create(tooltip)
                    .row(
                        IKey.lang("plannh.gui.toolbar.balance.title")
                            .style(TooltipTheme.Role.LABEL.code()));
                for (final BalanceMode mode : BalanceMode.values()) {
                    out.marker(active == mode ? ">" : " ", mode.displayName(), active == mode);
                }

                final String explanationKey;
                final String pinHintKey;
                switch (active) {
                    case NONE -> {
                        explanationKey = "plannh.gui.toolbar.balance.mode.none";
                        pinHintKey = "plannh.gui.toolbar.balance.pins.none";
                    }
                    case INPUT -> {
                        explanationKey = "plannh.gui.toolbar.balance.mode.input";
                        pinHintKey = "plannh.gui.toolbar.balance.pins.fixed";
                    }
                    case OUTPUT -> {
                        explanationKey = "plannh.gui.toolbar.balance.mode.output";
                        pinHintKey = "plannh.gui.toolbar.balance.pins.fixed";
                    }
                    case AUTO -> {
                        explanationKey = "plannh.gui.toolbar.balance.mode.auto";
                        pinHintKey = "plannh.gui.toolbar.balance.pins.auto";
                    }
                    default -> throw new IllegalStateException("Unknown balance mode: " + active);
                }
                out.separator()
                    .langRow(explanationKey)
                    .langRow(pinHintKey)
                    .separator()
                    .row(
                        IKey.lang("plannh.gui.toolbar.balance.cycle")
                            .style(TooltipTheme.Role.TUNABLE.code()))
                    .flush();
            })
            .tooltipAutoUpdate(true);

        return Flow.row()
            .name("screen.toolbar.layout")
            .coverChildren()
            .childPadding(2)
            .child(
                leftButton("edit.autoLayout", GuiIcon.AUTO_LAYOUT, canvas::autoLayout)
                    .addTooltipLine(IKey.lang("plannh.gui.toolbar.layout.title")))
            .child(snap)
            .child(balance);
    }

    private static Flow addGroup(final CanvasWidget canvas) {
        return Flow.row()
            .name("screen.toolbar.add")
            .coverChildren()
            .childPadding(2)
            .child(
                leftButton(
                    "add.group",
                    GuiIcon.GROUP,
                    () -> canvas.addGroup(canvas.getCanvasScreenCenterX(), canvas.getCanvasScreenCenterY()))
                        .addTooltipLine(IKey.lang("plannh.gui.toolbar.add.group.title")))
            .child(
                leftButton(
                    "add.note",
                    GuiIcon.NOTE,
                    () -> canvas.addNote(canvas.getCanvasScreenCenterX(), canvas.getCanvasScreenCenterY()))
                        .addTooltipLine(IKey.lang("plannh.gui.toolbar.add.note.title")));
    }

    private static Flow shareGroup(final CanvasWidget canvas) {
        return Flow.row()
            .name("screen.toolbar.share")
            .coverChildren()
            .childPadding(2)
            .child(
                leftButton("share.link", GuiIcon.SHARE, () -> PlanAPI.shareGraph(canvas.getGraph()))
                    .addTooltipLine(IKey.lang("plannh.gui.toolbar.share.title")))
            .child(leftButton("share.copy", GuiIcon.COPY, () -> {
                if (PlanAPI.copyToClipboard(canvas.getGraph())) {
                    PlanAPI.showMessage("plannh.share.copy_to_clipboard");
                }
            }).addTooltipLine(IKey.lang("plannh.gui.toolbar.copy.title")))
            .child(leftButton("share.import", GuiIcon.IMPORT, () -> {
                final Graph graph = PlanAPI.importFromClipboard();
                if (graph == null) return;
                PlanAPI.importGraph(graph);
                canvas.setGraph(graph);
                PlanAPI.showMessage("plannh.share.copy_from_clipboard");
            }).addTooltipLine(IKey.lang("plannh.gui.toolbar.import.title")));
    }

    private static ButtonWidget<?> leftButton(final String name, final IDrawable icon, final Runnable action) {
        return new ButtonWidget<>().name(name)
            .size(BUTTON_SIZE, BUTTON_SIZE)
            .background(GuiTextures.MC_BUTTON)
            .hoverBackground(GuiTextures.MC_BUTTON_HOVERED)
            .overlay(
                icon.asIcon()
                    .size(12))
            .onMousePressed(mouseButton -> {
                if (mouseButton != 0) return false;
                action.run();
                return true;
            });
    }

    private static void refreshGraph(final CanvasWidget canvas) {
        canvas.setGraph(Plan.getActiveGraph());
    }

    private static void cycleGraphs(final CanvasWidget canvas, final int direction) {
        final Plan plan = Plan.getInstance();
        final int size = plan.getGraphs()
            .size();
        if (size <= 1) return;
        plan.setActiveIndex((plan.getActiveIndex() + direction + size) % size);
        refreshGraph(canvas);
    }

    private static void addGraph(final CanvasWidget canvas) {
        final Plan plan = Plan.getInstance();
        final int size = plan.getGraphs()
            .size();
        plan.getGraphs()
            .add(new Graph("Slot " + (size + 1)));
        plan.setActiveIndex(size);
        refreshGraph(canvas);
    }

    private static Dialog<Boolean> deleteGraphDialog(final CanvasWidget canvas) {
        final Plan plan = Plan.getInstance();
        final Graph graph = Plan.getActiveGraph();
        final Dialog<Boolean> dialog = new Dialog<>("graph.delete.confirmation", confirmed -> {
            if (!confirmed || Plan.getInstance() != plan || Plan.getActiveGraph() != graph) return;
            final int size = plan.getGraphs()
                .size();
            if (size <= 1) return;
            final int active = plan.getActiveIndex();
            plan.getGraphs()
                .remove(active);
            if (active >= size - 1) plan.setActiveIndex(size - 2);
            refreshGraph(canvas);
        });
        dialog.width(260)
            .coverChildrenHeight()
            .padding(10)
            .background(GuiTextures.MC_BACKGROUND)
            .child(
                Flow.column()
                    .mainAxisAlignment(Alignment.MainAxis.CENTER)
                    .fullWidth()
                    .coverChildrenHeight()
                    .childPadding(8)
                    .child(
                        new TextWidget<>(IKey.lang("plannh.gui.toolbar.graph.delete.confirm", graph.getName()))
                            .fullWidth()
                            .textAlign(Alignment.Center)
                            .color(PlannhColors.TEXT_WHITE.getColor()))
                    .child(
                        new TextWidget<>(IKey.lang("plannh.gui.toolbar.graph.delete.irreversible")).fullWidth()
                            .textAlign(Alignment.Center)
                            .color(PlannhColors.ACCENT_RED.getColor()))
                    .child(
                        Flow.row()
                            .fullWidth()
                            .coverChildrenHeight()
                            .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
                            .child(
                                leftButton("graph.delete.cancel", IKey.EMPTY, () -> dialog.closeWith(false))
                                    .size(110, 20)
                                    .overlay(IKey.lang("gui.cancel")))
                            .child(
                                leftButton("graph.delete.confirm", IKey.EMPTY, () -> dialog.closeWith(true))
                                    .size(110, 20)
                                    .overlay(
                                        IKey.lang("plannh.gui.toolbar.graph.delete.button")
                                            .color(PlannhColors.ACCENT_RED.getColor())))));
        return dialog;
    }
}
