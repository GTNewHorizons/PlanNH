package com.sbancuz.plannh.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

import org.junit.jupiter.api.Test;

import com.sbancuz.plannh.api.PlanAPI;

class ChatHandlerTest {

    @Test
    void planShareUsesHoverItemNbtInsteadOfTheBookmarkCacheId() {
        final String itemNbt = planItemNbt();
        final IChatComponent message = itemLink(
            new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nei_bookmark 1234abcd"),
            new HoverEvent(HoverEvent.Action.SHOW_ITEM, new ChatComponentText(itemNbt)));
        final ClientChatReceivedEvent event = new ClientChatReceivedEvent(message);

        new ChatHandler().onClientChatReceived(event);

        assertNotSame(message, event.message);
        final ClickEvent click = event.message.getChatStyle()
            .getChatClickEvent();
        assertEquals(ClickEvent.Action.RUN_COMMAND, click.getAction());
        assertEquals("/plannh_import " + itemNbt, click.getValue());
        assertTrue(
            PlanAPI.containsPlanNHData(
                click.getValue()
                    .substring("/plannh_import ".length())));
        assertEquals(
            HoverEvent.Action.SHOW_ITEM,
            event.message.getChatStyle()
                .getChatHoverEvent()
                .getAction());
    }

    @Test
    void ordinaryNeiItemLinksRemainUnchanged() {
        final IChatComponent message = itemLink(
            new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nei_bookmark 1234abcd"),
            new HoverEvent(HoverEvent.Action.SHOW_ITEM, new ChatComponentText("{id:3s,Count:1b,Damage:0s}")));
        assertUnchanged(message);
    }

    @Test
    void missingOrMalformedItemHoverIsIgnored() {
        final ClickEvent click = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nei_bookmark 1234abcd");
        assertUnchanged(itemLink(click, null));
        assertUnchanged(
            itemLink(click, new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(planItemNbt()))));
        assertUnchanged(
            itemLink(click, new HoverEvent(HoverEvent.Action.SHOW_ITEM, new ChatComponentText("invalid NBT"))));
    }

    @Test
    void unrelatedCommandsWithPlanItemHoverAreIgnored() {
        final HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_ITEM, new ChatComponentText(planItemNbt()));
        assertUnchanged(itemLink(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/other_command"), hover));
        assertUnchanged(itemLink(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/nei_bookmark 1234abcd"), hover));
    }

    private static IChatComponent itemLink(final ClickEvent click, final HoverEvent hover) {
        final IChatComponent item = new ChatComponentText("[PlanNH Share]");
        item.getChatStyle()
            .setChatClickEvent(click);
        item.getChatStyle()
            .setChatHoverEvent(hover);
        return new ChatComponentTranslation("nei.chat.item_link.text", new ChatComponentText("Developer"), item);
    }

    private static String planItemNbt() {
        final NBTTagCompound tag = new NBTTagCompound();
        tag.setString(PlanAPI.PLANNH_DATA_KEY, "encoded graph");
        final NBTTagCompound item = new NBTTagCompound();
        item.setShort("id", (short) 3);
        item.setByte("Count", (byte) 1);
        item.setShort("Damage", (short) 0);
        item.setTag("tag", tag);
        return item.toString();
    }

    private static void assertUnchanged(final IChatComponent message) {
        final ClientChatReceivedEvent event = new ClientChatReceivedEvent(message);
        new ChatHandler().onClientChatReceived(event);
        assertSame(message, event.message);
    }
}
