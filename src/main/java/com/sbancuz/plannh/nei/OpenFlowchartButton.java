package com.sbancuz.plannh.nei;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;

import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.sbancuz.plannh.gui.FlowchartScreen;
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

    @Nullable
    private GuiScreen previousScreen = null;

    @Override
    public boolean onButtonPress(final boolean rightclick) {
        if (!rightclick) {
            final Minecraft mc = Minecraft.getMinecraft();
            if (mc.currentScreen instanceof GuiContainerWrapper) {
                mc.displayGuiScreen(previousScreen != null ? previousScreen : new GuiInventory(mc.thePlayer));
            } else {
                previousScreen = mc.currentScreen;
                final FlowchartScreen screen = FlowchartScreen.create();
                final ModularContainer container = new ModularContainer();
                container.constructClientOnly();
                mc.displayGuiScreen(new GuiContainerWrapper(container, screen));
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
