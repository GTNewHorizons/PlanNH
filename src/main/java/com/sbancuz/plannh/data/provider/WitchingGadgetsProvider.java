package com.sbancuz.plannh.data.provider;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.EffectComputer;
import com.sbancuz.plannh.data.effect.EffectResult;
import com.sbancuz.plannh.data.effect.Effects;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.properties.RecipeProperty;
import com.sbancuz.plannh.data.setting.ChoiceSettingDef;
import com.sbancuz.plannh.data.setting.Settings;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import thaumcraft.common.config.ConfigBlocks;
import witchinggadgets.client.nei.NEIInfernalBlastfurnaceHandler;
import witchinggadgets.client.nei.NEISpinningWheelHandler;
import witchinggadgets.common.WGContent;
import witchinggadgets.common.util.recipe.InfernalBlastfurnaceRecipe;

public final class WitchingGadgetsProvider implements PropertyProvider {

    // TODO: open an upstream PR to expose these copied constants.
    static final int SPINNING_WHEEL_TICKS = 120;
    static final int BELLOWS_DISCOUNT_TICKS = 40;

    static final RecipeProperty<Integer> BONUS_OUTPUT_INDEX = RecipeProperty
        .<Integer>builder("witchinggadgets.bonus_output_index", -1)
        .build();

    // The tile consumes one Fire essentia when it refills fuel; total refills are state-dependent.
    static final RecipeProperty<Integer> FIRE_ESSENTIA_PER_FUEL = RecipeProperty
        .<Integer>builder("witchinggadgets.fire_essentia_per_fuel", 1)
        .build();

    static final ChoiceSettingDef<ItemStack> BELLOWS = new ChoiceSettingDef<>(
        "witchinggadgets.bellows",
        null,
        ItemStack.class,
        () -> ConfigBlocks.blockWoodenDevice == null ? List.of()
            : List.of(new ItemStack(ConfigBlocks.blockWoodenDevice, 1, 0)),
        stack -> stack == null ? "-" : stack.getDisplayName()).counts(0, 4);

    private final MachineProfile SPINNING_WHEEL = MachineProfile.builder("witchinggadgets:spinning_wheel")
        .setting(Settings.MACHINES)
        .setting(Settings.TICK_MODIFIER)
        .effect(
            Effects.durationFromHandler()
                .applyParallelism())
        .provider(this)
        .icon(this::icon)
        .build();

    private final MachineProfile INFERNAL_BLAST_FURNACE = MachineProfile
        .builder("witchinggadgets:infernal_blast_furnace")
        .setting(Settings.MACHINES)
        .setting(Settings.TICK_MODIFIER)
        .setting(BELLOWS, 0, 4)
        .effect(blastEffect().applyParallelism())
        .outputMultiplier(
            (_, cfg, ctx,
                index) -> index == ctx.getOrDefault(BONUS_OUTPUT_INDEX, -1) ? bonusChance(BELLOWS.count(cfg, ctx)) : 1d)
        .provider(this)
        .icon(this::icon)
        .build();

    @Nonnull
    private IDrawable icon(final IRecipeHandler handler, final int recipeIndex) {
        if (handler instanceof NEISpinningWheelHandler && WGContent.BlockWoodenDevice != null)
            return new ItemDrawable(WGContent.BlockWoodenDevice, 0);
        if (handler instanceof NEIInfernalBlastfurnaceHandler && WGContent.BlockStoneDevice != null)
            return new ItemDrawable(WGContent.BlockStoneDevice, 2);
        return GuiTextures.PROCESSOR;
    }

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(NEISpinningWheelHandler.class, this);
        RecipePropertyAPI.registerExtractor(NEIInfernalBlastfurnaceHandler.class, this);

    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        return switch (handler) {
            case NEISpinningWheelHandler _ -> List.of(SPINNING_WHEEL);
            case NEIInfernalBlastfurnaceHandler _ -> List.of(INFERNAL_BLAST_FURNACE);
            default -> List.of();
        };
    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler, final int recipeIndex) {
        PropertyProvider.super.populate(recipe, handler, recipeIndex);

        if (handler instanceof NEISpinningWheelHandler) {
            recipe.property(RecipePropertyAPI.DURATION_TICKS, SPINNING_WHEEL_TICKS);
        } else if (handler instanceof NEIInfernalBlastfurnaceHandler) {
            final List<PositionedStack> inputs = handler.getIngredientStacks(recipeIndex);
            for (final PositionedStack ps : inputs) {
                if (ps != null && ps.item != null) {
                    final InfernalBlastfurnaceRecipe blastRecipe = InfernalBlastfurnaceRecipe
                        .getRecipeForInput(ps.item);
                    if (blastRecipe != null) {
                        recipe.property(RecipePropertyAPI.DURATION_TICKS, blastRecipe.getSmeltingTime());
                        recipe.property(FIRE_ESSENTIA_PER_FUEL, 1);
                        if (blastRecipe.getBonus() != null && !recipe.outputs()
                            .isEmpty()) recipe.property(
                                BONUS_OUTPUT_INDEX,
                                recipe.outputs()
                                    .size() - 1);
                        break;
                    }
                }
            }
        }

    }

    static int durationTicks(final int baseTicks, final int bellows) {
        return Math.max(1, baseTicks - BELLOWS_DISCOUNT_TICKS * bellows);
    }

    static double bonusChance(final int bellows) {
        return bellows == 0 ? 0.25d : 0.44d * bellows;
    }

    private static EffectComputer blastEffect() {
        return (cfg, ctx) -> new EffectResult(
            durationTicks(ctx.getOrDefault(RecipePropertyAPI.DURATION_TICKS, 0), BELLOWS.count(cfg, ctx)),
            0,
            1);
    }
}
