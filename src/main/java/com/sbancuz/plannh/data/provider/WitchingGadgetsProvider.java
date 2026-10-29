package com.sbancuz.plannh.data.provider;

import java.util.List;

import javax.annotation.Nonnull;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.Effects;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.setting.Settings;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import witchinggadgets.client.nei.NEIInfernalBlastfurnaceHandler;
import witchinggadgets.client.nei.NEISpinningWheelHandler;
import witchinggadgets.common.util.recipe.InfernalBlastfurnaceRecipe;

public final class WitchingGadgetsProvider implements PropertyProvider {

    private final MachineProfile PROFILE = MachineProfile.builder("wg")
        .setting(Settings.MACHINES)
        .setting(Settings.TICK_MODIFIER)
        .effect(
            Effects.clearCost()
                .applyParallelism())
        .provider(this)
        .icon(this::icon)
        .build();

    @Nonnull
    private IDrawable icon(final IRecipeHandler handler, final int recipeIndex) {
        return GuiTextures.MOON;
    }

    // TODO: Make a PR to expose this constant
    // Spinning Wheel: base process length (ticks) from TileEntitySpinningWheel
    private static final int SPINNING_WHEEL_TICKS = 120;

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(NEISpinningWheelHandler.class, this);
        RecipePropertyAPI.registerExtractor(NEIInfernalBlastfurnaceHandler.class, this);

    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        return switch (handler) {
            case NEISpinningWheelHandler _, NEIInfernalBlastfurnaceHandler _ -> List.of(PROFILE);
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
                        break;
                    }
                }
            }
        }

    }
}
