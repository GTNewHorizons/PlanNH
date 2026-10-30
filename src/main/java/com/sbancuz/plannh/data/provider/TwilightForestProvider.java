package com.sbancuz.plannh.data.provider;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.flowchart.resource.LongItemStack;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.setting.IntegerSettingDef;
import com.sbancuz.plannh.data.setting.Settings;

import codechicken.nei.recipe.FurnaceRecipeHandler;
import codechicken.nei.recipe.IRecipeHandler;
import twilightforest.block.TFBlocks;

/** Twilight Forest's cinder furnace overlay on the vanilla smelting registry. */
public final class TwilightForestProvider implements PropertyProvider {

    private static final int FURNACE_COOK_TICKS = 200;
    // TODO: open an upstream PR to expose these copied cinder-furnace constants.
    private static final int NEARBY_BLOCKS = 26;
    private static final int SPEED_LOG_FACTOR = 2;
    private static final int OUTPUT_LOG_FACTOR = 10;

    static final IntegerSettingDef CINDER_LOG_COUNT = new IntegerSettingDef(
        "twilightforest.cinder_log_count",
        0,
        0,
        NEARBY_BLOCKS) {

        @Override
        public IWidget settingsWidget(final MachineConfig cfg, final RecipeContext ctx, final Consumer<Runnable> edit) {
            return itemCountWidget(
                cfg,
                ctx,
                edit,
                () -> TFBlocks.cinderLog == null ? null : new ItemStack(TFBlocks.cinderLog));
        }
    };

    private final MachineProfile profile = MachineProfile.builder("twilightforest:cinder_furnace")
        .setting(Settings.MACHINES)
        .setting(CINDER_LOG_COUNT, 0, NEARBY_BLOCKS)
        .setting(Settings.TICK_MODIFIER)
        .outputMultiplier(
            (_, cfg, ctx, index) -> isBonusRecipe(ctx) ? expectedOutputMultiplier(cfg.value(CINDER_LOG_COUNT, ctx))
                : 1d)
        .duration((cfg, ctx) -> durationTicks(cfg.value(CINDER_LOG_COUNT, ctx)))
        .provider(this)
        .icon(this::icon)
        .build();

    @Nonnull
    private IDrawable icon(final IRecipeHandler handler, final int recipeIndex) {
        return TFBlocks.cinderFurnace == null ? GuiTextures.PROCESSOR : new ItemDrawable(TFBlocks.cinderFurnace);
    }

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(FurnaceRecipeHandler.class, this);
    }

    @Override
    public boolean canCraft(final IRecipeHandler handler, final int recipeIndex) {
        return isVanillaSmelting(handler);
    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        return canCraft(handler, recipeIndex) ? List.of(profile) : List.of();
    }

    static boolean isVanillaSmelting(final IRecipeHandler handler) {
        return handler instanceof FurnaceRecipeHandler furnace && "smelting".equals(furnace.getOverlayIdentifier());
    }

    /** Expected source-side speed, rather than the cinder furnace's random per-tick maximum. */
    static double expectedSpeedMultiplier(final int cinderLogs) {
        return cinderLogs < SPEED_LOG_FACTOR ? 1d : cinderLogs / (double) SPEED_LOG_FACTOR;
    }

    /** Expected source-side output multiplier, rather than the random maximum multiplier. */
    static double expectedOutputMultiplier(final int cinderLogs) {
        return cinderLogs < OUTPUT_LOG_FACTOR ? 1d : cinderLogs / (double) OUTPUT_LOG_FACTOR;
    }

    static int durationTicks(final int cinderLogs) {
        return Math.max(1, (int) Math.round(FURNACE_COOK_TICKS / expectedSpeedMultiplier(cinderLogs)));
    }

    static boolean isBonusRecipe(final RecipeContext ctx) {
        return ctx.inputs()
            .stream()
            .filter(input -> input.getType() == RecipePropertyAPI.ITEM)
            .map(input -> ((LongItemStack) input.getValue()).stack)
            .anyMatch(TwilightForestProvider::isBonusInput);
    }

    static boolean isBonusInput(final ItemStack input) {
        if (input == null || input.getItem() == null) return false;
        for (final int id : OreDictionary.getOreIDs(input)) {
            final String name = OreDictionary.getOreName(id);
            if (isBonusOreName(name)) return true;
        }
        return false;
    }

    static boolean isBonusOreName(final String name) {
        return name.startsWith("ore") || "logWood".equals(name);
    }
}
