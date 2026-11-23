package com.sbancuz.plannh.gui;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraftforge.client.event.GuiOpenEvent;

import com.cleanroommc.modularui.network.ModularNetwork;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.screen.ModularContainer;

import codechicken.nei.recipe.GuiRecipe;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** {@code FlowchartGui} shows a {@link FlowchartScreen} in a client-only container GUI. */
public final class FlowchartGui extends GuiContainerWrapper {

    private final Container serverContainer;

    @Nullable
    private final GuiScreen previousScreen;

    private FlowchartGui(final ModularContainer container, final Container serverContainer,
        @Nullable final GuiScreen previousScreen) {
        super(container, FlowchartScreen.create());
        this.serverContainer = serverContainer;
        this.previousScreen = previousScreen;
    }

    public static void openFlowchart(@Nullable final GuiScreen previousScreen) {
        final Minecraft minecraft = Minecraft.getMinecraft();
        final EntityPlayer player = minecraft.thePlayer;

        // `GuiRecipe.firstGui` is the GUI that the recipe was looked up from
        final GuiScreen origin = previousScreen instanceof final GuiRecipe<?> recipe ? recipe.firstGui : previousScreen;

        // Window 0 on the server is the inventory container. A client-only container, such as the creative inventory,
        // has window id 0 too.
        final Container serverContainer = origin instanceof final GuiContainer gui && gui.inventorySlots.windowId != 0
            ? gui.inventorySlots
            : player.inventoryContainer;

        final ModularContainer container = new ModularContainer();
        container.constructClientOnly();
        minecraft.displayGuiScreen(new FlowchartGui(container, serverContainer, previousScreen));
    }

    @Override
    public void initGui() {
        super.initGui();

        // The server doesn't know that a gui is open, so any inventory update through keybinds will get lost
        mc.thePlayer.openContainer = serverContainer;
    }

    /**
     * Show the screen the flowchart was opened from. When the flowchart was opened without a screen, show the player
     * inventory.
     */
    public void returnToPreviousScreen() {
        final GuiScreen screen = previousScreen != null ? previousScreen : new GuiInventory(mc.thePlayer);
        mc.displayGuiScreen(screen);
        ModularNetwork.CLIENT.reopenSyncerOf(screen);
    }

    private void closeServerContainer() {
        final EntityPlayer player = mc.thePlayer;

        // `EntityPlayer.closeScreen()` resets `openContainer` when the server closes the container
        if (player.openContainer != serverContainer) return;

        mc.getNetHandler()
            .addToSendQueue(new C0DPacketCloseWindow(serverContainer.windowId));

        // `Container.onContainerClosed()` on the server drops the cursor stack. Clear the cursor stack on the client
        // too
        player.inventory.setItemStack(null);
        player.openContainer = player.inventoryContainer;
    }

    /** {@code CloseListener} closes the server container when the flowchart closes without opening another screen. */
    public static final class CloseListener {

        // ModularUI2 closes the flowchart with `displayGuiScreen(null)`. `displayGuiScreen()` doesn't send a
        // close-window packet. Forge calls this listener after every listener with a higher priority.
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onGuiOpen(final GuiOpenEvent event) {
            final GuiScreen currentScreen = Minecraft.getMinecraft().currentScreen;
            if (event.gui == null && currentScreen instanceof final FlowchartGui flowchart) {
                flowchart.closeServerContainer();
            }
        }
    }
}
