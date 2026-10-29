package com.sbancuz.plannh.gui.setting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

class ItemChoiceWidgetTest {

    @Test
    void ghostMiddleClickRunsConfiguredReset() {
        final AtomicBoolean reset = new AtomicBoolean();
        final ItemChoiceWidget widget = new ItemChoiceWidget(
            () -> null,
            ignored -> true,
            ignored -> {},
            runnable -> runnable.run());
        widget.picker(List::of, () -> reset.set(true));

        final ItemStack ghost = new ItemStack(new Item(), 1);
        assertTrue(widget.handleDragAndDrop(ghost, 2));
        assertTrue(reset.get());
        assertEquals(0, ghost.stackSize);
    }
}
