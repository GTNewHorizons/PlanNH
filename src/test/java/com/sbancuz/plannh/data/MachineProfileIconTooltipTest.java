package com.sbancuz.plannh.data;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.screen.RichTooltip;

import codechicken.nei.recipe.RecipeHandlerRef;

class MachineProfileIconTooltipTest {

    @Test
    void nativeItemTooltipWinsOverCustomCallback() {
        final AtomicBoolean customCalled = new AtomicBoolean();
        final MachineProfile profile = MachineProfile.builder("test:native")
            .icon(
                (_, _) -> new ItemDrawable(new ItemStack(new Item().setUnlocalizedName("test.native"))),
                tooltip -> customCalled.set(true))
            .build();

        final TrackingTooltip tooltip = new TrackingTooltip();
        assertDoesNotThrow(() -> profile.addIconTooltip(profile.getIcon(RecipeHandlerRef.of(null, 0)), tooltip));
        assertFalse(customCalled.get());
        assertTrue(tooltip.itemAdded);
    }

    @Test
    void invalidItemUsesCustomTooltip() {
        final AtomicBoolean customCalled = new AtomicBoolean();
        final MachineProfile profile = MachineProfile.builder("test:invalid")
            .icon((_, _) -> new ItemDrawable((ItemStack) null), tooltip -> customCalled.set(true))
            .build();
        final RichTooltip tooltip = new RichTooltip();

        profile.addIconTooltip(profile.getIcon(RecipeHandlerRef.of(null, 0)), tooltip);

        assertTrue(customCalled.get());
        assertFalse(tooltip.isEmpty());
    }

    @Test
    void oneArgumentIconHasMeaningfulFallbackTooltip() {
        final MachineProfile profile = MachineProfile.builder("test:fallback")
            .icon((_, _) -> IDrawable.EMPTY)
            .build();
        final RichTooltip tooltip = new RichTooltip();

        profile.addIconTooltip(profile.getIcon(RecipeHandlerRef.of(null, 0)), tooltip);

        assertFalse(tooltip.isEmpty());
    }

    private static final class TrackingTooltip extends RichTooltip {

        private boolean itemAdded;

        @Override
        public RichTooltip addFromItem(final ItemStack stack) {
            itemAdded = true;
            return this;
        }
    }
}
