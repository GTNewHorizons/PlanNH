package com.sbancuz.plannh.data.provider;

import static codechicken.nei.PositionedStack.CHANCE_FULL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizons.modularui.api.math.Size;
import com.sbancuz.plannh.Compat;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.MachineProfileRegistry;
import com.sbancuz.plannh.data.RecipeHandlerAccess;
import com.sbancuz.plannh.data.effect.Effects;
import com.sbancuz.plannh.data.flowchart.Node;
import com.sbancuz.plannh.data.flowchart.Port;
import com.sbancuz.plannh.data.machine.MachineVariants;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.properties.RecipeProperty;
import com.sbancuz.plannh.data.properties.SummaryProperty;
import com.sbancuz.plannh.data.provider.gregtech.GTMachineIndex;
import com.sbancuz.plannh.data.provider.gregtech.GTNodeState;
import com.sbancuz.plannh.data.provider.gregtech.GTSettings;
import com.sbancuz.plannh.data.setting.Settings;
import com.sbancuz.plannh.mixins.GTNEIDefaultHandlerAccessor;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.FurnaceRecipeHandler;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.StackInfo;
import codechicken.nei.recipe.TemplateRecipeHandler;
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

    /**
     * Recipe this node was extracted from, passed to GT's OverclockDescriber. Several describers discard
     * the template and rebuild from the recipe. Node.properties is rebuilt by refresh() and never
     * serialized, and the balancer aggregates only Number values, so storing it here costs nothing.
     */
    public static final RecipeProperty<GTRecipe> GT_RECIPE = RecipeProperty.<GTRecipe>builder("gt.recipe", null)
        .build();

    /**
     * NEI handler's tab title, which contains the name of the machine the recipe list belongs to. The
     * machine picker defaults to that machine, the one the player had open in NEI.
     */
    public static final RecipeProperty<String> NEI_TITLE = RecipeProperty.<String>builder("gt.nei_title", "")
        .build();

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(FurnaceRecipeHandler.class, this);
        if (Compat.EFR.isLoaded) {
            EFRProvider.registerHandlers(this);
        }

        RecipePropertyAPI.registerExtractor(GTNEIDefaultHandler.class, this);

        MachineProfileRegistry.register(PROFILE);
        // reset with the shared registries so the index and the MachineVariants memo keyed off it empty together
        GTMachineIndex.reset();
        MachineVariants.register(GTMachineIndex.SOURCE);
        GTSettings.registerChartMinimums();
    }

    /**
     * The machine count is the one shared setting. Everything else a GregTech node sets is in its {@link GTNodeState}:
     * the machine, its structure and the overrides, which follow the machine's spec until set.
     */
    private static final MachineProfile PROFILE = MachineProfile.builder("gregtech:unified", "GT Unified")
        .setting(Settings.MACHINES)
        .nodeState(GTNodeState::load)
        .effect(
            Effects.durationFromHandler()
                .andThen(Effects.machineDriven()))
        .build();

    @Override
    public boolean canCraft(final IRecipeHandler handler, final int recipeIndex) {
        if (handler instanceof FurnaceRecipeHandler) return true;
        return handler instanceof GTNEIDefaultHandler;
    }

    @Override
    @Nullable
    public String getProfileId(final IRecipeHandler handler, final int recipeIndex) {
        if (handler instanceof FurnaceRecipeHandler) return PROFILE.id();
        if (handler instanceof GTNEIDefaultHandler) return PROFILE.id();
        return null;
    }

    private static <T> void extractMD(final Map<RecipeProperty<?>, Object> props, final GTRecipe r,
        final RecipeProperty<T> prop, final RecipeMetadataKey<T> key, final T defaultVal) {
        final T val = r.getMetadataOrDefault(key, defaultVal);
        if (!Objects.equals(val, defaultVal)) {
            props.put(prop, val);
        }
    }

    @Override
    @Nonnull
    public Map<RecipeProperty<?>, Object> extract(final Node node, final IRecipeHandler handler,
        final int recipeIndex) {
        return extractGTRecipe(node, handler, recipeIndex);
    }

    public static Map<RecipeProperty<?>, Object> extractGTRecipe(final Node node, final IRecipeHandler handler,
                                                                 final int recipeIndex) {
        final Map<RecipeProperty<?>, Object> props = new HashMap<>();
        GTRecipe r;
        RecipeMap<?> gthMap = null;
        List<PositionedStack> inputStacks = new ArrayList<>();
        List<PositionedStack> outputStacks = new ArrayList<>();

        if (handler instanceof final FurnaceRecipeHandler fh) {
            final List<TemplateRecipeHandler.CachedRecipe> fRecipes = RecipeHandlerAccess.getArecipes(fh);
            props.put(RecipePropertyAPI.DURATION_TICKS, FURNACE_COOK_TICKS);

            if (recipeIndex < 0 || recipeIndex >= fRecipes.size()) {
                return props;
            }
            final TemplateRecipeHandler.CachedRecipe cr = fRecipes.get(recipeIndex);
            if (cr == null) {
                return props;
            }
            final List<PositionedStack> ingredients = cr.getIngredients();
            if (ingredients == null || ingredients.isEmpty() || ingredients.getFirst().item == null) {
                return props;
            }

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
                return props;
            }
            // Kept out of gthMap: that variable also gates the fuel-backend and heating-coil
            // branches below, which the furnace maps must not take.
            props.put(RECIPE_MAP, recipeMap);

        } else if (handler instanceof final GTNEIDefaultHandler gth) {
            final List<TemplateRecipeHandler.CachedRecipe> recipes = RecipeHandlerAccess.getArecipes(gth);
            if (recipeIndex < 0 || recipeIndex >= recipes.size()) return props;

            final CachedDefaultRecipe cached = (CachedDefaultRecipe) recipes.get(recipeIndex);
            r = cached.mRecipe;

            inputStacks = cached.mInputs;
            outputStacks = cached.mOutputs;

            if (r == null) return props;

            gthMap = gth.getRecipeMap();

        } else {
            return props;
        }

        props.put(GT_RECIPE, r);
        props.put(
            NEI_TITLE,
            handler.getRecipeName()
                .trim());
        props.put(RecipePropertyAPI.DURATION_TICKS, r.mDuration);
        props.put(EU_PER_TICK, (long) r.mEUt);
        props.put(TOTAL_EU, (long) r.mEUt * r.mDuration);

        if (gthMap != null && (gthMap.getBackend() instanceof FuelBackend
            || gthMap.getBackend() instanceof LargeBoilerFuelBackend)) {
            props.put(EU_PER_TICK, 0L);
            props.put(TOTAL_EU, 0L);
        }

        extractMD(props, r, GLASS_TIER, GTRecipeConstants.GLASS, 3);
        extractMD(props, r, COIL_HEAT, GTRecipeConstants.COIL_HEAT, 0);

        if (gthMap != null && !props.containsKey(COIL_HEAT)
            && gthMap.getFrontend().getNEIProperties().neiSpecialInfoFormatter instanceof HeatingCoilSpecialValueFormatter
            && r.mSpecialValue > 0) {
            props.put(COIL_HEAT, r.mSpecialValue);
        }

        if (gthMap != null) {
            props.put(RECIPE_MAP, gthMap);
        }

        extractMD(props, r, FUSION_THRESHOLD, GTRecipeConstants.FUSION_THRESHOLD, 0L);

        final Sievert sievert = r.getMetadataOrDefault(GTRecipeConstants.SIEVERT, new Sievert(0, false));
        if (sievert.sievert > 0 || sievert.isExact) {
            props.put(SIEVERT, sievert.sievert);
            if (sievert.isExact) props.put(SIEVERT_EXACT, true);
        }

        extractMD(props, r, MASS, GTRecipeConstants.MASS, 0);

        if (r.mSpecialValue != 0) {
            props.put(SPECIAL_VALUE, r.mSpecialValue);
        }

        List<Port<?>> inputs = node.getInputs();
        List<Port<?>> outputs = node.getOutputs();

        for (PositionedStack ps : inputStacks) {
            if (ps instanceof GTNEIDefaultHandler.FixedPositionedStack fps && fps.isFluid())
                inputs.add(fluidPort(fps));
            else if(ps.item != null) inputs.add(Port.itemPort(ps));
        }

        for (PositionedStack ps : outputStacks) {
            if (ps instanceof GTNEIDefaultHandler.FixedPositionedStack fps && fps.isFluid())
                outputs.add(fluidPort(fps));
            else outputs.add(Port.itemPort(ps));
        }

        return props;
    }

    /**
     * The width and height GregTech draws this handler's recipe background at, or null for a handler that is not
     * GregTech's. For the GUI, which must not name GregTech.
     */
    @Nullable
    public static int[] recipeBackgroundSize(final IRecipeHandler handler) {
        if (!(handler instanceof final GTNEIDefaultHandlerAccessor gt)) return null;
        final Size size = gt.getNeiProperties().recipeBackgroundSize;
        return new int[] { size.width, size.height };
    }

    private static Port<FluidStack> fluidPort(GTNEIDefaultHandler.FixedPositionedStack fps) {
        return new Port<>(
            RecipePropertyAPI.FLUID,
            StackInfo.getFluid(fps.item)
                .copy(),
            (float) fps.getChance() / CHANCE_FULL,
            fps);
    }
}
