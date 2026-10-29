package com.sbancuz.plannh.gui.setting;

import java.util.function.Consumer;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.provider.bee.BeeSettings;
import com.sbancuz.plannh.data.setting.ChoiceSettingDef;
import com.sbancuz.plannh.gui.GuiHelper;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;
import com.sbancuz.plannh.gui.tooltips.TooltipTheme;

import forestry.api.apiculture.IHiveFrame;

/** Frame controls shared by Forestry and optional housing integrations. */
public final class BeeFramesWidget extends Flow {

    private static final int SLOT = 18;
    private static final int GAP = 3;

    private final MachineConfig cfg;
    private final RecipeContext ctx;
    private final Consumer<Runnable> edit;

    public BeeFramesWidget(final MachineConfig cfg, final RecipeContext ctx, final Consumer<Runnable> edit) {
        super(GuiAxis.X);
        this.cfg = cfg;
        this.ctx = ctx;
        this.edit = edit;

        name("bees.frames").childPadding(GAP)
            .coverChildren();
        for (int index = 0; index < BeeSettings.FRAMES.size(); index++) child(frame(index));
    }

    private ItemChoiceWidget frame(final int index) {
        final ChoiceSettingDef<ItemStack> frameDef = BeeSettings.FRAMES.get(index);
        return new ItemChoiceWidget(
            () -> cfg.value(frameDef, ctx),
            stack -> stack != null && stack.getItem() instanceof IHiveFrame,
            stack -> cfg.set(frameDef, stack),
            edit).picker(BeeSettings::frameChoices, () -> cfg.reset(frameDef))
                .name("bees.frame." + (index + 1))
                .size(SLOT)
                .hint("plannh.gui.bees.frame_hint")
                .tooltipBuilder(tooltip -> {
                    final TooltipBuilder out = TooltipBuilder.create(tooltip);
                    final ItemStack frame = cfg.value(frameDef, ctx);
                    out.entry(
                        IKey.lang("plannh.gui.bees.frame_slot"),
                        TooltipTheme.Role.IDENTITY,
                        Integer.toString(index + 1));
                    if (frame == null) out.langRow("plannh.gui.bees.frame_empty");
                    out.langRow("plannh.gui.bees.frame_reset");

                    if (GuiHelper.shiftHeld()) {
                        out.group(
                            "plannh.bees.calculation",
                            rows -> rows.langRows("plannh.gui.bees.frame_maintenance", "plannh.gui.bees.frame_order"));
                    } else out.langRow("plannh.bees.details_hint");
                    out.flush();
                });
    }
}
