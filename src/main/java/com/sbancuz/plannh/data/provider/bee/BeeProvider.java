package com.sbancuz.plannh.data.provider.bee;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import net.bdew.neiaddons.forestry.bees.BeeProduceHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.gtnewhorizon.gtnhlib.util.data.ItemId;
import com.mojang.authlib.GameProfile;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.ProfileFactory;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.effect.EffectResult;
import com.sbancuz.plannh.data.effect.steps.CoFHCompat;
import com.sbancuz.plannh.data.flowchart.Port;
import com.sbancuz.plannh.data.flowchart.resource.LongItemStack;
import com.sbancuz.plannh.data.properties.PropertyProvider;
import com.sbancuz.plannh.data.properties.RecipeProperty;
import com.sbancuz.plannh.data.setting.Settings;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;
import cpw.mods.fml.common.Loader;
import forestry.api.apiculture.BeeManager;
import forestry.api.apiculture.IAlleleBeeSpecies;
import forestry.api.apiculture.IBee;
import forestry.api.apiculture.IBeeGenome;
import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.IBeeModifier;
import forestry.api.apiculture.IBeekeepingMode;
import forestry.api.apiculture.IHiveFrame;
import forestry.api.genetics.AlleleManager;
import forestry.api.genetics.IAllele;
import forestry.apiculture.AlvearyBeeModifier;
import forestry.apiculture.ApiaryBeeModifier;
import forestry.apiculture.BeehouseBeeModifier;
import forestry.apiculture.blocks.BlockApicultureType;
import forestry.apiculture.genetics.Bee;
import forestry.core.config.Constants;
import forestry.plugins.PluginApiculture;

/** Shared bee extraction and calculations, plus Forestry's ordinary housings. */
public class BeeProvider implements PropertyProvider {

    public static final RecipeProperty<String> SPECIES = RecipeProperty.<String>builder("bee_species", "")
        .build();

    protected final List<MachineProfile> profiles;
    private final Supplier<IBeekeepingMode> modeSupplier;
    private final ProfileFactory<Housing> housingFactory;

    public BeeProvider() {
        this(BeeProvider::currentMode);
    }

    /** The supplier overload keeps headless tests independent of the client singleton. */
    public BeeProvider(final Supplier<IBeekeepingMode> modeSupplier) {
        if (modeSupplier == null) throw new IllegalArgumentException("Bee mode supplier is required");
        this.modeSupplier = modeSupplier;
        housingFactory = createHousingFactory();
        profiles = List.of(
            housingBuilder("forestry:apiary", new Housing(new ApiaryBeeModifier(), 3)).icon(
                (_, _) -> new ItemDrawable(PluginApiculture.blocks.apiculture, BlockApicultureType.APIARY.getMeta()))
                .build(),
            housingBuilder("forestry:beehouse", new Housing(new BeehouseBeeModifier(), 0)).icon(
                (_, _) -> new ItemDrawable(PluginApiculture.blocks.apiculture, BlockApicultureType.BEEHOUSE.getMeta()))
                .build(),
            housingBuilder("forestry:alveary", new Housing(new AlvearyBeeModifier(), 0))
                .icon((_, _) -> new ItemDrawable(PluginApiculture.blocks.alveary))
                .build());
    }

    private ProfileFactory<Housing> createHousingFactory() {
        return MachineProfile.template()
            .provider(this)
            .settings(List.of(Settings.MACHINES, Settings.TICK_MODIFIER))
            .derivedSettings(BeeSettings.ALLELES, BeeSettings.AlleleBinding::setting, binding -> (cfg, ctx) -> {
                final IAllele[] template = template(ctx);
                return template[binding.chromosome()
                    .ordinal()].getUID();
            })
            .icon((_, _) -> GuiTextures.PROCESSOR)
            .asFactory(
                (builder, housing) -> builder
                    .configureIf(housing.frameSlots() > 0, b -> b.setting(BeeSettings.FRAMES.getFirst()))
                    .effect((cfg, ctx) -> calculate(housing, cfg, ctx))
                    .applyParallelism()
                    .totalCost(
                        CoFHCompat.RF_COST,
                        CoFHCompat.RF_PER_T,
                        (result, cfg, ctx) -> (Number) result.properties()
                            .get(CoFHCompat.RF_COST)));
    }

    protected final MachineProfile.Builder housingBuilder(final String id, final Housing housing) {
        return housingFactory.builder(id, housing);
    }

    record Housing(BiFunction<MachineConfig, RecipeContext, IBeeModifier> modifier, int frameSlots,
        BiFunction<MachineConfig, RecipeContext, HousingExtras> extras) {

        Housing {
            if (modifier == null) throw new IllegalArgumentException("Bee housing modifier is required");
            if (extras == null) throw new IllegalArgumentException("Bee housing extras are required");
            if (frameSlots < 0 || frameSlots > BeeSettings.FRAMES.size())
                throw new IllegalArgumentException("Invalid frame slot count: " + frameSlots);
        }

        Housing(final IBeeModifier modifier, final int frameSlots) {
            this((cfg, ctx) -> modifier, frameSlots, (cfg, ctx) -> HousingExtras.EMPTY);
            if (modifier == null) throw new IllegalArgumentException("Bee housing modifier is required");
        }

        Housing(final BiFunction<MachineConfig, RecipeContext, IBeeModifier> modifier, final int frameSlots) {
            this(modifier, frameSlots, (cfg, ctx) -> HousingExtras.EMPTY);
        }

        Housing(final IBeeModifier modifier, final int frameSlots,
            final BiFunction<MachineConfig, RecipeContext, HousingExtras> extras) {
            this((cfg, ctx) -> modifier, frameSlots, extras);
            if (modifier == null) throw new IllegalArgumentException("Bee housing modifier is required");
        }
    }

    record HousingExtras(List<IBeeModifier> modifiers, double rfPerWorkCycle, double rfPerOperation) {

        private static final HousingExtras EMPTY = new HousingExtras(List.of(), 0, 0);

        HousingExtras(final List<IBeeModifier> modifiers, final double rfPerWorkCycle) {
            this(modifiers, rfPerWorkCycle, 0);
        }

        HousingExtras {
            if (modifiers == null) throw new IllegalArgumentException("Bee housing modifiers are required");
            if (!Double.isFinite(rfPerWorkCycle) || rfPerWorkCycle < 0)
                throw new IllegalArgumentException("Invalid bee housing energy");
            if (!Double.isFinite(rfPerOperation) || rfPerOperation < 0)
                throw new IllegalArgumentException("Invalid bee operation energy");
        }
    }

    @Override
    public void register() {
        if (Loader.isModLoaded("NEIAddons") && Loader.isModLoaded("Forestry"))
            RecipePropertyAPI.registerExtractor(BeeProduceHandler.class, this);
    }

    @Override
    public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
        return canCraft(handler, recipeIndex) ? profiles : List.of();
    }

    @Override
    public boolean canCraft(final IRecipeHandler handler, final int recipeIndex) {
        return handler.getClass()
            .getName()
            .equals("net.bdew.neiaddons.forestry.bees.BeeProduceHandler");
    }

    @Override
    public void populate(final RecipeContext.Builder recipe, final IRecipeHandler handler, final int recipeIndex) {
        if (!canCraft(handler, recipeIndex))
            throw new IllegalArgumentException("Bee production handler is unavailable");

        String speciesUid = null;
        for (final PositionedStack ingredient : handler.getIngredientStacks(recipeIndex)) {
            if (ingredient == null || ingredient.item == null || !BeeManager.beeRoot.isMember(ingredient.item))
                continue;

            final IBee bee = BeeManager.beeRoot.getMember(ingredient.item);
            if (bee == null || bee.getGenome() == null) continue;
            final IAlleleBeeSpecies species = bee.getGenome()
                .getPrimary();
            if (species != null) {
                speciesUid = species.getUID();
                break;
            }
        }
        if (speciesUid == null)
            throw new IllegalArgumentException("Bee production recipe has no species-bearing bee ingredient");

        for (final PositionedStack ingredient : handler.getIngredientStacks(recipeIndex)) {
            if (ingredient == null || ingredient.item == null || !BeeManager.beeRoot.isMember(ingredient.item))
                continue;

            final Port<LongItemStack> bee = Port.itemPort(ingredient);
            // The displayed queen/princess identifies the selected breed but is not consumed per work cycle.
            bee.getValue().amount = 0;
            recipe.input(bee);
        }

        final List<PositionedStack> displayedOutputs = new ArrayList<>();
        displayedOutputs.add(handler.getResultStack(recipeIndex));
        displayedOutputs.addAll(handler.getOtherStacks(recipeIndex));
        for (final PositionedStack stack : displayedOutputs) {
            if (stack == null || stack.item == null || BeeManager.beeRoot.isMember(stack.item)) continue;

            boolean duplicate = false;
            for (final Port<?> output : recipe.outputs()) {
                final ItemStack item = output.getAllStacks()
                    .getFirst().item;
                if (item.isItemEqual(stack.item) && ItemStack.areItemStackTagsEqual(item, stack.item)) {
                    duplicate = true;
                    break;
                }
            }
            if (duplicate) continue;

            final PositionedStack product = stack.copy();
            product.item.stackSize = 1;
            recipe.output(Port.itemPort(product));
        }
        recipe.property(SPECIES, speciesUid);
    }

    protected final void populateDefault(final RecipeContext.Builder recipe, final IRecipeHandler handler,
        final int recipeIndex) {
        PropertyProvider.super.populate(recipe, handler, recipeIndex);
    }

    private static IAllele[] template(final RecipeContext ctx) {
        final String species = ctx.get(SPECIES);
        final IAllele[] template = BeeManager.beeRoot.getTemplate(species);
        if (template == null) throw new IllegalArgumentException("Missing bee template: " + species);
        return template;
    }

    static IBeekeepingMode currentMode() {
        final Minecraft client = Minecraft.getMinecraft();
        return defaultMode(client.theWorld, client.thePlayer == null ? null : client.thePlayer.getGameProfile());
    }

    static IBeekeepingMode defaultMode(final World world, final GameProfile player) {
        if (world != null && !world.isRemote) return BeeManager.beeRoot.getBeekeepingMode(world);

        // Forestry synchronizes the player's tracker, not the client's common world tracker.
        if (world != null && player != null) {
            final String synced = BeeManager.beeRoot.getBreedingTracker(world, player)
                .getModeName();
            if (synced != null && !synced.isEmpty()) return BeeManager.beeRoot.getBeekeepingMode(synced);
        }

        // This is Forestry's offline/configured fallback, not a selectable PlanNH setting.
        return BeeManager.beeRoot.getBeekeepingMode(PluginApiculture.beekeepingMode);
    }

    static IBeeModifier combinedModifier(final List<? extends IBeeModifier> modifiers) {
        final List<IBeeModifier> snapshot = List.copyOf(modifiers);
        // Forestry's combiner only needs the modifier list, not a live housing or world.
        final IBeeHousing modifierHousing = (IBeeHousing) Proxy.newProxyInstance(
            IBeeHousing.class.getClassLoader(),
            new Class<?>[] { IBeeHousing.class },
            (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return switch (method.getName()) {
                        case "equals" -> proxy == (args == null ? null : args[0]);
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "BeeModifiers housing proxy";
                        default -> throw new UnsupportedOperationException(method.toString());
                    };
                }
                if ("getBeeModifiers".equals(method.getName())) return snapshot;
                throw new UnsupportedOperationException("No world or inventory in a bee modifier combiner");
            });
        return BeeManager.beeRoot.createBeeHousingModifier(modifierHousing);
    }

    private EffectResult calculate(final Housing description, final MachineConfig cfg, final RecipeContext ctx) {
        // Preserve frame and board order: capped bonuses depend on the preceding modifiers.
        final List<IBeeModifier> modifiers = new ArrayList<>();
        modifiers.add(
            description.modifier()
                .apply(cfg, ctx));

        for (int i = 0; i < description.frameSlots(); i++) {
            final ItemStack stack = cfg.value(BeeSettings.FRAMES.get(i), ctx);
            if (stack == null) continue;
            if (!(stack.getItem() instanceof IHiveFrame frame))
                throw new IllegalArgumentException("Selected item is not a bee frame");
            modifiers.add(frame.getBeeModifier());
        }

        final HousingExtras extras = description.extras()
            .apply(cfg, ctx);
        modifiers.addAll(extras.modifiers());

        final IAllele[] alleles = template(ctx).clone();
        for (final BeeSettings.AlleleBinding binding : BeeSettings.ALLELES) {
            final String uid = cfg.value(binding.setting(), ctx);
            final IAllele allele = AlleleManager.alleleRegistry.getAllele(uid);
            final var chromosome = binding.chromosome();
            if (allele == null || !AlleleManager.alleleRegistry.getRegisteredAlleles(chromosome)
                .contains(allele)) throw new IllegalArgumentException("Invalid " + chromosome + " allele: " + uid);
            alleles[chromosome.ordinal()] = allele;
        }

        final IBeeGenome genome = BeeManager.beeRoot.templateAsGenome(alleles, alleles);
        final IBeeModifier housing = combinedModifier(modifiers);
        final IBeekeepingMode selectedMode = modeSupplier.get();
        if (selectedMode == null) throw new IllegalStateException("Forestry beekeeping mode is unavailable");
        final IBeeModifier mode = selectedMode.getBeeModifier();

        // Forestry adds production bonuses, but multiplies lifespan bonuses.
        float production = housing.getProductionModifier(genome, 0);
        production += mode.getProductionModifier(genome, production);
        float lifespan = housing.getLifespanModifier(genome, genome, 1);
        lifespan *= mode.getLifespanModifier(genome, genome, lifespan);
        final double cycles = expectedWorkCycles(genome.getLifespan(), lifespan);

        // One operation covers the expected queen lifetime and nominal rebreeding time.
        final int handlerTicks = ctx.get(RecipePropertyAPI.DURATION_TICKS);
        final int cycleTicks = handlerTicks > 0 ? handlerTicks : PluginApiculture.ticksPerBeeWorkCycle;
        final double ticks = cycles * cycleTicks + Constants.APIARY_BREEDING_TIME;
        if (!Double.isFinite(ticks) || ticks < 1 || ticks > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Unrepresentable bee lifetime duration: " + ticks);

        final EffectResult result = new EffectResult((int) Math.round(ticks), 0, 1);
        result.properties()
            .put(CoFHCompat.RF_COST, extras.rfPerWorkCycle() * cycles + extras.rfPerOperation());

        final IAlleleBeeSpecies primary = genome.getPrimary();
        final IAlleleBeeSpecies secondary = genome.getSecondary();
        final float speed = genome.getSpeed();
        final Map<ItemId, Double> products = new HashMap<>();
        addExpectedProducts(products, primary.getProductChances(), 1, speed, production);
        addExpectedProducts(products, secondary.getProductChances(), 0.5f, speed, production);
        addExpectedProducts(products, primary.getSpecialtyChances(), 1, speed, production);

        final List<Port<?>> outputs = ctx.outputs();
        for (int i = 0; i < outputs.size(); i++) {
            final Port<?> port = outputs.get(i);
            final double baseAmount = Math.max(0d, port.getAmount()) * port.getChance();
            double amount = 0;
            if (port.getValue() instanceof LongItemStack item && baseAmount > 0) {
                final ItemStack stack = item.stack;
                final ItemId key = ItemId.create(stack.getItem(), stack.getItemDamage(), stack.getTagCompound());
                amount = products.getOrDefault(key, 0d) * cycles / baseAmount;
            }
            result.outputMultiplier(i, amount);
        }

        return result;
    }

    static void addExpectedProducts(final Map<ItemId, Double> amounts, final Map<ItemStack, Float> products,
        final float chanceFactor, final float speed, final float production) {
        for (final Map.Entry<ItemStack, Float> entry : products.entrySet()) {
            final ItemStack stack = entry.getKey();
            final Float baseChance = entry.getValue();
            if (stack == null || stack.getItem() == null || stack.stackSize <= 0) continue;
            if (baseChance == null || !Float.isFinite(baseChance) || baseChance < 0) continue;

            final float chance = Bee.getFinalChance(baseChance * chanceFactor, speed, production, 1);
            final double amount = stack.stackSize * Math.clamp((double) chance, 0, 1);
            final ItemId key = ItemId.create(stack.getItem(), stack.getItemDamage(), stack.getTagCompound());
            amounts.merge(key, amount, Double::sum);
        }
    }

    /** Expected stopping time of Forestry's fractional aging, rather than sampled lifecycles. */
    static double expectedWorkCycles(final int health, final float lifespanModifier) {
        if (health <= 0 || !Float.isFinite(lifespanModifier))
            throw new IllegalArgumentException("Invalid bee lifespan");
        if (lifespanModifier < 0.001f) return 1;

        final float aging = 1 / lifespanModifier;
        final int whole = (int) Math.floor(aging);
        final double fraction = aging - whole;
        if (whole == 0) return health / fraction;

        final double[] expected = new double[health + 1];
        for (int currentHealth = 1; currentHealth <= health; currentHealth++) {
            expected[currentHealth] = 1 + (1 - fraction) * expected[Math.max(0, currentHealth - whole)]
                + fraction * expected[Math.max(0, currentHealth - whole - 1)];
        }
        return expected[health];
    }
}
