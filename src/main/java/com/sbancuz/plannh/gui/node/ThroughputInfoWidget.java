package com.sbancuz.plannh.gui.node;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.EffectResult;
import com.sbancuz.plannh.data.flowchart.Node;
import com.sbancuz.plannh.data.flowchart.Plan;
import com.sbancuz.plannh.data.flowchart.Port;
import com.sbancuz.plannh.data.flowchart.balancer.BalanceMode;
import com.sbancuz.plannh.data.flowchart.balancer.BalanceResult;
import com.sbancuz.plannh.data.flowchart.balancer.Balancer;
import com.sbancuz.plannh.data.flowchart.balancer.Note;
import com.sbancuz.plannh.data.flowchart.balancer.Pin;
import com.sbancuz.plannh.data.flowchart.balancer.SolverMessage;
import com.sbancuz.plannh.data.properties.RecipeProperty;
import com.sbancuz.plannh.data.properties.SummaryProperty;
import com.sbancuz.plannh.data.setting.RateTarget;
import com.sbancuz.plannh.data.setting.SettingDef;
import com.sbancuz.plannh.data.setting.TargetKind;
import com.sbancuz.plannh.gui.GuiHelper;
import com.sbancuz.plannh.gui.GuiHelper.RateUnit;
import com.sbancuz.plannh.gui.LangScope;
import com.sbancuz.plannh.gui.common.FlowchartFlow;
import com.sbancuz.plannh.gui.common.FlowchartTextWidget;
import com.sbancuz.plannh.gui.common.FlowchartWidget;
import com.sbancuz.plannh.gui.common.IFlowchartDraggable;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;
import com.sbancuz.plannh.gui.tooltips.TooltipTheme;

public class ThroughputInfoWidget extends ParentWidget<ThroughputInfoWidget> implements IFlowchartDraggable {

    private static final LangScope THROUGHPUT = LangScope.ROOT.child("gui.node.throughput");
    private static final LangScope TARGET = LangScope.ROOT.child("gui.node.target");

    private final NodeWidget parent;
    private final Flow lines;

    private Flow row;
    private @Nullable Idle idle;

    private double lastOperations = Double.NaN;
    private int lastDuration = Integer.MIN_VALUE;

    private long lastVersion = Long.MIN_VALUE;
    private boolean lastShift;
    private RateUnit lastUnit;

    public ThroughputInfoWidget(NodeWidget parent) {
        this.parent = parent;
        name("node.info");
        fullWidth();
        coverChildrenHeight();

        lines = FlowchartFlow.col(parent)
            .name("node.info.lines")
            .fullWidth()
            .coverChildrenHeight();
        child(lines);

        rebuild();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        invalidateTooltip();
        rebuild();
    }

    @Override
    public FlowchartWidget<?, ?> getFlowchartParent() {
        return parent;
    }

    private void invalidateTooltip() {
        final long version = parent.getCanvas()
            .getGraph()
            .getVersion();
        final boolean shift = GuiHelper.shiftHeld();
        final RateUnit unit = Plan.getInstance()
            .getSummary()
            .getRateUnit();
        if (version == lastVersion && shift == lastShift && unit == lastUnit) return;
        lastVersion = version;
        lastShift = shift;
        lastUnit = unit;
        if (row != null) row.markTooltipDirty();
    }

    /** Rebuild the copies and duration row when the operations, the duration or the {@link Idle} reason changed. */
    private void rebuild() {
        final Node node = parent.getData();
        final Balancer.NodeBalance balance = parent.balance();
        final int duration = balance == null ? durationTicks(node, parent.effectSnapshot()) : balance.durationPerOp();
        final double operations = balance == null ? -1 : balance.operations();
        final Idle reason = balance != null && operations <= 0 ? readIdle(node) : null;

        if (operations == lastOperations && duration == lastDuration && reason == idle) return;
        lastOperations = operations;
        lastDuration = duration;
        idle = reason;

        final IKey copies;
        if (balance == null) copies = THROUGHPUT.lang("unbalanced");
        else if (idle != null) copies = THROUGHPUT.lang(idle.key);
        else copies = IKey.comp(THROUGHPUT.lang("copies_needed"), GuiHelper.multiple(operations));

        lines.removeAll();

        // the copy count or the idle reason, then the craft time. `writeTooltip` writes the details of both, and
        // `TargetFold` has the target of the machine
        row = FlowchartFlow.row(parent)
            .name("throughput")
            .fullWidth()
            .coverChildrenHeight()
            .mainAxisAlignment(Alignment.MainAxis.SPACE_BETWEEN)
            .childPadding(2)
            .tooltipBuilder(TooltipBuilder.build(this::writeTooltip))
            .child(new FlowchartTextWidget(copies, parent).name("node.info.copies"))
            .child(
                new FlowchartTextWidget(THROUGHPUT.lang("duration", GuiHelper.formatDuration(duration)), parent)
                    .name("node.info.duration")
                    .textAlign(Alignment.CenterRight));
        lines.child(row);

        scheduleResize();
    }

    /** Return the reason that this machine has 0 operations after the last balance. */
    private Idle readIdle(final Node node) {
        final BalanceResult result = parent.getCanvas()
            .getGraph()
            .balance();
        // `Balancer.balance` sets each machine to 0 operations when an AUTO solve stalls. The failure note has the
        // reason
        if (parent.balanceMode() == BalanceMode.AUTO && result instanceof BalanceResult.Fallback) {
            for (final Note note : result.notes()) {
                if (note.containsMessage(SolverMessage.NO_PIN)) return Idle.NO_TARGET;
            }
            return Idle.SOLVE_FAILED;
        }

        final MachineConfig cfg = node.getMachineConfig();
        final int copies = cfg.getCopies()
            .copies();
        if (cfg.isCopiesTargeted() && copies <= 0) return Idle.ZERO_COPIES;
        if (cfg.getTargetKind() != Pin.TARGET_RATE) return Idle.NOT_NEEDED;
        if (!parent.isRateTargeted()) return Idle.RATE_IGNORED;

        final RateTarget rates = cfg.getRates();
        final int outputCount = node.getOutputs()
            .size();
        for (int output = 0; output < outputCount; output++) {
            if (rates.rate(output) > 0) return Idle.NOT_NEEDED;
        }
        return Idle.NO_RATE;
    }

    private void writeTooltip(final TooltipBuilder out) {
        final Node node = parent.getData();
        final Balancer.NodeBalance balance = parent.balance();
        final boolean balanced = balance != null && balance.durationPerOp() > 0;
        final RateUnit unit = Plan.getInstance()
            .getSummary()
            .getRateUnit();
        final EffectResult effect = parent.effectSnapshot();
        final int ticks = balanced ? balance.durationPerOp() : durationTicks(node, effect);

        if (idle != null) writeIdle(out, idle);

        final IKey durationLabel = IKey.str(RecipePropertyAPI.DURATION_TICKS.displayName());
        if (ticks > 0) out.duration(durationLabel, ticks, unit, GuiHelper.shiftHeld());
        else out.text("gui.node.throughput.unbalanced");

        out.group(
            "summary.title.outputs",
            rows -> { if (balanced) ports(rows, node.getOutputs(), balance.effectiveOutputs(), ticks, unit); });
        out.group(
            "summary.title.inputs",
            rows -> { if (balanced) ports(rows, node.getInputs(), balance.effectiveInputs(), ticks, unit); });
        out.group("summary.title.properties", rows -> properties(rows, node, effect));

        writeTarget(out);
        out.shiftDetail(this::writeSettings);
    }

    /** Write what the state of {@code reason} means, then what the player can change. */
    private void writeIdle(final TooltipBuilder out, final Idle reason) {
        final String copies = StatCollector.translateToLocal(Pin.FIXED_COPIES.key());
        final String rate = StatCollector.translateToLocal(Pin.TARGET_RATE.key());
        final String targetRows = TARGET.translate("rows");
        final String copyCount = TARGET.translate("copies");
        final String balanceMode = LangScope.ROOT.translate("gui.toolbar.balance.title");
        final String modeName = parent.balanceMode()
            .displayName();
        final Object[] whyArgs = switch (reason) {
            case RATE_IGNORED -> new Object[] { modeName, rate };
            case NO_RATE -> new Object[] { rate };
            case NO_TARGET, SOLVE_FAILED, ZERO_COPIES, NOT_NEEDED -> new Object[0];
        };
        final Object[] fixArgs = switch (reason) {
            case NO_TARGET -> new Object[] { copies, rate, targetRows };
            case ZERO_COPIES -> new Object[] { copyCount, targetRows };
            case RATE_IGNORED -> new Object[] { balanceMode, BalanceMode.AUTO.displayName() };
            case SOLVE_FAILED, NO_RATE, NOT_NEEDED -> new Object[0];
        };

        out.scoped(THROUGHPUT, rows -> {
            rows.text(reason.key + ".why", whyArgs);
            rows.text(reason.key + ".fix", fixArgs);
        })
            .separator();
    }

    private void writeTarget(final TooltipBuilder out) {
        final List<Pin> held = parent.balanceMode()
            .pins()
            .stream()
            .filter(pin -> pin.hasWidget && pin != Pin.NONE)
            .toList();
        // A mode that pins nothing has nothing to choose between, so there is no fold to describe.
        if (held.isEmpty()) return;

        final MachineConfig cfg = parent.getData()
            .getMachineConfig();
        final Pin selected = cfg.getTargetKind();
        final TargetKind kind = TargetKind.of(cfg, selected);

        out.group(TARGET.lang("title"), rows -> {
            for (final Pin pin : held) rows.marker(IKey.lang(pin.key()), selected == pin);
            if (selected == Pin.NONE) rows.text("gui.node.target.none_hint");
            else if (kind != null) kind.tooltip(rows, parent);
        });
    }

    /** Every setting the machine has a row for, as the machine currently reads it. */
    @SuppressWarnings("unchecked")
    private void writeSettings(final TooltipBuilder out) {
        final Node node = parent.getData();
        final MachineConfig cfg = node.getMachineConfig();
        final RecipeContext ctx = node.recipeContext();

        out.group(THROUGHPUT.lang("settings"), rows -> {
            for (final SettingDef<?> def : visible(cfg, ctx)) {
                final Object value = cfg.getProfile()
                    .value(def, cfg, ctx);
                if (value != null) ((SettingDef<Object>) def).tooltip(value)
                    .accept(rows);
            }
        });
    }

    private static List<SettingDef<?>> visible(final MachineConfig cfg, final RecipeContext ctx) {
        return cfg.getProfile()
            .visibleSettings(cfg, ctx)
            .<SettingDef<?>>map(def -> def)
            .toList();
    }

    /**
     * One row per port actually moving: a port this craft does not move, or moves at zero, is not a
     * row.
     */
    private static void ports(final TooltipBuilder out, final List<Port<?>> ports, final Map<Integer, Double> perCycle,
        final int duration, final RateUnit unit) {
        for (int i = 0; i < ports.size(); i++) {
            final Double rate = perCycle.get(i);
            if (rate == null || rate <= 0) continue;
            final Port<?> port = ports.get(i);
            final String amount = port.getType()
                .formatAmount(GuiHelper.rate(rate, duration, unit));
            out.detail(IKey.str(port.getDisplayName()), TooltipTheme.RATE, GuiHelper.formatRate(amount, unit));
        }
    }

    /** Every summary property this machine actually spends, sorted so the list does not jump around. */
    @SuppressWarnings("unchecked")
    private static void properties(final TooltipBuilder out, final Node node, final EffectResult effect) {
        final Map<RecipeProperty<?>, Object> properties = effect == null ? node.getProperties()
            : node.displayProperties(effect);
        final List<SummaryProperty> found = new ArrayList<>();
        for (final Map.Entry<RecipeProperty<?>, Object> entry : properties.entrySet()) {
            if (!(entry.getKey() instanceof final SummaryProperty prop)) continue;
            if (!(entry.getValue() instanceof final Number num) || num.floatValue() == 0) continue;
            found.add(prop);
        }
        found.sort(Comparator.comparing(prop -> prop.formatDisplayName(prop.getDefaultValue())));

        for (final SummaryProperty prop : found) {
            final String name = prop.formatDisplayName(prop.getDefaultValue());
            final double value = ((Number) properties.get(prop)).doubleValue();
            // A property named per tick is energy, and the pack marks energy wherever it turns up.
            final TooltipTheme role = name.endsWith("/t") ? TooltipTheme.POWER : TooltipTheme.VALUE;
            out.detail(IKey.str(name), role, prop.formatAmount(value));
        }
    }

    /** The recipe's own craft time, which a machine knows before it has ever been solved. */
    private static int durationTicks(final Node node, final EffectResult effect) {
        if (effect != null) return Math.max(0, effect.durationTicks());
        final Object raw = node.getProperties().get(RecipePropertyAPI.DURATION_TICKS);
        return raw instanceof final Number n ? Math.max(0, n.intValue()) : 0;
    }

    /**
     * Each constant is a reason for 0 operations after a balance. The lang file has a row label and two tooltip rows
     * for each constant. Each key starts with the prefix of {@link #THROUGHPUT}.
     */
    private enum Idle {

        NO_TARGET("idle.no_target"),
        SOLVE_FAILED("idle.solve_failed"),
        ZERO_COPIES("idle.zero_copies"),
        RATE_IGNORED("idle.rate_ignored"),
        NO_RATE("idle.no_rate"),
        NOT_NEEDED("idle.not_needed");

        private final String key;

        private Idle(final String key) {
            this.key = key;
        }
    }
}
