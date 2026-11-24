package com.sbancuz.plannh.data.provider.bee;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.setting.BooleanSettingDef;
import com.sbancuz.plannh.data.setting.ChoiceSettingDef;
import com.sbancuz.plannh.data.setting.SettingDef;
import com.sbancuz.plannh.gui.GuiHelper;
import com.sbancuz.plannh.gui.LangScope;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;
import com.sbancuz.plannh.gui.tooltips.TooltipTheme;

import codechicken.nei.ItemList;
import codechicken.nei.recipe.StackInfo;
import forestry.api.apiculture.EnumBeeChromosome;
import forestry.api.apiculture.IHiveFrame;
import forestry.api.genetics.AlleleManager;
import forestry.api.genetics.IAllele;
import forestry.api.genetics.IAlleleFloat;
import forestry.api.genetics.IAlleleInteger;

public final class BeeSettings {

    private static final LangScope BEES = LangScope.ROOT.child("bees");

    public static final ChoiceSettingDef<String> SPEED = allele("bee_speed", EnumBeeChromosome.SPEED);
    public static final ChoiceSettingDef<String> LIFESPAN = allele("bee_lifespan", EnumBeeChromosome.LIFESPAN);

    public record AlleleBinding(ChoiceSettingDef<String> setting, EnumBeeChromosome chromosome) {}

    public static final List<AlleleBinding> ALLELES = List
        .of(new AlleleBinding(SPEED, EnumBeeChromosome.SPEED), new AlleleBinding(LIFESPAN, EnumBeeChromosome.LIFESPAN));

    public static final BooleanSettingDef MAGIC_PRODUCTION_AURA = aura("bee_magic_production_aura");
    public static final BooleanSettingDef MAGIC_LIFESPAN_AURA = aura("bee_magic_lifespan_aura");

    public static final List<ChoiceSettingDef<ItemStack>> FRAMES = List
        .of(frame("bee_frame_1"), frame("bee_frame_2"), frame("bee_frame_3"));

    private BeeSettings() {}

    private static BooleanSettingDef aura(final String key) {
        return new BooleanSettingDef(key, false) {

            @Override
            public IWidget settingsWidget(final MachineConfig cfg, final RecipeContext ctx,
                final Consumer<Runnable> edit) {
                return ((Widget<?>) super.settingsWidget(cfg, ctx, edit))
                    .tooltipBuilder(TooltipBuilder.build(BEES, out -> {
                        final boolean enabled = cfg.value(this, ctx);
                        out.entry(
                            IKey.str(getLabel()),
                            TooltipTheme.TUNABLE,
                            BEES.lang(enabled ? "aura_on" : "aura_off"))
                            .resetHint(cfg.isExplicit(this))
                            .shiftDetail(
                                detail -> detail.group(
                                    "requirements",
                                    rows -> rows.texts(
                                        "aura_booster_source",
                                        "aura_renewal",
                                        "aura_steady_state",
                                        "aura_cost",
                                        "aura_provider_limit")));
                    }))
                    .tooltipAutoUpdate(true);
            }
        };
    }

    private static ChoiceSettingDef<String> allele(final String key, final EnumBeeChromosome chromosome) {
        return new ChoiceSettingDef<>(
            key,
            "",
            String.class,
            () -> AlleleManager.alleleRegistry.getRegisteredAlleles(chromosome)
                .stream()
                .sorted(Comparator.comparing(IAllele::getUID))
                .map(IAllele::getUID)
                .toList(),
            uid -> {
                final IAllele allele = AlleleManager.alleleRegistry.getAllele(uid);
                return allele == null ? uid : allele.getName();
            }) {

            @Override
            public Consumer<TooltipBuilder> tooltip(final String uid) {
                return super.tooltip(uid).andThen(tooltip -> tooltip.scoped(BEES, out -> {
                    final IAllele allele = AlleleManager.alleleRegistry.getAllele(uid);
                    if (allele instanceof IAlleleFloat speed) {
                        out.entry("genetic_speed", TooltipTheme.TUNABLE, GuiHelper.multiple(speed.getValue()));
                    } else if (allele instanceof IAlleleInteger lifespan) {
                        out.entry("base_lifespan", TooltipTheme.VALUE, BEES.lang("work_cycles", lifespan.getValue()));
                    }

                    out.shiftDetail(
                        detail -> detail.group(
                            "calculation",
                            rows -> rows.texts("lifetime_model", "supplied_conditions", "unmodeled_effects")));
                }));
            }
        };
    }

    private static ChoiceSettingDef<ItemStack> frame(final String key) {
        return new ChoiceSettingDef<>(
            key,
            null,
            ItemStack.class,
            BeeSettings::frameChoices,
            stack -> stack == null ? StatCollector.translateToLocal("plannh.settings.bee_no_frame")
                : stack.getDisplayName()) {

            @Override
            public IWidget settingsWidget(final MachineConfig cfg, final RecipeContext ctx,
                final Consumer<Runnable> edit) {
                return new BeeFramesWidget(cfg, ctx, edit);
            }

            @Override
            public String getEditorLabel() {
                return StatCollector.translateToLocal("plannh.gui.bees.frames");
            }

            @Override
            public boolean isFullWidthEditor() {
                return true;
            }

            @Override
            public List<? extends SettingDef<?>> editedSettings() {
                return FRAMES;
            }

            @Override
            protected boolean acceptsItem(final ItemStack stack) {
                return stack != null && stack.getItem() instanceof IHiveFrame;
            }
        };
    }

    public static List<ItemStack> frameChoices() {
        final List<ItemStack> frames = new ArrayList<>();
        final Map<Item, Set<NBTTagCompound>> seen = new HashMap<>();
        for (final ItemStack stack : ItemList.items) {
            if (stack == null || !(stack.getItem() instanceof IHiveFrame)
                || !seen.computeIfAbsent(stack.getItem(), _ -> new HashSet<>())
                    .add(StackInfo.itemStackToNBT(stack, false)))
                continue;
            final ItemStack copy = stack.copy();
            copy.stackSize = 1;
            frames.add(copy);
        }
        frames.sort(Comparator.comparing(ItemStack::getDisplayName));
        return frames;
    }
}
