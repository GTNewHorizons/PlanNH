package com.sbancuz.plannh.gui.node;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.sbancuz.plannh.data.effect.EffectResult;
import com.sbancuz.plannh.data.flowchart.Node;
import com.sbancuz.plannh.data.flowchart.Plan;
import com.sbancuz.plannh.data.flowchart.Port;
import com.sbancuz.plannh.data.flowchart.Summary;
import com.sbancuz.plannh.data.flowchart.Summary.PortDisplay;
import com.sbancuz.plannh.data.flowchart.balancer.Balancer;
import com.sbancuz.plannh.data.setting.RateTarget;
import com.sbancuz.plannh.gui.GuiHelper;
import com.sbancuz.plannh.gui.GuiHelper.RateUnit;
import com.sbancuz.plannh.gui.PlannhColors;
import com.sbancuz.plannh.gui.tooltips.TooltipTheme;
import com.sbancuz.plannh.mixins.GuiContainerManagerAccessor;

import codechicken.lib.vec.Rectangle4i;
import codechicken.nei.PositionedStack;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.item.ItemFluidDisplay;
import codechicken.nei.recipe.Badge;
import codechicken.nei.recipe.StackInfo;
import lombok.Getter;

/**
 * {@link PortSlot} draws the count and the chance badge of one port stack in the stack's NEI slot. When the port
 * display is {@link PortDisplay#RATE}, the count is the solved rate in the plan's unit. The rate of an output with a
 * rate target is coloured. When the port display is {@link PortDisplay#RECIPE}, or the stack is a {@code Tank} or
 * smaller than an item, {@code PositionedStack.draw()} draws the stack with the recipe's count.
 */
public final class PortSlot {

    private static final int ITEM_SIZE = 16;

    private final NodeWidget parent;
    private final PortWidget owner;
    private final PositionedStack stack;
    private final Rectangle4i badgeArea;
    private final int itemX;
    private final int itemY;
    private final boolean fitsRate;

    // what the slot was last built from, compared by identity
    private @Nullable Balancer.NodeBalance builtBalance;
    private @Nullable EffectResult builtEffect;
    private @Nullable PortDisplay builtDisplay;
    private @Nullable RateUnit builtUnit;
    private @Nullable ItemStack builtItem;

    private List<Badge> badges = List.of();
    private @Nullable String rateText;
    /** The mark of {@link #rateText}. {@code null} when {@link #rateText} doesn't have a colour. */
    @Getter
    private @Nullable TargetMark rateTarget;
    private float rateScale;
    private @Nullable ItemStack rateItem;

    public PortSlot(final NodeWidget parent, final PortWidget owner, final PositionedStack stack) {
        this.parent = parent;
        this.owner = owner;
        this.stack = stack;
        badgeArea = new Rectangle4i(stack.relx, stack.rely, stack.width, stack.height);

        // `PositionedStack.draw()` centres the 16 pixel item in the stack's area
        itemX = stack.relx + (stack.width - ITEM_SIZE) / 2;
        itemY = stack.rely + (stack.height - ITEM_SIZE) / 2;

        // `PositionedStack.Tank.draw()` draws a fluid gauge in place of an item. The count doesn't fit on a smaller
        // stack
        fitsRate = !(stack instanceof PositionedStack.Tank) && stack.width >= ITEM_SIZE && stack.height >= ITEM_SIZE;
    }

    /** Rebuild the count and the badges when the solve, the effect, the display, the unit or the item changed. */
    public void refresh() {
        final Balancer.NodeBalance balance = parent.balance();
        final EffectResult effect = parent.effectSnapshot();
        final Summary summary = Plan.getInstance()
            .getSummary();
        final PortDisplay display = summary.getPortDisplay();
        final RateUnit unit = summary.getRateUnit();
        final boolean unchanged = balance == builtBalance && effect == builtEffect
            && display == builtDisplay
            && unit == builtUnit
            && stack.item == builtItem;
        if (unchanged) return;

        builtBalance = balance;
        builtEffect = effect;
        builtDisplay = display;
        builtUnit = unit;
        builtItem = stack.item;

        final String rate = display == PortDisplay.RATE ? formatRate(balance, unit) : null;
        rateScale = rate == null ? 1 : countScale(rate);
        rateTarget = rate == null ? null : markTarget(effect);
        rateText = rateTarget == null ? rate : rateTarget.role.prefix() + rate;
        rateItem = rate == null ? null : itemWithoutAmount(stack.item);
        badges = chanceBadges(effect);
    }

    public boolean draw() {
        if (rateText == null) stack.draw(0, 0);
        else {
            GuiContainerManager.drawItem(itemX, itemY, rateItem, false, "");
            GuiContainerManagerAccessor.plannh$drawBigStackSize(itemX, itemY, rateText, rateScale);
        }
        for (Badge badge : badges) {
            badge.draw(badgeArea);
        }
        return true;
    }

    private @Nullable String formatRate(@Nullable final Balancer.NodeBalance balance, final RateUnit unit) {
        if (!fitsRate || owner.getPortType() == PortWidget.PortType.CATALYST) return null;
        final double perCycle = owner.perCycle(balance);
        if (perCycle <= 0) return null;

        final String rate = GuiHelper.formatSlotRate(GuiHelper.rate(perCycle, balance.durationPerOp(), unit));
        // NEI writes the amount of a fluid in liters: "144L"
        return StackInfo.isFluidDisplayItem(stack.item) ? rate + "L" : rate;
    }

    /** NEI's dynamic font size for a count of this many characters. */
    private static float countScale(final String count) {
        final int length = count.length();
        if (length <= 2) return 1f;
        if (length == 3) return 0.8f;
        if (length == 4) return 0.6f;
        return 0.5f;
    }

    /**
     * {@code FluidDisplayRenderer} writes the recipe amount of a fluid on the bottom edge beside the rate. For a fluid,
     * return a copy with an amount of 0. The renderer skips an amount of 0.
     */
    private static ItemStack itemWithoutAmount(final ItemStack item) {
        if (!StackInfo.isFluidDisplayItem(item)) return item;
        final FluidStack fluid = StackInfo.getFluid(item);
        final ItemStack empty = fluid == null ? null : ItemFluidDisplay.createStack(fluid.getFluid(), 0);
        return empty == null ? item : empty;
    }

    /**
     * The mark of an output with a rate target, when {@link NodeWidget#isRateTargeted} is true. {@code null} on any
     * other port, or when {@link RateTarget#binding} returns {@code null}.
     */
    private @Nullable TargetMark markTarget(@Nullable final EffectResult effect) {
        final Node node = owner.getNode();
        final RateTarget rates = node.getMachineConfig()
            .getRates();
        final int index = owner.getIndex()
            .leftInt();
        if (!(parent.isRateTargeted() && !owner.isInput() && rates.rate(index) > 0 && effect != null)) return null;

        final RateTarget.Live binding = rates.binding(effect.quantities(false, node.getOutputs()));
        if (binding == null) return null;
        return binding.output() == index ? TargetMark.MET : TargetMark.OVERSHOT;
    }

    /**
     * Return the badges of {@code RecipeWidget.getBadges()} with the effective chance in place of the recipe's chance.
     * A chance the user set gets {@link PlannhColors#WARNING}.
     */
    private List<Badge> chanceBadges(@Nullable final EffectResult effect) {
        final Port<?> port = owner.getPort();
        final boolean input = owner.isInput();
        final int index = owner.getIndex()
            .leftInt();
        final Double override = owner.getNode()
            .getMachineConfig()
            .chanceOverride(input, index);
        final List<Badge> handlerBadges = stack.getBadges();

        // a handler's badges, such as GT's NC*, are about the recipe chance. Show them only while the user hasn't set a
        // chance
        if (override == null && handlerBadges != null) return handlerBadges;
        if (input && port.getAmount() <= 0) return List.of(Badge.notConsumed());
        if (override == null && input && port.getChance() == 0) return List.of(Badge.notConsumedParallel());

        final double chance = effect == null ? port.getChance() : effect.multiplier(input, index);
        if (override == null && chance == 1) return List.of();
        final Badge badge = input ? Badge.consumeChance((float) chance) : Badge.outputChance((float) chance);
        if (override != null) badge.setColor(PlannhColors.WARNING.getColor())
            .setShadow(true);
        return List.of(badge);
    }

    /** The colour of a targeted rate and the tooltip line for the colour. */
    public enum TargetMark {

        /** The target of {@link RateTarget#binding}. The output makes this rate at the solved machine count. */
        MET(TooltipTheme.GOOD, "plannh.gui.node.port.target.met"),
        /** The output overshoots this target. The machine count for another target of the machine is higher. */
        OVERSHOT(TooltipTheme.CAUTION, "plannh.gui.node.port.target.overshot");

        @Getter
        private final TooltipTheme role;
        @Getter
        private final String langKey;

        TargetMark(final TooltipTheme role, final String langKey) {
            this.role = role;
            this.langKey = langKey;
        }
    }
}
