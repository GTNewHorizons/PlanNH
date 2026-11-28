package com.sbancuz.plannh.gui;

import java.util.Locale;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.IPanelHandler;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
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
import com.sbancuz.plannh.data.flowchart.Summary;
import com.sbancuz.plannh.data.flowchart.balancer.BalanceMode;
import com.sbancuz.plannh.gui.theme.PlannhThemes;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;
import com.sbancuz.plannh.gui.tooltips.TooltipTheme;
import com.sbancuz.plannh.nei.config.KeybindsConfig.Shortcut;

/** The graph controls that sit above a {@link CanvasWidget}. */
public final class FlowchartToolbar extends Flow {

    private static final LangScope TOOLBAR = LangScope.ROOT.child("gui.toolbar");
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
            .widgetTheme(PlannhThemes.TOOLBAR_FIELD)
            .value(
                new StringValue.Dynamic(
                    () -> Plan.getActiveGraph()
                        .getName(),
                    value -> Plan.getActiveGraph()
                        .setName(value)));

        // MUI2 themes have no focused state: show the field's hover box while it is being edited.
        name.background(
            new DynamicDrawable(
                () -> name.isFocused() ? name.getWidgetTheme(
                    name.getPanel()
                        .getTheme())
                    .getHoverTheme()
                    .getBackground() : IDrawable.EMPTY));

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
                                    .widgetTheme(PlannhThemes.TEXT))
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
                return IKey.str(direction < 0 ? "<" : ">");
            }
            return IKey.str(direction < 0 ? "-" : "+")
                .color(direction < 0 ? PlannhColors.NEGATIVE::getColor : PlannhColors.POSITIVE::getColor);
        }))
            .tooltipBuilder(TooltipBuilder.build(TOOLBAR, out -> {
                final boolean shift = GuiHelper.shiftHeld();
                final String action = shift ? (direction < 0 ? "delete" : "add")
                    : (direction < 0 ? "previous" : "next");
                out.title("graph." + action + ".title")
                    .hint(direction < 0 ? "graph.delete.shift" : "graph.add.shift");
                if (!(shift && direction < 0)) return;

                final boolean last = Plan.getInstance()
                    .getGraphs()
                    .size() <= 1;
                out.line(TooltipTheme.WARNING, TOOLBAR.lang(last ? "graph.delete.last" : "graph.delete.warning"));
            }))
            .tooltipAutoUpdate(true);
    }

    /** The undo and redo buttons. A button's tooltip has the NEI bindings of the button that have a key. */
    private static Flow historyGroup(final CanvasWidget canvas) {
        return Flow.row()
            .name("screen.toolbar.history")
            .coverChildren()
            .childPadding(2)
            .child(
                leftButton("edit.undo", GuiIcon.UNDO, canvas::undoGraph)
                    .tooltipBuilder(TooltipBuilder.build(TOOLBAR, out -> {
                        out.title("undo.title");
                        final String binding = Shortcut.UNDO.keyName();
                        if (binding != null) out.hint("shortcut", binding);
                    })))
            .child(
                leftButton("edit.redo", GuiIcon.REDO, canvas::redoGraph)
                    .tooltipBuilder(TooltipBuilder.build(TOOLBAR, out -> {
                        out.title("redo.title");
                        final String binding = Shortcut.REDO.keyName();
                        if (binding != null) out.hint("shortcut", binding);
                        final String alternate = Shortcut.REDO_ALT.keyName();
                        if (alternate != null) out.hint("alternate", alternate);
                    })));
    }

    /** Auto layout, snap to grid, the port display and the balance mode. */
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
                IKey.lang("plannh.gui.toolbar.balance.label", IKey.lang("plannh.gui.toolbar.balance.off")))
            .stateOverlay(
                BalanceMode.INPUT,
                IKey.lang("plannh.gui.toolbar.balance.label", IKey.lang("plannh.gui.toolbar.balance.input")))
            .stateOverlay(
                BalanceMode.OUTPUT,
                IKey.lang("plannh.gui.toolbar.balance.label", IKey.lang("plannh.gui.toolbar.balance.output")))
            .stateOverlay(
                BalanceMode.AUTO,
                IKey.lang("plannh.gui.toolbar.balance.label", IKey.lang("plannh.gui.toolbar.balance.auto")))
            .tooltipBuilder(TooltipBuilder.build(TOOLBAR, out -> {
                final BalanceMode active = canvas.getGraph()
                    .getBalanceMode();
                out.title("balance.title");
                for (final BalanceMode mode : BalanceMode.values()) {
                    out.marker(IKey.str(mode.displayName()), active == mode);
                }

                final String pins = switch (active) {
                    case NONE -> "balance.pins.none";
                    case INPUT, OUTPUT -> "balance.pins.fixed";
                    case AUTO -> "balance.pins.auto";
                };
                out.separator()
                    .text(
                        "balance.mode." + active.name()
                            .toLowerCase(Locale.ROOT))
                    .text(pins)
                    .separator()
                    .hint("balance.cycle");
            }))
            .tooltipAutoUpdate(true);

        return Flow.row()
            .name("screen.toolbar.layout")
            .coverChildren()
            .childPadding(2)
            .child(
                leftButton("edit.autoLayout", GuiIcon.AUTO_LAYOUT, canvas::autoLayout)
                    .addTooltipLine(IKey.lang("plannh.gui.toolbar.layout.title")))
            .child(snap)
            .child(portDisplayToggle())
            .child(balance);
    }

    /** The toggle between the recipe amount and the rate in every port slot. */
    private static CycleButtonWidget portDisplayToggle() {
        final Summary summary = Plan.getInstance()
            .getSummary();
        final IDrawable recipe = GuiIcon.CYCLES.asIcon()
            .size(12);
        final IDrawable rate = GuiIcon.THROUGHPUT.asIcon()
            .size(12);
        return new CycleButtonWidget().name("edit.portDisplay")
            .size(BUTTON_SIZE, BUTTON_SIZE)
            .background(GuiTextures.MC_BUTTON)
            .hoverBackground(GuiTextures.MC_BUTTON_HOVERED)
            .value(new EnumValue.Dynamic<>(Summary.PortDisplay.class, summary::getPortDisplay, summary::setPortDisplay))
            .stateOverlay(Summary.PortDisplay.RECIPE, recipe)
            .stateOverlay(Summary.PortDisplay.RATE, rate)
            .tooltipBuilder(TooltipBuilder.build(TOOLBAR, out -> {
                final Summary.PortDisplay active = summary.getPortDisplay();
                out.title("port_display.title");
                for (final Summary.PortDisplay display : Summary.PortDisplay.values()) {
                    out.marker(TOOLBAR.lang("port_display." + display.name()), active == display);
                }
                final String binding = Shortcut.PORT_DISPLAY.keyName();
                if (binding != null) out.separator()
                    .hint("shortcut", binding);
            }))
            .tooltipAutoUpdate(true);
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
        final ButtonWidget<?> share = leftButton("share.link", GuiIcon.SHARE, () -> {
            if (!GuiHelper.shiftHeld()) PlanAPI.shareGraph(canvas.getGraph());
            else if (PlanAPI.copyGuideNHToClipboard(canvas.getGraph())) {
                PlanAPI.showMessage("plannh.share.guidenh_to_clipboard");
            }
        });

        // Shift switches the action to the GuideNH export, so the title follows it
        share.tooltipAutoUpdate(true);
        share.tooltipBuilder(
            TooltipBuilder.build(
                TOOLBAR,
                out -> out.title(GuiHelper.shiftHeld() ? "share.guidenh.title" : "share.title")
                    .hint("share.shift")));

        return Flow.row()
            .name("screen.toolbar.share")
            .coverChildren()
            .childPadding(2)
            .child(share)
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
                            .widgetTheme(PlannhThemes.TEXT)
                            .fullWidth()
                            .textAlign(Alignment.CenterLeft))
                    .child(
                        new TextWidget<>(IKey.lang("plannh.gui.toolbar.graph.delete.irreversible")).fullWidth()
                            .textAlign(Alignment.CenterLeft)
                            .widgetTheme(PlannhThemes.TEXT_NEGATIVE))
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
                                    .widgetTheme(PlannhThemes.DANGER_BUTTON)
                                    .overlay(IKey.lang("plannh.gui.toolbar.graph.delete.button")))));
        return dialog;
    }
}
