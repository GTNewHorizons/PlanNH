package com.sbancuz.plannh.data.provider;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.Effects;
import com.sbancuz.plannh.data.flowchart.Port;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.properties.RecipeProperty;
import com.sbancuz.plannh.data.properties.ResourceProperty;
import com.sbancuz.plannh.data.properties.SummaryProperty;
import com.sbancuz.plannh.data.setting.Settings;

import WayofTime.alchemicalWizardry.ModBlocks;
import WayofTime.alchemicalWizardry.api.alchemy.energy.Reagent;
import WayofTime.alchemicalWizardry.client.nei.NEIAlchemyRecipeHandler;
import WayofTime.alchemicalWizardry.client.nei.NEIAlchemyRecipeHandler.CachedAlchemyRecipe;
import WayofTime.alchemicalWizardry.client.nei.NEIAltarRecipeHandler;
import WayofTime.alchemicalWizardry.client.nei.NEIAltarRecipeHandler.CachedAltarRecipe;
import WayofTime.alchemicalWizardry.client.nei.NEIBindingRitualHandler;
import WayofTime.alchemicalWizardry.client.nei.NEIBloodOrbShapedHandler;
import WayofTime.alchemicalWizardry.client.nei.NEIBloodOrbShapelessHandler;
import WayofTime.alchemicalWizardry.client.nei.NEICalcinatorHandler;
import WayofTime.alchemicalWizardry.client.nei.NEICalcinatorHandler.CachedReagentInfo;
import WayofTime.alchemicalWizardry.client.nei.NEIMeteorRecipeHandler;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;

public final class BloodMagicProvider implements PropertyProvider {

    // TODO: open an upstream PR to expose these copied machine constants.
    static final int ALCHEMY_TABLE_TICKS = 100;
    static final int CALCINATOR_TICKS = 200;
    static final int CALCINATOR_LP_AMOUNT = 2000;

    @Nonnull
    private IDrawable icon(final IRecipeHandler handler, final int recipeIndex) {
        if (handler instanceof NEIAltarRecipeHandler && ModBlocks.blockAltar != null)
            return new ItemDrawable(ModBlocks.blockAltar);
        if (handler instanceof NEIAlchemyRecipeHandler && ModBlocks.blockWritingTable != null)
            return new ItemDrawable(ModBlocks.blockWritingTable);
        if (handler instanceof NEICalcinatorHandler && ModBlocks.blockAlchemicCalcinator != null)
            return new ItemDrawable(ModBlocks.blockAlchemicCalcinator);
        return GuiTextures.PROCESSOR;
    }

    public static final RecipeProperty<Integer> LP_AMOUNT = SummaryProperty.builder("bloodmagic.lp_amount", 0)
        .build();

    public static final RecipeProperty<Integer> LP_TIER = RecipeProperty.builder("bloodmagic.lp_tier", 0)
        .build();

    /** Calcinator output is a reagent quantity, not an ordinary item count. */
    static final ResourceProperty<ReagentValue> REAGENT = ResourceProperty
        .<ReagentValue>builder("bloodmagic.reagent", new ReagentValue(new Reagent("unknown", 0, 0, 0, 0), 0))
        .displayFormatter(
            value -> value.reagent()
                .name())
        .amountExtractor(ReagentValue::amount)
        .amountUpdater((value, amount) -> value.amount(amount))
        .connectionChecker(
            (a, b) -> Objects.equals(
                a.getValue()
                    .reagent(),
                b.getValue()
                    .reagent()))
        .hashCodeExtractor(
            value -> value.reagent()
                .hashCode())
        .colorProvider(
            value -> value.reagent()
                .red() << 16
                | value.reagent()
                    .green() << 8
                | value.reagent()
                    .blue())
        .build();

    private final MachineProfile ALTAR = MachineProfile.builder("bloodmagic:altar")
        .setting(Settings.MACHINES)
        .setting(Settings.LP_PER_TICK)
        .setting(Settings.TICK_MODIFIER)
        .effect(
            Effects.durationFromTotal(LP_AMOUNT, Settings.LP_PER_TICK)
                .amortizeCost(LP_AMOUNT)
                .applyParallelism())
        .provider(this)
        .icon(this::icon)
        .build();

    private final MachineProfile ALCHEMY = MachineProfile.builder("bloodmagic:alchemy")
        .setting(Settings.MACHINES)
        .setting(Settings.TICK_MODIFIER)
        .effect(
            Effects.durationFromHandler()
                .amortizeCost(LP_AMOUNT)
                .applyParallelism())
        .provider(this)
        .icon(this::icon)
        .build();

    private final MachineProfile CALCINATOR = MachineProfile.builder("bloodmagic:calcinator")
        .setting(Settings.MACHINES)
        .setting(Settings.TICK_MODIFIER)
        .effect(
            Effects.durationFromHandler()
                .amortizeCost(LP_AMOUNT)
                .applyParallelism())
        .provider(this)
        .icon(this::icon)
        .build();

    @Nullable
    private static Field ALTAR_LP;

    @Nullable
    private static Field ALTAR_TIER;

    @Nullable
    private static Field ALCHEMY_LP;

    static {
        try {
            ALTAR_LP = CachedAltarRecipe.class.getDeclaredField("lp_amount");
            ALTAR_LP.setAccessible(true);
            ALTAR_TIER = CachedAltarRecipe.class.getDeclaredField("tier");
            ALTAR_TIER.setAccessible(true);
            ALCHEMY_LP = CachedAlchemyRecipe.class.getDeclaredField("lp");
            ALCHEMY_LP.setAccessible(true);
        } catch (final Exception ignored) {}
    }

    @Override
    public StackRole otherStackRole(final IRecipeHandler handler, final int recipeIndex, final PositionedStack stack) {
        // The blood orb is retained by the alchemy table, not produced or consumed per operation.
        return handler instanceof NEIAlchemyRecipeHandler ? StackRole.IGNORE
            : PropertyProvider.super.otherStackRole(handler, recipeIndex, stack);
    }

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(NEIAlchemyRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(NEIAltarRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(NEIBindingRitualHandler.class, this);
        RecipePropertyAPI.registerExtractor(NEIBloodOrbShapedHandler.class, this);
        RecipePropertyAPI.registerExtractor(NEIBloodOrbShapelessHandler.class, this);
        RecipePropertyAPI.registerExtractor(NEICalcinatorHandler.class, this);
        RecipePropertyAPI.registerExtractor(NEIMeteorRecipeHandler.class, this);

    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        return switch (handler) {
            case NEIAlchemyRecipeHandler _ -> List.of(ALCHEMY);
            case NEIAltarRecipeHandler _ -> List.of(ALTAR);
            case NEICalcinatorHandler _ -> List.of(CALCINATOR);
            default -> List.of();
        };
    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler,
        final int recipeIndex) {
        PropertyProvider.super.populate(recipe, handler, recipeIndex);
        if (!(handler instanceof final TemplateRecipeHandler trh)) return;

        final List<TemplateRecipeHandler.CachedRecipe> recipes = trh.arecipes;
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) return;

        final TemplateRecipeHandler.CachedRecipe cached = recipes.get(recipeIndex);

        if (cached instanceof final CachedAltarRecipe r) {
            final int lp = getIntField(ALTAR_LP, r);
            final int tier = getIntField(ALTAR_TIER, r);
            if (lp > 0) recipe.property(LP_AMOUNT, lp);
            if (tier > 0) recipe.property(LP_TIER, tier);
        } else if (cached instanceof final CachedAlchemyRecipe r) {
            final int lp = getIntField(ALCHEMY_LP, r);
            if (lp > 0) recipe.property(LP_AMOUNT, lp);
            recipe.property(RecipePropertyAPI.DURATION_TICKS, ALCHEMY_TABLE_TICKS);
        } else if (cached instanceof final CachedReagentInfo r) {
            recipe.property(LP_AMOUNT, CALCINATOR_LP_AMOUNT);
            recipe.property(RecipePropertyAPI.DURATION_TICKS, CALCINATOR_TICKS);
            final PositionedStack display = r.getResult();
            if (display != null && display.item != null) {
                if (!recipe.outputs().isEmpty()) recipe.outputs().remove(recipe.outputs().size() - 1);
                recipe.output(reagentPort(r.getReagent(), r.getAmount(), display));
            }
        }
    }

    @Nonnull
    static Port<ReagentValue> reagentPort(final Reagent reagent, final int amount, final PositionedStack display) {
        return new Port<>(REAGENT, new ReagentValue(reagent, amount), 1f, display.copy());
    }

    static final class ReagentValue {

        private final Reagent reagent;
        private long amount;

        ReagentValue(final Reagent reagent, final long amount) {
            this.reagent = Objects.requireNonNull(reagent);
            this.amount = amount;
        }

        Reagent reagent() {
            return reagent;
        }

        long amount() {
            return amount;
        }

        void amount(final long newAmount) {
            amount = newAmount;
        }
    }

    private static int getIntField(@Nullable final Field field, final Object instance) {
        if (field == null) return 0;
        try {
            return field.getInt(instance);
        } catch (final Exception ignored) {
            return 0;
        }
    }
}
