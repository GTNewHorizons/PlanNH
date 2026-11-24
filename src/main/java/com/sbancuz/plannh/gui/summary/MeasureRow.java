package com.sbancuz.plannh.gui.summary;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.sbancuz.plannh.data.flowchart.Summary;
import com.sbancuz.plannh.data.flowchart.resource.LongFluidStack;
import com.sbancuz.plannh.data.flowchart.resource.LongItemStack;
import com.sbancuz.plannh.data.properties.ResourceProperty;
import com.sbancuz.plannh.gui.GuiHelper;
import com.sbancuz.plannh.gui.LangScope;
import com.sbancuz.plannh.gui.theme.PlannhThemes;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;
import com.sbancuz.plannh.gui.tooltips.TooltipTheme;

import codechicken.nei.item.ItemFluidDisplay;

final class MeasureRow extends SummaryFlow {

    private static final float NAME_RATIO = 2 / 3f;
    private static final LangScope SUMMARY = LangScope.ROOT.child("summary.tooltip.measure");

    MeasureRow(final Summary.Line.Measure<?> measure, final String suffix, final double scale,
        final Summary.Section section, final Summary.Mode mode) {
        super(GuiAxis.X);
        name("summary.measure");

        final String raw = measure.displayAmount(measure.amount() * scale);
        final String displaySuffix = section == Summary.Section.PROPERTIES ? "" : suffix;
        final String amount = raw.isEmpty() ? "" : raw + displaySuffix;
        final boolean resourceRow = section == Summary.Section.OUTPUTS || section == Summary.Section.INPUTS;
        final boolean rate = resourceRow && mode == Summary.Mode.THROUGHPUT;
        final ItemStack stack = switch (measure.resource()) {
            case LongItemStack item -> item.stack.copy();
            case LongFluidStack fluid -> ItemFluidDisplay.createStack(fluid.stack.getFluid(), 0);
            default -> null;
        };
        if (stack != null) stack.stackSize = 1;
        final boolean hasItem = stack != null && stack.getItem() != null;
        final TextWidget<?> text = new TextWidget<>(IKey.str(measure.displayName()))
            .name("summary.measure.name")
            .widgetTheme(amount.isEmpty() ? PlannhThemes.TEXT_INFO : PlannhThemes.TEXT)
            .textAlign(Alignment.CenterLeft);
        final Widget<?> label = resourceRow && hasItem
            ? SummaryFlow.row().widthRel(NAME_RATIO)
                .coverChildrenHeight()
                .childPadding(3)
                .child(new ItemDrawable(stack).asWidget().name("summary.measure.icon").size(12))
                .child(text.expanded())
            : text.widthRel(NAME_RATIO);

        fullWidth().coverChildrenHeight(SummaryBody.LINE_H)
            .paddingLeft(SummaryBody.TEXT_X)
            .paddingTop(2)
            .paddingBottom(2)
            .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
            .widgetTheme(PlannhThemes.ROW)
            .child(label)
            .child(
                new TextWidget<>(IKey.str(amount)).name("summary.measure.amount")
                    .widgetTheme(PlannhThemes.TEXT_INFO)
                    .textAlign(Alignment.CenterRight)
                    .widthRel(1 - NAME_RATIO))
            .tooltipPos(RichTooltip.Pos.NEXT_TO_MOUSE)
            .tooltipBuilder(TooltipBuilder.build(SUMMARY, out -> {
                if (hasItem) out.item(stack);
                else out.title(IKey.str(measure.displayName()));
                out.separator();

                if (section == Summary.Section.MACHINE_COUNTS) {
                    out.entry("load", TooltipTheme.VALUE, GuiHelper.multiple(measure.amount() * scale));
                } else if (rate) {
                    out.entry("rate", TooltipTheme.RATE, amount);
                    out.ratesInEveryUnit(unit -> {
                        final String perUnit = measure.displayAmount(measure.amount() * unit.secondsPerUnit);
                        return GuiHelper.formatRate(perUnit, unit);
                    });
                } else {
                    out.entry(resourceRow ? "per_cycle" : "amount", TooltipTheme.VALUE, raw);
                }

                if (measure.label() instanceof ResourceProperty<?> resource) {
                    resource.addTooltip(out, measure.resource());
                }
            }))
            .tooltipAutoUpdate(true);
    }
}
