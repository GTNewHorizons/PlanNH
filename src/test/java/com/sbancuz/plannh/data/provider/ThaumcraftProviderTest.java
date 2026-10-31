package com.sbancuz.plannh.data.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.glease.tc4tweak.api.infusionrecipe.EnhancedInfusionRecipe;
import net.glease.tc4tweak.api.infusionrecipe.RecipeIngredient;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagByte;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.gtnewhorizons.aspectrecipeindex.ModItems;
import com.gtnewhorizons.aspectrecipeindex.common.items.ItemAspect;
import com.gtnewhorizons.aspectrecipeindex.nei.InfusionRecipeHandler;
import com.gtnewhorizons.aspectrecipeindex.nei.ResearchInfo;
import com.gtnewhorizons.aspectrecipeindex.nei.TemplateThaumHandler;
import com.gtnewhorizons.aspectrecipeindex.nei.arcaneworkbench.ShapedArcaneRecipeHandler;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineConfig;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.RecipeContext;
import com.sbancuz.plannh.data.flowchart.Port;
import com.sbancuz.plannh.data.serialization.Serializer;
import com.sbancuz.plannh.mixins.PositionedStackAccessor;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import thaumcraft.api.research.ResearchItem;

class ThaumcraftProviderTest {

    @BeforeAll
    static void initializeMinecraftFixture() {
        ProviderTestFixture.bootstrap();
        ModItems.itemAspect = ProviderTestFixture.item("aspect");
    }

    @Test
    void infusionUsesAllCachedRealItemsEvenWhenTheirDisplayOrderChanges() throws Exception {
        final ItemStack central = new ItemStack(ProviderTestFixture.item("infusion_central"));
        ItemAspect.setAspect(central, Aspect.FIRE);
        final ItemStack first = new ItemStack(ProviderTestFixture.item("infusion_first"));
        final ItemStack second = new ItemStack(ProviderTestFixture.item("infusion_second"));
        final AspectList aspects = new AspectList().add(Aspect.AIR, 3);
        final InfusionRecipe source = new EnhancedInfusionRecipe(
            "TC_TEST",
            new ItemStack(ProviderTestFixture.item("infusion_output")),
            2,
            aspects,
            RecipeIngredient.item(false, central),
            List.of(RecipeIngredient.item(false, first), RecipeIngredient.item(false, second))) {};
        final InfusionRecipeHandler handler = nativeHandler(InfusionRecipeHandler.class);
        final TemplateRecipeHandler.CachedRecipe cached = nativeCachedRecipe(
            handler,
            source,
            "com.gtnewhorizons.aspectrecipeindex.nei.InfusionRecipeHandler$InfusionCachedRecipe");
        final List<PositionedStack> real = new ArrayList<>();
        for (final PositionedStack stack : cached.getIngredients()) {
            if (stack.item.getItem() != ModItems.itemAspect) real.add(stack);
        }
        final List<PositionedStack> reordered = new ArrayList<>(List.of(real.get(0), real.get(2), real.get(1)));
        for (final PositionedStack stack : cached.getIngredients()) {
            if (stack.item.getItem() == ModItems.itemAspect) reordered.add(stack);
        }
        cached.getIngredients()
            .clear();
        cached.getIngredients()
            .addAll(reordered);
        installCachedRecipe(handler, cached);

        final RecipeContext.Builder builder = RecipeContext.builder();
        new ThaumcraftProvider().populate(builder, handler, 0);
        final RecipeContext context = buildNativeRecipe(builder);

        assertEquals(
            4,
            context.inputs()
                .size());
        assertTrue(
            context.inputs()
                .stream()
                .anyMatch(port -> port.getType() == RecipePropertyAPI.ITEM));
        assertEquals(3, context.get(ThaumcraftProvider.ESSENTIA_UNITS));
        assertEquals(
            1,
            context.outputs()
                .size());
    }

    @Test
    void dynamicInfusionOutputUsesTheCachedTransformationWithoutAnOutputRegistryLookup() throws Exception {
        final ItemStack central = new ItemStack(ProviderTestFixture.item("dynamic_infusion_central"));
        final InfusionRecipe source = new EnhancedInfusionRecipe(
            "TC_TEST",
            new Object[] { "transformed", new NBTTagByte((byte) 1) },
            2,
            new AspectList().add(Aspect.AIR, 3),
            RecipeIngredient.item(false, central),
            List.of()) {};
        final InfusionRecipeHandler handler = nativeHandler(InfusionRecipeHandler.class);
        final var cached = nativeCachedRecipe(
            handler,
            source,
            "com.gtnewhorizons.aspectrecipeindex.nei.InfusionRecipeHandler$InfusionCachedRecipe");
        installCachedRecipe(handler, cached);
        final RecipeContext.Builder recipe = RecipeContext.builder();
        new ThaumcraftProvider(_ -> true).populate(recipe, handler, 0);
        final RecipeContext context = buildNativeRecipe(recipe);
        assertEquals(
            2,
            context.inputs()
                .size());
        assertEquals(3, context.get(ThaumcraftProvider.ESSENTIA_UNITS));
        assertTrue(
            context.outputs()
                .getFirst()
                .getAllStacks()
                .getFirst().item.getTagCompound()
                    .getBoolean("transformed"));
        assertTrue(new ThaumcraftProvider(_ -> true).canCraft(handler, 0));
    }

    @Test
    void arcaneUsesCachedAspectsAndDoesNotExposeFakeAspectItems() throws Exception {
        final AspectList aspects = new AspectList().add(Aspect.FIRE, 4);
        final ShapedArcaneRecipe source = new ShapedArcaneRecipe(
            "TC_TEST",
            new ItemStack(ProviderTestFixture.item("arcane_output")),
            aspects,
            "x",
            'x',
            new ItemStack(ProviderTestFixture.item("arcane_input")));
        final ShapedArcaneRecipeHandler handler = nativeHandler(ShapedArcaneRecipeHandler.class);
        final TemplateRecipeHandler.CachedRecipe cached = nativeCachedRecipe(
            handler,
            source,
            "com.gtnewhorizons.aspectrecipeindex.nei.arcaneworkbench.ShapedArcaneRecipeHandler$ArcaneShapedCachedRecipe");
        installCachedRecipe(handler, cached);

        final RecipeContext.Builder builder = RecipeContext.builder();
        new ThaumcraftProvider().populate(builder, handler, 0);
        final RecipeContext context = buildNativeRecipe(builder);

        assertEquals(
            2,
            context.inputs()
                .size());
        assertTrue(
            context.inputs()
                .stream()
                .anyMatch(port -> port.getType() == RecipePropertyAPI.ITEM));
        assertTrue(
            context.inputs()
                .stream()
                .anyMatch(port -> port.getType() == MagicResources.STORED_VIS));
        assertEquals(4, context.get(ThaumcraftProvider.STORED_VIS_UNITS));
    }

    @Test
    void cachedResearchChecksAllPrerequisitesAndAllowsResearchFreeCrafting() throws Exception {
        final ShapedArcaneRecipe source = new ShapedArcaneRecipe(
            "",
            new ItemStack(ProviderTestFixture.item("free_arcane_output")),
            new AspectList(),
            "x",
            'x',
            new ItemStack(ProviderTestFixture.item("free_arcane_input")));
        final ShapedArcaneRecipeHandler handler = nativeHandler(ShapedArcaneRecipeHandler.class);
        final var cached = nativeCachedRecipe(
            handler,
            source,
            "com.gtnewhorizons.aspectrecipeindex.nei.arcaneworkbench.ShapedArcaneRecipeHandler$ArcaneShapedCachedRecipe");
        installCachedRecipe(handler, cached);
        final Field prerequisites = Class
            .forName("com.gtnewhorizons.aspectrecipeindex.nei.TemplateThaumHandler$CachedThaumRecipe")
            .getDeclaredField("prereqs");
        prerequisites.setAccessible(true);
        @SuppressWarnings("unchecked")
        final List<ResearchInfo> research = (List<ResearchInfo>) prerequisites.get(cached);
        research.clear();
        assertTrue(new ThaumcraftProvider(_ -> false).canCraft(handler, 0));
        research.add(new ResearchInfo(new ResearchItem("rod", "TEST"), true));
        research.add(new ResearchInfo(new ResearchItem("cap", "TEST"), true));
        assertFalse(new ThaumcraftProvider("rod"::equals).canCraft(handler, 0));
        assertTrue(new ThaumcraftProvider(_ -> true).canCraft(handler, 0));
    }

    @Test
    void boostSupplyAndExplicitPortOverridesSurviveSerialization() throws Exception {
        final Field field = ThaumcraftProvider.class.getDeclaredField("INFERNAL_FURNACE");
        field.setAccessible(true);
        final MachineProfile profile = (MachineProfile) field.get(new ThaumcraftProvider());
        final MachineConfig cfg = new MachineConfig(profile);
        cfg.set(ThaumcraftProvider.BOOST_SOURCE, ThaumcraftProvider.FurnaceBoostSource.ESSENTIA_NOZZLE);
        cfg.inputMultiplier(2, 0d);
        final MachineConfig loaded = Serializer.GSON.fromJson(Serializer.GSON.toJson(cfg), MachineConfig.class);
        loaded.setProfile(profile);
        assertEquals(
            ThaumcraftProvider.FurnaceBoostSource.ESSENTIA_NOZZLE,
            loaded.value(
                ThaumcraftProvider.BOOST_SOURCE,
                RecipeContext.builder()
                    .build()));
        assertEquals(0d, loaded.inputMultiplier(2));
    }

    private static void installCachedRecipe(final TemplateRecipeHandler handler,
        final TemplateRecipeHandler.CachedRecipe cached) throws Exception {
        final Field recipes = TemplateRecipeHandler.class.getDeclaredField("arecipes");
        recipes.setAccessible(true);
        recipes.set(handler, new ArrayList<>(List.of(cached)));
    }

    private static <T extends TemplateThaumHandler> T nativeHandler(final Class<T> type) throws Exception {
        final T handler = ProviderTestFixture.allocate(type);
        handler.arecipes = new ArrayList<>();
        final Field aspects = TemplateThaumHandler.class.getDeclaredField("aspects");
        aspects.setAccessible(true);
        aspects.set(handler, new ArrayList<>());
        return handler;
    }

    private static RecipeContext buildNativeRecipe(final RecipeContext.Builder recipe) {
        // Unit tests do not run the accessor mixin that the live item-port matcher uses.
        for (final Port<?> port : recipe.inputs()) {
            if (port.getType() == RecipePropertyAPI.ITEM) port.getAllStacks()
                .replaceAll(AccessorStack::new);
        }
        return recipe.build();
    }

    private static final class AccessorStack extends PositionedStack implements PositionedStackAccessor {

        private boolean permutated;

        private AccessorStack(final PositionedStack source) {
            super(source.item, source.relx, source.rely, false);
            items = source.items;
            item = source.item;
        }

        @Override
        public boolean getPermutated() {
            return permutated;
        }

        @Override
        public void setPermutated(final boolean value) {
            permutated = value;
        }
    }

    private static TemplateRecipeHandler.CachedRecipe nativeCachedRecipe(final Object handler, final Object source,
        final String className) throws Exception {
        final Class<?> cachedType = Class.forName(className);
        for (final var constructor : cachedType.getDeclaredConstructors()) {
            final Class<?>[] parameters = constructor.getParameterTypes();
            if (parameters.length != 3 || !parameters[0].isInstance(handler)
                || !parameters[1].isInstance(source)
                || parameters[2] != boolean.class) continue;
            constructor.setAccessible(true);
            final TemplateRecipeHandler.CachedRecipe cached = (TemplateRecipeHandler.CachedRecipe) constructor
                .newInstance(handler, source, true);
            final Field prereqs = Class
                .forName("com.gtnewhorizons.aspectrecipeindex.nei.TemplateThaumHandler$CachedThaumRecipe")
                .getDeclaredField("prereqs");
            prereqs.setAccessible(true);
            @SuppressWarnings("unchecked")
            final List<ResearchInfo> research = (List<ResearchInfo>) prereqs.get(cached);
            research.add(new ResearchInfo(new ResearchItem("TC_TEST", "TEST"), true));
            return cached;
        }
        throw new NoSuchMethodException(className);
    }

    private static PositionedStack display() {
        return new PositionedStack(new ItemStack(Items.redstone), 0, 0);
    }

}
