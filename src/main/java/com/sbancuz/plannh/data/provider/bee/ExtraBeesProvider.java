package com.sbancuz.plannh.data.provider.bee;

import java.util.List;
import java.util.function.Supplier;

import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;

import codechicken.nei.recipe.IRecipeHandler;
import forestry.api.apiculture.IBeekeepingMode;
import forestry.apiculture.AlvearyBeeModifier;
import forestry.plugins.PluginApiculture;

/** Extra Bees' alveary profile. The optional board implementation is isolated here. */
public final class ExtraBeesProvider extends BeeProvider {

    private final List<MachineProfile> profiles;

    public ExtraBeesProvider() {
        this(BeeProvider::currentMode);
    }

    public ExtraBeesProvider(final Supplier<IBeekeepingMode> modeSupplier) {
        super(modeSupplier);
        final Housing alveary = new Housing(new AlvearyBeeModifier(), 3, ExtraBeesProvider::resolveModules);
        profiles = List.of(
            housingBuilder("forestry:alveary", alveary).settings(AlvearyStimulators.BOARD_TYPES)
                .icon((_, _) -> new ItemDrawable(PluginApiculture.blocks.alveary))
                .build());
    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        return canCraft(handler, recipeIndex) ? profiles : List.of();
    }

    private static HousingExtras resolveModules(final MachineConfig cfg, final RecipeContext ctx) {
        // Resolve each board once and append it after the ordinary frame modifiers.
        final AlvearyStimulators.Resolved resolved = AlvearyStimulators.resolve(cfg, ctx);
        return new HousingExtras(resolved.modifiers(), resolved.rfPerWorkCycle());
    }
}
