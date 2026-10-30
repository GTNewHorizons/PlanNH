package com.sbancuz.plannh.data.provider.bee;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.flowchart.Port;
import com.sbancuz.plannh.data.flowchart.resource.MagicResources;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import cpw.mods.fml.common.registry.GameRegistry;
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
                .inputMultiplier((effect, cfg, ctx, index) -> {
                    if (index < 0 || index >= ctx.inputs()
                        .size()) return 1d;
                    final Port<?> port = ctx.inputs()
                        .get(index);
                    if (port.getType() != MagicResources.AURA_CHARGE) return 1d;
                    if (!(port.getValue() instanceof MagicResources.Amount amount)) return 1d;

                    final boolean supplied = switch (amount.id()) {
                        case "production" -> cfg.value(BeeSettings.MAGIC_PRODUCTION_AURA, ctx);
                        case "death" -> cfg.value(BeeSettings.MAGIC_LIFESPAN_AURA, ctx);
                        default -> false;
                    };
                    if (effect.durationTicks() < 0) throw new IllegalArgumentException("Negative effect duration");
                    return supplied ? effect.durationTicks() : 0d;
                })
                .icon(
                    (_, _) -> Config.magicApiary == null ? GuiTextures.PROCESSOR : new ItemDrawable(Config.magicApiary))
                .build());
    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        return canCraft(handler, recipeIndex) ? profiles : List.of();
    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler, final int recipeIndex) {
        super.populate(recipe, handler, recipeIndex);

        final int y = Stream.concat(
            recipe.inputs()
                .stream(),
            recipe.outputs()
                .stream())
            .flatMap(
                port -> port.getAllStacks()
                    .stream())
            .mapToInt(stack -> stack.rely + stack.height + 4)
            .max()
            .orElse(40);
        final var aspectItem = GameRegistry.findItem("aspectrecipeindex", "aspect");
        final String[] ids = { "production", "death" };
        final String[] aspects = { "aer", "perditio" };
        final String[] names = { StatCollector.translateToLocal("plannh.bees.aura_production"),
            StatCollector.translateToLocal("plannh.bees.aura_death") };
        final int[] colors = { 0xF0D05A, 0x9A62C8 };
        for (int index = 0; index < ids.length; index++) {
            final PositionedStack display;
            if (aspectItem != null) {
                display = MagicResources.aspectDisplay(aspectItem, aspects[index], 1, 8 + index * 20, y);
            } else {
                ItemStack visual = Config.visAuraProvider == null ? null : new ItemStack(Config.visAuraProvider);
                if (visual == null || visual.getItem() == null)
                    visual = Config.magicApiary == null ? null : new ItemStack(Config.magicApiary);
                display = visual == null || visual.getItem() == null ? null
                    : new PositionedStack(visual, 8 + index * 20, y, false);
            }
            if (display == null) throw new IllegalStateException("Magic Apiary resource display is unavailable");

            recipe.input(
                MagicResources.port(MagicResources.AURA_CHARGE, ids[index], names[index], colors[index], 1, display));
        }
    }

}
