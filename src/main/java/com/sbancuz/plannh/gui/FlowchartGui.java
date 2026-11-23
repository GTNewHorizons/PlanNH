package com.sbancuz.plannh.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.screen.ModularContainer;

/** {@code FlowchartGui} shows a {@link FlowchartScreen} in a client-only container GUI. */
public final class FlowchartGui extends GuiContainerWrapper {

    private FlowchartGui(final ModularContainer container, final FlowchartScreen screen) {
        super(container, screen);
    }

    public static void openFlowchart() {
        final ModularContainer container = new ModularContainer();
        container.constructClientOnly();
        Minecraft.getMinecraft()
            .displayGuiScreen(new FlowchartGui(container, FlowchartScreen.create()));
    }

    @Override
    public void initGui() {
        super.initGui();

        // TODO: Maybe upstream?
        // The server doesn't know that a gui is open, so any inventory update through keybinds will get lost
        final EntityPlayer player = mc.thePlayer;
        player.openContainer = player.inventoryContainer;
    }
}
