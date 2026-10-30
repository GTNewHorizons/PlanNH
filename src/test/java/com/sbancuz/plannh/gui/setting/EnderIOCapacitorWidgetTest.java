package com.sbancuz.plannh.gui.setting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.provider.ProviderTestFixture;
import com.sbancuz.plannh.data.setting.Settings;

import crazypants.enderio.EnderIO;
import crazypants.enderio.material.ItemCapacitor;

class EnderIOCapacitorWidgetTest {

    @BeforeAll
    static void installNativeItemFixture() {
        if (EnderIO.itemBasicCapacitor == null)
            EnderIO.itemBasicCapacitor = ProviderTestFixture.item(ItemCapacitor.class, "capacitor");
    }

    @Test
    void editorMapsNativeCapacitorMetadataAndResets() {
        final MachineProfile profile = MachineProfile.builder("test:enderio_capacitor")
            .setting(Settings.EIO_CAPACITOR)
            .build();
        final MachineConfig cfg = new MachineConfig(profile);
        final RecipeContext ctx = RecipeContext.builder()
            .build();
        final ItemChoiceWidget editor = (ItemChoiceWidget) Settings.EIO_CAPACITOR
            .settingsWidget(cfg, ctx, Runnable::run);

        for (int tier = 0; tier <= 6; tier++) {
            final ItemStack ghost = new ItemStack(EnderIO.itemBasicCapacitor, 3, tier);
            assertTrue(editor.handleDragAndDrop(ghost, 0));
            assertEquals(tier, cfg.getOrDefault(Settings.EIO_CAPACITOR));
            assertEquals(0, ghost.stackSize);
        }

        for (int metadata = 7; metadata <= 9; metadata++) {
            final ItemStack ghost = new ItemStack(EnderIO.itemBasicCapacitor, 1, metadata);
            assertTrue(editor.handleDragAndDrop(ghost, 0));
            assertEquals(metadata - 7, cfg.getOrDefault(Settings.EIO_CAPACITOR));
        }

        final ItemStack invalidMetadata = new ItemStack(EnderIO.itemBasicCapacitor, 1, 10);
        assertFalse(editor.handleDragAndDrop(invalidMetadata, 0));
        assertEquals(1, invalidMetadata.stackSize);
        final ItemStack foreignItem = new ItemStack(ProviderTestFixture.item("foreign_capacitor"), 1);
        assertFalse(editor.handleDragAndDrop(foreignItem, 0));
        assertEquals(1, foreignItem.stackSize);

        final ItemStack resetGhost = new ItemStack(EnderIO.itemBasicCapacitor, 1, 6);
        assertTrue(editor.handleDragAndDrop(resetGhost, 2));
        assertFalse(cfg.isExplicit(Settings.EIO_CAPACITOR));
        assertEquals(0, cfg.getOrDefault(Settings.EIO_CAPACITOR));
    }
}
