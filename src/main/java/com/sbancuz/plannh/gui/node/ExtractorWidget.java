package com.sbancuz.plannh.gui.node;

import java.util.ArrayList;
import java.util.List;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.sbancuz.plannh.api.PlanAPI;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.flowchart.Node;
import com.sbancuz.plannh.gui.common.SelectionGrid;
import com.sbancuz.plannh.gui.common.SelectionPicker;

/** The selected extractor is an icon; its alternatives share the item picker's panel and layout. */
final class ExtractorWidget extends ButtonWidget<ExtractorWidget> {

    private final Node data;

    ExtractorWidget(final NodeWidget node) {
        data = node.getData();
        final SelectionPicker picker = new SelectionPicker(this, () -> {
            final List<ButtonWidget<?>> buttons = new ArrayList<>();
            for (final MachineProfile profile : data.getAvailableProfiles()) {
                final IDrawable icon = profile.getIcon(data.getHandlerRef());
                buttons.add(
                    new ButtonWidget<>().name("extractor.choice")
                        .size(SelectionGrid.SLOT)
                        .background(GuiTextures.SLOT_ITEM)
                        .overlay(
                            icon.asIcon()
                                .size(16))
                        .tooltipBuilder(tooltip -> profile.addIconTooltip(icon, tooltip))
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
        name("node.options.extractor");
        size(SelectionGrid.SLOT);
        background(GuiTextures.SLOT_ITEM);
        final MachineProfile selected = data.getSelectedProfile();
        final IDrawable icon = selected == null ? GuiTextures.PROCESSOR : selected.getIcon(data.getHandlerRef());
        overlay(
            icon.asIcon()
                .size(16));
        // TODO: Fix
        if (selected != null) tooltipBuilder(tooltip -> selected.addIconTooltip(icon, tooltip));
        onMousePressed(mouse -> {
            if (mouse != 0 && mouse != 1 || data.getAvailableProfiles()
                .isEmpty()) return false;
            picker.toggle();
            return true;
        });
    }
}
