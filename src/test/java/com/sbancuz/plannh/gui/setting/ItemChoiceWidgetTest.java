package com.sbancuz.plannh.gui.setting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.cleanroommc.modularui.api.UpOrDown;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.provider.ProviderTestFixture;
import com.sbancuz.plannh.data.setting.ChoiceSettingDef;

class ItemChoiceWidgetTest {

    @BeforeAll
    static void initializeMinecraftFixture() {
        ProviderTestFixture.bootstrap();
    }

    @Test
    void selectionAndResetUpdateTheValueAndConsumeTheGhost() {
        final Item accepted = Items.apple;
        final ChoiceSettingDef<ItemStack> setting = new ChoiceSettingDef<>(
            "test_item_choice",
            null,
            ItemStack.class,
            () -> List.of(new ItemStack(accepted, 1, 0), new ItemStack(accepted, 1, 1), new ItemStack(accepted, 1, 2)),
            stack -> stack == null ? "" : Integer.toString(stack.getItemDamage())).counts(0, 3);
        final MachineConfig cfg = new MachineConfig(
            MachineProfile.builder("test:item_choice")
                .setting(setting)
                .build());
        final RecipeContext ctx = RecipeContext.builder()
            .build();
        final ItemChoiceWidget widget = new ItemChoiceWidget(
            setting,
            cfg,
            ctx,
            Runnable::run,
            () -> List.of(new ItemStack(accepted, 1, 0), new ItemStack(accepted, 1, 1), new ItemStack(accepted, 1, 2)),
            value -> value,
            stack -> stack.getItem() == accepted ? stack : null);

        final ItemStack ghost = new ItemStack(accepted, 4, 1);
        assertTrue(widget.handleDragAndDrop(ghost, 0));
        assertEquals(
            1,
            cfg.value(setting, ctx)
                .getItemDamage());
        assertEquals(3, cfg.value(setting, ctx).stackSize);
        assertEquals(0, ghost.stackSize);

        setting.setCount(cfg, ctx, 0);
        assertNull(cfg.value(setting, ctx));
        assertTrue(cfg.isExplicit(setting));

        final ItemStack resetGhost = new ItemStack(accepted, 1);
        assertTrue(widget.handleDragAndDrop(resetGhost, 2));
        assertFalse(cfg.isExplicit(setting));
        assertEquals(0, setting.count(cfg, ctx));
        assertEquals(0, resetGhost.stackSize);
    }

    @Test
    void itemCountEditorScrollsWithinProfileBounds() {
        final Item item = Items.apple;
        final ChoiceSettingDef<ItemStack> setting = new ChoiceSettingDef<>(
            "test_item_count",
            null,
            ItemStack.class,
            () -> List.of(new ItemStack(item)),
            stack -> "item").counts(1, 9);
        final MachineConfig cfg = new MachineConfig(
            MachineProfile.builder("test:item_count")
                .setting(setting, 1, 2)
                .build());
        final RecipeContext ctx = RecipeContext.builder()
            .build();
        final ItemChoiceWidget widget = (ItemChoiceWidget) setting.settingsWidget(cfg, ctx, Runnable::run);

        assertTrue(widget.onMouseScroll(UpOrDown.UP, 1));
        assertEquals(1, setting.count(cfg, ctx));
        final ItemStack previous = cfg.value(setting, ctx);
        assertTrue(widget.onMouseScroll(UpOrDown.UP, 1));
        assertEquals(2, setting.count(cfg, ctx));
        assertEquals(1, previous.stackSize);
        assertTrue(widget.onMouseScroll(UpOrDown.UP, 1));
        assertEquals(2, setting.count(cfg, ctx));

        assertTrue(widget.onMouseScroll(UpOrDown.DOWN, 1));
        assertTrue(widget.onMouseScroll(UpOrDown.DOWN, 1));
        assertEquals(1, setting.count(cfg, ctx));
    }

}
