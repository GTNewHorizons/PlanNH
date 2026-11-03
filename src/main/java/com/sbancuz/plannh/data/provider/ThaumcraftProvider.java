package com.sbancuz.plannh.data.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.drawable.UITexture;
import com.gtnewhorizons.aspectrecipeindex.ModItems;
import com.gtnewhorizons.aspectrecipeindex.common.items.ItemAspect;
import com.gtnewhorizons.aspectrecipeindex.nei.AlchemyRecipeHandler;
import com.gtnewhorizons.aspectrecipeindex.nei.AspectCombinationHandler;
import com.gtnewhorizons.aspectrecipeindex.nei.InfusionRecipeHandler;
import com.gtnewhorizons.aspectrecipeindex.nei.ItemsContainingAspectHandler;
import com.gtnewhorizons.aspectrecipeindex.nei.arcaneworkbench.ShapedArcaneRecipeHandler;
import com.gtnewhorizons.aspectrecipeindex.nei.arcaneworkbench.ShapelessArcaneRecipeHandler;
import com.gtnewhorizons.aspectrecipeindex.nei.arcaneworkbench.WandRecipeHandler;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.ProfileFactory;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.EffectComputer;
import com.sbancuz.plannh.data.effect.EffectResult;
import com.sbancuz.plannh.data.effect.Effects;
import com.sbancuz.plannh.data.flowchart.Port;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.properties.RecipeProperty;
import com.sbancuz.plannh.data.properties.ResourceProperty;
import com.sbancuz.plannh.data.properties.SummaryProperty;
import com.sbancuz.plannh.data.setting.ChoiceSettingDef;
import com.sbancuz.plannh.data.setting.EnumSettingDef;
import com.sbancuz.plannh.data.setting.IntegerSettingDef;
import com.sbancuz.plannh.data.setting.Settings;
import com.sbancuz.plannh.gui.tooltips.TooltipBuilder;
import com.sbancuz.plannh.mixins.CachedThaumRecipeAccessor;
import com.sbancuz.plannh.mixins.InfusionCachedRecipeAccessor;
import com.sbancuz.plannh.mixins.ResearchInfoAccessor;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.FurnaceRecipeHandler;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.TemplateRecipeHandler;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ConfigBlocks;

public class ThaumcraftProvider implements PropertyProvider {

    private final Predicate<String> researchCompletion;

    private static final UITexture INFERNAL_FURNACE_ICON = UITexture.builder()
        .location("thaumcraft:textures/misc/r_infernalfurnace.png")
        .build();
    private static final UITexture THAUMATORIUM_ICON = UITexture.builder()
        .location("thaumcraft:textures/blocks/alchemyblock.png")
        .build();

    // TODO: open an upstream PR to expose these copied furnace constants.
    private static final int FIRE_VIS_TICKS = 80;
    private static final int NO_FIRE_VIS_TICKS = 140;
    private static final int BELLOWS_DISCOUNT = 20;

    static final ChoiceSettingDef<ItemStack> BELLOWS = new ChoiceSettingDef<>(
        "thaumcraft.bellows",
        null,
        ItemStack.class,
        () -> ConfigBlocks.blockWoodenDevice == null ? List.of()
            : List.of(new ItemStack(ConfigBlocks.blockWoodenDevice, 1, 0)),
        stack -> stack == null ? "-" : stack.getDisplayName()).counts(0, 3);
    static final IntegerSettingDef VIS_DISCOUNT = new IntegerSettingDef("thaumcraft.vis_discount", 0, 0, 90);
    static final EnumSettingDef<FurnaceBoostSource> BOOST_SOURCE = new EnumSettingDef<>(
        "thaumcraft.furnace_boost_source",
        FurnaceBoostSource.NONE,
        FurnaceBoostSource.class);

    enum FurnaceBoostSource {
        NONE,
        NETWORK_VIS,
        ESSENTIA_NOZZLE
    }

    static final RecipeProperty<Integer> BONUS_OUTPUT_INDEX = RecipeProperty
        .<Integer>builder("thaumcraft.infernal_bonus_output", -1)
        .build();

    static final RecipeProperty<Integer> BONUS_BASE_AMOUNT = RecipeProperty
        .<Integer>builder("thaumcraft.infernal_bonus_base", 0)
        .build();

    @Nonnull
    private IDrawable icon(final IRecipeHandler handler, final int recipeIndex) {
        if (ConfigBlocks.blockTable == null || ConfigBlocks.blockStoneDevice == null) return GuiTextures.BOOKMARK;
        if (handler instanceof InfusionRecipeHandler) return new ItemDrawable(ConfigBlocks.blockStoneDevice, 2);
        if (handler instanceof ShapedArcaneRecipeHandler || handler instanceof ShapelessArcaneRecipeHandler
            || handler instanceof WandRecipeHandler) return new ItemDrawable(ConfigBlocks.blockTable, 15);
        return GuiTextures.BOOKMARK;
    }

    public static final RecipeProperty<int[]> VIS_COST = SummaryProperty.builder("thaumcraft.vis_cost", new int[6])
        .build();

    public static final RecipeProperty<Integer> INSTABILITY = RecipeProperty
        .<Integer>builder("thaumcraft.instability", 0)
        .build();

    public static final RecipeProperty<String> RESEARCH_KEY = RecipeProperty
        .<String>builder("thaumcraft.research_key", "")
        .build();

    public static final RecipeProperty<Integer> NUM_COMPONENTS = RecipeProperty
        .<Integer>builder("thaumcraft.num_components", 0)
        .build();

    public static final RecipeProperty<Integer> ESSENTIA_UNITS = RecipeProperty
        .<Integer>builder("thaumcraft.essentia_units", 0)
        .build();

    public static final RecipeProperty<Integer> STORED_VIS_UNITS = RecipeProperty
        .<Integer>builder("thaumcraft.stored_vis_units", 0)
        .build();

    private final ProfileFactory<EffectComputer> BASE_FACTORY = MachineProfile.template()
        .setting(Settings.MACHINES)
        .setting(Settings.TICK_MODIFIER)
        .provider(this)
        .asFactory(
            (builder, effect) -> builder.effect(effect)
                .icon(this::icon));

    private final MachineProfile ARCANE = BASE_FACTORY
        .builder(
            "thaumcraft:arcane",
            Effects.durationFromTotal(STORED_VIS_UNITS, Settings.VIS_PER_TICK)
                .applyParallelism())
        .setting(Settings.VIS_PER_TICK)
        .setting(VIS_DISCOUNT)
        .inputMultiplier(this::arcaneInputMultiplier)
        .build();

    private final MachineProfile INFUSION = BASE_FACTORY
        .builder("thaumcraft:infusion", Effects.durationFromFormula((cfg, ctx) -> {
            final int tv = ctx.getOrDefault(ESSENTIA_UNITS, 0);
            final int nc = ctx.getOrDefault(NUM_COMPONENTS, 0);
            return tv * 10 + nc * 60;
        })
            .applyParallelism())
        .build();

    private final MachineProfile CRUCIBLE = BASE_FACTORY
        .builder(
            "thaumcraft:crucible",
            Effects.durationFromConfigOrProperty()
                .applyParallelism())
        .setting(Settings.DURATION_TICKS)
        .icon(
            (_, _) -> ConfigBlocks.blockMetalDevice == null ? GuiTextures.BUBBLE
                : new ItemDrawable(ConfigBlocks.blockMetalDevice, 0))
        .build();

    private final MachineProfile THAUMATORIUM = BASE_FACTORY.builder(
        "thaumcraft:thaumatorium",
        // TileThaumatorium transfers one essentia per five-tick check, then completes on the next check.
        Effects.durationFromFormula((cfg, ctx) -> thaumatoriumDuration(ctx.getOrDefault(ESSENTIA_UNITS, 0)))
            .applyParallelism())
        .icon(
            (_, _) -> THAUMATORIUM_ICON,
            tooltip -> TooltipBuilder.create(tooltip)
                .langRow("tc.research_name.THAUMATORIUM")
                .flush())
        .build();

    private final MachineProfile INFERNAL_FURNACE = BASE_FACTORY.builder(
        "thaumcraft:infernal_furnace",
        Effects.durationFromFormula(
            (cfg, ctx) -> cycleDurationTicks(BELLOWS.count(cfg, ctx), boostSource(cfg, ctx) != FurnaceBoostSource.NONE))
            .applyParallelism())
        .setting(BELLOWS, 0, 3)
        .setting(BOOST_SOURCE)
        .inputMultiplier(ThaumcraftProvider::infernalInputMultiplier)
        .outputMultiplier(this::infernalBonusMultiplier)
        .icon(
            (_, _) -> INFERNAL_FURNACE_ICON,
            tooltip -> TooltipBuilder.create(tooltip)
                .langRow("tc.research_name.INFERNALFURNACE")
                .langRow("plannh.tooltip.thaumcraft.furnace_steady_state")
                .flush())
        .build();

    private static final String[] PRIMAL_TAGS = { "aer", "terra", "ignis", "aqua", "ordo", "perditio" };

    public ThaumcraftProvider() {
        this(ThaumcraftProvider::researchCompleteForPlayer);
    }

    ThaumcraftProvider(final Predicate<String> researchCompletion) {
        this.researchCompletion = Objects.requireNonNull(researchCompletion);
    }

    static int thaumatoriumDuration(final int essentiaUnits) {
        return Math.max(5, 5 * (essentiaUnits + 1));
    }

    /** The furnace's countdown excludes the tick that starts the next cycle. */
    static int durationTicks(final int bellows, final boolean fireVis) {
        return (fireVis ? FIRE_VIS_TICKS : NO_FIRE_VIS_TICKS) - BELLOWS_DISCOUNT * bellows;
    }

    static int cycleDurationTicks(final int bellows, final boolean boosted) {
        return durationTicks(bellows, boosted) + 1;
    }

    private static FurnaceBoostSource boostSource(final MachineConfig cfg, final RecipeContext ctx) {
        final FurnaceBoostSource source = cfg.getProfile()
            .value(BOOST_SOURCE, cfg, ctx);
        return source == null ? FurnaceBoostSource.NONE : source;
    }

    private static double infernalInputMultiplier(final EffectResult effect, final MachineConfig cfg,
        final RecipeContext ctx, final int index) {
        if (index < 0 || index >= ctx.inputs()
            .size()) return 1d;
        final var type = ctx.inputs()
            .get(index)
            .getType();
        final FurnaceBoostSource source = boostSource(cfg, ctx);
        if (type != MagicResources.NETWORK_VIS && type != MagicResources.ESSENTIA) return 1d;
        if (source == FurnaceBoostSource.NONE) return 0d;
        if (source == FurnaceBoostSource.NETWORK_VIS && type != MagicResources.NETWORK_VIS) return 0d;
        if (source == FurnaceBoostSource.ESSENTIA_NOZZLE && type != MagicResources.ESSENTIA) return 0d;

        final int countdown = durationTicks(BELLOWS.count(cfg, ctx), true);
        final double activeCookFraction = (double) countdown / (countdown + 1);
        final double timedCost = effect.durationTicks() * activeCookFraction;
        return source == FurnaceBoostSource.ESSENTIA_NOZZLE ? timedCost / 600d : timedCost;
    }

    private double arcaneInputMultiplier(final EffectResult effect, final MachineConfig cfg, final RecipeContext ctx,
        final int index) {
        if (index < 0 || index >= ctx.inputs()
            .size()) return 1d;
        final Port<?> port = ctx.inputs()
            .get(index);
        if (port.getType() != MagicResources.STORED_VIS || !(port.getValue() instanceof MagicResources.Amount amount)
            || amount.amount <= 0) return 1d;

        final int base = amount.amount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) amount.amount;
        final int charged = chargedCentiVis(base, cfg.value(VIS_DISCOUNT, ctx));
        return charged / (base * 100d);
    }

    static int chargedCentiVis(final int baseVis, final int discountPercent) {
        if (baseVis <= 0) return 0;
        final int raw = Math.multiplyExact(baseVis, 100);
        return (int) ((float) raw * (1f - discountPercent / 100f));
    }

    static double bonusChance(final int bellows) {
        return bellows == 0 ? 0.25d : 0.44d * bellows;
    }

    static double bonusMultiplier(final int displayAmount, final int baseAmount, final int bellows) {
        return (baseAmount + bonusChance(bellows)) / Math.max(1, displayAmount);
    }

    private double infernalBonusMultiplier(final EffectResult eff, final MachineConfig cfg, final RecipeContext ctx,
        final int index) {
        if (index != ctx.getOrDefault(BONUS_OUTPUT_INDEX, -1)) return 1d;
        final int bellows = BELLOWS.count(cfg, ctx);
        final int displayAmount = ctx.outputs()
            .get(index)
            .getValue() instanceof com.sbancuz.plannh.data.flowchart.resource.LongItemStack item ? (int) item.amount
                : 1;
        return bonusMultiplier(displayAmount, ctx.getOrDefault(BONUS_BASE_AMOUNT, 0), bellows);
    }

    @Override
    public StackRole otherStackRole(final IRecipeHandler handler, final int recipeIndex, final PositionedStack stack) {
        // Thaumcraft aspect/vis displays are not item ports.
        return handler instanceof ShapedArcaneRecipeHandler || handler instanceof ShapelessArcaneRecipeHandler
            || handler instanceof WandRecipeHandler
            || handler instanceof AlchemyRecipeHandler
            || handler instanceof InfusionRecipeHandler ? StackRole.IGNORE
                : PropertyProvider.super.otherStackRole(handler, recipeIndex, stack);
    }

    @Override
    public void register() {
        RecipePropertyAPI.registerExtractor(ItemsContainingAspectHandler.class, this);
        RecipePropertyAPI.registerExtractor(AspectCombinationHandler.class, this);
        RecipePropertyAPI.registerExtractor(ShapedArcaneRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(WandRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(ShapelessArcaneRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(AlchemyRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(InfusionRecipeHandler.class, this);
        RecipePropertyAPI.registerExtractor(FurnaceRecipeHandler.class, this);
    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        if (isInfernalFurnace(handler)) return List.of(INFERNAL_FURNACE);
        if (!canCraft(handler, recipeIndex)) return List.of();
        if (!(handler instanceof TemplateRecipeHandler)) return List.of();
        final String overlay = handler.getOverlayIdentifier();
        if (overlay == null) return List.of();
        return switch (overlay) {
            case "thaumcraft.arcane.shaped", "thaumcraft.arcane.shapeless", "thaumcraft.wands" -> List.of(ARCANE);
            case "thaumcraft.alchemy" -> {
                final List<MachineProfile> profiles = new ArrayList<>();
                profiles.add(CRUCIBLE);
                if (researchCompletion.test("THAUMATORIUM")) profiles.add(THAUMATORIUM);
                yield profiles;
            }
            case "thaumcraft.infusion" -> List.of(INFUSION);
            default -> List.of();
        };
    }

    @Override
    public boolean canCraft(final IRecipeHandler handler, final int recipeIndex) {
        if (isInfernalFurnace(handler)) return true;
        if (!(handler instanceof TemplateRecipeHandler template) || !switch (handler.getOverlayIdentifier()) {
            case "thaumcraft.alchemy", "thaumcraft.infusion", "thaumcraft.arcane.shaped",
                 "thaumcraft.arcane.shapeless", "thaumcraft.wands" -> true;
            case null, default -> false;
        }) return false;
        final var recipes = template.arecipes;
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) return false;
        final var cached = recipes.get(recipeIndex);
        if (cached.getResult() == null || cached.getResult().item == null
            || cached.getIngredients().isEmpty() || cachedAspects(cached) == null) return false;
        final List<String> researchKeys = cachedResearchKeys(cached);
        // Native cached recipes already distinguish their prerequisites, including research-free recipes.
        return researchKeys.isEmpty() || allResearchComplete(researchKeys, researchCompletion);
    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler, final int recipeIndex) {
        if (isInfernalFurnace(handler)) {
            populateInfernalFurnace(recipe, handler, recipeIndex);
            return;
        }
        if (!(handler instanceof final TemplateRecipeHandler trh)) return;

        final var recipes = trh.arecipes;
        if (recipeIndex < 0 || recipeIndex >= recipes.size()) return;

        final TemplateRecipeHandler.CachedRecipe cached = recipes.get(recipeIndex);
        final String overlay = trh.getOverlayIdentifier();
        if (overlay == null) return;

        final var resultPos = cached.getResult();
        if (resultPos == null || resultPos.item == null) return;

        switch (overlay) {
            case "thaumcraft.alchemy" -> {
                populateCrucible(recipe, trh, recipeIndex);
            }
            case "thaumcraft.infusion" -> {
                populateInfusion(recipe, cached);
            }
            case "thaumcraft.arcane.shaped", "thaumcraft.arcane.shapeless", "thaumcraft.wands" -> {
                populateItems(recipe, cached);
                final AspectList aspects = cachedAspects(cached);
                addAspectInputs(recipe, aspects, cached, MagicResources.STORED_VIS);
                recipe.property(VIS_COST, aspectListToPrimals(aspects));
                recipe.property(STORED_VIS_UNITS, primalVis(aspects));
            }
        }

        final List<String> researchKeys = cachedResearchKeys(cached);
        if (!researchKeys.isEmpty()) recipe.property(RESEARCH_KEY, String.join(", ", researchKeys));
    }

    private static boolean isInfernalFurnace(final IRecipeHandler handler) {
        return handler instanceof FurnaceRecipeHandler furnace && "smelting".equals(furnace.getOverlayIdentifier());
    }

    private void populateInfernalFurnace(final RecipeContext.Builder recipe, final IRecipeHandler handler,
        final int recipeIndex) {
        PropertyProvider.super.populate(recipe, handler, recipeIndex);

        final List<PositionedStack> ingredients = handler.getIngredientStacks(recipeIndex);
        final PositionedStack source = ingredients == null || ingredients.isEmpty() ? null : ingredients.getFirst();

        recipe.input(
            MagicResources.port(
                MagicResources.NETWORK_VIS,
                "thaumcraft:ignis",
                Aspect.FIRE.getName(),
                Aspect.FIRE.getColor(),
                1,
                MagicResources.aspectDisplay(ModItems.itemAspect, Aspect.FIRE.getTag(), 1, 64, 66)));
        recipe.input(
            MagicResources.port(
                MagicResources.ESSENTIA,
                "thaumcraft:ignis",
                Aspect.FIRE.getName(),
                Aspect.FIRE.getColor(),
                1,
                MagicResources.aspectDisplay(ModItems.itemAspect, Aspect.FIRE.getTag(), 1, 84, 66)));

        if (source == null || source.item == null) return;
        final ItemStack bonus = ThaumcraftApi.getSmeltingBonus(source.item);
        if (bonus == null) return;

        final ItemStack bonusDisplay = bonus.copy();
        recipe.property(BONUS_BASE_AMOUNT, Math.max(0, bonusDisplay.stackSize));
        bonusDisplay.stackSize = Math.max(1, bonusDisplay.stackSize);
        recipe.property(
            BONUS_OUTPUT_INDEX,
            recipe.outputs()
                .size());
        recipe.output(Port.itemPort(new PositionedStack(bonusDisplay, 132, 18)));
    }

    private static void populateCrucible(final RecipeContext.Builder recipe, final TemplateRecipeHandler handler,
        final int recipeIndex) {
        final var cached = handler.arecipes.get(recipeIndex);
        final AspectList aspects = cachedAspects(cached);
        if (aspects == null) return;

        final List<PositionedStack> displayed = cached.getIngredients();
        final PositionedStack catalyst = findCatalystDisplay(displayed);
        if (catalyst == null || cached.getResult() == null) return;

        populateItems(recipe, cached);
        addAspectInputs(recipe, aspects, cached, MagicResources.ESSENTIA);
        recipe.property(ESSENTIA_UNITS, sumVis(aspects));
    }

    private static void populateInfusion(final RecipeContext.Builder recipe,
        final TemplateRecipeHandler.CachedRecipe cached) {
        final PositionedStack result = cached.getResult();
        final List<PositionedStack> displayed = realDisplays(cached.getIngredients());
        final AspectList aspects = cachedAspects(cached);
        if (result == null || result.item == null || displayed.isEmpty() || aspects == null) return;

        populateItems(recipe, cached);
        addAspectInputs(recipe, aspects, cached, MagicResources.ESSENTIA);
        final int essentiaUnits = sumVis(aspects);
        recipe.property(ESSENTIA_UNITS, essentiaUnits);
        final int instability = ((InfusionCachedRecipeAccessor) cached).plannh$getInstability();
        if (instability > 0) recipe.property(INSTABILITY, instability);
        recipe.property(NUM_COMPONENTS, Math.max(0, displayed.size() - 1));
    }

    private static void populateItems(final RecipeContext.Builder recipe,
        final TemplateRecipeHandler.CachedRecipe cached) {
        for (final PositionedStack item : realDisplays(cached.getIngredients())) recipe.input(Port.fromStack(item));
        recipe.output(Port.fromStack(cached.getResult()));
    }

    private static void addAspectInputs(final RecipeContext.Builder recipe, final AspectList aspects,
        final TemplateRecipeHandler.CachedRecipe cached, final ResourceProperty<MagicResources.Amount> type) {
        int row = 0;

        for (final Aspect aspect : aspects.getAspectsSortedAmount()) {
            if (aspect == null || type == MagicResources.STORED_VIS && !isPrimal(aspect)) continue;
            recipe.input(
                MagicResources.port(
                    type,
                    "thaumcraft:" + aspect.getTag(),
                    aspect.getName(),
                    aspect.getColor(),
                    aspects.getAmount(aspect),
                    aspectDisplay(cached, aspect, aspects.getAmount(aspect), 8 + row % 6 * 20, 130 + row / 6 * 20)));
            row++;
        }
    }

    @Nullable
    private static AspectList cachedAspects(final Object cached) {
        return cached instanceof CachedThaumRecipeAccessor recipe ? recipe.plannh$getAspects() : null;
    }

    private static List<String> cachedResearchKeys(final Object cached) {
        if (!(cached instanceof CachedThaumRecipeAccessor recipe)) return List.of();
        final List<String> result = new ArrayList<>();
        for (final var entry : recipe.plannh$getPrereqs()) {
            final var research = ((ResearchInfoAccessor) entry).plannh$getResearchItem();
            if (research != null && research.key != null && !research.key.isEmpty()) {
                result.add(research.key);
            }
        }
        return result;
    }

    @Nullable
    private static PositionedStack findCatalystDisplay(final List<PositionedStack> displayed) {
        for (final PositionedStack stack : displayed) {
            if (stack != null && stack.item != null && !isAspectDisplay(stack)) return stack;
        }
        return null;
    }

    private static boolean researchCompleteForPlayer(final String researchKey) {
        return ThaumcraftApiHelper.isResearchComplete(
            Minecraft.getMinecraft()
                .getSession()
                .getUsername(),
            researchKey);
    }

    @Nullable
    private static PositionedStack findAspectDisplay(final @Nullable List<PositionedStack> displayed,
        final Aspect aspect) {
        if (displayed == null) return null;
        for (final PositionedStack stack : displayed) {
            if (isAspectDisplay(stack) && aspect == ItemAspect.getAspect(stack.item)) return stack;
        }
        return null;
    }

    static boolean allResearchComplete(final Iterable<String> keys, final Predicate<String> complete) {
        boolean found = false;
        for (final String key : keys) {
            if (key == null || key.isEmpty()) continue;
            found = true;
            if (!complete.test(key)) return false;
        }
        return found;
    }

    private static int sumVis(final @Nullable AspectList aspects) {
        if (aspects == null) return 0;
        int total = 0;
        for (final Aspect a : aspects.getAspects()) {
            if (a != null) total += aspects.getAmount(a);
        }
        return total;
    }

    private static int primalVis(final @Nullable AspectList aspects) {
        if (aspects == null) return 0;
        int total = 0;
        for (final Aspect aspect : aspects.getAspects()) {
            if (aspect != null && isPrimal(aspect)) total += aspects.getAmount(aspect);
        }
        return total;
    }

    @Nonnull
    private static int[] aspectListToPrimals(final @Nullable AspectList aspects) {
        final int[] result = new int[6];
        if (aspects == null) return result;
        final Aspect[] aspectArray = aspects.getAspects();
        if (aspectArray == null) return result;
        for (final Aspect aspect : aspectArray) {
            if (aspect == null) continue;
            final String tag = aspect.getTag();
            if (tag == null) continue;
            for (int i = 0; i < 6; i++) {
                if (PRIMAL_TAGS[i].equals(tag)) {
                    result[i] = aspects.getAmount(aspect);
                    break;
                }
            }
        }
        return result;
    }

    private static boolean isPrimal(final Aspect aspect) {
        for (final String tag : PRIMAL_TAGS) if (tag.equals(aspect.getTag())) return true;
        return false;
    }

    private static List<PositionedStack> realDisplays(final @Nullable List<PositionedStack> displayed) {
        if (displayed == null || displayed.isEmpty()) return List.of();
        final List<PositionedStack> result = new ArrayList<>();
        for (final PositionedStack stack : displayed) {
            if (stack != null && stack.item != null && !isAspectDisplay(stack)) result.add(stack);
        }
        return result;
    }

    private static boolean isAspectDisplay(final PositionedStack stack) {
        if (stack.item == null) return false;
        if (!(stack.item.getItem() instanceof ItemAspect)
            && stack.item.getItem() != com.gtnewhorizons.aspectrecipeindex.ModItems.itemAspect) return false;
        // An untagged ARI item is a real ingredient, not an aspect-cost display.
        return ItemAspect.getAspect(stack.item) != null;
    }

    private static PositionedStack aspectDisplay(final TemplateRecipeHandler.CachedRecipe cached, final Aspect aspect,
        final int amount, final int x, final int y) {
        final PositionedStack displayed = findAspectDisplay(cached.getIngredients(), aspect);
        if (displayed != null) return normalizedAspectDisplay(displayed, amount);
        final List<PositionedStack> other = cached.getOtherStacks();
        final PositionedStack otherDisplay = findAspectDisplay(other, aspect);
        if (otherDisplay != null) return normalizedAspectDisplay(otherDisplay, amount);
        return MagicResources.aspectDisplay(ModItems.itemAspect, aspect.getTag(), amount, x, y);
    }

    private static PositionedStack normalizedAspectDisplay(final PositionedStack source, final int amount) {
        final ItemStack stack = source.item.copy();
        stack.stackSize = Math.max(1, amount);
        stack.setItemDamage(1);
        return new PositionedStack(stack, source.relx, source.rely, false);
    }

}
