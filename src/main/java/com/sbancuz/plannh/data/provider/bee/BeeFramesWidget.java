package com.sbancuz.plannh.data.provider.bee;

import java.util.function.Consumer;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.setting.ChoiceSettingDef;
import com.sbancuz.plannh.gui.LangScope;
import com.sbancuz.plannh.gui.setting.ItemChoiceWidget;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;
import com.sbancuz.plannh.gui.tooltips.TooltipTheme;

import forestry.api.apiculture.IHiveFrame;

/** Frame controls shared by Forestry and optional housing integrations. */
public final class BeeFramesWidget extends Flow {

    private static final LangScope BEES = LangScope.ROOT.child("gui.bees");

    private static final int SLOT = 18;
    private static final int GAP = 3;

    public BeeFramesWidget(final MachineConfig cfg, final RecipeContext ctx, final Consumer<Runnable> edit) {
        super(GuiAxis.X);
        name("bees.frames").childPadding(GAP)
            .coverChildren();

        children(BeeSettings.FRAMES.size(), index -> {
            final ChoiceSettingDef<ItemStack> frameDef = BeeSettings.FRAMES.get(index);
            return new ItemChoiceWidget(
                frameDef,
                () -> cfg.value(frameDef, ctx),
                stack -> stack != null && stack.getItem() instanceof IHiveFrame,
                stack -> cfg.set(frameDef, stack),
                edit).picker(BeeSettings::frameChoices, () -> cfg.reset(frameDef))
                    .name("bees.frame." + (index + 1))
                    .size(SLOT)
                    .hint("plannh.gui.bees.frame_hint")
                    .tooltipBuilder(TooltipBuilder.build(BEES, out -> {
                        out.entry("frame_slot", TooltipTheme.VALUE, Integer.toString(index + 1));
                        if (cfg.value(frameDef, ctx) == null) out.text("frame_empty");
                        out.hint("frame_reset")
                            .shiftDetail(
                                detail -> detail.group(
                                    IKey.lang("plannh.bees.calculation"),
                                    rows -> rows.texts("frame_maintenance", "frame_order")));
                    }));
        });
    }
}
