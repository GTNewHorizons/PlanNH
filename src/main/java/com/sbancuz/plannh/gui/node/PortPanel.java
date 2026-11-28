package com.sbancuz.plannh.gui.node;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import javax.annotation.Nullable;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.text.TextRenderer;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.DoubleValue;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.effect.EffectResult;
import com.sbancuz.plannh.data.flowchart.Plan;
import com.sbancuz.plannh.data.flowchart.Port;
import com.sbancuz.plannh.data.flowchart.Summary;
import com.sbancuz.plannh.data.setting.RateTarget;
import com.sbancuz.plannh.gui.GuiHelper.RateUnit;
import com.sbancuz.plannh.gui.common.ItemSelectionGrid;
import com.sbancuz.plannh.gui.common.MiddleClickReset;
import com.sbancuz.plannh.gui.common.PickerContent;
import com.sbancuz.plannh.gui.common.ResetTextFieldWidget;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;

/**
 * The right-click panel of a port that a craft consumes or produces, with the port's alternatives and chance. An output
 * gets a target row when {@link NodeWidget#isRateTargeted} is true.
 */
public final class PortPanel extends Flow implements PickerContent {

    private static final int ROW_HEIGHT = 14;
    private static final int FIELD_WIDTH = 56;
    private static final int GAP = 4;
    private static final double PERCENT = 100;
    private static final String CHANCE_KEY = "plannh.gui.node.port.chance";
    private static final String TARGET_KEY = "plannh.gui.node.port.target";
    private static final String PERCENT_KEY = "plannh.gui.node.port.percent";

    private final NodeWidget parent;
    private final MachineConfig config;
    private final Port<?> port;
    private final boolean input;
    private final int index;
    private final int labelWidth;
    private final int sideWidth;
    private final int pickerWidth;
    private final int pickerHeight;

    /** The panel of {@code owner}, with the grid of {@code alternatives} on top when the port has alternatives. */
    public PortPanel(final NodeWidget parent, final PortWidget owner, @Nullable final ItemSelectionGrid alternatives) {
        super(GuiAxis.Y);
        name("port.panel");
        childPadding(GAP);
        crossAxisAlignment(Alignment.CrossAxis.START);

        this.parent = parent;
        config = owner.getNode()
            .getMachineConfig();
        port = owner.getPort();
        input = owner.isInput();
        index = owner.getIndex()
            .leftInt();

        labelWidth = widestText(List.of(CHANCE_KEY, TARGET_KEY));
        final List<String> sideKeys = new ArrayList<>(List.of(PERCENT_KEY));
        for (final RateUnit unit : RateUnit.VALUES) sideKeys.add(unit.suffixKey());
        sideWidth = widestText(sideKeys);

        int width = labelWidth + FIELD_WIDTH + sideWidth + 2 * GAP;
        int height = ROW_HEIGHT;
        if (alternatives != null) {
            child(alternatives);
            width = Math.max(width, alternatives.pickerWidth());
            height += alternatives.pickerHeight() + GAP;
        }
        child(chanceRow());

        if (!input && parent.isRateTargeted()) {
            child(targetRow());
            height += ROW_HEIGHT + GAP;
        }

        pickerWidth = width;
        pickerHeight = height;
        size(width, height);
    }

    @Override
    public int pickerWidth() {
        return pickerWidth;
    }

    @Override
    public int pickerHeight() {
        return pickerHeight;
    }

    private static int widestText(final Iterable<String> langKeys) {
        final FontRenderer font = TextRenderer.getFontRenderer();
        int widest = 0;
        for (final String langKey : langKeys) {
            widest = Math.max(widest, font.getStringWidth(StatCollector.translateToLocalFormatted(langKey)));
        }
        return widest;
    }

    /** The chance in percent. Middle-click to remove the user's chance. */
    private ResetRow chanceRow() {
        final double MAX_PERCENT = 1_000_000;
        final DoubleValue.Dynamic value = new DoubleValue.Dynamic(() -> {
            final EffectResult effect = parent.effectSnapshot();
            final double chance = effect == null ? port.getChance() : effect.multiplier(input, index);
            return chance * PERCENT;
        }, typed -> parent.commitEdit(() -> config.overrideChance(input, index, typed / PERCENT)));
        final TextFieldWidget field = new ResetTextFieldWidget().name("port.panel.chance")
            .size(FIELD_WIDTH, ROW_HEIGHT)
            .value(value)
            .numbersDouble(0, MAX_PERCENT);

        final ResetRow row = new ResetRow(
            () -> config.chanceOverride(input, index) != null,
            () -> parent.commitEdit(() -> config.overrideChance(input, index, null)));
        row.tooltipBuilder(
            tooltip -> row.addResetRows(TooltipBuilder.create(tooltip))
                .flush())
            .tooltipAutoUpdate(true);
        return row.fill(CHANCE_KEY, field, IKey.lang(PERCENT_KEY));
    }

    /** The rate target of this output in the plan's unit. Middle-click to remove the target. */
    private ResetRow targetRow() {
        final Summary summary = Plan.getInstance()
            .getSummary();
        final RateTarget rates = config.getRates();
        final DoubleValue.Dynamic value = new DoubleValue.Dynamic(
            () -> rates.rate(index) * summary.getRateUnit().secondsPerUnit,
            typed -> parent.commitEdit(() -> rates.setRate(index, typed / summary.getRateUnit().secondsPerUnit)));
        final TextFieldWidget field = new ResetTextFieldWidget().name("port.panel.target")
            .size(FIELD_WIDTH, ROW_HEIGHT)
            .value(value)
            .numbersDouble(0, RateTarget.MAX_RATE);

        final ResetRow row = new ResetRow(
            () -> rates.rate(index) > 0,
            () -> parent.commitEdit(() -> rates.setRate(index, 0)));
        row.tooltipBuilder(
            tooltip -> TooltipBuilder.create(tooltip)
                .langRow("plannh.settings.middle_reset_hint")
                .flush())
            .tooltipAutoUpdate(true);

        final IKey unit = IKey.dynamicKey(
            () -> IKey.lang(
                summary.getRateUnit()
                    .suffixKey()));
        return row.fill(TARGET_KEY, field, unit);
    }

    /** A row of the panel that a middle-click resets, like a settings row. */
    private final class ResetRow extends Flow implements MiddleClickReset {

        private final BooleanSupplier explicit;
        private final Runnable resetAction;

        private ResetRow(final BooleanSupplier explicit, final Runnable resetAction) {
            super(GuiAxis.X);
            this.explicit = explicit;
            this.resetAction = resetAction;
        }

        @Override
        public boolean isExplicit() {
            return explicit.getAsBoolean();
        }

        @Override
        public void reset() {
            resetAction.run();
        }

        /** Add the label, the field and the unit, in the columns of the panel. */
        private ResetRow fill(final String labelKey, final TextFieldWidget field, final IKey side) {
            name("port.panel.row").fullWidth()
                .height(ROW_HEIGHT)
                .childPadding(GAP)
                .child(new TextWidget<>(IKey.lang(labelKey)).size(labelWidth, ROW_HEIGHT))
                .child(field)
                .child(new TextWidget<>(side).size(sideWidth, ROW_HEIGHT));
            return this;
        }
    }
}
