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
import com.sbancuz.plannh.gui.PlannhColors;
import com.sbancuz.plannh.gui.theme.PlannhWidgetThemes;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;
import com.sbancuz.plannh.gui.tooltips.TooltipTheme;

import codechicken.nei.item.ItemFluidDisplay;

final class MeasureRow extends SummaryFlow {

    private static final float NAME_RATIO = 2 / 3f;

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
            .color((amount.isEmpty() ? PlannhColors.INFO : PlannhColors.TEXT)::getColor)
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
            .widgetTheme(PlannhWidgetThemes.ROW)
            .child(label)
            .child(
                new TextWidget<>(IKey.str(amount)).name("summary.measure.amount")
                    .color(PlannhColors.INFO::getColor)
                    .textAlign(Alignment.CenterRight)
                    .widthRel(1 - NAME_RATIO))
            .tooltipPos(RichTooltip.Pos.NEXT_TO_MOUSE)
            .tooltipBuilder(tooltip -> {
                if (hasItem) tooltip.addFromItem(stack);
                else tooltip.addLine(TooltipTheme.key(TooltipTheme.Role.LABEL, measure.displayName()));
                final TooltipBuilder out = TooltipBuilder.create(tooltip).separator();

                if (section == Summary.Section.MACHINE_COUNTS) {
                    out.entry(
                        IKey.lang("plannh.summary.tooltip.measure.workload"),
                        TooltipTheme.Role.IDENTITY,
                        TooltipBuilder.multiple(measure.amount() * scale));
                } else {
                    out.entry(
                        IKey.lang(rate ? "plannh.gui.node.port.rate"
                            : resourceRow ? "plannh.summary.tooltip.measure.per_cycle"
                                : "plannh.summary.tooltip.measure.amount"),
                        rate ? TooltipTheme.Role.RATE : TooltipTheme.Role.IDENTITY,
                        rate ? amount : raw);
                }

                if (rate) {
                    if (GuiHelper.shiftHeld()) {
                        out.group("plannh.gui.node.port.per_unit", rows -> {
                            for (final GuiHelper.RateUnit unit : GuiHelper.RateUnit.VALUES) {
                                rows.entry(IKey.EMPTY, TooltipTheme.Role.RATE,
                                    TooltipBuilder.rate(measure.displayAmount(measure.amount() * unit.secondsPerUnit), unit));
                            }
                        });
                    } else out.langRow("plannh.gui.node.port.shift_hint");
                }
                out.flush();
                if (measure.label() instanceof ResourceProperty<?> resource) {
                    resource.addTooltip(tooltip, measure.resource());
                }
            })
            .tooltipAutoUpdate(true);
    }
}
