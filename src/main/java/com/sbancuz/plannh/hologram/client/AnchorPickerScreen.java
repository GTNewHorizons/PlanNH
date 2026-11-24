package com.sbancuz.plannh.hologram.client;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.sbancuz.plannh.PlanNH;
import com.sbancuz.plannh.api.PlanAPI;
import com.sbancuz.plannh.data.flowchart.Graph;
import com.sbancuz.plannh.data.flowchart.GraphData;
import com.sbancuz.plannh.data.flowchart.Group;
import com.sbancuz.plannh.data.flowchart.Node;
import com.sbancuz.plannh.data.flowchart.Note;
import com.sbancuz.plannh.data.flowchart.Plan;
import com.sbancuz.plannh.hologram.Anchors;
import com.sbancuz.plannh.hologram.BlockAnchor;

/** Client-only dialog that attaches one element of the active graph to a block, or detaches the block's element. */
public final class AnchorPickerScreen extends ModularScreen {

    private static final int WIDTH = 240;
    private static final int HEIGHT = 180;
    private static final int MARGIN = 8;
    private static final int BUTTON_WIDTH = 64;
    private static final int BUTTON_HEIGHT = 16;
    private static final String ATTACHED_MARK = "§a";

    private AnchorPickerScreen(final ModularPanel panel) {
        super(PlanNH.MODID, panel);
    }

    public static void open(final BlockAnchor anchor) {
        final Plan plan = Plan.getInstance();
        final Graph active = Plan.getActiveGraph();
        final GraphData attached = Anchors.attachedAt(plan.getGraphs(), anchor);

        final ModularPanel panel = ModularPanel.defaultPanel("hologram_picker", WIDTH, HEIGHT);
        panel.child(
            new TextWidget<>(IKey.lang("plannh.hologram.picker.title")).name("picker.title")
                .pos(MARGIN, 7));

        if (active.allData()
            .findAny()
            .isEmpty()) {
            panel.child(
                new TextWidget<>(IKey.lang("plannh.hologram.picker.empty")).name("picker.empty")
                    .pos(MARGIN, 24));
        } else {
            final var list = new ListWidget<>().name("picker.list")
                .pos(MARGIN, 20)
                .size(WIDTH - 2 * MARGIN, HEIGHT - 20 - 28);
            active.allData()
                .forEach(
                    element -> list.child(elementButton(plan, active, element, anchor, element == attached, panel)));
            panel.child(list);
        }

        panel.child(button("picker.detach", "plannh.hologram.picker.detach", () -> {
            detach(plan, attached);
            panel.closeIfOpen();
        }).pos(MARGIN, HEIGHT - 24)
            .setEnabledIf(_ -> attached != null))
            .child(
                button("picker.cancel", "plannh.hologram.picker.cancel", panel::closeIfOpen).right(MARGIN)
                    .top(HEIGHT - 24));
        ClientGUI.open(new AnchorPickerScreen(panel));
    }

    private static ButtonWidget<?> elementButton(final Plan plan, final Graph active, final GraphData element,
        final BlockAnchor anchor, final boolean isAttached, final ModularPanel panel) {
        final String label = StatCollector.translateToLocal(typeKey(element)) + ": " + HologramText.of(element);
        return button("picker.element", null, () -> {
            PlanAPI.recordEdit(active, () -> Anchors.attach(plan.getGraphs(), element, anchor));
            PlanAPI.save();
            panel.closeIfOpen();
        }).overlay(IKey.str((isAttached ? ATTACHED_MARK : "") + label))
            .fullWidth()
            .height(BUTTON_HEIGHT);
    }

    private static void detach(final Plan plan, final GraphData attached) {
        for (final Graph graph : plan.getGraphs()) {
            if (graph == null || graph.allData()
                .noneMatch(data -> data == attached)) continue;
            PlanAPI.recordEdit(graph, () -> attached.setAnchor(null));
            PlanAPI.save();
            return;
        }
    }

    private static String typeKey(final GraphData element) {
        if (element instanceof Node) return "plannh.hologram.picker.node";
        if (element instanceof Group) return "plannh.hologram.picker.group";
        if (element instanceof Note) return "plannh.hologram.picker.note";
        return "plannh.hologram.picker.node";
    }

    private static ButtonWidget<?> button(final String name, final String key, final Runnable action) {
        final ButtonWidget<?> button = new ButtonWidget<>().name(name)
            .size(BUTTON_WIDTH, BUTTON_HEIGHT)
            .onMousePressed(mouseButton -> {
                if (mouseButton != 0) return false;
                action.run();
                return true;
            });
        return key == null ? button : button.overlay(IKey.lang(key));
    }
}
