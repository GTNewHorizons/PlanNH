package com.sbancuz.plannh.data.provider;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.Effects;
import com.sbancuz.plannh.data.effect.steps.CoFHCompat;
import com.sbancuz.plannh.data.flowchart.Port;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.setting.IntegerSettingDef;
import com.sbancuz.plannh.data.setting.Settings;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.FurnaceRecipeHandler;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;
import mods.railcraft.common.blocks.machine.alpha.EnumMachineAlpha;
import tonius.neiintegration.mods.railcraft.RecipeHandlerBlastFurnace;
import tonius.neiintegration.mods.railcraft.RecipeHandlerBlastFurnace.CachedBlastFurnaceRecipe;
import tonius.neiintegration.mods.railcraft.RecipeHandlerCokeOven;
import tonius.neiintegration.mods.railcraft.RecipeHandlerCokeOven.CachedCokeOvenRecipe;
import tonius.neiintegration.mods.railcraft.RecipeHandlerRockCrusher;
import tonius.neiintegration.mods.railcraft.RecipeHandlerRollingMachine;

// TODO: open an upstream PR to expose these copied machine constants.
// Rock Crusher and Rolling Machine have no time/energy in their cached recipe classes
public class RailcraftProvider implements PropertyProvider {

    private static final int STEAM_PER_BATCH = 8000;
    private static final int STEAM_OVEN_COOK_TICKS = 256;
    static final IntegerSettingDef STEAM_OVEN_BATCH_SIZE = new IntegerSettingDef(
        "railcraft.steam_oven_batch_size",
        1,
        1,
        9);

    private final MachineProfile PROFILE = MachineProfile.builder("railcraft")
        .setting(Settings.MACHINES)
        .setting(Settings.TICK_MODIFIER)
        .effect(
            Effects.durationFromHandler()
                .amortizeCost(CoFHCompat.RF_COST)
                .applyParallelism())
        .provider(this)
        .icon(this::icon)
        .build();

    private final MachineProfile STEAM_OVEN_PROFILE = MachineProfile.builder("railcraft:steam_oven")
        .setting(Settings.MACHINES)
        .setting(STEAM_OVEN_BATCH_SIZE, 1, 9)
        .setting(Settings.TICK_MODIFIER)
        .effect(
            Effects.constant(STEAM_OVEN_COOK_TICKS, 0)
                .applyParallelism())
        .inputMultiplier(
            (_, cfg, ctx, index) -> ctx.inputs()
                .get(index)
                .getType() == RecipePropertyAPI.FLUID ? 1d : cfg.value(STEAM_OVEN_BATCH_SIZE, ctx))
        .outputMultiplier((_, cfg, ctx, index) -> cfg.value(STEAM_OVEN_BATCH_SIZE, ctx))
        .provider(this)
        .icon((_, _) -> machineIcon(EnumMachineAlpha.STEAM_OVEN.getItem()))
        .build();

    @Nonnull
    private IDrawable icon(final IRecipeHandler handler, final int recipeIndex) {
        final var machine = switch (handler) {
            case RecipeHandlerCokeOven _ -> EnumMachineAlpha.COKE_OVEN;
            case RecipeHandlerBlastFurnace _ -> EnumMachineAlpha.BLAST_FURNACE;
            case RecipeHandlerRockCrusher _ -> EnumMachineAlpha.ROCK_CRUSHER;
            case RecipeHandlerRollingMachine _ -> EnumMachineAlpha.ROLLING_MACHINE;
            default -> null;
        };
        return machine == null ? GuiTextures.PROCESSOR : machineIcon(machine.getItem());
    }

    private static IDrawable machineIcon(final ItemStack stack) {
        return stack == null || stack.getItem() == null ? GuiTextures.PROCESSOR : new ItemDrawable(stack);
    }

    @Override
    public StackRole otherStackRole(final IRecipeHandler handler, final int recipeIndex, final PositionedStack stack) {
        return handler instanceof RecipeHandlerBlastFurnace ? StackRole.IGNORE
            : PropertyProvider.super.otherStackRole(handler, recipeIndex, stack);
    }

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(RecipeHandlerCokeOven.class, this);
        RecipePropertyAPI.registerExtractor(RecipeHandlerBlastFurnace.class, this);
        RecipePropertyAPI.registerExtractor(RecipeHandlerRockCrusher.class, this);
        RecipePropertyAPI.registerExtractor(RecipeHandlerRollingMachine.class, this);
        RecipePropertyAPI.registerExtractor(FurnaceRecipeHandler.class, this);

    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler,
        final int recipeIndex) {
        PropertyProvider.super.populate(recipe, handler, recipeIndex);
        if (isVanillaSmelting(handler)) {
            final Fluid steam = FluidRegistry.getFluid("steam");
            if (steam == null) throw new IllegalStateException("Railcraft steam fluid is unavailable");
            final FluidStack batch = new FluidStack(steam, STEAM_PER_BATCH);
            recipe.input(Port.fluid(batch, 1f, 20, 20));
            return;
        }
        if (!(handler instanceof final TemplateRecipeHandler trh)) return;

        final List<TemplateRecipeHandler.CachedRecipe> recipes = trh.arecipes;
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) return;

        final var cached = recipes.get(recipeIndex);

        if (handler instanceof final RecipeHandlerCokeOven rco) {
            final var r = (CachedCokeOvenRecipe) cached;
            if (r.cookTime > 0) {
                recipe.property(RecipePropertyAPI.DURATION_TICKS, r.cookTime);
            }
        } else if (handler instanceof final RecipeHandlerBlastFurnace rbf) {
            final var r = (CachedBlastFurnaceRecipe) cached;
            if (r.cookTime > 0) {
                recipe.property(RecipePropertyAPI.DURATION_TICKS, r.cookTime);
            }
        }
        // Rock Crusher and Rolling Machine: no time/RF in cached recipe — values would be estimates

    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        if (isVanillaSmelting(handler)) return List.of(STEAM_OVEN_PROFILE);
        return switch (handler) {
            case RecipeHandlerCokeOven _, RecipeHandlerBlastFurnace _,
                 RecipeHandlerRockCrusher _, RecipeHandlerRollingMachine _ -> List.of(PROFILE);
            default -> List.of();
        };
    }

    static boolean isVanillaSmelting(final IRecipeHandler handler) {
        return handler instanceof FurnaceRecipeHandler furnace && "smelting".equals(furnace.getOverlayIdentifier());
    }
}
