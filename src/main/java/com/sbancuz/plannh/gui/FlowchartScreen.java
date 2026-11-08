package com.sbancuz.plannh.gui;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.menu.Menu;
import com.sbancuz.plannh.PlanNH;
import com.sbancuz.plannh.api.PlanAPI;
import com.sbancuz.plannh.gui.summary.SummaryWidget;
import com.sbancuz.plannh.nei.config.AppearanceConfig;
import com.sbancuz.plannh.nei.config.KeybindsConfig;

import codechicken.nei.NEIClientConfig;
import lombok.Getter;

public class FlowchartScreen extends ModularScreen {

    @Getter
    private final CanvasWidget canvas;

    private final ParentWidget<?> messages;

    private FlowchartScreen(final ModularPanel panel, final CanvasWidget canvas, final ParentWidget<?> messages) {
        super(PlanNH.MODID, panel);
        getContext().setSettings(new UISettings());
        getContext().getUISettings()
            .getRecipeViewerSettings()
            .enable();
        this.messages = messages;
        this.canvas = canvas;
    }

    public static FlowchartScreen create() {
        final ModularPanel panel = ModularPanel.defaultPanel("flowchart_main")
            .fullScreenInvisible();
        final Menu<?> contextMenu = new Menu<>();
        final CanvasWidget canvas = new CanvasWidget(contextMenu, panel);

        final var actions = new ListWidget<>().name("context.menu")
            .coverChildrenHeight()
            .width(110);
        actions.child(
            contextAction(
                "context.addNote",
                "plannh.gui.toolbar.add.note.title",
                () -> { canvas.addNote(canvas.getCanvasMouseX(), canvas.getCanvasMouseY()); }));
        actions.child(
            contextAction(
                "context.addGroup",
                "plannh.gui.toolbar.add.group.title",
                () -> { canvas.addGroup(canvas.getCanvasMouseX(), canvas.getCanvasMouseY()); }));
        actions.child(
            contextAction(
                "context.addMachineGroup",
                "plannh.gui.group.add_machine_group",
                () -> { canvas.addMachineGroup(canvas.getCanvasMouseX(), canvas.getCanvasMouseY()); }));
        contextMenu.setEnabledIf(_ -> canvas.isMenuOpen())
            .coverChildren()
            .background()
            .relativeToScreen()
            .child(actions);

        final ParentWidget<?> messages = new ParentWidget<>().name("screen.messages")
            .relativeToScreen()
            .background(IDrawable.EMPTY)
            .hoverBackground(IDrawable.EMPTY);

        panel.child(
            Flow.column()
                .name("screen.column")
                .full()
                .childPadding(FlowchartBounds.TOOLBAR_GAP)
                .child(new FlowchartToolbar(canvas))
                .child(canvas));
        panel.child(new SummaryWidget(canvas));
        panel.child(contextMenu);
        panel.child(messages);
        return new FlowchartScreen(panel, canvas, messages);
    }

    private static ButtonWidget<?> contextAction(final String name, final String key, final Runnable action) {
        return new ButtonWidget<>().name(name)
            .fullWidth()
            .background(
                new Rectangle().color(PlannhColors.CONTEXT_BG.getColor()),
                new Rectangle().hollow()
                    .color(PlannhColors.CONTEXT_BORDER.getColor()))
            .hoverBackground(new Rectangle().color(PlannhColors.CONTEXT_HOVER.getColor()))
            .overlay(IKey.lang(key))
            .onMousePressed(button -> {
                if (button != 0) return false;
                action.run();
                return true;
            });
    }

    @Override
    public void onResize(final int width, final int height) {
        final FlowchartBounds bounds = bounds(width, height);
        updateLayout(bounds);
        super.onResize(width, height);
    }

    @Override
    public void onFrameUpdate() {
        // NEI does not run its layout while hidden or disabled.
        if (!NEIClientConfig.isEnabled() || NEIClientConfig.isHidden()) {
            final FlowchartBounds bounds = bounds(getScreenArea().width, getScreenArea().height);
            updateLayout(bounds);
        }
        super.onFrameUpdate();
    }

    private static FlowchartBounds bounds(final int width, final int height) {
        return FlowchartBounds.calculate(
            width,
            height,
            AppearanceConfig.itemColumns(),
            NEIClientConfig.isEnabled() && !NEIClientConfig.isHidden());
    }

    /** Receives NEI's final bounds, including user-configured item-panel padding. */
    public void updateLayout(final FlowchartBounds bounds) {
        final ModularPanel panel = getMainPanel();
        if (panel.getArea().x == bounds.x() && panel.getArea().y == bounds.y()
            && panel.getArea().width == bounds.width()
            && panel.getArea().height == bounds.height()) return;
        panel.pos(bounds.x(), bounds.y())
            .size(bounds.width(), bounds.height());
        panel.scheduleResize();
    }

    @Override
    public void onClose() {
        PlanAPI.save();
        super.onClose();
    }

    public void showMessage(final String message) {
        messages.child(
            new CursorMessageWidget(
                IKey.str(message),
                getContext().getAbsMouseX(),
                getContext().getAbsMouseY(),
                Math.clamp(getScreenArea().width - 18, 10, 280)));
        messages.scheduleResize();
    }

    @Override
    public boolean onMouseScroll(final UpOrDown direction, final int amount) {
        super.onFrameUpdate();
        return super.onMouseScroll(direction, amount);
    }

    @Override
    public boolean onKeyPressed(final char typedChar, final int keyCode) {
        final boolean undo = KeybindsConfig.isUndo(keyCode);
        if (undo || KeybindsConfig.isRedo(keyCode)) {
            if (undo) canvas.undoGraph();
            else canvas.redoGraph();
            return true;
        }
        return super.onKeyPressed(typedChar, keyCode);
    }
}
