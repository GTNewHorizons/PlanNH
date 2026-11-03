package com.sbancuz.plannh.data.provider.bee;

import java.util.HashMap;
import java.util.Map;

import forestry.api.apiculture.BeeManager;
import forestry.api.apiculture.IBeeModifier;
import magicbees.bees.AuraCharge;
import magicbees.tileentity.TileEntityMagicApiary;

/** Native MagicBees apiary modifiers for the four supported aura states. */
public final class MagicBeesHousing {

    private final Map<Integer, IBeeModifier> modifiers = new HashMap<>(4);

    public IBeeModifier modifier(final boolean productionAura, final boolean lifespanAura) {
        final int flags = (productionAura ? AuraCharge.PRODUCTION.flag : 0)
            | (lifespanAura ? AuraCharge.DEATH.flag : 0);

        // Each cached modifier retains its own tile, whose aura state must remain unchanged.
        return modifiers.computeIfAbsent(flags, MagicBeesHousing::createModifier);
    }

    private static IBeeModifier createModifier(final int flags) {
        // The empty synthetic inventory supplies no frame modifiers. Forestry's native adapter retains this tile.
        final TileEntityMagicApiary tile = new TileEntityMagicApiary();
        tile.getAuraCharges()
            .readFromFlags(flags);

        return BeeManager.beeRoot.createBeeHousingModifier(tile);
    }
}
