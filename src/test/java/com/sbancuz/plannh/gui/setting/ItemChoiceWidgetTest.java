package com.sbancuz.plannh.gui.setting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

import com.cleanroommc.modularui.api.UpOrDown;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.setting.IntegerSettingDef;

class ItemChoiceWidgetTest {

    @Test
    void selectionAndResetUpdateTheValueAndConsumeTheGhost() {
        final Item accepted = new Item();
        final IntegerSettingDef setting = new IntegerSettingDef("test_item_choice", 0, 0, 2);
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
            value -> new ItemStack(accepted, 1, value),
            stack -> stack.getItem() == accepted ? stack.getItemDamage() : null);

        final ItemStack ghost = new ItemStack(accepted, 4, 1);
        assertTrue(widget.handleDragAndDrop(ghost, 0));
        assertEquals(1, cfg.value(setting, ctx));
        assertEquals(0, ghost.stackSize);

        final ItemStack resetGhost = new ItemStack(accepted, 1);
        assertTrue(widget.handleDragAndDrop(resetGhost, 2));
        assertFalse(cfg.isExplicit(setting));
        assertEquals(0, cfg.value(setting, ctx));
        assertEquals(0, resetGhost.stackSize);
    }

    @Test
    void itemCountEditorScrollsWithinProfileBounds() {
        final Item item = new Item();
        final IntegerSettingDef setting = new IntegerSettingDef("test_item_count", 0, 0, 9) {

            @Override
            public IWidget settingsWidget(final MachineConfig cfg, final RecipeContext ctx,
                final Consumer<Runnable> edit) {
                return itemCountWidget(cfg, ctx, edit, () -> new ItemStack(item));
            }
        };
        final MachineConfig cfg = new MachineConfig(
            MachineProfile.builder("test:item_count")
                .setting(setting, 1, 2)
                .build());
        final RecipeContext ctx = RecipeContext.builder()
            .build();
        final ItemChoiceWidget widget = (ItemChoiceWidget) setting.settingsWidget(cfg, ctx, Runnable::run);

        assertTrue(widget.onMouseScroll(UpOrDown.UP, 1));
        assertEquals(2, cfg.value(setting, ctx));
        assertTrue(widget.onMouseScroll(UpOrDown.UP, 1));
        assertEquals(2, cfg.value(setting, ctx));

        assertTrue(widget.onMouseScroll(UpOrDown.DOWN, 1));
        assertTrue(widget.onMouseScroll(UpOrDown.DOWN, 1));
        assertEquals(1, cfg.value(setting, ctx));
    }
}
