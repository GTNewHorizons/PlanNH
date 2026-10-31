package com.sbancuz.plannh.data.provider;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.sbancuz.plannh.Compat;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.Effects;
import com.sbancuz.plannh.data.flowchart.Port;
import com.sbancuz.plannh.data.flowchart.resource.LongItemStack;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.properties.RecipeProperty;
import com.sbancuz.plannh.data.properties.SummaryProperty;
import com.sbancuz.plannh.data.provider.gregtech.GTOverclockStep;
import com.sbancuz.plannh.data.provider.gregtech.GTSpecialHandlers;
import com.sbancuz.plannh.data.setting.Settings;
import com.sbancuz.plannh.mixins.PositionedStackAccessor;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.FurnaceRecipeHandler;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;
import gregtech.api.enums.ItemList;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.RecipeMetadataKey;
import gregtech.api.recipe.maps.FuelBackend;
import gregtech.api.recipe.maps.LargeBoilerFuelBackend;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeConstants;
import gregtech.api.util.recipe.Sievert;
import gregtech.nei.GTNEIDefaultHandler;
import gregtech.nei.GTNEIDefaultHandler.CachedDefaultRecipe;
import gregtech.nei.formatter.HeatingCoilSpecialValueFormatter;

public class GTProvider implements PropertyProvider {

    @Nonnull
    private IDrawable icon(final IRecipeHandler handler, final int recipeIndex) {
        return itemIcon(ItemList.Machine_LV_Macerator, GuiTextures.PROCESSOR);
    }

    static IDrawable itemIcon(final ItemList machine, final IDrawable fallback) {
        final var stack = machine.get(1L);
        return stack == null ? fallback : new ItemDrawable(stack);
    }

    // Vanilla furnace: base cook time of 200 ticks (10 seconds)
    private static final int FURNACE_COOK_TICKS = 200;

    public static final RecipeProperty<Integer> SPECIAL_VALUE = RecipeProperty.<Integer>builder("gt.special_value", 0)
        .build();
    static final RecipeProperty<Integer> GLASS_TIER = RecipeProperty.<Integer>builder("gt.bartworks.glass_tier", 3)
        .build();
    public static final RecipeProperty<Integer> SIEVERT = RecipeProperty.<Integer>builder("gt.bartworks.sievert", 0)
        .build();
    public static final RecipeProperty<Boolean> SIEVERT_EXACT = RecipeProperty
        .<Boolean>builder("gt.bartworks.sievert_exact", false)
        .build();
    public static final RecipeProperty<Integer> MASS = RecipeProperty.<Integer>builder("gt.bartworks.mass", 0)
        .build();

    public static final RecipeProperty<Long> TOTAL_EU = SummaryProperty.<Long>builder("gt.total_eu", 0L)
        .build();
    public static final RecipeProperty<Long> EU_PER_TICK = SummaryProperty.<Long>builder("gt.eu_per_tick", 0L)
        .perSec(true)
        .build();

    public static final RecipeProperty<Integer> COIL_HEAT = RecipeProperty.<Integer>builder("gt.coil_heat", 0)
        .build();
    public static final RecipeProperty<Long> FUSION_THRESHOLD = RecipeProperty.<Long>builder("gt.fusion_threshold", 0L)
        .build();

    public static final RecipeProperty<RecipeMap<?>> RECIPE_MAP = RecipeProperty
        .<RecipeMap<?>>builder("gt.recipe_map", null)
        .build();

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(FurnaceRecipeHandler.class, this);
        if (Compat.EFR.isLoaded) {
            EFRProvider.registerHandlers(this);
        }

        RecipePropertyAPI.registerExtractor(GTNEIDefaultHandler.class, this);

        new GTSteamProvider().register();
    }

    static java.util.function.BiPredicate<MachineConfig, RecipeContext> multiblockOnly() {
        return (cfg, ctx) -> Boolean.TRUE
            .equals(cfg.getOrDefault(Settings.GT_MULTIBLOCK, Settings.GT_MULTIBLOCK::getDefaultValue));
    }

    private static boolean hasHeat(final MachineConfig cfg, final RecipeContext ctx) {
        return ctx.contains(GLASS_TIER) || ctx.contains(COIL_HEAT);
    }

    private static boolean isEoH(final MachineConfig cfg, final RecipeContext ctx) {
        final RecipeMap<?> map = ctx.getOrDefault(RECIPE_MAP, null);
        return map != null && "gt.recipe.eyeofharmony".equals(map.unlocalizedName);
    }

    private final MachineProfile profile = MachineProfile.builder("gregtech:unified")
        .setting(Settings.VOLTAGE)
        .setting(Settings.AMP)
        .setting(Settings.SPEED)
        .setting(Settings.PARALLELS, (cfg, ctx) -> !isEoH(cfg, ctx))
        .setting(Settings.MACHINES)
        .setting(Settings.PERFECT_OC)
        .setting(Settings.GT_MULTIBLOCK)
        .setting(Settings.LASER_OC, multiblockOnly())
        .setting(Settings.EUT_DISCOUNT, multiblockOnly())
        .setting(Settings.EUT_INCREASE_PER_OC, multiblockOnly())
        .setting(Settings.DURATION_DECREASE_PER_OC, multiblockOnly())
        .setting(Settings.MAX_OVERCLOCKS, multiblockOnly())
        .setting(Settings.MAX_REGULAR_OC, multiblockOnly())
        .setting(Settings.MAX_TIER_SKIPS, multiblockOnly())
        .setting(Settings.UNLIMITED_SKIPS, multiblockOnly())
        .setting(Settings.NO_OVERCLOCK, multiblockOnly())
        .setting(Settings.HEAT_OC, GTProvider::hasHeat)
        .setting(Settings.MACHINE_HEAT, multiblockOnly().and(GTProvider::hasHeat))
        .setting(Settings.RECIPE_HEAT, multiblockOnly().and(GTProvider::hasHeat))
        .setting(Settings.HEAT_DISCOUNT, multiblockOnly().and(GTProvider::hasHeat))
        .setting(Settings.HEAT_DISCOUNT_MULT, multiblockOnly().and(GTProvider::hasHeat))
        .setting(Settings.CATALYST_ASTRAL_ARRAYS, GTProvider::isEoH)
        .derive(
            Settings.PERFECT_OC,
            (cfg, ctx) -> routeDefault(
                ctx,
                Set.of(
                    "gt.recipe.largechemicalreactor",
                    "gtpp.recipe.flotationcell",
                    "gg.recipe.naquadah_fuel_refine_factory",
                    "gtpp.recipe.matterfab2",
                    "gtpp.recipe.oremill",
                    "bw.recipe.cal",
                    "gtnhlanth.recipe.digester",
                    "gt.recipe.nanoforge",
                    "gt.recipe.plasmaforge"),
                true,
                Settings.PERFECT_OC))
        .derive(
            Settings.UNLIMITED_SKIPS,
            (cfg, ctx) -> routeDefault(
                ctx,
                Set.of(
                    "gg.recipe.naquadah_fuel_refine_factory",
                    "gt.recipe.nanoforge",
                    "gt.recipe.plasmaforge",
                    "gtpp.recipe.alloyblastsmelter",
                    "gt.recipe.transcendentplasmamixerrecipes"),
                true,
                Settings.UNLIMITED_SKIPS))
        .derive(
            Settings.NO_OVERCLOCK,
            (cfg, ctx) -> routeDefault(
                ctx,
                Set.of("gt.recipe.transcendentplasmamixerrecipes", "gtpp.recipe.algae_pond"),
                true,
                Settings.NO_OVERCLOCK))

        .effect(
            Effects.durationFromHandler()
                .andThen(
                    GTOverclockStep.create()
                        .applyIf(GTProvider::hasHeat, GTOverclockStep::withHeat)
                        .applyIf((cfg, ctx) -> ctx.contains(FUSION_THRESHOLD), GTOverclockStep::withPerfectOC)
                        .route(
                            "gt.recipe.eyeofharmony",
                            step -> step.withCatalyst(
                                Settings.CATALYST_ASTRAL_ARRAYS,
                                v -> (int) Math
                                    .pow(2, (int) Math.floor(Math.log(8.0 * Math.min(v, 8637)) / Math.log(1.7)))))))
        .icon(this::icon)
        .provider(this)
        .build();

    private static <T> T routeDefault(final RecipeContext ctx, final Set<String> routes, final T value,
        final com.sbancuz.plannh.data.setting.SettingDef<T> def) {
        final RecipeMap<?> map = ctx.getOrDefault(RECIPE_MAP, null);
        return map != null && routes.contains(map.unlocalizedName) ? value : def.getDefaultValue();
    }

    @Override
    public boolean canCraft(final IRecipeHandler handler, final int recipeIndex) {
        if (handler instanceof FurnaceRecipeHandler) return true;
        return handler instanceof GTNEIDefaultHandler;
    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        return canCraft(handler, recipeIndex) ? List.of(profile) : List.of();
    }

    private static <T> void extractMD(final RecipeContext.Builder recipe, final GTRecipe r,
        final RecipeProperty<T> prop, final RecipeMetadataKey<T> key, final T defaultVal) {
        final T val = r.getMetadataOrDefault(key, defaultVal);
        if (!Objects.equals(val, defaultVal)) {
            recipe.property(prop, val);
        }
    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler, final int recipeIndex) {
        populateGTRecipe(recipe, handler, recipeIndex);
    }

    public static void populateGTRecipe(final RecipeContext.Builder recipe, final IRecipeHandler handler,
        final int recipeIndex) {
        GTRecipe r;
        RecipeMap<?> gthMap = null;
        List<PositionedStack> inputStacks = List.of();
        List<PositionedStack> outputStacks = List.of();

        if (handler instanceof final FurnaceRecipeHandler fh) {
            final List<TemplateRecipeHandler.CachedRecipe> fRecipes = fh.arecipes;
            recipe.property(RecipePropertyAPI.DURATION_TICKS, FURNACE_COOK_TICKS);

            if (recipeIndex < 0 || recipeIndex >= fRecipes.size()) {
                return;
            }
            final TemplateRecipeHandler.CachedRecipe cr = fRecipes.get(recipeIndex);
            if (cr == null) {
                return;
            }
            final List<PositionedStack> ingredients = cr.getIngredients();
            if (ingredients == null || ingredients.isEmpty() || ingredients.getFirst().item == null) {
                return;
            }
            inputStacks = ingredients;
            final PositionedStack result = cr.getResult();
            if (result != null) outputStacks = List.of(result);

            final String overlay = fh.getOverlayIdentifier();
            final RecipeMap<?> recipeMap;
            if (EFRProvider.SMOKER_OVERLAY.equals(overlay)) {
                recipeMap = RecipeMaps.efrSmokingRecipes;
            } else if (EFRProvider.BLAST_FURNACE_OVERLAY.equals(overlay)) {
                recipeMap = RecipeMaps.efrBlastingRecipes;
            } else {
                recipeMap = RecipeMaps.furnaceRecipes;
            }

            r = recipeMap.findRecipeQuery()
                .items(ingredients.getFirst().item)
                .find();
            if (r == null) {
                return;
            }

        } else if (handler instanceof final GTNEIDefaultHandler gth) {
            final List<TemplateRecipeHandler.CachedRecipe> recipes = gth.arecipes;
            if (recipeIndex < 0 || recipeIndex >= recipes.size()) return;

            final CachedDefaultRecipe cached = (CachedDefaultRecipe) recipes.get(recipeIndex);
            r = cached.mRecipe;

            inputStacks = cached.mInputs;
            outputStacks = cached.mOutputs;

            if (r == null) return;

            gthMap = gth.getRecipeMap();

        } else {
            return;
        }

        recipe.property(RecipePropertyAPI.DURATION_TICKS, r.mDuration);
        recipe.property(EU_PER_TICK, (long) r.mEUt);
        recipe.property(TOTAL_EU, (long) r.mEUt * r.mDuration);

        if (gthMap != null && (gthMap.getBackend() instanceof FuelBackend
            || gthMap.getBackend() instanceof LargeBoilerFuelBackend)) {
            recipe.property(EU_PER_TICK, 0L);
            recipe.property(TOTAL_EU, 0L);
        }

        extractMD(recipe, r, GLASS_TIER, GTRecipeConstants.GLASS, 3);
        extractMD(recipe, r, COIL_HEAT, GTRecipeConstants.COIL_HEAT, 0);

        if (gthMap != null && !recipe.contains(COIL_HEAT)
            && gthMap.getFrontend().getNEIProperties().neiSpecialInfoFormatter instanceof HeatingCoilSpecialValueFormatter
            && r.mSpecialValue > 0) {
            recipe.property(COIL_HEAT, r.mSpecialValue);
        }

        if (gthMap != null) {
            recipe.property(RECIPE_MAP, gthMap);
        }

        extractMD(recipe, r, FUSION_THRESHOLD, GTRecipeConstants.FUSION_THRESHOLD, 0L);

        final Sievert sievert = r.getMetadataOrDefault(GTRecipeConstants.SIEVERT, new Sievert(0, false));
        if (sievert.sievert > 0 || sievert.isExact) {
            recipe.property(SIEVERT, sievert.sievert);
            if (sievert.isExact) recipe.property(SIEVERT_EXACT, true);
        }

        extractMD(recipe, r, MASS, GTRecipeConstants.MASS, 0);

        if (r.mSpecialValue != 0) {
            recipe.property(SPECIAL_VALUE, r.mSpecialValue);
        }

        final GTSpecialHandlers.GTSpecialHandler special = GTSpecialHandlers.get(gthMap);
        if (special != null) special.populate(r, recipe, inputStacks, outputStacks);
        else buildGenericPorts(inputStacks, outputStacks, recipe);

        // Special handlers build ports directly, so normalize their copied GT displays here too.
        for (final Port<?> output : recipe.outputs()) {
            if (!(output.getValue() instanceof LongItemStack)) continue;
            for (final PositionedStack stack : output.getAllStacks()) {
                if (stack instanceof GTNEIDefaultHandler.FixedPositionedStack fixed
                    && !fixed.isInput()
                    && stack.items.length > 1) {
                    ((PositionedStackAccessor) stack).setPermutated(false);
                }
            }
        }
    }

    private static void buildGenericPorts(final List<PositionedStack> inputStacks,
        final List<PositionedStack> outputStacks, final RecipeContext.Builder recipe) {
        for (final PositionedStack ps : inputStacks) {
            if (ps.item != null) {
                recipe.input(port(ps));
            }
        }
        for (final PositionedStack ps : outputStacks) {
            recipe.output(port(ps));
        }
    }

    private static Port<?> port(final PositionedStack stack) {
        final Port<?> port = Port.fromStack(stack);
        // GT may render a single item while retaining the actual recipe count separately.
        if (port.getValue() instanceof LongItemStack item
            && stack instanceof GTNEIDefaultHandler.FixedPositionedStack fixed) {
            item.amount = fixed.realStackSize;
        }
        return port;
    }

}
