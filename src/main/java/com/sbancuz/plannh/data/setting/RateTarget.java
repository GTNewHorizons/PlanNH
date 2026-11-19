package com.sbancuz.plannh.data.setting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.sbancuz.plannh.data.flowchart.Port;
import com.sbancuz.plannh.gui.GuiHelper.RateUnit;
import com.sbancuz.plannh.gui.common.FlowchartFlow;
import com.sbancuz.plannh.gui.common.FlowchartTextWidget;
import com.sbancuz.plannh.gui.node.NodeWidget;
import com.sbancuz.plannh.gui.theme.PlannhThemes;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;
import com.sbancuz.plannh.gui.tooltips.TooltipTheme;

/**
 * The output-rate target: how much of each output a machine should produce, per second. Keyed by output index. An
 * output with no entry has no target.
 */
public final class RateTarget implements TargetKind {

    public static final double MAX_RATE = 1_000_000;

    private final SortedMap<Integer, Double> rates = new TreeMap<>();

    public double rate(final int output) {
        final Double rate = rates.get(output);
        return rate == null ? 0 : rate;
    }

    /** Remove the target when {@code rate} is zero or less. */
    public void setRate(final int output, final double rate) {
        if (rate <= 0) rates.remove(output);
        else rates.put(output, rate);
    }

    public void copyFrom(final RateTarget source) {
        rates.putAll(source.rates);
    }

    /**
     * The extent these targets imply. Every output of a machine shares one extent, so only the largest target is met
     * exactly.
     */
    public double extent(final double[] outQty) {
        final Live binding = binding(outQty);
        return binding == null ? 0 : binding.extent();
    }

    /** The target with the largest extent, the first one on a tie. Null when no target is live. */
    public @Nullable Live binding(final double[] outQty) {
        Live binding = null;
        for (final Live live : liveTargets(outQty)) {
            if (binding == null || live.extent() > binding.extent()) binding = live;
        }
        return binding;
    }

    /** Every target on an output with a quantity above zero, in output order. */
    public List<Live> liveTargets(final double[] outQty) {
        final List<Live> live = new ArrayList<>();
        for (final Map.Entry<Integer, Double> target : rates.entrySet()) {
            final int output = target.getKey();
            final Double rate = target.getValue();
            if (!(rate != null && rate > 0 && output >= 0 && output < outQty.length && outQty[output] > 0)) continue;
            live.add(new Live(output, rate, outQty[output]));
        }
        return live;
    }

    @Override
    public IWidget widget(final NodeWidget parent, final Consumer<Runnable> edit) {
        final IKey hint = IKey.lang("plannh.gui.node.target.rates.hint");
        return FlowchartFlow.row(parent)
            .name("target.rates.hint")
            .fullWidth()
            .coverChildrenHeight()
            .padding(2)
            .child(new FlowchartTextWidget(hint, parent).widgetTheme(PlannhThemes.CARD_MUTED));
    }

    /** Every target still in force, one row each. */
    @Override
    public void tooltip(final TooltipBuilder out, final NodeWidget parent) {
        final List<Port<?>> outputs = parent.getData()
            .getOutputs();
        for (final Map.Entry<Integer, Double> target : rates.entrySet()) {
            final int output = target.getKey();
            if (output < 0 || output >= outputs.size()) continue;
            final Port<?> port = outputs.get(output);
            final String amount = port.getType()
                .formatAmount(target.getValue());
            out.detail(port.getDisplayName(), TooltipTheme.Role.RATE, TooltipBuilder.rate(amount, RateUnit.SECONDS));
        }
    }

    /** A target on an output with a quantity above zero: {@code quantity} per operation, {@code rate} per second. */
    public record Live(int output, double rate, double quantity) {

        public double extent() {
            return rate / quantity;
        }
    }
}
