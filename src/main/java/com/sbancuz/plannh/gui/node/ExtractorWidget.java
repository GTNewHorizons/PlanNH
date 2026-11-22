package com.sbancuz.plannh.gui.node;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.sbancuz.plannh.api.PlanAPI;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.flowchart.Node;
import com.sbancuz.plannh.gui.GuiIcon;
import com.sbancuz.plannh.gui.common.ItemWidget;
import com.sbancuz.plannh.gui.common.SelectionGrid;
import com.sbancuz.plannh.gui.common.SelectionPicker;

/** The selected extractor is an icon; its alternatives share the item picker's panel and layout. */
public final class ExtractorWidget extends ItemWidget<ExtractorWidget> {

    private final Node data;

    /**
     * The icon of the selected profile. A left or right click opens a picker of the node's profiles. In the picker, a
     * click on another profile changes the node's profile in one plan edit.
     */
    public ExtractorWidget(final NodeWidget node) {
        super(
            readItem(
                readIcon(
                    node.getData(),
                    node.getData()
                        .getSelectedProfile())));
        data = node.getData();
        final SelectionPicker picker = new SelectionPicker(this, () -> {
            final List<ButtonWidget<?>> buttons = new ArrayList<>();
            for (final MachineProfile profile : data.getAvailableProfiles()) {
                final IDrawable icon = readIcon(data, profile);
                buttons.add(
                    iconSlot(new ItemWidget<>(readItem(icon)), icon, profile).name("extractor.choice")
                        .onMousePressed(mouse -> {
                            if (mouse != 0 && mouse != 1) return false;
                            if (profile.equals(data.getSelectedProfile())) return true;
                            node.commitEdit(() -> {
                                // TODO
                                if (data.selectProfile(
                                    data.getAvailableProfiles()
                                        .indexOf(profile)))
                                    node.getCanvas()
                                        .setGraph(
                                            node.getCanvas()
                                                .getGraph());
                                else PlanAPI.showMessage("plannh.error.recipe_edit", data.getMachineName());
                            });
                            return true;
                        }));
            }
            return new SelectionGrid().choices(buttons);
        });
        final MachineProfile selected = data.getSelectedProfile();
        iconSlot(this, readIcon(data, selected), selected);
        name("node.options.extractor");
        onMousePressed(mouse -> {
            if (mouse != 0 && mouse != 1 || data.getAvailableProfiles()
                .isEmpty()) return false;
            picker.toggle();
            return true;
        });
    }

    private static IDrawable readIcon(final Node data, @Nullable final MachineProfile profile) {
        return profile == null ? GuiIcon.MACHINE : profile.getIcon(data.getHandlerRef());
    }

    private static @Nullable ItemStack readItem(final IDrawable icon) {
        return icon instanceof ItemDrawable item ? item.getItem() : null;
    }

    /**
     * A slot with the icon of {@code profile}. An item gets the item's tooltip from {@link ItemWidget}, and a GUI icon
     * gets the tooltip of {@link MachineProfile#addIconTooltip}.
     */
    private static ItemWidget<?> iconSlot(final ItemWidget<?> slot, final IDrawable icon,
        @Nullable final MachineProfile profile) {
        slot.size(SelectionGrid.SLOT)
            .padding(1)
            .background(GuiTextures.SLOT_ITEM)
            .tooltipAutoUpdate(true);
        if (slot.getShownItem() != null) return slot;

        final IDrawable guiIcon = icon.asIcon()
            .size(icon instanceof GuiIcon ? 12 : 16);
        if (profile == null) return slot.overlay(guiIcon);
        return slot.overlay(guiIcon)
            .tooltipBuilder(profile::addIconTooltip);
    }
}
