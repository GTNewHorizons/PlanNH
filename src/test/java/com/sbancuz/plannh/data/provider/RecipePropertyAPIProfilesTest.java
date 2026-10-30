package com.sbancuz.plannh.data.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.BiFunction;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.sbancuz.plannh.api.RecipePropertyAPI;
import com.sbancuz.plannh.data.MachineProfile;
import com.sbancuz.plannh.data.properties.PropertyProvider;

import codechicken.nei.ItemStackMap;
import codechicken.nei.api.ItemFilter;
import codechicken.nei.api.ItemInfo;
import codechicken.nei.recipe.FurnaceRecipeHandler;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.util.ItemStackFilterParser;
import sun.misc.Unsafe;

class RecipePropertyAPIProfilesTest {

    private static final Unsafe UNSAFE = unsafe();

    @BeforeAll
    static void initializeMinecraftFixture() {
        ProviderTestFixture.bootstrap();
    }

    @BeforeEach
    void resetRegistryBeforeTest() {
        RecipePropertyAPI.reset();
    }

    @AfterEach
    void resetRegistryAfterTest() {
        RecipePropertyAPI.reset();
    }

    @Test
    void gathersDistinctProvidersRegisteredOnHandlerSuperclasses() {
        final TestProvider parent = new TestProvider("parent");
        final TestProvider child = new TestProvider("child");
        RecipePropertyAPI.registerExtractor(FurnaceRecipeHandler.class, parent);
        RecipePropertyAPI.registerExtractor(FurnaceRecipeHandler.class, parent);
        RecipePropertyAPI.registerExtractor(ChildFurnaceHandler.class, child);

        assertEquals(
            List.of("child", "parent"),
            RecipePropertyAPI.getProfiles(allocate(ChildFurnaceHandler.class), 0)
                .stream()
                .map(MachineProfile::id)
                .toList());
    }

    @Test
    void canCraftFailuresDoNotHideOtherProviders() {
        RecipePropertyAPI
            .registerExtractor(FurnaceRecipeHandler.class, new TestProvider("runtime", Failure.CAN_CRAFT_RUNTIME));
        RecipePropertyAPI
            .registerExtractor(FurnaceRecipeHandler.class, new TestProvider("linkage", Failure.CAN_CRAFT_LINKAGE));
        RecipePropertyAPI.registerExtractor(FurnaceRecipeHandler.class, new TestProvider("survivor"));

        assertEquals(
            List.of("survivor"),
            RecipePropertyAPI.getProfiles(allocate(FurnaceRecipeHandler.class), 0)
                .stream()
                .map(MachineProfile::id)
                .toList());
    }

    @Test
    void profileFailuresDoNotHideOtherProviders() {
        RecipePropertyAPI
            .registerExtractor(FurnaceRecipeHandler.class, new TestProvider("runtime", Failure.PROFILES_RUNTIME));
        RecipePropertyAPI
            .registerExtractor(FurnaceRecipeHandler.class, new TestProvider("linkage", Failure.PROFILES_LINKAGE));
        RecipePropertyAPI.registerExtractor(FurnaceRecipeHandler.class, new TestProvider("survivor"));

        assertEquals(
            List.of("survivor"),
            RecipePropertyAPI.getProfiles(allocate(FurnaceRecipeHandler.class), 0)
                .stream()
                .map(MachineProfile::id)
                .toList());
    }

    @Test
    void directMetadataHiddenProfilesDoNotHideSiblings() {
        final Item hiddenItem = ProviderTestFixture.item("recipe_api_direct_hidden");
        final ItemStack hiddenStack = new ItemStack(hiddenItem, 1, 4);
        final ItemStack visibleStack = new ItemStack(ProviderTestFixture.item("recipe_api_direct_visible"));
        final boolean added = !ItemInfo.hiddenItems.contains(hiddenStack);
        if (added) ItemInfo.hiddenItems.add(hiddenStack);

        try {
            RecipePropertyAPI.registerExtractor(
                FurnaceRecipeHandler.class,
                new TestProvider(
                    itemProfile("direct-hidden", hiddenStack),
                    itemProfile("same-provider-visible", visibleStack)));
            RecipePropertyAPI.registerExtractor(
                FurnaceRecipeHandler.class,
                new TestProvider(itemProfile("other-provider-visible", visibleStack)));

            assertTrue(ItemInfo.isHidden(hiddenStack));
            assertEquals(
                List.of("same-provider-visible", "other-provider-visible"),
                RecipePropertyAPI.getProfiles(allocate(FurnaceRecipeHandler.class), 0)
                    .stream()
                    .map(MachineProfile::id)
                    .toList());
        } finally {
            if (added) ItemInfo.hiddenItems.remove(hiddenStack);
        }
    }

    @Test
    void wildcardMetadataHidesMatchingItemStacks() {
        final Item hiddenItem = ProviderTestFixture.item("recipe_api_wildcard_hidden");
        final ItemStack hiddenStack = new ItemStack(hiddenItem, 1, 9);
        final ItemStack wildcard = ItemStackMap.wildcard(hiddenItem);
        final boolean added = !ItemInfo.hiddenItems.contains(wildcard);
        if (added) ItemInfo.hiddenItems.add(wildcard);

        try {
            final ItemStack visibleStack = new ItemStack(ProviderTestFixture.item("recipe_api_wildcard_visible"));
            RecipePropertyAPI.registerExtractor(
                FurnaceRecipeHandler.class,
                new TestProvider(
                    itemProfile("wildcard-hidden", hiddenStack),
                    itemProfile("wildcard-visible", visibleStack)));

            assertTrue(ItemInfo.isHidden(hiddenStack));
            assertEquals(
                List.of("wildcard-visible"),
                RecipePropertyAPI.getProfiles(allocate(FurnaceRecipeHandler.class), 0)
                    .stream()
                    .map(MachineProfile::id)
                    .toList());
        } finally {
            if (added) ItemInfo.hiddenItems.remove(wildcard);
        }
    }

    @Test
    void nbtHiddenRulesHideMatchingItemStacks() {
        final Item item = ProviderTestFixture.item("recipe_api_rule_hidden");
        final ItemStack hiddenStack = new ItemStack(item);
        final NBTTagCompound hiddenTag = new NBTTagCompound();
        hiddenTag.setString("plannh_visibility", "hidden");
        hiddenStack.setTagCompound(hiddenTag);
        final ItemStack visibleStack = new ItemStack(item);
        final NBTTagCompound visibleTag = new NBTTagCompound();
        visibleTag.setString("plannh_visibility", "visible");
        visibleStack.setTagCompound(visibleTag);

        final ItemFilter hiddenRule = ItemStackFilterParser.parse("tag.plannh_visibility=hidden");
        assertNotNull(hiddenRule);
        ItemInfo.hiddenItemsRules.filters.add(hiddenRule);

        try {
            RecipePropertyAPI.registerExtractor(
                FurnaceRecipeHandler.class,
                new TestProvider(itemProfile("rule-hidden", hiddenStack), itemProfile("rule-visible", visibleStack)));

            assertTrue(ItemInfo.isHidden(hiddenStack));
            assertEquals(
                List.of("rule-visible"),
                RecipePropertyAPI.getProfiles(allocate(FurnaceRecipeHandler.class), 0)
                    .stream()
                    .map(MachineProfile::id)
                    .toList());
        } finally {
            ItemInfo.hiddenItemsRules.filters.remove(hiddenRule);
        }
    }

    @Test
    void missingAndThrowingIconsRemainAvailable() {
        RecipePropertyAPI.registerExtractor(
            FurnaceRecipeHandler.class,
            new TestProvider(
                new ProfileSpec("missing-icon", null),
                new ProfileSpec("null-icon", (_, _) -> null),
                new ProfileSpec("throwing-icon", (_, _) -> { throw new IllegalStateException("broken icon"); })));
        RecipePropertyAPI.registerExtractor(FurnaceRecipeHandler.class, new TestProvider("other-provider"));

        assertEquals(
            List.of("missing-icon", "null-icon", "throwing-icon", "other-provider"),
            RecipePropertyAPI.getProfiles(allocate(FurnaceRecipeHandler.class), 0)
                .stream()
                .map(MachineProfile::id)
                .toList());
    }

    @SuppressWarnings("unchecked")
    private static <T> T allocate(final Class<T> type) {
        try {
            return (T) UNSAFE.allocateInstance(type);
        } catch (final InstantiationException failure) {
            throw new AssertionError("Could not allocate native handler fixture", failure);
        }
    }

    private static Unsafe unsafe() {
        try {
            final Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (final ReflectiveOperationException failure) {
            throw new AssertionError("Could not access the test fixture runtime", failure);
        }
    }

    private enum Failure {
        NONE,
        CAN_CRAFT_RUNTIME,
        CAN_CRAFT_LINKAGE,
        PROFILES_RUNTIME,
        PROFILES_LINKAGE
    }

    private record ProfileSpec(String id, BiFunction<IRecipeHandler, Integer, IDrawable> icon) {}

    private static ProfileSpec itemProfile(final String id, final ItemStack icon) {
        return new ProfileSpec(id, (_, _) -> new ItemDrawable(icon));
    }

    private static final class TestProvider implements PropertyProvider {

        private final Failure failure;
        private final List<MachineProfile> profiles;

        private TestProvider(final String id) {
            this(Failure.NONE, List.of(new ProfileSpec(id, null)));
        }

        private TestProvider(final String id, final Failure failure) {
            this(failure, List.of(new ProfileSpec(id, null)));
        }

        private TestProvider(final ProfileSpec... profiles) {
            this(Failure.NONE, List.of(profiles));
        }

        private TestProvider(final Failure failure, final List<ProfileSpec> specifications) {
            this.failure = failure;
            this.profiles = specifications.stream()
                .map(specification -> {
                    final MachineProfile.Builder builder = MachineProfile.builder(specification.id())
                        .provider(this);
                    if (specification.icon() != null) builder.icon(specification.icon());
                    return builder.build();
                })
                .toList();
        }

        @Override
        public void register() {}

        @Override
        public boolean canCraft(final IRecipeHandler handler, final int recipeIndex) {
            switch (failure) {
                case CAN_CRAFT_RUNTIME -> throw new IllegalStateException("broken canCraft");
                case CAN_CRAFT_LINKAGE -> throw new LinkageError("broken canCraft linkage");
                default -> {
                    return true;
                }
            }
        }

        @Override
        public List<MachineProfile> getProfiles(final IRecipeHandler handler, final int recipeIndex) {
            switch (failure) {
                case PROFILES_RUNTIME -> throw new IllegalStateException("broken getProfiles");
                case PROFILES_LINKAGE -> throw new LinkageError("broken getProfiles linkage");
                default -> {
                    return profiles;
                }
            }
        }
    }

    private static final class ChildFurnaceHandler extends FurnaceRecipeHandler {
    }
}
