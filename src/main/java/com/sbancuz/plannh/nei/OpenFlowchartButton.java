package com.sbancuz.plannh.nei;

import java.util.List;

import net.minecraft.client.Minecraft;

import com.sbancuz.plannh.gui.FlowchartGui;
import com.sbancuz.plannh.gui.GuiIcon;

import codechicken.nei.Button;
import codechicken.nei.drawable.DrawableResource;

public class OpenFlowchartButton extends Button {

    public OpenFlowchartButton() {
        final int size = GuiIcon.FLOWCHART.size();
        icon = new DrawableResource(
            GuiIcon.FLOWCHART.texture()
                .getLocation(),
            0,
            0,
            size,
            size,
            0,
            0,
            0,
            0,
            size,
            size);
    }

    @Override
    public boolean onButtonPress(final boolean rightclick) {
        if (!rightclick) {
            final Minecraft mc = Minecraft.getMinecraft();
            if (mc.currentScreen instanceof final FlowchartGui flowchart) {
                flowchart.returnToPreviousScreen();
            } else {
                FlowchartGui.openFlowchart(mc.currentScreen);
            }
            return true;
        }
        return false;
    }

    private static final int BUTTON_WIDTH = 20;

    @Override
    public int contentWidth() {
        return BUTTON_WIDTH;
    }

    @Override
    public void addTooltips(List<String> tooltip) {
        tooltip.add("Open / Close flowchart");
    }
}
