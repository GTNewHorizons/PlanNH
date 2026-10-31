package com.sbancuz.plannh.data.provider;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.Effects;
import com.sbancuz.plannh.data.effect.steps.CoFHCompat;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.properties.RecipeProperty;
import com.sbancuz.plannh.data.setting.ChoiceSettingDef;
import com.sbancuz.plannh.data.setting.Settings;
import com.sbancuz.plannh.gui.setting.ItemChoiceWidget;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.FurnaceRecipeHandler;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.ShapedRecipeHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;
import crazypants.enderio.EnderIO;
import crazypants.enderio.config.Config;
import crazypants.enderio.machine.MachineRecipeInput;
import crazypants.enderio.machine.alloy.AlloyRecipeManager;
import crazypants.enderio.nei.AlloySmelterRecipeHandler;
import crazypants.enderio.nei.AlloySmelterRecipeHandler.AlloySmelterRecipe;
import crazypants.enderio.nei.EnchanterRecipeHandler;
import crazypants.enderio.nei.SagMillRecipeHandler;
import crazypants.enderio.nei.SagMillRecipeHandler.MillRecipe;
import crazypants.enderio.nei.SliceAndSpliceRecipeHandler;
import crazypants.enderio.nei.SliceAndSpliceRecipeHandler.SliceAndSpliceRecipe;
import crazypants.enderio.nei.SoulBinderRecipeHandler;
import crazypants.enderio.nei.SoulBinderRecipeHandler.SoulBinderRecipeNEI;
import crazypants.enderio.nei.VatRecipeHandler;
import crazypants.enderio.nei.VatRecipeHandler.InnerVatRecipe;
import crazypants.enderio.power.Capacitors;

public class EnderIOProvider implements PropertyProvider {

    private static final int MAX_CAPACITOR_TIER = 6;

    public static final ChoiceSettingDef<Integer> CAPACITOR = new ChoiceSettingDef<>(
        "eio_capacitor",
        0,
        Integer.class,
        () -> IntStream.rangeClosed(0, MAX_CAPACITOR_TIER)
            .boxed()
            .toList(),
        tier -> Integer.toString(tier)) {

        @Override
        public IWidget settingsWidget(final MachineConfig cfg, final RecipeContext ctx, final Consumer<Runnable> edit) {
            return new ItemChoiceWidget(
                this,
                cfg,
                ctx,
                edit,
                () -> EnderIO.itemBasicCapacitor == null ? List.of()
                    : Arrays.stream(Capacitors.VALUES, 0, MAX_CAPACITOR_TIER + 1)
                        .map(Capacitors::getItemStack)
                        .toList(),
                tier -> EnderIO.itemBasicCapacitor == null ? null
                    : Capacitors.VALUES[Math.clamp(tier, 0, MAX_CAPACITOR_TIER)].getItemStack(),
                stack -> {
                    if (stack == null || stack.getItem() == null || stack.getItem() != EnderIO.itemBasicCapacitor)
                        return null;
                    final int metadata = stack.getItemDamage();
                    if (metadata >= 0 && metadata <= MAX_CAPACITOR_TIER) return metadata;
                    return switch (metadata) {
                        case 7, 8, 9 -> metadata - 7;
                        default -> null;
                    };
                });
        }
    };

    private static final Map<Class<?>, Supplier<Block>> ICONS = Map.of(
        AlloySmelterRecipeHandler.class,
        () -> EnderIO.blockAlloySmelter,
        FurnaceRecipeHandler.class,
        () -> EnderIO.blockAlloySmelter,
        SagMillRecipeHandler.class,
        () -> EnderIO.blockCrusher,
        EnchanterRecipeHandler.class,
        () -> EnderIO.blockEnchanter,
        SliceAndSpliceRecipeHandler.class,
        () -> EnderIO.blockSliceAndSplice,
        SoulBinderRecipeHandler.class,
        () -> EnderIO.blockSoulFuser,
        VatRecipeHandler.class,
        () -> EnderIO.blockVat);

    @Nonnull
    private IDrawable icon(final IRecipeHandler handler, final int recipeIndex) {
        if (handler != null) {
            for (final Map.Entry<Class<?>, Supplier<Block>> entry : ICONS.entrySet()) {
                if (entry.getKey()
                    .isAssignableFrom(handler.getClass())) {
                    final Block block = entry.getValue()
                        .get();
                    if (block != null) return new ItemDrawable(block);
                    break;
                }
            }
        }
        return GuiTextures.PROCESSOR;
    }

    private static final String PROFILE_ID = "enderio";
    private static final int[] CAPACITOR_RF_PER_T = Arrays.stream(Capacitors.VALUES)
        .limit(MAX_CAPACITOR_TIER + 1L)
        .mapToInt(capacitor -> capacitor.capacitor.getMaxEnergyExtracted())
        .toArray();

    static final RecipeProperty<Integer> FIXED_RF_PER_TICK = RecipeProperty
        .<Integer>builder("enderio.fixed_rf_per_tick", 0)
        .build();

    private static long energy(final RecipeContext ctx) {
        final Number cost = ctx.getOrDefault(CoFHCompat.RF_COST, 0L);
        return cost.longValue();
    }

    public static final RecipeProperty<Integer> EXPERIENCE = RecipeProperty.<Integer>builder("enderio.experience", 0)
        .build();

    @Nullable
    private static Field MILL_OUTPUT_CHANCE;

    private final MachineProfile profile = MachineProfile.builder(PROFILE_ID)
        .setting(Settings.MACHINES)
        .setting(Settings.TICK_MODIFIER)
        .setting(CAPACITOR, (cfg, ctx) -> energy(ctx) > 0 && ctx.getOrDefault(FIXED_RF_PER_TICK, null) == null)
        .setting(Settings.RF_PER_TICK, (cfg, ctx) -> energy(ctx) > 0)
        .setting(Settings.DURATION_TICKS, (cfg, ctx) -> energy(ctx) > 0)
        .derive(Settings.RF_PER_TICK, (cfg, ctx) -> {
            final Integer fixed = ctx.getOrDefault(FIXED_RF_PER_TICK, null);
            if (fixed != null) return fixed;
            final int capacitor = cfg.getOrDefault(CAPACITOR);
            final int index = Math.clamp(capacitor, 0, CAPACITOR_RF_PER_T.length - 1);
            return CAPACITOR_RF_PER_T[index];
        })
        .derive(Settings.DURATION_TICKS, (cfg, ctx) -> {
            final long cost = energy(ctx);
            final int rate = cfg.getProfile()
                .value(Settings.RF_PER_TICK, cfg, ctx);
            if (cost <= 0 || rate <= 0) return 0;
            return (int) Math.min(Integer.MAX_VALUE, (cost - 1) / rate + 1);
        })
        .effect(
            Effects.durationFromSetting(Settings.DURATION_TICKS)
                .withCostPerT(
                    CoFHCompat.RF_PER_T,
                    (current, cfg, ctx) -> energy(ctx) > 0 ? (long) cfg.getProfile()
                        .value(Settings.RF_PER_TICK, cfg, ctx) : 0L)
                .applyParallelism())
        .icon(this::icon)
        .provider(this)
        .build();

    private final MachineProfile crafterProfile = MachineProfile.builder("enderio:crafter")
        .setting(Settings.MACHINES)
        .setting(Settings.TICK_MODIFIER)
        .setting(CAPACITOR)
        // TODO: Open an upstream PR to expose TileCrafter tick intervals and completion semantics instead of copying
        // them here.
        .effect(Effects.duration((cfg, ctx) -> {
            final int capacitor = Math.clamp(cfg.value(CAPACITOR, ctx), 0, MAX_CAPACITOR_TIER);
            final int interval;
            if (capacitor == 0) interval = 20;
            else if (capacitor == 1) interval = 10;
            else interval = 2;
            // TileCrafter checks ticksSinceLastCraft <= ticksPerCraft, so completion is one tick later.
            return interval + 1;
        })
            .withTotalCost(CoFHCompat.RF_COST, (current, cfg, ctx) -> Config.crafterRfPerCraft)
            .withCostPerT(CoFHCompat.RF_PER_T, (current, cfg, ctx) -> current.energyPerT())
            .applyParallelism())
        .icon(
            (handler, recipeIndex) -> EnderIO.blockCrafter == null ? GuiTextures.PROCESSOR
                : new ItemDrawable(EnderIO.blockCrafter))
        .provider(this)
        .build();

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(AlloySmelterRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(SagMillRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(VatRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(EnchanterRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(SliceAndSpliceRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(SoulBinderRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(FurnaceRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(ShapedRecipeHandler.class, this);
        registerOptionalExtractor("appeng.integration.modules.NEIHelpers.NEIAEShapedRecipeHandler");
        registerOptionalExtractor("appeng.integration.modules.NEIHelpers.NEIAEShapelessRecipeHandler");

        Field f = null;
        try {
            f = MillRecipe.class.getDeclaredField("outputChance");
            f.setAccessible(true);
        } catch (final Exception ignored) {}
        MILL_OUTPUT_CHANCE = f;
    }

    private void registerOptionalExtractor(final String className) {
        try {
            RecipePropertyAPI.registerExtractor(Class.forName(className), this);
        } catch (final ReflectiveOperationException | LinkageError ignored) {}
    }

    @Override
    public boolean canCraft(final IRecipeHandler handler, final int recipeIndex) {
        if (isAlloySmelting(handler, recipeIndex) || new CraftingRecipeSupport(handler, recipeIndex).isEligible())
            return true;
        if (!(handler instanceof TemplateRecipeHandler trh)) return false;
        final String overlay = trh.getOverlayIdentifier();
        return overlay != null && (overlay.startsWith("EnderIO") || overlay.equals("EIOEnchanter"));
    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        if (new CraftingRecipeSupport(handler, recipeIndex).isEligible()) return List.of(crafterProfile);
        return canCraft(handler, recipeIndex) ? List.of(profile) : List.of();
    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler,
        final int recipeIndex) {
        final CraftingRecipeSupport crafting = new CraftingRecipeSupport(handler, recipeIndex);
        final boolean isCrafting = crafting.isEligible();
        if (isCrafting) crafting.selectMissingPermutations();
        PropertyProvider.super.populate(recipe, handler, recipeIndex);
        if (isCrafting) {
            crafting.addContainerReturns(recipe);
            recipe.property(CoFHCompat.RF_COST, (long) Config.crafterRfPerCraft);
            return;
        }
        if (!(handler instanceof final TemplateRecipeHandler trh)) return;

        final List<TemplateRecipeHandler.CachedRecipe> recipes = trh.arecipes;
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) return;

        final TemplateRecipeHandler.CachedRecipe cached = recipes.get(recipeIndex);

        if (cached instanceof final AlloySmelterRecipe r) {
            applyEnergy(recipe, r.getEnergy());
        } else if (cached instanceof final MillRecipe r) {
            applyEnergy(recipe, r.getEnergy());
            applyMillChances(recipe, r);
        } else if (cached instanceof final SliceAndSpliceRecipe r) {
            applyEnergy(recipe, r.getEnergy());
        } else if (cached instanceof final SoulBinderRecipeNEI r) {
            applyEnergy(recipe, r.getEnergy());
            if (r.getExperience() > 0) recipe.property(EXPERIENCE, r.getExperience());
        } else if (cached instanceof final InnerVatRecipe r) {
            applyEnergy(recipe, r.getEnergy());
            recipe.property(FIXED_RF_PER_TICK, Config.vatPowerUserPerTickRF);
        }

        if (isAlloySmelting(handler, recipeIndex)) {
            final PositionedStack input = firstIngredient(handler, recipeIndex);
            if (input != null && input.item != null) {
                recipe.property(
                    CoFHCompat.RF_COST,
                    (long) AlloyRecipeManager.getInstance()
                        .getVanillaRecipe()
                        .getEnergyRequired(MachineRecipeInput.create(0, input.item)));
            }
        }
    }

    private static boolean isAlloySmelting(final IRecipeHandler handler, final int recipeIndex) {
        if (!(handler instanceof FurnaceRecipeHandler furnace) || !"smelting".equals(furnace.getOverlayIdentifier()))
            return false;
        final PositionedStack input;
        final PositionedStack result;
        try {
            input = firstIngredient(handler, recipeIndex);
            result = handler.getResultStack(recipeIndex);
        } catch (final RuntimeException ignored) {
            return false;
        }
        if (input == null || result == null || result.item == null) return false;

        final ItemStack[] candidates = input.items == null ? new ItemStack[] { input.item } : input.items;
        for (final ItemStack candidate : candidates) {
            if (candidate == null || !AlloyRecipeManager.getInstance()
                .getVanillaRecipe()
                .isValidInput(MachineRecipeInput.create(0, candidate))) return false;
        }
        return true;
    }

    @Nullable
    private static PositionedStack firstIngredient(final IRecipeHandler handler, final int recipeIndex) {
        try {
            for (final PositionedStack stack : handler.getIngredientStacks(recipeIndex)) {
                if (stack != null && stack.item != null) return stack;
            }
        } catch (final RuntimeException ignored) {}
        return null;
    }

    private static void applyEnergy(final RecipeContext.Builder recipe, final int energy) {
        recipe.property(CoFHCompat.RF_COST, (long) energy);
    }

    private static void applyMillChances(final RecipeContext.Builder recipe, final MillRecipe r) {
        if (MILL_OUTPUT_CHANCE == null) return;
        try {
            final float[] chances = (float[]) MILL_OUTPUT_CHANCE.get(r);
            for (int i = 0; i < chances.length && i < recipe.outputs()
                .size(); i++) {
                recipe.outputs()
                    .get(i)
                    .setChance(chances[i]);
            }
        } catch (final Exception ignored) {}
    }

}
