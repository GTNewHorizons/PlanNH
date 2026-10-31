package com.sbancuz.plannh.data.provider.bee;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.setting.ChoiceSettingDef;
import com.sbancuz.plannh.gui.setting.ItemChoiceWidget;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;
import com.sbancuz.plannh.gui.tooltips.TooltipTheme;

/** Compact item-and-count editors for the Industrial Apiary's four upgrade slots. */
public final class GendustryUpgradesWidget extends Flow {

    private static final int SLOT = 18;
    private static final int GAP = 3;

    public GendustryUpgradesWidget(final MachineConfig cfg, final RecipeContext ctx, final Consumer<Runnable> edit,
        final List<ChoiceSettingDef<ItemStack>> upgrades) {
        super(GuiAxis.X);
        name("gendustry.upgrades").childPadding(GAP)
            .coverChildren();
        for (int slot = 0; slot < upgrades.size(); slot++) {
            final int index = slot;
            final ChoiceSettingDef<ItemStack> upgrade = upgrades.get(index);
            final ItemChoiceWidget item = new ItemChoiceWidget(
                upgrade,
                () -> cfg.value(upgrade, ctx),
                selected -> GendustryProvider.acceptsUpgrade(index, cfg, ctx, selected),
                selected -> GendustryProvider.selectUpgrade(index, cfg, ctx, selected),
                edit)
                    .picker(
                        () -> GendustryProvider.upgradeChoices(index, cfg, ctx),
                        () -> GendustryProvider.resetUpgrade(index, cfg))
                    .scrollHandler(direction -> {
                        if (cfg.value(upgrade, ctx) == null) return;
                        final int current = upgrade.count(cfg, ctx);
                        final int next = Math.clamp(current + direction, 1, upgrade.maximumCount(cfg, ctx));
                        if (next != current) edit.accept(() -> upgrade.setCount(cfg, ctx, next));
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
                            upgrade.count(cfg, ctx) + "/" + Math.max(0, upgrade.maximumCount(cfg, ctx)));
                        out.langRow("plannh.settings.item_count_scroll")
                            .flush();
                    })
                    .tooltipAutoUpdate(true);
            child(item);
        }
    }
}
