package com.sbancuz.plannh.data.provider.bee;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import net.bdew.gendustry.api.ApiaryModifiers;
import net.bdew.gendustry.api.items.IApiaryUpgrade;
import net.bdew.gendustry.config.Fluids;
import net.bdew.gendustry.config.Tuning;
import net.bdew.gendustry.machines.apiary.MachineApiary;
import net.bdew.gendustry.machines.extractor.MachineExtractor;
import net.bdew.gendustry.machines.imprinter.MachineImprinter;
import net.bdew.gendustry.machines.liquifier.MachineLiquifier;
import net.bdew.gendustry.machines.mproducer.MachineMutagenProducer;
import net.bdew.gendustry.machines.mutatron.MachineMutatron;
import net.bdew.gendustry.machines.replicator.MachineReplicator;
import net.bdew.gendustry.machines.sampler.MachineSampler;
import net.bdew.gendustry.machines.transposer.MachineTransposer;
import net.bdew.gendustry.nei.ExtractorHandler;
import net.bdew.gendustry.nei.ImprinterHandler;
import net.bdew.gendustry.nei.LiquifierHandler;
import net.bdew.gendustry.nei.MutagenProducerHandler;
import net.bdew.gendustry.nei.MutatronHandler;
import net.bdew.gendustry.nei.ReplicatorHandler;
import net.bdew.gendustry.nei.SamplerHandler;
import net.bdew.gendustry.nei.TransposerHandler;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.gtnewhorizon.gtnhlib.util.data.ItemId;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.Effects;
import com.sbancuz.plannh.data.effect.steps.CoFHCompat;
import com.sbancuz.plannh.data.flowchart.Port;
import com.sbancuz.plannh.data.setting.ChoiceSettingDef;
import com.sbancuz.plannh.data.setting.IntegerSettingDef;
import com.sbancuz.plannh.data.setting.SettingDef;
import com.sbancuz.plannh.data.setting.Settings;
import com.sbancuz.plannh.gui.setting.GendustryUpgradesWidget;

import codechicken.nei.ItemList;
import codechicken.nei.recipe.IRecipeHandler;
import cpw.mods.fml.common.registry.GameRegistry;
import forestry.api.apiculture.DefaultBeeModifier;
import forestry.api.apiculture.IBeeGenome;
import forestry.core.config.Constants;
import forestry.plugins.PluginApiculture;

public final class GendustryProvider extends BeeProvider {

    private static final String PROFILE_ID = "gendustry:basic";
    private static final String INDUSTRIAL_APIARY_ID = "gendustry:industrial_apiary";
    private static final Map<Class<?>, String> ICONS = Map.of(
        MutatronHandler.class,
        "Mutatron",
        MutagenProducerHandler.class,
        "MutagenProducer",
        SamplerHandler.class,
        "Sampler",
        ImprinterHandler.class,
        "Imprinter",
        ExtractorHandler.class,
        "Extractor",
        LiquifierHandler.class,
        "Liquifier",
        ReplicatorHandler.class,
        "Replicator",
        TransposerHandler.class,
        "Transposer");
    private static final List<ChoiceSettingDef<ItemStack>> UPGRADES = List.of(
        upgrade("gendustry_apiary_upgrade_1"),
        upgrade("gendustry_apiary_upgrade_2"),
        upgrade("gendustry_apiary_upgrade_3"),
        upgrade("gendustry_apiary_upgrade_4"));
    private static final List<IntegerSettingDef> UPGRADE_COUNTS = List.of(
        new IntegerSettingDef("gendustry_apiary_upgrade_1_count", 1, 1, 8),
        new IntegerSettingDef("gendustry_apiary_upgrade_2_count", 1, 1, 8),
        new IntegerSettingDef("gendustry_apiary_upgrade_3_count", 1, 1, 8),
        new IntegerSettingDef("gendustry_apiary_upgrade_4_count", 1, 1, 8));

    private final MachineProfile PROFILE = MachineProfile.builder(PROFILE_ID)
        .setting(Settings.MACHINES)
        .setting(Settings.TICK_MODIFIER)
        .effect(
            Effects.durationFromHandler()
                .amortizeCost(CoFHCompat.RF_COST)
                .applyParallelism())
        .provider(this)
        .icon(this::icon)
        .build();
    private final MachineProfile INDUSTRIAL_APIARY = housingBuilder(
        INDUSTRIAL_APIARY_ID,
        new Housing(new DefaultBeeModifier(), 0, GendustryProvider::resolveUpgrades)).setting(UPGRADES.getFirst())
            .icon(this::icon)
            .build();

    public GendustryProvider() {
        super();
    }

    @Nonnull
    private IDrawable icon(final IRecipeHandler handler, final int recipeIndex) {
        String name = "IndustrialApiary";
        for (Class<?> type = handler.getClass(); type != null; type = type.getSuperclass()) {
            if (!ICONS.containsKey(type)) continue;
            name = ICONS.get(type);
            break;
        }
        final Block block = GameRegistry.findBlock("gendustry", name);
        return block == null ? GuiTextures.PROCESSOR : new ItemDrawable(block);
    }

    @Override
    public void register() {
        super.register();
        RecipePropertyAPI.registerExtractor(MutatronHandler.class, this);
        RecipePropertyAPI.registerExtractor(MutagenProducerHandler.class, this);
        RecipePropertyAPI.registerExtractor(SamplerHandler.class, this);
        RecipePropertyAPI.registerExtractor(ImprinterHandler.class, this);
        RecipePropertyAPI.registerExtractor(ExtractorHandler.class, this);
        RecipePropertyAPI.registerExtractor(LiquifierHandler.class, this);
        RecipePropertyAPI.registerExtractor(ReplicatorHandler.class, this);
        RecipePropertyAPI.registerExtractor(TransposerHandler.class, this);

    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        if (super.canCraft(handler, recipeIndex)) return List.of(INDUSTRIAL_APIARY);
        return switch (handler) {
            case MutatronHandler _, MutagenProducerHandler _, SamplerHandler _,
                 ImprinterHandler _, ExtractorHandler _, LiquifierHandler _,
                 ReplicatorHandler _, TransposerHandler _ -> List.of(PROFILE);
            default -> List.of();
        };
    }

    @Override
    public boolean canCraft(final IRecipeHandler handler, final int recipeIndex) {
        return !getProfiles(handler, recipeIndex).isEmpty();
    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler,
        final int recipeIndex) {
        if (super.canCraft(handler, recipeIndex)) {
            super.populate(recipe, handler, recipeIndex);
            return;
        }
        populateDefault(recipe, handler, recipeIndex);

        final float mj;
        final float rate;

        switch (handler) {
            case MutatronHandler _ -> {
                mj = MachineMutatron.mjPerItem();
                rate = MachineMutatron.powerUseRate() * MachineMutatron.maxStoredEnergy();
                recipe.input(Port.fluid(new FluidStack(Fluids.mutagen(), MachineMutatron.mutagenPerItem()), 1f, 32, 19));
            }
            case MutagenProducerHandler _ -> {
                mj = MachineMutagenProducer.mjPerItem();
                rate = MachineMutagenProducer.powerUseRate() * MachineMutagenProducer.maxStoredEnergy();
            }
            case SamplerHandler _ -> {
                mj = MachineSampler.mjPerItem();
                rate = MachineSampler.powerUseRate() * MachineSampler.maxStoredEnergy();
            }
            case ImprinterHandler _ -> {
                mj = MachineImprinter.mjPerItem();
                rate = MachineImprinter.powerUseRate() * MachineImprinter.maxStoredEnergy();
            }
            case ExtractorHandler _ -> {
                mj = MachineExtractor.mjPerItem();
                rate = MachineExtractor.powerUseRate() * MachineExtractor.maxStoredEnergy();
            }
            case LiquifierHandler _ -> {
                mj = MachineLiquifier.mjPerItem();
                rate = MachineLiquifier.powerUseRate() * MachineLiquifier.maxStoredEnergy();
            }
            case ReplicatorHandler _ -> {
                mj = MachineReplicator.mjPerItem();
                rate = MachineReplicator.powerUseRate() * MachineReplicator.maxStoredEnergy();
            }
            case TransposerHandler _ -> {
                mj = MachineTransposer.mjPerItem();
                rate = MachineTransposer.powerUseRate() * MachineTransposer.maxStoredEnergy();
            }
            default -> {
                return;
            }
        }

        // bdlib consumes powerUseRate * stored energy; use a continuously replenished full buffer.
        if (!Float.isFinite(rate) || rate <= 0 || !Float.isFinite(mj) || mj < 0)
            throw new IllegalArgumentException("Invalid Gendustry processing energy configuration");
        final int ticks = (int) Math.ceil((double) mj / rate);
        recipe.property(RecipePropertyAPI.DURATION_TICKS, Math.max(1, ticks));
        final float rfMjRatio = Tuning.getSection("Power").getFloat("RF_MJ_Ratio");
        if (!Float.isFinite(rfMjRatio) || rfMjRatio < 0)
            throw new IllegalArgumentException("Invalid Gendustry RF/MJ conversion");
        recipe.property(CoFHCompat.RF_COST, (long) (mj * rfMjRatio));
    }

    private static ChoiceSettingDef<ItemStack> upgrade(final String key) {
        return new ChoiceSettingDef<>(
            key,
            null,
            ItemStack.class,
            GendustryProvider::upgradeChoices,
            stack -> stack == null ? "-" : stack.getDisplayName()) {

            @Override
            public IWidget settingsWidget(final MachineConfig cfg, final RecipeContext ctx,
                final Consumer<Runnable> edit) {
                return new GendustryUpgradesWidget(cfg, ctx, edit, UPGRADES, UPGRADE_COUNTS);
            }

            @Override
            public String getEditorLabel() {
                return StatCollector.translateToLocal("plannh.gui.gendustry.upgrades");
            }

            @Override
            public boolean isFullWidthEditor() {
                return true;
            }

            @Override
            public List<? extends SettingDef<?>> editedSettings() {
                final List<SettingDef<?>> edited = new ArrayList<>(UPGRADES);
                edited.addAll(UPGRADE_COUNTS);
                return edited;
            }
        };
    }

    public static List<ItemStack> upgradeChoices() {
        final Map<ItemId, ItemStack> choices = new HashMap<>();
        for (final ItemStack stack : ItemList.items) {
            if (stack == null || stack.getItem() == null || !(stack.getItem() instanceof IApiaryUpgrade)) continue;
            final ItemStack copy = stack.copy();
            copy.stackSize = 1;
            choices.putIfAbsent(ItemId.create(copy.getItem(), copy.getItemDamage(), copy.getTagCompound()), copy);
        }
        return choices.values()
            .stream()
            .sorted(
                (left, right) -> left.getDisplayName()
                    .compareTo(right.getDisplayName()))
            .toList();
    }

    public static List<ItemStack> upgradeChoices(final int slot, final MachineConfig cfg, final RecipeContext ctx) {
        return upgradeChoices().stream()
            .filter(stack -> acceptsUpgrade(slot, cfg, ctx, stack))
            .toList();
    }

    public static boolean acceptsUpgrade(final int slot, final MachineConfig cfg, final RecipeContext ctx,
        final ItemStack candidate) {
        return candidate != null && candidate.getItem() instanceof IApiaryUpgrade
            && maximumUpgradeCount(slot, cfg, ctx, candidate) > 0;
    }

    public static int maximumUpgradeCount(final int slot, final MachineConfig cfg, final RecipeContext ctx) {
        final ItemStack selected = cfg.value(UPGRADES.get(slot), ctx);
        return maximumUpgradeCount(slot, cfg, ctx, selected);
    }

    public static int maximumUpgradeCount(final int slot, final MachineConfig cfg, final RecipeContext ctx,
        final ItemStack candidate) {
        if (candidate == null || !(candidate.getItem() instanceof IApiaryUpgrade)) return 8;
        final List<ItemStack> installed = new ArrayList<>();
        for (int index = 0; index < UPGRADES.size(); index++) {
            if (index == slot) continue;
            final ItemStack selected = cfg.value(UPGRADES.get(index), ctx);
            if (selected == null || !(selected.getItem() instanceof IApiaryUpgrade)) continue;
            final ItemStack copy = selected.copy();
            copy.stackSize = configuredCount(cfg, ctx, index);
            installed.add(copy);
        }
        return maximumUpgradeCount(candidate, installed);
    }

    private static int maximumUpgradeCount(final ItemStack candidate, final List<ItemStack> installed) {
        if (candidate == null || !(candidate.getItem() instanceof IApiaryUpgrade upgrade)) return 0;
        int maximum = Math.min(8, candidate.getMaxStackSize());
        final int nativeMaximum = upgrade.getMaxNumber(candidate);
        if (nativeMaximum <= 0) return 0;
        int sameTotal = 0;
        for (final ItemStack other : installed) {
            if (other == null || !(other.getItem() instanceof IApiaryUpgrade otherUpgrade)
                || otherUpgrade.getStackingId(other) != upgrade.getStackingId(candidate)) continue;
            sameTotal += other.stackSize;
            maximum = Math.min(maximum, otherUpgrade.getMaxNumber(other) - sameTotal);
        }
        return Math.min(maximum, nativeMaximum - sameTotal);
    }

    public static void selectUpgrade(final int slot, final MachineConfig cfg, final RecipeContext ctx,
        final ItemStack selected) {
        final ItemStack copy = selected.copy();
        copy.stackSize = 1;
        final int maximum = maximumUpgradeCount(slot, cfg, ctx, copy);
        if (maximum < 1) return;
        cfg.set(UPGRADES.get(slot), copy);
        cfg.set(UPGRADE_COUNTS.get(slot), Math.clamp(configuredCount(cfg, ctx, slot), 1, maximum));
    }

    public static void resetUpgrade(final int slot, final MachineConfig cfg) {
        cfg.reset(UPGRADES.get(slot));
        cfg.reset(UPGRADE_COUNTS.get(slot));
    }

    public static int configuredUpgradeCount(final int slot, final MachineConfig cfg, final RecipeContext ctx) {
        final int configured = configuredCount(cfg, ctx, slot);
        final int maximum = maximumUpgradeCount(slot, cfg, ctx);
        return maximum < 1 ? configured : Math.clamp(configured, 1, maximum);
    }

    public static int nextUpgradeCount(final int slot, final MachineConfig cfg, final RecipeContext ctx,
        final int direction) {
        final ItemStack selected = cfg.value(UPGRADES.get(slot), ctx);
        if (selected == null || !(selected.getItem() instanceof IApiaryUpgrade))
            return configuredUpgradeCount(slot, cfg, ctx);
        final int maximum = maximumUpgradeCount(slot, cfg, ctx);
        if (maximum < 1) return configuredUpgradeCount(slot, cfg, ctx);
        final int current = configuredUpgradeCount(slot, cfg, ctx);
        return Math.clamp(current + Integer.signum(direction), 1, maximum);
    }

    private static int configuredCount(final MachineConfig cfg, final RecipeContext ctx, final int slot) {
        return Math.clamp(cfg.value(UPGRADE_COUNTS.get(slot), ctx), 1, 8);
    }

    private static HousingExtras resolveUpgrades(final MachineConfig cfg, final RecipeContext ctx) {
        final ApiaryModifiers modifiers = readUpgrades(cfg, ctx);
        final double rfPerTick = rfPerTick(
            MachineApiary.baseMjPerTick(),
            modifiers.energy,
            Tuning.getSection("Power")
                .getFloat("RF_MJ_Ratio"));
        return new HousingExtras(
            List.of(new IndustrialBeeModifier(modifiers)),
            rfPerTick * PluginApiculture.ticksPerBeeWorkCycle,
            rfPerTick * Constants.APIARY_BREEDING_TIME);
    }

    private static ApiaryModifiers readUpgrades(final MachineConfig cfg, final RecipeContext ctx) {
        final ApiaryModifiers modifiers = new ApiaryModifiers();
        final List<ItemStack> selected = new ArrayList<>();
        for (int index = 0; index < UPGRADES.size(); index++) {
            final ItemStack selectedStack = cfg.value(UPGRADES.get(index), ctx);
            if (selectedStack == null || !(selectedStack.getItem() instanceof IApiaryUpgrade)) continue;
            final ItemStack stack = selectedStack.copy();
            final int maximum = maximumUpgradeCount(stack, selected);
            if (maximum < 1) continue;
            stack.stackSize = Math.clamp(configuredCount(cfg, ctx, index), 1, maximum);
            selected.add(stack);
        }
        validateUpgradeStacks(selected);
        for (final ItemStack stack : selected) ((IApiaryUpgrade) stack.getItem()).applyModifiers(modifiers, stack);
        return modifiers;
    }

    static void validateUpgradeStacks(final List<ItemStack> selected) {
        final Map<Long, Integer> installed = new HashMap<>();
        for (final ItemStack stack : selected) {
            if (stack == null || !(stack.getItem() instanceof IApiaryUpgrade upgrade))
                throw new IllegalArgumentException("Selected item is not a Gendustry apiary upgrade");
            if (stack.stackSize <= 0 || stack.stackSize > stack.getMaxStackSize())
                throw new IllegalArgumentException("Invalid Gendustry apiary upgrade stack");
            final int total = installed.merge(upgrade.getStackingId(stack), stack.stackSize, Integer::sum);
            if (total > upgrade.getMaxNumber(stack))
                throw new IllegalArgumentException("Gendustry apiary upgrade exceeds its limit");
        }
    }

    static double rfPerTick(final double baseMjPerTick, final double energy, final double rfPerMj) {
        if (!Double.isFinite(baseMjPerTick) || baseMjPerTick < 0
            || !Double.isFinite(energy)
            || energy < 0
            || !Double.isFinite(rfPerMj)
            || rfPerMj < 0) throw new IllegalArgumentException("Invalid Gendustry apiary energy configuration");
        return baseMjPerTick * energy * rfPerMj;
    }

    private static final class IndustrialBeeModifier extends DefaultBeeModifier {

        private final ApiaryModifiers modifiers;

        private IndustrialBeeModifier(final ApiaryModifiers modifiers) {
            this.modifiers = modifiers;
        }

        @Override
        public float getProductionModifier(final IBeeGenome genome, final float currentModifier) {
            return modifiers.production;
        }

        @Override
        public float getLifespanModifier(final IBeeGenome genome, final IBeeGenome mate, final float currentModifier) {
            return modifiers.lifespan;
        }
    }
}
