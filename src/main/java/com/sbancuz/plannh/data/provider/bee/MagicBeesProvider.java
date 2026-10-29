package com.sbancuz.plannh.data.provider.bee;

import java.util.List;
import java.util.function.Supplier;

import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.sbancuz.plannh.data.MachineProfile;

import codechicken.nei.recipe.IRecipeHandler;
import forestry.api.apiculture.IBeekeepingMode;
import magicbees.main.Config;

/** MagicBees' native apiary profile and aura assumptions. */
public final class MagicBeesProvider extends BeeProvider {

    private final List<MachineProfile> profiles;

    public MagicBeesProvider() {
        this(BeeProvider::currentMode);
    }

    public MagicBeesProvider(final Supplier<IBeekeepingMode> modeSupplier) {
        super(modeSupplier);
        final MagicBeesHousing magic = new MagicBeesHousing();
        final Housing housing = new Housing(
            (cfg, ctx) -> magic.modifier(
                cfg.value(BeeSettings.MAGIC_PRODUCTION_AURA, ctx),
                cfg.value(BeeSettings.MAGIC_LIFESPAN_AURA, ctx)),
            3);
        profiles = List.of(
            housingBuilder("magicbees:apiary", housing).setting(BeeSettings.MAGIC_PRODUCTION_AURA)
                .setting(BeeSettings.MAGIC_LIFESPAN_AURA)
                .icon((_, _) -> Config.magicApiary == null ? GuiTextures.SUN : new ItemDrawable(Config.magicApiary))
                .build());
    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        return canCraft(handler, recipeIndex) ? profiles : List.of();
    }
}
