package com.sbancuz.plannh.gui.setting;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.provider.bee.GendustryProvider;
import com.sbancuz.plannh.data.setting.ChoiceSettingDef;
import com.sbancuz.plannh.data.setting.IntegerSettingDef;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;
import com.sbancuz.plannh.gui.tooltips.TooltipTheme;

/** Compact item-and-count editors for the Industrial Apiary's four upgrade slots. */
public final class GendustryUpgradesWidget extends Flow {

    private static final int SLOT = 18;
    private static final int GAP = 3;

    public GendustryUpgradesWidget(final MachineConfig cfg, final RecipeContext ctx, final Consumer<Runnable> edit,
        final List<ChoiceSettingDef<ItemStack>> upgrades, final List<? extends IntegerSettingDef> counts) {
        super(GuiAxis.X);
        name("gendustry.upgrades").childPadding(GAP)
            .coverChildren();
        for (int slot = 0; slot < upgrades.size(); slot++) {
            final int index = slot;
            final ChoiceSettingDef<ItemStack> upgrade = upgrades.get(index);
            final ItemChoiceWidget item = new ItemChoiceWidget(() -> {
                final ItemStack selected = cfg.value(upgrade, ctx);
                if (selected == null || selected.getItem() == null) return null;
                final ItemStack display = selected.copy();
                display.stackSize = GendustryProvider.configuredUpgradeCount(index, cfg, ctx);
                return display;
            },
                selected -> GendustryProvider.acceptsUpgrade(index, cfg, ctx, selected),
                selected -> GendustryProvider.selectUpgrade(index, cfg, ctx, selected),
                edit)
                    .picker(
                        () -> GendustryProvider.upgradeChoices(index, cfg, ctx),
                        () -> GendustryProvider.resetUpgrade(index, cfg))
                    .scrollHandler(direction -> {
                        final int current = GendustryProvider.configuredUpgradeCount(index, cfg, ctx);
                        final int next = GendustryProvider.nextUpgradeCount(index, cfg, ctx, direction);
                        if (next != current) edit.accept(() -> cfg.set(counts.get(index), next));
                    })
                    .name("gendustry.upgrade." + (index + 1) + ".item")
                    .size(SLOT)
                    .hint("plannh.settings.item_choice_hint")
                    .tooltipBuilder(tooltip -> {
                        final ItemStack selected = cfg.value(upgrade, ctx);
                        final TooltipBuilder out = TooltipBuilder.create(tooltip);
                        if (selected != null) out.entry(
                            IKey.lang("plannh.gui.gendustry.installed"),
                            TooltipTheme.Role.TUNABLE,
                            GendustryProvider.configuredUpgradeCount(index, cfg, ctx) + "/"
                                + Math.max(0, GendustryProvider.maximumUpgradeCount(index, cfg, ctx)));
                        out.langRow("plannh.gui.gendustry.upgrade_scroll")
                            .flush();
                    })
                    .tooltipAutoUpdate(true);
            child(item);
        }
    }
}
