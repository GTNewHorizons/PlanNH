package com.sbancuz.plannh.nei;

import net.minecraft.client.gui.inventory.GuiContainer;

import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.sbancuz.plannh.gui.FlowchartBounds;
import com.sbancuz.plannh.gui.FlowchartScreen;
import com.sbancuz.plannh.mixins.GuiContainerAccessor;
import com.sbancuz.plannh.nei.config.AppearanceConfig;

import codechicken.lib.vec.Rectangle4i;
import codechicken.nei.LayoutManager;
import codechicken.nei.LayoutStyleMinecraft;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.PanelWidget;
import codechicken.nei.VisiblityData;

public class FlowchartLayoutStyle extends LayoutStyleMinecraft {

    @Override
    public void layout(final GuiContainer gui, final VisiblityData visibility) {
        if (!(gui instanceof GuiContainerWrapper
            && ((GuiContainerWrapper) gui).getScreen() instanceof FlowchartScreen)) {
            super.layout(gui, visibility);
            return;
        }

        final GuiContainerAccessor accessor = (GuiContainerAccessor) gui;
        final int screenW = accessor.getWidth();
        final int screenH = accessor.getHeight();

        final int columns = AppearanceConfig.itemColumns();
        final FlowchartBounds bounds = FlowchartBounds.calculate(
            screenW,
            screenH,
            columns,
            NEIClientConfig.isEnabled() && visibility.showNEI && visibility.showItemPanel);

        accessor.setXSize(bounds.containerWidth());
        accessor.setYSize(Math.max(0, screenH));
        accessor.setGuiLeft(bounds.containerLeft());
        accessor.setGuiTop(0);
        final Rectangle4i itemBounds = LayoutManager.itemPanel.calculateBounds(gui);
        ((FlowchartScreen) ((GuiContainerWrapper) gui).getScreen())
            .updateLayout(bounds.alignToItemPanel(itemBounds.x, itemBounds.y));
        super.layout(gui, visibility);
        // The superclass lays out the footer before resizing the item panel it is anchored to.
        super.layoutFooter(gui, visibility);

        // resizeHeader runs before grid.refresh, so both labels need a final value here.
        refreshPageLabel(LayoutManager.itemPanel);
        refreshPageLabel(LayoutManager.bookmarkPanel);
    }

    private static void refreshPageLabel(final PanelWidget<?> panel) {
        panel.pageLabel.text = panel.getLabelText();
    }
}
