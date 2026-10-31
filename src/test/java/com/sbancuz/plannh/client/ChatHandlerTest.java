package com.sbancuz.plannh.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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
        final NBTTagCompound plan = new NBTTagCompound();
        plan.setString(PlanAPI.PLANNH_DATA_KEY, "encoded graph");
        final NBTTagCompound item = new NBTTagCompound();
        item.setShort("id", (short) 3);
        item.setByte("Count", (byte) 1);
        item.setTag("tag", plan);
        final ClientChatReceivedEvent event = itemLink(item.toString());

        new ChatHandler().onClientChatReceived(event);

        assertEquals(
            "/plannh_import " + item,
            event.message.getChatStyle()
                .getChatClickEvent()
                .getValue());

        final ClientChatReceivedEvent ordinary = itemLink("{id:3s,Count:1b,Damage:0s}");
        final IChatComponent unchanged = ordinary.message;
        new ChatHandler().onClientChatReceived(ordinary);
        assertSame(unchanged, ordinary.message);
    }

    private static ClientChatReceivedEvent itemLink(final String nbt) {
        final IChatComponent item = new ChatComponentText("[Shared Item]");
        item.getChatStyle()
            .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nei_bookmark 1234abcd"))
            .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new ChatComponentText(nbt)));
        return new ClientChatReceivedEvent(
            new ChatComponentTranslation("nei.chat.item_link.text", new ChatComponentText("Developer"), item));
    }
}
