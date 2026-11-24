package com.sbancuz.plannh.gui.tooltips;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import org.apache.commons.lang3.StringUtils;

import com.cleanroommc.modularui.api.MCHelper;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.sbancuz.plannh.Compat;
import com.sbancuz.plannh.gui.GuiHelper;
import com.sbancuz.plannh.gui.GuiHelper.RateUnit;
import com.sbancuz.plannh.gui.LangScope;
import com.slprime.chromatictooltips.api.TooltipLines;
import com.slprime.chromatictooltipscompat.CompatConfig;

import cpw.mods.fml.common.Loader;

/**
 * Writes the lines of a tooltip. {@link #build} returns the tooltip of a widget:
 *
 * <pre>
 * .tooltipBuilder(TooltipBuilder.build(PORT, out -> out.title("title").hint("configure_hint")))
 * </pre>
 *
 * A {@code String} argument for a line, a label, or a group header is the last part of a lang key. The
 * {@link LangScope} of the builder has the prefix of the key. An {@link IKey} argument is text that is ready to show,
 * and a value is display text. {@link TooltipTheme} has the colour of each kind of text.
 */
public final class TooltipBuilder {

    private static final String INDENT = "   ";

    // each level of indent narrows the text of a row
    private static final int MAX_INDENT = 4;

    private static final String COLON = ": ";
    private static final String LIST_SEPARATOR = ", ";
    private static final String MARK = "> ";
    private static final String NO_MARK = "  ";

    private static final LangScope TOOLTIP = LangScope.ROOT.child("gui.tooltip");
    private static final LangScope SETTINGS = LangScope.ROOT.child("settings");

    // When the MUI2 compat of ChromaticTooltipsCompat is on, `RichTooltipMixin` passes each line of a `RichTooltip` to
    // `TooltipHandler` of ChromaticTooltips, and `DividerConverter` turns a divider line into a divider component
    private static final boolean CHROMATIC = Loader.isModLoaded(Compat.IDs.CHROMATICTOOLTIPSCOMPAT)
        && CompatConfig.modularUI2Enabled;

    // `DividerConverter` matches a colour code, then three dashes or more
    private static final String CHROMATIC_DIVIDER = TooltipTheme.DIVIDER.prefix() + "---";
    // `MCHelper.getItemToolTip()` of MUI2 puts §7 before each line after the name, and `ModifierConverter` matches a
    // modifier marker such as `§!shift` only at the start of a line
    private static final String ITEM_LINE_PREFIX = EnumChatFormatting.GRAY.toString();
    private static final String MODIFIER_PREFIX = "§!";

    private LangScope scope;
    private final List<Consumer<RichTooltip>> rows = new ArrayList<>();
    private int indent;

    private TooltipBuilder(final LangScope scope) {
        this.scope = scope;
    }

    public static Consumer<RichTooltip> build(final Consumer<TooltipBuilder> body) {
        return build(LangScope.ROOT, body);
    }

    public static Consumer<RichTooltip> build(final LangScope scope, final Consumer<TooltipBuilder> body) {
        return tooltip -> {
            final TooltipBuilder out = new TooltipBuilder(scope);
            body.accept(out);
            for (final Consumer<RichTooltip> row : out.rows) row.accept(tooltip);
        };
    }

    /** Write the rows of {@code body}. The names that {@code body} passes are under {@code scope}. */
    public TooltipBuilder scoped(final LangScope scope, final Consumer<TooltipBuilder> body) {
        final LangScope outer = this.scope;
        this.scope = scope;
        body.accept(this);
        this.scope = outer;
        return this;
    }

    /**
     * Write the lines of the tooltip of {@code stack}. The first line is the name of the stack. When CHROMATIC is true,
     * strip the §7 before a modifier marker of ChromaticTooltips. ChromaticTooltips then puts the badge of the modifier
     * in the header.
     */
    public TooltipBuilder item(final ItemStack stack) {
        if (!CHROMATIC) {
            rows.add(tooltip -> tooltip.addFromItem(stack));
            return this;
        }

        rows.add(tooltip -> {
            final List<String> lines = MCHelper.getItemToolTip(stack);
            tooltip.addLine(lines.get(0));
            if (lines.size() > 1) tooltip.spaceLine();
            for (final String line : lines.subList(1, lines.size())) {
                final String bare = StringUtils.removeStart(line, ITEM_LINE_PREFIX);
                tooltip.addLine(bare.startsWith(MODIFIER_PREFIX) ? bare : line);
            }
        });
        return this;
    }

    public TooltipBuilder title(final String name, final Object... args) {
        return title(scope.lang(name, args));
    }

    public TooltipBuilder title(final IKey text) {
        return add(TooltipTheme.NAME.style(text));
    }

    /** One line of text for each name. */
    public TooltipBuilder texts(final String... names) {
        for (final String name : names) text(name);
        return this;
    }

    public TooltipBuilder text(final String name, final Object... args) {
        return line(TooltipTheme.BODY.style(scope.lang(name, args)));
    }

    public TooltipBuilder hint(final String name, final Object... args) {
        return line(TooltipTheme.HINT, scope.lang(name, args));
    }

    /** Write {@code text} at the current indent. */
    public TooltipBuilder line(final IKey text) {
        if (indent == 0) return add(text);
        return add(IKey.comp(IKey.str(INDENT.repeat(indent)), text));
    }

    public TooltipBuilder line(final TooltipTheme role, final IKey text) {
        return line(role.style(text));
    }

    /** Write an {@link #entry} one level further in than the current indent. */
    public TooltipBuilder detail(final IKey label, final TooltipTheme role, final String value) {
        indent();
        entry(label, role, value);
        return outdent();
    }

    public TooltipBuilder indent() {
        indent = Math.min(MAX_INDENT, indent + 1);
        return this;
    }

    public TooltipBuilder outdent() {
        indent = Math.max(0, indent - 1);
        return this;
    }

    public TooltipBuilder entry(final String name, final TooltipTheme role, final String value) {
        return entry(scope.lang(name), role, IKey.str(value));
    }

    public TooltipBuilder entry(final String name, final TooltipTheme role, final IKey value) {
        return entry(scope.lang(name), role, value);
    }

    public TooltipBuilder entry(final IKey label, final TooltipTheme role, final String value) {
        return entry(label, role, IKey.str(value));
    }

    public TooltipBuilder entry(final IKey label, final TooltipTheme role, final IKey value) {
        return line(IKey.comp(TooltipTheme.BODY.style(label), TooltipTheme.BODY.style(COLON), role.style(value)));
    }

    /** Write a label without a value, for a setting that is on. {@code flag()} skips a blank label. */
    public TooltipBuilder flag(final IKey label) {
        if (label.get()
            .isBlank()) return this;
        return line(TooltipTheme.BODY.style(label));
    }

    /** A row of a list of choices. The mark and the colour tell the choice in use from the others. */
    public TooltipBuilder marker(final IKey label, final boolean selected) {
        final TooltipTheme role = selected ? TooltipTheme.SELECTED : TooltipTheme.MUTED;
        return line(role.style(IKey.comp(IKey.str(selected ? MARK : NO_MARK), label)));
    }

    /**
     * Write a duration in {@code unit} and in ticks. When {@code allUnits} is true, write the duration in each unit of
     * {@link RateUnit#VALUES} that is a duration, then in ticks.
     */
    public TooltipBuilder duration(final IKey label, final int ticks, final RateUnit unit, final boolean allUnits) {
        final String inTicks = GuiHelper.formatDuration(ticks, RateUnit.TICKS);
        if (!allUnits) {
            final String inUnit = GuiHelper.formatDuration(ticks, unit);
            return entry(label, TooltipTheme.TIME, TOOLTIP.lang("duration", inUnit, inTicks));
        }

        final List<String> each = new ArrayList<>();
        for (final RateUnit duration : RateUnit.VALUES) {
            if (duration.duration) each.add(GuiHelper.formatDuration(ticks, duration));
        }
        each.add(inTicks);
        return entry(label, TooltipTheme.TIME, String.join(LIST_SEPARATOR, each));
    }

    /** Write a group with the rate in each unit of {@link RateUnit#VALUES} while shift is down. */
    public TooltipBuilder ratesInEveryUnit(final Function<RateUnit, String> rateIn) {
        return shiftDetail(
            detail -> detail.group(
                TOOLTIP.lang("per_unit"),
                rows -> {
                    for (final RateUnit unit : RateUnit.VALUES) rows.value(TooltipTheme.RATE, rateIn.apply(unit));
                }));
    }

    /**
     * Write the rows of {@code detail} while shift is down. When CHROMATIC is true, write the {@code §!shift} marker,
     * and ChromaticTooltips puts the [shift] badge in the header. Else write the shift hint while shift is up.
     */
    public TooltipBuilder shiftDetail(final Consumer<TooltipBuilder> detail) {
        final boolean held = GuiHelper.shiftHeld();
        if (CHROMATIC) rows.add(tooltip -> tooltip.addLine(TooltipLines.SHIFT_MODIFIER));
        if (held) detail.accept(this);
        else if (!CHROMATIC) line(TooltipTheme.HINT, TOOLTIP.lang("shift_hint"));
        return this;
    }

    public TooltipBuilder value(final TooltipTheme role, final String value) {
        return line(role.style(value));
    }

    public TooltipBuilder group(final String name, final Consumer<TooltipBuilder> body) {
        return group(scope.lang(name), body);
    }

    /**
     * Write a divider, the header, and the rows of {@code body}. The rows are one level in. When {@code body} doesn't
     * write a row, don't write the divider or the header. ChromaticTooltips centres the text of a divider with text, so
     * the header is a separate line at the left edge.
     */
    public TooltipBuilder group(final IKey header, final Consumer<TooltipBuilder> body) {
        final TooltipBuilder nested = new TooltipBuilder(scope);
        nested.indent = Math.min(MAX_INDENT, indent + 1);
        body.accept(nested);
        if (nested.rows.isEmpty()) return this;

        separator().line(TooltipTheme.NAME.style(header));
        rows.addAll(nested.rows);
        return this;
    }

    /** The divider between two groups, as wide as the tooltip. */
    public TooltipBuilder separator() {
        if (CHROMATIC) return add(IKey.str(CHROMATIC_DIVIDER));
        rows.add(tooltip -> tooltip.addLine(TooltipRule.INSTANCE));
        return this;
    }

    /** Write whether the user set the value. When the user set the value, write the hint for the middle-click reset. */
    public TooltipBuilder resetHint(final boolean explicit) {
        line(TooltipTheme.BODY.style(SETTINGS.lang(explicit ? "choice_manual" : "choice_auto")));
        if (explicit) line(TooltipTheme.HINT, SETTINGS.lang("middle_reset_hint"));
        return this;
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    private TooltipBuilder add(final IKey text) {
        rows.add(tooltip -> tooltip.addLine(text));
        return this;
    }
}
